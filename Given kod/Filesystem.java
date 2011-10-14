
import java.io.Serializable;
import java.util.Map;
import java.util.Stack;
import java.util.Set;

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
    FolderBlock workDir;
    int workDirId;
    

    public FileSystem() { 
    }
    
    // Set workDir, returns if succeded or not
    public boolean setWorkDir(String[] path){
        boolean result = false;
        int id = getFolderId(path);
        if(id != -1){
            workDir = FolderBlock.load(readFile(id));
            workDirId = getFolderId(path);
            result = true;  
        }   
        
        return result;
    }
    
    public void format(){
        // clean the block array
        blockArray = new byte[NUM_BLOCKS][BLOCK_SIZE];
        freeBlocks = new boolean[NUM_BLOCKS];
        for(int i=0; i<NUM_BLOCKS; i++){
            freeBlocks[i] = true;
            releaseBlock(i);
        }
        
        // Set up the root folder and its inode
        Inode inode = new Inode(true);
        blockArray[0] = inode.save();
        FolderBlock folderBlock = new FolderBlock();
        writeFile(0,FolderBlock.save(folderBlock));
        
        workDir = folderBlock;
        workDirId = 0;
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
    
    // Returns the blockId of the last folder from a given path
    public int getFolderId(String[] path){
         
        // Init
        boolean validPath = true;
        int i = 0;
        int folderId = 0; // Root folder
        
        // For each "path", or until "invalid path" is detected
        while(i<path.length && validPath == true){
            // Fetch correct folder
            FolderBlock folder = FolderBlock.load(readFile(folderId));
            
            // Check if path corresponds with one of the filenames in the folder
            if(isFileInFolder(path[i], folder))
            {
                // Check if file is a folder
                int inodId = folder.getFileListing().get(path[i]);
                if(isAFolder(inodId)){
                    // Splendid! We found a folder. Now we just have to make
                    // sure all of the remaining "path" is folders aswell
                }else {
                    validPath = false;
                }
            }else {
                validPath = false;
            }
        }

        // If invalid path, return -1
        if(validPath == false)
        {
            folderId = -1;
        }
        return folderId;
    }
    
    public boolean isFileInFolder(String fileName, FolderBlock folder){
        boolean result = false;
        Map map = folder.getFileListing();
        if(map.get(fileName) != null)
            result = true;        
        return result;
    }
    
    public boolean isFileInFolder(String fileName){
        return isFileInFolder(fileName, workDir);
    }
    
    public boolean isAFolder(int fileId) {
        boolean result = false;
        
        // If withing block range
        if (NUM_BLOCKS > fileId && fileId > 0) {
            
            // Check if Inode is a block
            Inode inode = new Inode(blockArray[fileId]);           
            if(inode.getType() == 1)
            {
                result = true;
            }
        }
        
        //Return
        return result;
    }
    
    public byte[] readFile(int fileId) {
        byte[] data = null;
        if (NUM_BLOCKS > fileId && fileId >= 0) {
            int readBytes = 0;

            Inode inode = new Inode(blockArray[fileId]);           
            int blockId = inode.getDataPtr();
            data = new byte[inode.getSize()];

            boolean done = false;
            while(!done){
                int numOfBytesToRead = BLOCK_SIZE-1-4;
                if(numOfBytesToRead >= data.length-readBytes)
                    numOfBytesToRead = data.length-readBytes;
                System.arraycopy(blockArray[blockId], 0, data, readBytes, numOfBytesToRead);
                readBytes += numOfBytesToRead;

                if(readBytes < data.length)
                    blockId = byteArrayToInt(blockArray[blockId], BLOCK_SIZE-1-4);
                else
                    done = true;          
            }
        }
        return data;
    }
    
    public void releaseBlock(int blockId){
        boolean freeNextBlock = false;
        int nextBlockId = byteArrayToInt(blockArray[blockId], BLOCK_SIZE-1-4);
        if(nextBlockId != -1)
            freeNextBlock = true;
        
        // Set next block id to -1 
        intToByteArray(-1, blockArray[blockId], BLOCK_SIZE-1-4);
        freeBlocks[blockId] = true;
        
        //Release next block if any
        if(freeNextBlock)
            releaseBlock(nextBlockId);
    }
    
    public int touchFile(String fileName, boolean asFolder){
        int result = -1;
        if(isFileInFolder(fileName) == false){
            int inodeBlock = getFreeBlock();
            Inode inode = new Inode(asFolder);
            inode.setSize(0);
            
            // Save to the block array so writeFile() can find it
            blockArray[inodeBlock] = inode.save();

            if(asFolder){
                FolderBlock folderBlock = new FolderBlock();
                writeFile(inodeBlock, FolderBlock.save(folderBlock));
            }
            
            // Update Folder with the new file and save to disk.
            workDir.addFile(inodeBlock, fileName);
            writeFile(workDirId, FolderBlock.save(workDir));
        }
        return result;
    }
    
    public int writeFile(int fileId, byte[] data) {
        int result = 0;
        if (NUM_BLOCKS > fileId && fileId >= 0) {
//            if(fileId == -1){
//                fileId = getFreeBlock();
//                Inode inode = new Inode();
//                blockArray[fileId] = inode.save();
//                result = fileId;
//            }
            
            int writtenBytes = 0;
            int writtenBlocks = 0;

            Inode inode = new Inode(blockArray[fileId]);           
            inode.setSize(data.length);
            //System.arraycopy(inode.save(), 0, blockArray[fileId], 0, BLOCK_SIZE);
            int blockId = inode.getDataPtr();
            if(blockId == -1){
                blockId = getFreeBlock();
                inode.setDataPtr(blockId);
                blockArray[0] = inode.save();
            }
            blockArray[fileId] = inode.save();
                
            
            boolean done = false;
            while(!done){
                int i = 0;
                while(i < BLOCK_SIZE-4 && writtenBytes < data.length ) {
                    blockArray[blockId][i] = data[i+BLOCK_SIZE*writtenBlocks];
                    writtenBytes++;
                    i++;
                }
                writtenBlocks++;
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
        }else{
            // Block out-of-range
            result = -1;
        }
        return result;
    }

}

/* Kodkyrkogården,
 * här ligger gammal "odöd" kod,
 * sparad av sentimentala själ.  

                 _____
              /~/~   ~\
             | |       \
             \ \        \
              \ \        \
             --\ \       .\'
            --==\ \     ,,.''.,
                ''"'',,}{,,
 */

//    public boolean changeWorkDir(String[] path){
//        // TODO: Debug this method. It feels a bit shaky.
//        // Create temps and open root
//        
//        // Save root dir in tmp
//        Integer tmpId = 0;
//        FolderBlock tmpWorkDir = FolderBlock.load(readFile(tmpId));
//        
//        // Build tmp stack
//        Stack tmpIdStack = new Stack();
//        Stack tmpPathStack = new Stack();
//        tmpIdStack.push(0);
//        tmpPathStack.push("");
//        
//        //For each "path" String
//        boolean result = true;
//        int i=0;
//        do{
//            // Check if path exist in directory
//            tmpId = tmpWorkDir.getFileListing().get(path[i]);
//            if(tmpId != null){
//                tmpWorkDir = FolderBlock.load(readFile(tmpId));
//                tmpIdStack.push(tmpId);
//                tmpPathStack.push(path[i]);
//                result = false;
//            }
//            i++;
//        }while(result == true && i<path.length);
//        
//        if(result == true){
//            //workDir = 
//            workDirPathIds = tmpIdStack;
//            workDirPathNames = tmpPathStack;
//        }
//        return result;
//    }
