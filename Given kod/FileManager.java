
public class FileManager {

    private FileSystem m_BlockDevice;

    public FileManager(FileSystem p_BlockDevice) {
        m_BlockDevice = p_BlockDevice;
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

    public String cd(String[] p_asPath) {
        System.out.print("Changing directory to ");
        dumpArray(p_asPath);
        System.out.print("");
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
