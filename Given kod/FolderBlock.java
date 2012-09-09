
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/*
 *  Copyright Mattias Liljeson Oct 12, 2011
 */

/**
 * Class used for working with block containing folders. Max number of files are 
 * 255. If more files are to be able to be added more blocks than one 
 * block (FolderBlock block) needs to be allocated.
 * @author Mattias Liljeson <mattiasliljeson.gmail.com>
 */
public class FolderBlock implements Serializable{
    //
    // Static variables and methods
    //
    
    // Not ours. stolen from: 
    // http://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array
    public static FolderBlock load(byte[] data){
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        FolderBlock folderBlockInstance = null;
        Object tmpObj = null;
        try{
            ObjectInput in = new ObjectInputStream(bis);
            tmpObj = in.readObject();
            
            bis.close();
            in.close();
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
        catch(ClassNotFoundException ex){
            ex.printStackTrace();
        }
        
        if(tmpObj instanceof FolderBlock)
                folderBlockInstance = (FolderBlock)tmpObj;
        return folderBlockInstance;
    }
    
    public static byte[] save(FolderBlock folderBlockInstance){
        byte[] data = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try{
            out = new ObjectOutputStream(bos);
            out.writeObject(folderBlockInstance);
            data = bos.toByteArray(); 
            out.close();
            bos.close(); //not needed, added for concistency
        }
        catch(IOException ignore){}
        
        return data;
    }
    
    //
    // Instance variables and methods
    //
    
    private Map<String,Integer> folderContentsMap;
    
    public FolderBlock(){
        folderContentsMap = new HashMap<String, Integer>();
    }
   
    public boolean addFile(int inodePtr, String name){
        boolean success = false;
        
        if(isFileInFolder(name) == false){
            folderContentsMap.put(name, inodePtr);
            success = true;
        }
        return success;
    }
    
    public boolean removeFile(String name){
        boolean success = false;
        if(folderContentsMap.remove(name) != null)
            success = true;
        return success;
    }
    
   public boolean rename(String oldName, String newName) {
        boolean success = false;
        if(isFileInFolder(oldName)){
            int id = getFileId(oldName);
            if(addFile(id, newName)){
                success = removeFile(oldName);
            }
        }
        return success;
    }
    
    /**
     * Returns the ID of a file if it can be found in folder.
     * @param The name of the file.
     * @return The ID of the file, or -1 if file couldn't be found.
     */
    public int getFileId(String name){
        int result = -1;
        Integer id = folderContentsMap.get(name);
        if(id != null)
            result = id;
        return result;
    }
    
    public boolean isFileInFolder(String fileName){
        boolean result = false;
        Map map = folderContentsMap;
        if(map.get(fileName) != null)
            result = true;
        return result;
    }
    
    /**
     * Get the contents of a folder in form of a string array.
     * @return String array with the file names.
     */
    public String[] getFileNames(){
        Set<String> set = folderContentsMap.keySet();
        return set.toArray(new String[set.size()]);   
    }
    
    /**
     * Get id of file (inode).
     * @param fileName The filename.
     * @return id of the file (inode id/ptr).
     */
//    public int getFileIdFromString(String fileName){
//        Integer tmp = folderContentsMap.get(fileName);
//        if(tmp == null)
//            tmp = -1;
//        return tmp;
//    }
}
