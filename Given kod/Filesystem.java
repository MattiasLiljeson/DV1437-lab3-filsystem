
import java.io.Serializable;
import java.util.ArrayList;

class FileSystem implements Serializable {
    //
    // Static variables and methods
    //
    
    public final static int NUM_BLOCKS = 250;
    public final static int BLOCK_SIZE = 512;
    
    /**
     * Convert an int to a byte array.
     * @param value Which int to convert.
     * @param dst Which byte array to save to.
     * @param start Where in the dst array to save the int.
     * @return The dst byte array.
     */
    public static final byte[] intToByteArray(int value, byte[] dst, int start) {
        byte[] tmp = intToByteArray(value); 
        System.arraycopy(tmp, 0, dst, start, 4);
        return dst;
    }
    
    // Not ours. Stolen from: http://snippets.dzone.com/posts/show/93
    /**
     * Convert an int to a byte array. 
     * @param value Which int to convert. 
     * @return A byte array with 4 elements.
     */
    public static final byte[] intToByteArray(int value) {
        return new byte[] {
            (byte)(value >>> 24),
            (byte)(value >>> 16),
            (byte)(value >>> 8),
            (byte)value};
    }
    
    /**
     * Convert a part of a byte array to an int.
     * @param byteArray Which byte array to use
     * @param start Where to start in the byte array.
     * @return The resulting int.
     */
    public static final int byteArrayToInt(byte[] byteArray, int start) {
        byte[] tmp = new byte[4];
        System.arraycopy(byteArray, start, tmp, 0, 4);
        return byteArrayToInt(tmp);
    }
    
    /**
     * Convert a 4 element byte array to an int. Does NOT look for wrong byte 
     * array size. Just uses the 4 first elements.
     * @param b
     * @return 
     */
    public static final int byteArrayToInt(byte [] b) {
        return (b[0] << 24)
            + ((b[1] & 0xFF) << 16)
            + ((b[2] & 0xFF) << 8)
            + (b[3] & 0xFF);
    }
    
    //
    // Instance variables and methods
    //
    
    boolean[] freeBlocks = new boolean[NUM_BLOCKS];
    byte[][] blockArray = new byte[NUM_BLOCKS][BLOCK_SIZE];
    //int folderId;
    

    public FileSystem() { 
    }
    
    /**
     * Sets workDir and folderId. Returns false if path doesn't exist.
     * @param Path Path to look up.
     * @return The id of the last folder if the path exists and -1 if it doesn't.
     */
//    public boolean setWorkDir(String[] path){
//        boolean result = false;
//        int id = getFolderId(path);
//        if(id != -1){
//            folderId = id;
//            result = true;  
//        }
//        return result;
//    }
    
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
        //workDirId = 0;
    }
    
    /**
     * Get an unused block for example when allocating array. This block will not
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
                if(isFolder(folderId)){
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
    
    public boolean appendToFile(String name, String[] path, byte[] newData) {
        boolean result = false;
        int workDirId = getFolderId(path);
        FolderBlock workDir = FolderBlock.load(readFile(workDirId));
        int id = workDir.getFileId(name);
        
        // If file exist and is not a folder
        if(id != -1 && isFolder(id) == false){
            
            // Merge data with old data
            byte[] oldData = readFile(id);
            byte[] mergedData = new byte[oldData.length + newData.length];
            System.arraycopy(oldData, 0, mergedData, 0, oldData.length);
            System.arraycopy(newData, 0, mergedData, oldData.length, newData.length);
            
            // Write merged data
            result = writeFile(id, mergedData);
        }
        return result;
    }
    
    public String mergeFiles(String[] srcPath, String[] dstPath) {
        String result = "";
        
        // Lookup source
        FolderBlock srcFolder = getFolder(getFolderPath(srcPath));
        if (srcFolder == null) {
            result = "Source folder invalid";
        } else {
            if (srcPath.length < 1) {
                // This option should be prevented by "Shell.java" 
                result = "Corrupt source path";
            } else {
                String srcName = srcPath[srcPath.length - 1];
                int scrId = srcFolder.getFileId(srcName);
                if (scrId == -1) {
                    result = "No source file found";
                } else {
                    if (isFolder(scrId)) {
                        result = "No source file found";
                    } else {

                        // Lookup destination
                        int dstFolderId = getFolderId(getFolderPath(dstPath));
                        if (dstFolderId == -1) {
                            result = "Destination folder invalid";
                        } else {
                            if (dstPath.length < 1) {
                                result = "Corrupt destination path"; // Relativly impossible to happen
                            } else {
                                String dstName = dstPath[dstPath.length - 1];
                                FolderBlock dstFolder = FolderBlock.load(readFile(dstFolderId));
                                if (dstFolder.isFileInFolder(dstName) == false) {
                                    result = "No destination file found";
                                } else {
                                    int dstId = dstFolder.getFileId(dstName);
                                    if (isFolder(dstId) == false) {
                                        result = "Destination has to be a file";
                                    } else {
                                    }

                                    // Load source data
                                    byte[] srcData = readFile(scrId);

                                    // Add source data to destination data
                                    byte[] dstData = readFile(dstId);
                                    byte[] mergedData = new byte[dstData.length + srcData.length];
                                    System.arraycopy(dstData, 0, mergedData, 0, dstData.length);
                                    System.arraycopy(srcData, 0, mergedData, dstData.length, srcData.length);

                                    // Write merged data
                                    if (writeFile(dstId, mergedData)) {
                                        result = "File " + srcName + " appended to file " + dstName;
                                    }

                                    // Remove source, as it is now appended to destination
                                    int srcFolderId = getFolderId(getFolderPath(dstPath));
                                    removeFile(srcName, srcFolderId);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Did we succeed or not?
        return result;
    }
    
    public String mergeFiles(int scrId, int dstId) {
        
        return "";
    }
    
    public String copy(String[] srcPath, String[] dstPath) {
        String result = "";
        
        /* NOTE TO SELF : Prolonged exposure to large "if else" statments could 
         * result in, lack of sleep, hair loss, swearing, insanity and/or eye cancer.
         */
        
        // Lookup source
        FolderBlock srcFolder = getFolder(getFolderPath(srcPath));
        if (srcFolder == null) {
            result = "Source folder invalid";
        } else {
            if (srcPath.length < 1) {
                // This option should be prevented by "Shell.java" 
                result = "Corrupt source path";
            } else {
                String srcName = srcPath[srcPath.length - 1];
                int scrId = srcFolder.getFileId(srcName);
                if (scrId == -1) {
                    result = "No source file found";
                } else {
                    
                    // Lookup destination folder
                    int dstFolderId = getFolderId(getFolderPath(dstPath));
                    if (dstFolderId == -1) {
                        result = "Destination folder invalid";
                    } else {
                        if (dstPath.length < 1) {
                            result = "Corrupt destination path"; // Relativly impossible to happen
                        } else {
                            String dstName = dstPath[dstPath.length - 1];
               
                            // Lookup destination file name
                            FolderBlock dstFolder = FolderBlock.load(readFile(dstFolderId));
                            if (dstFolder.isFileInFolder(dstName)) {
                                result = "Destination name already exist";
                            } else {

                                // Perform copy
                                if (copyFile(srcName, dstName, scrId, dstFolderId)) {
                                    result = "File copied";
                                } else {
                                    result = "Copying failed";
                                } 
                            }
                        }
                    }
                }
            }
        }
        
        // Did we succeed or not?
        return result;
    }
    
    public boolean copyFile(String scrName, int scrId, int dstFolderId) {
        return copyFile(scrName, scrName, scrId, dstFolderId);
    }
    
    public boolean copyFile(String scrName, String dstName, int scrId, int dstFolderId) {
        boolean result = false;

        // If source file is a folder
        if (isFolder(scrId)) {
            
            // Create new folder in destination folder
            int newFolderId = touchFile(dstName, true, dstFolderId);
            if (newFolderId != -1) {
                result = true;

                // Fetch fileNames in source
                FolderBlock srcFolder = FolderBlock.load(readFile(scrId));
                String[] files = srcFolder.getFileNames();

                // For every file
                for (String f : files) {
                    // Fetch new destination folder and continue deep copying
                    int fileId = srcFolder.getFileId(f);
                    result = copyFile(f, fileId, newFolderId);
                }
            }
        } else {
            
            // Create new file in destination folder
            int dstId = touchFile(dstName, false, dstFolderId);
            if (dstId != -1) {
                result = true;

                // Copy source file and write content to new file
                byte[] file = readFile(scrId);
                writeFile(dstId, file);
            }
        }
        return result;
    }
    
    public boolean rename(String oldName, String newName, String[] path) {
        int workDirId = getFolderId(path);
        FolderBlock workDir = FolderBlock.load(readFile(workDirId));
        boolean result = workDir.rename(oldName, newName);
        writeFile(workDirId, FolderBlock.save(workDir));
        return result;
    }
    
    /** Get just the folder path of a full path. 
     * @param path The String array
     * @return A String array containing all elements but the last one.
     */
    public String[] getFolderPath(String[] path) {
        String[] folderPath = new String[0];
        if (path.length > 0) {
            folderPath = new String[path.length - 1];
            System.arraycopy(path, 0, folderPath, 0, path.length - 1);
        } 
        return folderPath;
    }
    
    public String[] getFileNames(String[] path) {
        int workDirId = getFolderId(path);
        FolderBlock workDir = FolderBlock.load(readFile(workDirId));
        return workDir.getFileNames();
    }
    
    public int getNextBlockId(int blockId) {
        int id = byteArrayToInt(blockArray[blockId], BLOCK_SIZE-4);
        return id;
    }
    
    public String[] getFolderNames(String[] path) {
        String[] files = getFileNames(path);
        int folderId = getFolderId(path);
        ArrayList<String> folders = new ArrayList<String>(); 
        for (String file : files) {
            if(isFolder(file, folderId) == true)
                folders.add(file);
        } 
        return folders.toArray(new String[folders.size()]);
    }
    
    public void setNextBlockId(int blockId, int nextBlockId) {
        intToByteArray(nextBlockId, blockArray[blockId], BLOCK_SIZE-4);
    }
    
    public String[] getNonFolderNames(String[] path) {    
        String[] files = getFileNames(path);
        int folderId = getFolderId(path);
        ArrayList<String> folders = new ArrayList<String>(); 
        for (String file : files) {
            if(isFolder(file, folderId) == false)
                folders.add(file);
        } 
        return folders.toArray(new String[folders.size()]);
    }
    
    /**
     * See if a file is a folder
     * @param fileId Id of the file.
     * @return true if the file is a folder, otherwise false.
     */
    public boolean isFolder(int fileId) {
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
    
    /**
     * Checks whether the supplied file is a folder.
     * @param fileName the file to look up
     * @return true if a the supplied filename is a folder.
     */
    public boolean isFolder(String fileName, int folderId) {
        boolean result = false;
        FolderBlock workDir = FolderBlock.load(readFile(folderId));
        int id = workDir.getFileId(fileName);
        if(id != -1)
            result = isFolder(id);
        return result;
    }
    
    /**
     * Checks whether the block id is in range.
     * @param blockId Which id to look up.
     * @return true if in range.
     */
    public boolean isIdValid(int blockId) {
        boolean result = false;
        if((blockId < NUM_BLOCKS) && (blockId >= 0))
            result = true;
        return result;
    }
    
    /**
     * Checks if a path is valid.
     * @param path Path of folders.
     * @return true if the path is valid;
     */
    public boolean isPathValid(String path[]) {
        boolean failed = false;
        int folderId = 0; //root folder
        int i = 0;
        
        while(failed == false && i < path.length) {
            FolderBlock folder = FolderBlock.load(readFile(folderId));
            
            failed = true;
            if(folder.isFileInFolder(path[i])) {
                folderId = folder.getFileId(path[i]);
                if(isFolder(folderId)) {
                    folder = FolderBlock.load(readFile(folderId));
                    failed = false;
                }
            }
            i++;
        }
        return !failed;
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
    
    public String readTextFromFile(String fileName, String[] path) {
        String text = "";
        int workDirId = getFolderId(path);
        FolderBlock workDir = FolderBlock.load(readFile(workDirId));
        int id = workDir.getFileId(fileName);
        if (id == -1) {
            text = "No such file";
        }
		else if(isFolder(id)) {
			text = "Can't read folder as text";
		} 
		else {
            byte[] data = readFile(id);
            text = "Content of file (size " + data.length + " bytes):\n"; 
            
            if(data.length>0)
                text = text + new String(data);
            else
                text = text + "<empty>";
        }
        return text;
    }
    
    public boolean removeFile(String fileName, String[] path) {
        int folderId = getFolderId(path);
        boolean result = removeFile(fileName, folderId);
        return result;
    }

    public boolean removeFile(String fileName, int folderId /*String[] path*/) {
        boolean succees = false;
        FolderBlock folder = FolderBlock.load(readFile(folderId));
        int id = folder.getFileId(fileName);
        
        // If file exist in parent folder
        if(id != -1){
            
            // If file is a folder
            if(isFolder(id)){
                // Empty folder 
                FolderBlock subFolder = FolderBlock.load(readFile(id));
                emptyFolder(id);
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
    
    /**
     * Remove a folder contents, recursively
     * @param folderId Id of the folder
     * @return True if the folder contains subfolders.
     */
    public boolean emptyFolder(int folderId) {
        boolean result = false;
        
        FolderBlock folder = FolderBlock.load(readFile(folderId));
        String[] fileNames = folder.getFileNames();
        
        // For every file in folder
        for (String fileName : fileNames) {
            
            // If file is a folder
            if(isFolder(fileName, folder.getFileId(fileName))){
                
                // Empty sub-folder, recursively
                int subFolderId = folder.getFileId(fileName);
                emptyFolder(subFolderId);
                
                // Delete file and its inode
//                Inode inode = new Inode(blockArray[subFolderId]);           
//                int blockId = inode.getDataPtr();
//                releaseBlock(blockId);
                releaseBlock(subFolderId);
                
                // Remove file from folder
                result = folder.removeFile(fileName);
                
            }else{
                //Delete file from folder
                removeFile(fileName, folderId);
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
     * @param fileName Name of the file.
     * @param asFolder If the file is going to be a folder.
     * @param path Path to the folder where you want to create the file.
     * @return -1 if the path is invalid or if the file already exists. 
     * Otherwise its fileId (the inode id). 
     */
    public int touchFile(String fileName, boolean asFolder, String[] path) {
        int result = -1;
        int workDirId = getFolderId(path);
        if(workDirId != -1) {
            result = touchFile(fileName, asFolder, workDirId);
        }
        return result;
    }
    
    public int touchFile(String fileName, boolean asFolder, int folderId) {

        FolderBlock folder = FolderBlock.load(readFile(folderId));
        
        int result = -1;
        if (folder.isFileInFolder(fileName) == false) {
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
     * @return True if successful.
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
    
    /**
     * Write a file to the "disk" using it's filename.
     * @param name Filename of the file.
     * @param data byte array of data. 
     * @return 
     */
    public boolean writeToFile(String name, byte[] data, String[] path) {
        boolean result = false;
        int workDirId = getFolderId(path);
        FolderBlock workDir = FolderBlock.load(readFile(workDirId));
        int id = workDir.getFileId(name);
        
        // If file exist and is not a folder
        if(id != -1 && isFolder(id) == false){
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
