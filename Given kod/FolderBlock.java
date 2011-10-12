
import java.util.ArrayList;

/*
 *  Copyright Mattias Liljeson Oct 12, 2011
 */

/**
 * Class used for working with block containing folders. Max number of files are 
 * 255. If more files are to be able to be added more blocks than one 
 * block (FolderBlock block) needs to be allocated.
 * @author Mattias Liljeson <mattiasliljeson.gmail.com>
 */
public class FolderBlock {
    final static public byte ERR_TOO_MANY_FILES_IN_FOLDER = 1;
    //final static public byte ERR_FILE_NAME_TOO_LONG = 2;
    
    private byte numFiles;
    private byte[] filePtrs;
    private byte[] fileNamePtrs;
    
    public FolderBlock(byte[] block){
        load(block);
    }
    
    public boolean load(byte[] block){
        boolean success = false;
        if(block.length >= 512) {
           numFiles     = block[0];
           
            // If you use 255 as size for the arrays, 
            // you'll never to make them bigger.
            filePtrs     = new byte[255];
            fileNamePtrs = new byte[255];
           
            for(int i=0; i<numFiles; i++){
                // The first element (0) is used for numfiles in the saved block.
                // Therefore '+1'.
                filePtrs[i]     = block[1+i];
                fileNamePtrs[i] = block[1+numFiles+i];
            }
           
            success = true;
        }
        return success;
    }
    
    public byte[] save(){
        byte[] block = new byte[512];
        
        block[0] = numFiles;
        for(int i=0; i<numFiles; i++){
           // The first element (0) is used for numfiles in the saved block.
           // Therefore '+1'.
           block[1+i]          = filePtrs[i];
           block[1+numFiles+i] = fileNamePtrs[i];
        }
        
        return block;
    }
    
    public byte addFile(Inode inode, String name){
        int result = 0;
        
        if(numFiles > 255)
            result = ERR_TOO_MANY_FILES_IN_FOLDER;
        // Other ifs
        else{
            filePtrs[numFiles] = inode.save();
        }
        
        return result;
    }
}
