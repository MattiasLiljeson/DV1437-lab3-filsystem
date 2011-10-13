
import java.util.Stack;

public class FileManager {

    private FileSystem fileSystem;
    

    public FileManager(FileSystem p_BlockDevice) {
        fileSystem = p_BlockDevice;
        
        // Add root map
        workDir = new Stack<String>();
        workDir.add("Root");
    }

    public String format() {
        return new String("Diskformat successfull");
    }

    public String ls(String[] p_asPath) {
        System.out.print("Listing directory ");
        dumpArray(p_asPath);
        System.out.print("");
        return new String("");
    }

    public String create(String[] p_asPath, byte[] p_abContents) {
        System.out.print("Creating file ");
        dumpArray(p_asPath);
        System.out.print("");
        return new String("");
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

    public String mkdir(String[] p_asPath) {
        System.out.print("Creating directory ");
        dumpArray(p_asPath);
        System.out.print("");
        return new String("");
    }
    
    Stack<String> workDir;
    
    public String cd(String[] path) { 
        // Backup old workdir
         Stack<String> tmp_workDir = (Stack<String>) workDir.clone();
        
         // Manipulate workDir based on commands in path array
         for(String p : path){
             if( p.equals("..")){
                 // Avoid pop if "Root" is reached
                 if(workDir.size() > 1)
                     workDir.pop();
             }else if( p.equals(".")){
                 // Nothing
             }else {
                 workDir.add(p);
             }
         }
         
         // Restore workDir if new workDir does't exist in filesystem
         if(fileSystem.isPathValid(path) == false){
             System.out.print("No such directory");
             Stack<String> workDir = (Stack<String>) tmp_workDir.clone();
         }
         
         // Print path
        System.out.print("PATH: ~");
        for(String d : workDir){
            System.out.print("/"+d);
        }
         
         // Return
         return new String("");
    }

    public String pwd() {
        return new String("/unknown/");
    }

    private void dumpArray(String[] p_asArray) {
        for (int nIndex = 0; nIndex < p_asArray.length; nIndex++) {
            System.out.print(p_asArray[nIndex] + "=>");
        }
    }
}
