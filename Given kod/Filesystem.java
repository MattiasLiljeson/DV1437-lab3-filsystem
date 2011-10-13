
import java.io.Serializable;
import java.util.Map;
import java.util.Stack;

class FileSystem implements Serializable {
    //
    // Static variables and methods
    //
    
    public final static int NUM_BLOCKS = 250;
    public final static int BLOCK_SIZE = 512;
    
    // Not ours. Stolen from: http://snippets.dzone.com/posts/show/93
    public static final byte[] intToByteArray(int value) {
        return new byte[] {
            (byte)(value >>> 24),
            (byte)(value >>> 16),
            (byte)(value >>> 8),
            (byte)value};
    }
    public static final byte[] intToByteArray(int value, byte[] dst, int start) {
        byte[] tmp = intToByteArray(value); 
        System.arraycopy(tmp, 0, dst, start, 4);
        return dst;
    }
    
    public static final int byteArrayToInt(byte [] b) {
        return (b[0] << 24)
            + ((b[1] & 0xFF) << 16)
            + ((b[2] & 0xFF) << 8)
            + (b[3] & 0xFF);
    }
    public static final int byteArrayToInt(byte[] byteArray, int start) {
        byte[] tmp = new byte[4];
        System.arraycopy(byteArray, start, tmp, 0, 4);
        return byteArrayToInt(tmp);
    }
    
    //
    // Instance  variables and methods
    //
    
    boolean[] freeBlocks = new boolean[NUM_BLOCKS];
    byte[][] blockArray = new byte[NUM_BLOCKS][BLOCK_SIZE];
    Stack<Integer> workDirPathIds;
    Stack<String> workDirPathNames;
    FolderBlock workDir;

    public FileSystem() {
        workDirPathIds = new Stack<Integer>();
        workDirPathNames = new Stack<String>();
        
        blockArray = new byte[NUM_BLOCKS][BLOCK_SIZE];
        freeBlocks = new boolean[NUM_BLOCKS];
        for(int i=0; i<NUM_BLOCKS; i++){
            freeBlocks[i] = true;
        }
        
        // Set up the root folder and its inode
        Inode inode = new Inode();
        blockArray[0] = inode.save();
        FolderBlock folderBlock = new FolderBlock();
        writeFile(0,FolderBlock.save(folderBlock));
        
        workDir = folderBlock;
        workDirPathIds.push(0);
        workDirPathNames.push("root");
    }
    
    
    public int touchFile(String fileName, boolean asFolder){
        // Like JESUS!
        int result = -1;
        if(isFileInFolder(fileName) == false){
            int inodeBlock; // = getFreeBlock();
            int dataBlock; // = getFreeBlock();
            

            /* NOTE: When writing data larger than one block, 
             * since "freeBlock = false" is done by "writeFile", is it possible that
             * writeFile uses one of the above "preoccupied" blocks? 
            */

            Inode inode = new Inode();
            inode.setDataPtr(dataBlock);
            inode.setSize(0);

            if(asFolder){
                inode.setType((byte)1);

                FolderBlock folderBlock = new FolderBlock();
                writeFile(inodeBlock, )
                byte[] data;
                // Write folder data to datablock
                writeFile(inodeBlock, data);
            }
            else{
                inode.setType((byte)0);
            }

            // Fetch workdir inode and fetch dataptr.
            int dataptr = workDirPathIds.peek();

            // Use readFile(dataPtr) and create a byte array.
            readFile(dataptr);

            // create a folderBlock object with the above given bytearray
            // Add file/folder to the folderBlock.
            // Save the folderBlock object as bytearray
            // replace data with writeFile()
            
            
            workDir.addFile(inodeBlock, fileName);
            writeFile(workDirPathIds.peek(), FolderBlock.save(workDir));
        }
        return result;
    }
    
    public int writeFile(int fileId, byte[] data) {
        int result = 0;
        if (!(NUM_BLOCKS > fileId && fileId > 2)) {
            // Block out-of-range
            result = -1;
        }
        else{
            if(fileId == -1){
                fileId = getFreeBlock();
                result = fileId;
            }
            
            int writtenBytes = 0;

            Inode inode = new Inode(blockArray[fileId]);           
            inode.setSize(data.length);
            //System.arraycopy(inode.save(), 0, blockArray[fileId], 0, BLOCK_SIZE);
            blockArray[fileId] = inode.save();
            int blockId = inode.getDataPtr();
            
            boolean done = false;
            while(!done){
                for (int i = 0; i < BLOCK_SIZE-4; i++) {
                    blockArray[blockId][i] = data[i+writtenBytes];
                }
                writtenBytes += BLOCK_SIZE;
                freeBlocks[blockId] = false;
                
                int nextBlockId = byteArrayToInt(blockArray[blockId], BLOCK_SIZE-1-4);
                if(writtenBytes < data.length){
                    if(nextBlockId == -1){
                        blockId = getFreeBlock();
                        intToByteArray(nextBlockId, blockArray[blockId], BLOCK_SIZE-1-4);
                    }
                    else{
                        blockId = nextBlockId;
                    }
                }
                else{
                    done = true;
                    if(nextBlockId != -1)
                        releaseBlock(nextBlockId);
                }
            }
        }
        return result;
    }
        
    public byte[] readFile(int fileId) {
        byte[] data;
        if (!(NUM_BLOCKS > fileId && fileId > 0)) {
            // Block out-of-range
            data = null;
        }
        else{
            int readBytes = 0;

            Inode inode = new Inode(blockArray[fileId]);           
            int blockId = inode.getDataPtr();
            data = new byte[inode.getSize()];

            boolean done = false;
            while(!done){
                System.arraycopy(blockArray[blockId], 0, data, readBytes, BLOCK_SIZE-1-4);
                readBytes += BLOCK_SIZE;

                if(readBytes < data.length)
                    blockId = byteArrayToInt(blockArray[blockId], BLOCK_SIZE-1-4);
                else
                    done = true;          
            }
        }
        return data;
    }
    
    public void releaseBlock(int blockId){
        int nextBlockId = byteArrayToInt(blockArray[blockId],BLOCK_SIZE-1-4);
        if(nextBlockId != -1)
            releaseBlock(nextBlockId);
        
        // Set next block id to -1 
        intToByteArray(-1, blockArray[blockId], BLOCK_SIZE-1-4);
        freeBlocks[blockId] = true;
    }

    private int getFreeBlock() {
        int i=0;
        int freeBlock = 0;
        while(i<NUM_BLOCKS && freeBlock == 0){
            if(freeBlocks[i] == true)
                freeBlock = i;
            i++;
        }
        
        // Return as -1 if no block is available
        if(freeBlock == 0)
            freeBlock = -1;
        return freeBlock;
        // Setting the block as used is done by writeBlock()
    }
    
    public boolean isFileInFolder(String fileName){
        boolean result = false;
        Map map = workDir.getFileListing();
        if(map.get(fileName) != null)
            result = true;        
        return result;
    }   
}
