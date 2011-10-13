
import java.io.*;
import java.util.HashMap;
import java.util.Map;

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
        catch(IOException            ignore){}
        catch(ClassNotFoundException ignore){}
        
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
        
        if(folderContentsMap.get(name) != null){
            folderContentsMap.put(name, inodePtr);
            success = true;
        }
        return success;
    }
    
    public Map<String, Integer> getFileListing(){
        return folderContentsMap;
    }
    
    public int getInodePtrFromString(String fileName){
        Integer tmp = folderContentsMap.get(fileName);
        if(tmp == null)
            tmp = -1;
        return tmp;
    }
}
