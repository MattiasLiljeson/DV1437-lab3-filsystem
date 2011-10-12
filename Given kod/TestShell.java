
public class TestShell {

    public static void main(String[] args) {
        FileSystem BlockTest = new FileSystem();
        FileManager FS = new FileManager(BlockTest);
        Shell Bash = new Shell(FS, null);
        Bash.start();
    }
}
