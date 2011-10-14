
import java.io.*;

public class Shell {

    private FileManager fileManager;
    private InputStream m_Stream;

    public Shell(FileManager p_Filesystem, InputStream p_Stream) {
        fileManager = p_Filesystem;

        if (p_Stream == null) {
            m_Stream = System.in;
        } else {
            m_Stream = p_Stream;
        }
    }

    public void start() {
        String[] asCommands = {"quit", "format", "ls", "create", "cat", "save", "read",
            "rm", "copy", "append", "rename", "mkdir", "cd", "pwd", "help"};

        boolean bRun = true;
        String sCommand;
        String[] asCommandArray;

        while (bRun) {
            System.out.print("[" + fileManager.pwd() + "]$ ");
            sCommand = readLine();
            asCommandArray = split(sCommand, ' ');
            if (asCommandArray.length == 0) {
            } else {
                int nIndex;
                for (nIndex = 0; nIndex < asCommands.length; nIndex++) {
                    if (asCommandArray[0].compareTo(asCommands[nIndex]) == 0) {
                        break;
                    }
                }
                switch (nIndex) {
                    case 0: // quit
                        return;

                    case 1: // format
                        if (asCommandArray.length != 1) {
                            System.out.println("Usage: format");
                        } else {
                            System.out.println(fileManager.format());
                        }
                        break;
                    case 2: // ls
                        if (asCommandArray.length == 1) {
                            System.out.println(fileManager.ls(split(".", '/')));
                        } else {
                            if (asCommandArray.length != 2) {
                                System.out.println("Usage: ls <path>");
                            } else {
                                System.out.println(fileManager.ls(split(asCommandArray[1], '/')));
                            }
                        }
                        break;
                    case 3: // create
                        if (asCommandArray.length != 2) {
                            System.out.println("Usage: create <file>");
                        } else {
                            System.out.println("Enter data. Empty line to end.");
                            System.out.println(fileManager.create(split(asCommandArray[1], '/'), readBlock()));
                        }
                        break;

                    case 4: // cat
                        if (asCommandArray.length != 2) {
                            System.out.println("Usage: cat <file>");
                        } else {
                            System.out.println(fileManager.cat(split(asCommandArray[1], '/')));
                        }
                        break;
                    case 5: // save
                        if (asCommandArray.length != 2) {
                            System.out.println("Usage: save <real-file>");
                        } else {
                            System.out.println(fileManager.save(asCommandArray[1]));
                        }
                        break;
                    case 6: // read
                        if (asCommandArray.length != 2) {
                            System.out.println("Usage: read <real-file>");
                        } else {
                            System.out.println(fileManager.read(asCommandArray[1]));
                        }
                        break;

                    case 7: // rm
                        if (asCommandArray.length != 2) {
                            System.out.println("Usage: rm <file>");
                        } else {
                            System.out.println(fileManager.rm(split(asCommandArray[1], '/')));
                        }
                        break;

                    case 8: // copy
                        if (asCommandArray.length != 3) {
                            System.out.println("Usage: copy <source> <destination>");
                        } else {
                            System.out.println(fileManager.copy(split(asCommandArray[1], '/'), split(asCommandArray[2], '/')));
                        }
                        break;

                    case 9: // append
                        if (asCommandArray.length != 3) {
                            System.out.println("Usage: append <source> <destination>");
                        } else {
                            System.out.println(fileManager.append(split(asCommandArray[1], '/'), split(asCommandArray[2], '/')));
                        }
                        break;

                    case 10: // rename
                        if (asCommandArray.length != 3) {
                            System.out.println("Usage: rename <old file> <new file>");
                        } else {
                            System.out.println(fileManager.rename(split(asCommandArray[1], '/'), split(asCommandArray[2], '/')));
                        }
                        break;

                    case 11: // mkdir
                        if (asCommandArray.length != 2) {
                            System.out.println("Usage: mkdir <directory name>");
                        } else {
                            System.out.println(fileManager.mkdir(split(asCommandArray[1], '/')));
                        }
                        break;

                    case 12: // cd
                        if (asCommandArray.length != 2) {
                            System.out.println("Usage: cd <path>");
                        } else {
                            System.out.println(fileManager.cd(split(asCommandArray[1], '/')));
                        }
                        break;

                    case 13: // pwd
                        if (asCommandArray.length != 1) {
                            System.out.println("Usage: pwd");
                        } else {
                            System.out.println(fileManager.pwd());
                        }
                        break;

                    case 14: // help
                        printHelp();
                        break;

                    default:
                        System.out.println("Unknown command " + asCommandArray[0]);
                }
            }
        }
    }

    private void printHelp() {
        System.out.println("OSD Disk Tool .oO Help Screen Oo.");
        System.out.println("-----------------------------------------------------------------------------------");
        System.out.println("* quit:                             Quit OSD Disk Tool");
        System.out.println("* format;                           Formats disk");
        System.out.println("* ls     <path>:                    Lists contents of <path>.");
        System.out.println("* create <path>:                    Creates a file and stores contents in <path>");
        System.out.println("* cat    <path>:                    Dumps contents of <file>.");
        System.out.println("* save   <real-file>:               Saves disk to <real-file>");
        System.out.println("* read   <real-file>:               Reads <real-file> onto disk");
        System.out.println("* rm     <file>:                    Removes <file>");
        System.out.println("* copy   <source>    <destination>: Copy <source> to <destination>");
        System.out.println("* append <source>    <destination>: Appends contents of <source> to <destination>");
        System.out.println("* rename <old-file>  <new-file>:    Renames <old-file> to <new-file>");
        System.out.println("* mkdir  <directory>:               Creates a new directory called <directory>");
        System.out.println("* cd     <directory>:               Changes current working directory to <directory>");
        System.out.println("* pwd:                              Get current working directory");
        System.out.println("* help:                             Prints this help screen");
    }

// With compliments to: Christoffer Nilsson (chna01) for fixing
// the bug where some one character arguments where ignored, i.e.
// "ls a bb" acted as "ls bb"
    private String[] split(String p_sString, char p_cDel) {
        //skapar en tokenizer med str�ngen p_sString
        //och avskiljaren p_cDel
        java.util.StringTokenizer st = new java.util.StringTokenizer(p_sString,
                p_cDel + "");

        int nrOfTokens = st.countTokens();//r�kanar antal avskilljare(Tokens)
        String[] asStrings = new String[nrOfTokens];

        int nr = 0;
        while (st.hasMoreTokens()) {//s�l�nge som det finns fler avskiljare
            asStrings[nr] = st.nextToken();
            nr++;
        }
        return asStrings;
    }

    private byte[] readBlock() {
        byte[] abTempBuffer = new byte[1024];
        byte bTemp;
        int nIndex = 0;
        boolean bEnter = false;

        for (nIndex = 0; nIndex < 1024; nIndex++) {
            try {
                bTemp = (byte) m_Stream.read();
            } catch (IOException io) {
                bTemp = '?';
            }

            if (bTemp == '\n' || bTemp == '\r') {
                if (bEnter) {
                    break;
                } else {
                    bEnter = true;
                }
            } else {
                bEnter = false;
            }
            abTempBuffer[nIndex] = bTemp;
        }

        return abTempBuffer;

    }

    private String readLine() {
        byte[] abTempBuffer = new byte[1024];
        byte bTemp;
        int nIndex = 0;

        for (nIndex = 0; nIndex < 1024; nIndex++) {
            try {
                bTemp = (byte) m_Stream.read();
            } catch (IOException io) {
                bTemp = '\n';
            }

            if (bTemp == '\n' || bTemp == '\r') {
                break;
            }
            abTempBuffer[nIndex] = bTemp;
        }
        String sTemp = new String(abTempBuffer, 0, nIndex);
        sTemp = sTemp.trim();

        return sTemp;
    }

    private void dumpArray(String[] p_asArray) {
        for (int nIndex = 0; nIndex < p_asArray.length; nIndex++) {
            System.out.print(p_asArray[nIndex] + "->");
        }
        System.out.println();
    }
}
