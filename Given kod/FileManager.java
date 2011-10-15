
import java.util.Stack;

public class FileManager {

    private FileSystem fileSystem;
    private Stack<String> workDir;
    

    public FileManager(FileSystem p_BlockDevice) {
        fileSystem = p_BlockDevice;
        fileSystem.format();
        
        // Add root map
        workDir = new Stack<String>();
    }

    public String format() {
        return new String("Diskformat successful");
    }

    public String ls(String[] p_asPath) {
        return ls();
    }
    
    public String ls() {
        String result = "";
        String[] folderNames = fileSystem.getFolderNames();
        String[] fileNames = fileSystem.getNonFolderNames();
        
        
        // Append all folders to String
        result = "<" + folderNames.length + " folders>";
        for(int i=0; i<folderNames.length; i++){
            result = result + "\n" + folderNames[i];
        }
        
        // Append all files to String
        result = result + "\n<" + fileNames.length + " files>";
        for(int i=0; i<fileNames.length; i++){
            result = result + "\n" + fileNames[i];
        }
        
        // If no files
        if(fileNames.length == 0 && folderNames.length == 0){
            result = "<empty>";
        }
        
        //Return
        return result;
    }

    public String create(String[] p_asPath, byte[] data) {
        String result = ""; 
        String name = p_asPath[0] +".file";
        
        if(fileSystem.touchFile(name, false) != -1){
           result = "File created";
        }else
        {
            result = "Name already exists";
        }
        
        return result;
    }

    public String cat(String[] p_asPath) {
        System.out.print("Dumping contents of file ");
        dumpArray(p_asPath);
        System.out.print("");
        return new String("");
    }

    public String save(String p_sPath) {
        System.out.print("Saving blockdevice to file " + p_sPath);
        return new String("");
    }

    public String read(String p_sPath) {
        System.out.print("Reading file " + p_sPath + " to blockdevice");
        return new String("");
    }

    public String rm(String[] p_asPath) {
        System.out.print("Removing file ");
        dumpArray(p_asPath);
        System.out.print("");
        return new String("");
    }

    public String copy(String[] p_asSource, String[] p_asDestination) {
        System.out.print("Copying file from ");
        dumpArray(p_asSource);
        System.out.print(" to ");
        dumpArray(p_asDestination);
        System.out.print("");
        return new String("");
    }

    public String append(String[] p_asSource, String[] p_asDestination) {
        System.out.print("Appending file ");
        dumpArray(p_asSource);
        System.out.print(" to ");
        dumpArray(p_asDestination);
        System.out.print("");
        return new String("");
    }

    public String rename(String[] p_asSource, String[] p_asDestination) {
        System.out.print("Renaming file ");
        dumpArray(p_asSource);
        System.out.print(" to ");
        dumpArray(p_asDestination);
        System.out.print("");
        return new String("");
    }

    public String mkdir(String[] name) {
        String result = ""; 
        if(fileSystem.touchFile(name[0], true) != -1){
            result = "Directory created";
        }else
        {
            result = "Name already exists";
        }
        return result;
    }
    
    public String cd(String[] path) {
        String result = "";
        
        // Backup old workdir
         Stack<String> tmp_workDir = (Stack<String>) workDir.clone();
        
         // Manipulate workDir based on commands in path array
         for(String p : path){
             if( p.equals("..") ){
                 if(workDir.size()>0){
                     workDir.pop();
                 }
             }else if( p.equals(".")){
                 // Nothing
             }else {
                 workDir.add(p);
             }
         }
         
         // Restore workDir if new workDir doesn't exist in filesystem
         if(fileSystem.setWorkDir(getWorkDirArray()) == false){
             result = result + "No such directory";
             workDir = (Stack<String>) tmp_workDir.clone();
         }else{
             result = result + "Directory changed\n" + ls();
         }
         
         // Return
         return result;
    }
    
    private String[] getWorkDirArray() {
        String array[] = new String[workDir.size()];
        for(int i=0; i<workDir.size(); i++){
            array[i] = workDir.get(i);
        }
        return array;
    }

    public String pwd() {
        // Print path
        String path = "~";
        for(String d : workDir){
            path = path+"/"+d;
        }
        
        return new String(path);
    }

    private void dumpArray(String[] p_asArray) {
        for (int nIndex = 0; nIndex < p_asArray.length; nIndex++) {
            System.out.print(p_asArray[nIndex] + "=>");
        }
    }
}
