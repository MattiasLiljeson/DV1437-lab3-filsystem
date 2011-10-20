
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Stack;

public class FileManager {

    private FileSystem fileSystem;
    private Stack<String> workPath;
    

    public FileManager(FileSystem p_BlockDevice) {
        fileSystem = p_BlockDevice;
        format();
        
        // Add root map
        workPath = new Stack<String>();
        
        // Load example filestructure
        System.out.println(read("default"));
        
    }

    public String format() {
        fileSystem.format();
        workPath = new Stack<String>();
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
        result = "<" + folderNames.length + " folder>";
        for(int i=0; i<folderNames.length; i++){
            result = result + "\n" + folderNames[i];
        }
        
        // Append all files to String
        result = result + "\n<" + fileNames.length + " file>";
        for(int i=0; i<fileNames.length; i++){
            result = result + "\n" + fileNames[i];
        }
        
        // If no files
        if(fileNames.length == 0 && folderNames.length == 0){
            result = "<empty>";
        }
        
        // Return
        return result;
    }
    
    // Loads a file and makes it into a file in the filesytem
    public String loadfile(String fileName) {
        String result = ""; 
        StringBuilder content = new StringBuilder();

        try {
            // Read content
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String line = null;
            while ((line = in.readLine()) != null) {
                content.append(line).append(System.getProperty("line.separator"));
            }
            in.close();
            
            // Write content to new file
            String[] p_asPath = new String[1];
            p_asPath[0] = fileName;
            result = create(p_asPath, content.toString().getBytes());
        } catch (Exception e) {
            result = "File not found";
        }

        return result;
    }

    public String create(String[] p_asPath, byte[] data) {
        String result = ""; 
        String name = p_asPath[0];
        
        // Add "new line" to text to make it more readable in case we append two files.
        data[data.length-1] = '\n';
        
        // Create file
        if (fileSystem.touchFile(name, false) != -1) {
            result = "File created";

            // Write data to file
            if(fileSystem.writeToFile(name, data)){
                result = result + "\nWrite succeeded";
            }else {
                result = result + "\nWrite failed";
            }
            
        } else {
            result = "Name already exists";
        }
        
        return result;
    }

    public String cat(String[] p_asPath) {
        String fileName = p_asPath[0];
        return fileSystem.readTextFromFile(fileName);
    }
    
    /*
     * Save as a UNIX file. what is a UNIX file? Wikipeida says nothing. Our 
     * guess is that it is a file without a file extension. Thats how we choose 
     * to implement it.
     */
    public String save(String p_sPath) {
        System.out.print("Saving blockdevice to file \"" + p_sPath+"\"\n");
        String result = "Writing file failed";
        
        try{
            FileOutputStream fs = new FileOutputStream(p_sPath);
            ObjectOutputStream os = new ObjectOutputStream(fs);
            os.writeObject(fileSystem);
            os.close();
            result = "File written successfully";
        }
        catch(IOException ex) {
            result = "Failed to save file. IO error";
        }
        
        return result;
    }

    public String read(String p_sPath) {
        System.out.print("Reading file \"" + p_sPath + "\" to blockdevice\n");
        String result = "Loading file failed";
        
        try{
            FileInputStream fileStream = new FileInputStream(p_sPath);
            ObjectInputStream os = new ObjectInputStream(fileStream);
            format();
            fileSystem = (FileSystem)os.readObject();
            result = "File loaded successfully";
        }
        catch(IOException ex) {
            result = "File not found or other IO error";
        }
        catch(ClassNotFoundException ex) {
            result = "Wrong type of file or from other version of program";
        }
        
        return result;
    }

    public String rm(String[] p_asPath) {
        String result = "No such file";
        String name = p_asPath[0];
        
        // Create file
        if (fileSystem.removeFile(name)) {
            result = "File removed";
        }
        
        return result;
    }

    public String copy(String[] p_asSource, String[] p_asDestination) {
        // Get the full source path
        String[] src = getAddedPath(p_asSource);
        
        // Get the full destination path
        String[] dst = getAddedPath(p_asDestination);
        
        // Copy paths
        return fileSystem.copy(src, dst);
    }

    public String append(String[] p_asSource, String[] p_asDestination) {
         // Get the full source path
        String[] src = getAddedPath(p_asSource);
        
        // Get the full destination path
        String[] dst = getAddedPath(p_asDestination);
  

        // Append files
        return fileSystem.mergeFiles(src, dst);
    }

    public String rename(String[] p_asSource, String[] p_asDestination) {
        String result = "";
        String oldName = p_asSource[0];
        String newName = p_asDestination[0];
        if(fileSystem.rename(oldName, newName)){
            result = "File renamed";
        }else{
            result = "Couldn't rename";
        }
        return result;
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
    
    public String cd(String[] addedPath) {
        String result = "";

        // Set workPath to new path, if path is valid
        if (fileSystem.setWorkDir(getAddedPath(addedPath))) {
            workPath = getAddedPathStack(addedPath);
            result = "Directory changed\n" + ls();

        } else {
            result = "No such directory";
        }

        // Return
        return result;
    }

    public String pwd() {
        // Print path
        String path = "~";
        for(String d : workPath){
            path = path+"/"+d;
        }
        
        return new String(path);
    }

    private void dumpArray(String[] p_asArray) {
        for (int nIndex = 0; nIndex < p_asArray.length; nIndex++) {
            System.out.print(p_asArray[nIndex] + "=>");
        }
    }
    
     // Returns a stack with the combined workpath and addedPath
    private Stack<String> getAddedPathStack(String[] addedPath) {
        // Backup old workdir
         Stack<String> path = (Stack<String>) workPath.clone();
         
          // Manipulate workPath based on commands in path array
         for(String p : addedPath){
             if( p.equals("..") ){
                 if(path.size()>0){
                     path.pop();
                 }
             }else if( p.equals(".")){
                 // Nothing
             }else {
                 path.add(p);
             }
         }
         
         return path;
    }
    
    // Returns an array with the combined workpath and addedPath
    private String[] getAddedPath(String[] addedPath) {
        Stack<String> path = getAddedPathStack(addedPath);
        
         return path.toArray(new String[path.size()]);
    }
}
