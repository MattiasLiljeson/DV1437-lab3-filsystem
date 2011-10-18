
import java.io.Serializable;
import java.util.Map;
import java.util.ArrayList;

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
    int workDirId;
    

    public FileSystem() { 
    }
    
    /**
     * Sets workDir and workDirId. Returns false if path doesn't exist.
     * @param Path Path to look up.
     * @return The id of the last folder if the path exists and -1 if it doesn't.
     */
    public boolean setWorkDir(String[] path){
        boolean result = false;
        int id = getFolderId(path);
        if(id != -1){
            workDirId = id;
            result = true;  
        }
        return result;
    }
    
    /**
     * Formats the file system. This erases all data and sets up a clean root 
     * folder.
     */
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
        writeInode(0, inode);
        FolderBlock folderBlock = new FolderBlock();
        writeFile(0,FolderBlock.save(folderBlock));
        
        // Reset workDir to root bock;
        workDirId = 0;
    }
    
    /**
     * get a unused block for example when allocating array. This block will not
     * be reserved. If you use this method two times in a row without writing to
     * the first block this method will return the same block. The reservation 
     * is done by writeFile(). This is done so that empty blocks aren't being 
     * reserved and therefore never written to.
     * @return The ID of an empty block.
     */
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
    
    /**
     * Returns dataId of last the last folder in path.
     * @param Path to look up.
     * @return The id of the last folder if the path exists and -1 if it doesn't.
     */
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
            if(folder.isFileInFolder(path[i]))
            {
                // Check if file is a folder
                folderId = folder.getFileId(path[i]);
                if(isAFolder(folderId)){
                    // Splendid! We found a folder. Now we just have to make
                    // sure all of the remaining "path" is folders aswell
                }else {
                    validPath = false;
                }
            }else {
                validPath = false;
            }
            i++;
        }

        // If invalid path, return -1
        if(validPath == false)
        {
            folderId = -1;
        }
        return folderId;
    }
    
    public FolderBlock getFolder(String[] path) {
        FolderBlock folder = null;
        int id = getFolderId(path);
        if(id != -1){
            folder = FolderBlock.load(readFile(id));
        }
        return folder;
    }
    
    public String copy(String[] srcPath, String[] dstPath) {
        String result = "";
        
        // Lookup source
        FolderBlock srcFolder = getFolder(getFolderPath(dstPath));
        if (srcFolder == null) {
            return "Source folder invalid";
        }
        if (srcPath.length < 1) {
            return "Corrupted path"; // This should be relativly impossible to happen
        }
        String srcName = srcPath[srcPath.length - 1];
        int scrId = srcFolder.getFileId(srcName);
        if(scrId == -1){
            return "No source file";
        }

        // Lookup destination folder
        int dstFolderId = getFolderId(dstPath);
        if (dstFolderId == -1) {
            return "Destination folder invalid";
        }
        if (dstPath.length < 1) {
            return "Corrupted path"; // This should be relativly impossible to happen
        }
        String dstName = srcPath[srcPath.length - 1];
        
        // Lookup destination file name
        FolderBlock dstFolder = FolderBlock.load(readFile(dstFolderId));
        if(dstFolder.isFileInFolder(dstName)){
            return "Destination name already exists";
        }
        
        // Perform copy
        if(copyFile(srcName, dstName, scrId, dstFolderId)){
            result = "File copied";
        }
            
        
        return result;
    }
    
    public boolean copyFile(String scrName, int scrId, int dstFolderId) {
        return copyFile(scrName, scrName, scrId, dstFolderId);
    }
    
    public boolean copyFile(String scrName, String dstName, int scrId, int dstFolderId) {
        boolean result = false;

        // If source file is a folder
        if (isAFolder(scrId)) {
            
            // Create new folder in destination folder
            touchFile(scrName, true, dstFolderId);
                    
            // Fetch files in source
            FolderBlock srcFolder = FolderBlock.load(readFile(scrId));
            String[] files = srcFolder.getFileNames();

            // For every file
            for (String f : files) {
                
                // Fetch new destination folder and continue deep copying
                int fileId = srcFolder.getFileId(f);
                FolderBlock dstFolder = FolderBlock.load(readFile(dstFolderId));
                int destFolderId = dstFolder.getFileId(f);
                copyFile(f, fileId, destFolderId);
            }
            
        } else {
            
            // Create new file in destination folder
            touchFile(scrName, false, dstFolderId);
            
            // Copy source file and write content to new file
            byte[] file = readFile(scrId);
            writeFile(dstFolderId, file);
        }
        return result;
    }
    
    public boolean rename(String oldName, String newName) {
        FolderBlock workDir = FolderBlock.load(readFile(workDirId));
        boolean result = workDir.rename(oldName, newName);
        writeFile(workDirId, FolderBlock.save(workDir));
        return result;
    }
    
    // Get just the folder path of a full path
    public String[] getFolderPath(String[] path) {
        String[] folderPath = new String[0];
        if (path.length > 0) {
            folderPath = new String[path.length - 1];
            System.arraycopy(path, 0, folderPath, 0, path.length - 1);
        } 
        return folderPath;
    }
    
    public String[] getFileNames() {
        FolderBlock workDir = FolderBlock.load(readFile(workDirId));
        return workDir.getFileNames();
    }
    
    public int getNextBlockId(int blockId) {
        int id = byteArrayToInt(blockArray[blockId], BLOCK_SIZE-4);
        return id;
    }
    
    public void setNextBlockId(int blockId, int nextBlockId) {
        try{
        intToByteArray(nextBlockId, blockArray[blockId], BLOCK_SIZE-4);
        } catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    public String[] getFolderNames() {
        String[] files = getFileNames();
        ArrayList<String> folders = new ArrayList<String>(); 
        for (String f : files) {
            if(isAFolder(f))
                folders.add(f);
        } 
        return folders.toArray(new String[folders.size()]);
    }
    
    public String[] getNonFolderNames() {
        String[] files = getFileNames();
        ArrayList<String> folders = new ArrayList<String>(); 
        for (String f : files) {
            if(isAFolder(f) == false)
                folders.add(f);
        } 
        return folders.toArray(new String[folders.size()]);
    }
    
    /**
     * See if a file is a folder
     * @param fileId Id of the file.
     * @return true if the file is a folder, otherwise false.
     */
    public boolean isAFolder(int fileId) {
        boolean result = false;

        // If withing block range
        if(isIdValid(fileId)) {

            // Check if Inode is a folder
            Inode inode = new Inode(blockArray[fileId]);
            if (inode.getType() == 1) {
                result = true;
            }
        }

        //Return
        return result;
    }
    
    public boolean isAFolder(String fileName) {
        boolean result = false;
        FolderBlock workDir = FolderBlock.load(readFile(workDirId));
        int id = workDir.getFileId(fileName);
        if(id != -1)
            result = isAFolder(id);
        return result;
    }
    
    public boolean isIdValid(int blockId) {
        return ((NUM_BLOCKS > blockId) && (blockId >= 0));
    }
    
    /**
     * Looks up if a file exists in a supplied folder.
     * @param fileName Name of the file.
     * @return true if file exists. 
     */ 
    public boolean isFileInFolder(String fileName){
        FolderBlock workDir = FolderBlock.load(readFile(workDirId));
        return workDir.isFileInFolder(fileName);
    }
    
    /**
     * Read a file from the file system.
     * @param fileId The ID of the file (the file inodes id).
     * @return A byte array of the data contained in the file. This is the 
     * actual data in array and not limited by block size
     */
    public byte[] readFile(int fileId) {
        byte[] data = null;
        if (isIdValid(fileId)) {
            int readBytes = 0;

            Inode inode = new Inode(blockArray[fileId]);           
            int blockId = inode.getDataPtr();
            data = new byte[inode.getSize()];

            boolean done = false;
            while(!done){
                int numOfBytesToRead = BLOCK_SIZE-4;
                if(numOfBytesToRead >= data.length-readBytes)
                    numOfBytesToRead = data.length-readBytes;
                System.arraycopy(blockArray[blockId], 0, data, readBytes, numOfBytesToRead);
                readBytes += numOfBytesToRead;

                // Continue reading next block if data remains
                if(readBytes < data.length){
                    blockId = getNextBlockId(blockId);
                }
                   
                else
                    done = true;          
            }
        }
        return data;
    }
    
    public String readTextFromFile(String fileName) {
        String text = "";
        FolderBlock workDir = FolderBlock.load(readFile(workDirId));
        int id = workDir.getFileId(fileName);
        if (id == -1) {
            text = "No such file";
        } else {
            byte[] data = readFile(id);
            text = "Content of file (size " + data.length + " bytes):\n"; 
            
            if(data.length>0)
                text = text + data.toString();
            else
                text = text + "<empty>";
        }
        return text;
    }
    
    public boolean removeFile(String fileName) {
        FolderBlock workDir = FolderBlock.load(readFile(workDirId));
        return removeFile(fileName, workDir, workDirId);
    }

    public boolean removeFile(String fileName, FolderBlock folder, int folderId) {
        boolean succees = false;
        int id = folder.getFileId(fileName);
        
        // If file exist in parent folder
        if(id != -1){
            
            // If file is a folder
            if(isAFolder(id)){
                // Empty folder 
                FolderBlock subFolder = FolderBlock.load(readFile(id));
                emptyFolder(subFolder, id);
            }
            
            // Delete file and its inode from memory
            Inode inode = new Inode(blockArray[id]);
            int blockId = inode.getDataPtr();
            releaseBlock(blockId);
            releaseBlock(id);
            
            // Delete file from parent folder
            succees = folder.removeFile(fileName);
            
            //Save changes done to parent folder
            writeFile(folderId, FolderBlock.save(folder));
        }
        return succees;
    }
    
    public boolean emptyFolder(FolderBlock folder, int folderId) {
        boolean result = false;
        String[] files = folder.getFileNames();
        
        // For every file in folder
        for (String f : files) {
            
            // If file is a folder
            if(isAFolder(f)){
                
                // Empty sub-folder
                int subFolderId = folder.getFileId(f);
                FolderBlock subFolder = FolderBlock.load(readFile(subFolderId));
                emptyFolder(subFolder, subFolderId);
                
                // Delete file and its inode
                Inode inode = new Inode(blockArray[subFolderId]);           
                int blockId = inode.getDataPtr();
                releaseBlock(blockId);
                releaseBlock(subFolderId);
                
                // Remove file from folder
                result = folder.removeFile(f);
                
            }else{
                //Delete file from folder
                removeFile(f, folder, folderId);
            } 
        }
        
        //Save changes done to parent folder
        writeFile(folderId, FolderBlock.save(folder));
        
        return result;
    }
    
    
    /**
     * Sets the block, and all of the blocks it points to via its nextBlock, 
     * nextBlock to -1. This is done to prepare the block for new data.
     * @param dataId 
     */
    public void releaseBlock(int blockId){
        boolean freeNextBlock = false;
        int nextBlockId = getNextBlockId(blockId);
        if(nextBlockId != -1)
            freeNextBlock = true;
        
        // Set next block id to -1
        setNextBlockId(blockId, -1);
        freeBlocks[blockId] = true;
        
        //Release next block if any
        if(freeNextBlock)
            releaseBlock(nextBlockId);
    }
    
    /**
     * 'Touches' a file. Creates a inode in the given folder and gives it the
     * supplied file name. If the file doesn't already exist.
     * @param path Path to the folder where you want to create the file.
     * @param fileName Name of the file.
     * @param asFolder If the file is going to be a folder.
     * @return -1 if the path is invalid or if the file already exists. 
     * Otherwise its fileId (the inode id). 
     */
    public int touchFile(String fileName, boolean asFolder) {
        int result = -1;
        result = touchFile(fileName, asFolder, workDirId);
        return result;
    }
    
    public int touchFile(String fileName, boolean asFolder, int folderId) {
        FolderBlock folder = FolderBlock.load(readFile(folderId));
        
        int result = -1;
        if (isFileInFolder(fileName) == false) {
            int inodeBlock = getFreeBlock();
            Inode inode = new Inode(asFolder);
            inode.setSize(0);

            // Save inode so writeFile() can find it
            writeInode(inodeBlock, inode);

            // If folder, create and save new folderBlock
            if (asFolder) {
                FolderBlock folderBlock = new FolderBlock();
                writeFile(inodeBlock, FolderBlock.save(folderBlock));
            }

            // Update Folder with the new file and save to disk.
            folder.addFile(inodeBlock, fileName);
            writeFile(folderId, FolderBlock.save(folder));
            folder = FolderBlock.load(readFile(folderId));

            // Return the access ID to the new file  
            result = inodeBlock;
        }
        return result;
    }
    
    public void writeInode(int blockId, Inode inode) {
        if(isIdValid(blockId)){
            blockArray[blockId] = inode.save();
            freeBlocks[blockId] = false;
        }     
    }
    
    /**
     * Write a byte array to disk. Not affected by block size. This allocates, 
     * deallocates blocks as needed by itself
     * @param inodeId The id of the the file which is going to be written (its 
     * inode id)
     * @param data A byte array of data. The size of the array doesn't have to 
     * be the same as the block size 
     * @return 
     */
    public boolean writeFile(int inodeId, byte[] data) {
        boolean result = false;
        if (isIdValid(inodeId)) {
            
            int writtenBytes = 0;
            int writtenBlocks = 0;

            Inode inode = new Inode(blockArray[inodeId]);           
            inode.setSize(data.length);

            int dataId = inode.getDataPtr();
            if(dataId == -1){
                dataId = getFreeBlock();
                inode.setDataPtr(dataId);
            }
            writeInode(inodeId, inode);    
            
            boolean done = false;
            while(!done){
                int i = 0;
                while(i < BLOCK_SIZE-4 && writtenBytes < data.length ) {
                    try{
                    blockArray[dataId][i] = data[writtenBytes];
                    }catch(Exception ex){
                        ex.printStackTrace();
                         i = 0;
                    }
                    writtenBytes++;
                    i++;
                }
                writtenBlocks++;
                freeBlocks[dataId] = false;
                
                
                int nextBlockId = getNextBlockId(dataId);
                if(writtenBytes < data.length){
                    if(nextBlockId == -1){
                        nextBlockId = getFreeBlock();
                        setNextBlockId(dataId, nextBlockId);
                    }
                    dataId = nextBlockId;
                }
                else {
                    done = true;
                    if(nextBlockId != -1)
                        releaseBlock(nextBlockId);
                    
                }
            }
            result = true;
        }
        return result;
    }
    
    public boolean writeToFile(String name, byte[] data) {
        boolean result = false;
        FolderBlock workDir = FolderBlock.load(readFile(workDirId));
        int id = workDir.getFileId(name);
        
        // If file exist and is not a folder
        if(id != -1 && isAFolder(id) == false){
            // Write data
            result = writeFile(id, data);
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
//        boolean success = true;
//        int i=0;
//        do{
//            // Check if path exist in directory
//            tmpId = tmpWorkDir.getFileListing().get(path[i]);
//            if(tmpId != null){
//                tmpWorkDir = FolderBlock.load(readFile(tmpId));
//                tmpIdStack.push(tmpId);
//                tmpPathStack.push(path[i]);
//                success = false;
//            }
//            i++;
//        }while(success == true && i<path.length);
//        
//        if(success == true){
//            //workDir = 
//            workDirPathIds = tmpIdStack;
//            workDirPathNames = tmpPathStack;
//        }
//        return success;
//    }
