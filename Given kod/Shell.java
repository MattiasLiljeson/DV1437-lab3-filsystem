
import java.io.IOException;
import java.io.InputStream;

import print.*;
import print.color.*;
import print.exception.*;

public class Shell {

    private FileManager fileManager;
    private InputStream m_Stream;

    // Color output:
    ColoredPrinterWIN printer;
        
    public Shell(FileManager p_Filesystem, InputStream p_Stream) {
        fileManager = p_Filesystem;

        if (p_Stream == null) {
            m_Stream = System.in;
        } else {
            m_Stream = p_Stream;
        }
        
        printer = new ColoredPrinterWIN(new ColoredPrinterWIN.Builder(10,false));
        
        // C64 style
        printer.setForegroundColor(Ansi.FColor.WHITE);
		//printer.setAttribute(Ansi.Attribute.DARK);
    }
	
	private void printWarningMsg(String msg) {
		setWarningColors();
		printer.println(msg);
		setNormalColors();
	}
	
	private void printInfoMsg(String msg) {
		setInfoColors();
		printer.println(msg);
		setNormalColors();
	}
	
	private void setNormalColors() {
		printer.setForegroundColor(Ansi.FColor.WHITE);
		printer.print("");
    }
	private void setFolderColors() {
		printer.setForegroundColor(Ansi.FColor.CYAN);
    }
	private void setFileColors() {
		printer.setForegroundColor(Ansi.FColor.WHITE);
    }
    private void setInfoColors() {
		printer.setForegroundColor(Ansi.FColor.YELLOW);
    }
	private void setWarningColors() {
		printer.setForegroundColor(Ansi.FColor.RED);
	}
	
    /**
     * Reset console colors to std ones used by windows/nix. 
     */
    public void resetColors() {
		printer.setAttribute(Ansi.Attribute.NONE);
        printer.setBackgroundColor(Ansi.BColor.BLACK);
        printer.setForegroundColor(Ansi.FColor.WHITE);
		printer.print("");
    }
    
    public void start() {
        String[] asCommands = {"quit", "format", "ls", "create", "cat", "save", "read",
            "rm", "copy", "append", "rename", "mkdir", "cd", "pwd", "help", "load"};

        boolean bRun = true;
        String sCommand;
        String[] asCommandArray;

        while (bRun) {
            System.out.print("[" + fileManager.pwd() + "]$ ");
			
            sCommand = readLine();
			
			while(sCommand.length() < 2)
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
						resetColors();
                        return;

                    case 1: // format
                        if (asCommandArray.length != 1) {
							printWarningMsg("Usage: format");
                        } else {
                            printInfoMsg(fileManager.format());
                        }
                        break;
                    case 2: // ls
                        if (asCommandArray.length == 1) {
							
                            printInfoMsg(fileManager.ls(split(".", '/')));
                        } else {
                            if (asCommandArray.length == 2) {
								printInfoMsg(fileManager.ls(split(asCommandArray[1], '/')));
                            } else {
								printWarningMsg("Usage: ls <path>");
                            }
                        }
                        break;
                    case 3: // create
                        if (asCommandArray.length != 2) {
                            printWarningMsg("Usage: create <file>");
                        } else {
							System.out.println("Enter data. Empty line to end.");
							System.out.println(fileManager.create(split(asCommandArray[1], '/'), readBlock()));
                        }
                        break;

                    case 4: // cat
                        if (asCommandArray.length != 2) {
                            printWarningMsg("Usage: cat <file>");
                        } else {
                            System.out.print(fileManager.cat(split(asCommandArray[1], '/')));
                        }
                        break;
                    case 5: // save
                        if (asCommandArray.length != 2) {
                            printWarningMsg("Usage: save <real-file>");
                        } else {
                            printInfoMsg(fileManager.save(asCommandArray[1]));
                        }
                        break;
                    case 6: // read
                        if (asCommandArray.length != 2) {
                            printWarningMsg("Usage: read <real-file>");
                        } else {
                            printInfoMsg(fileManager.read(asCommandArray[1]));
                        }
                        break;

                    case 7: // rm
                        if (asCommandArray.length != 2) {
                            printWarningMsg("Usage: rm <file>");
                        } else {
                            printInfoMsg(fileManager.rm(split(asCommandArray[1], '/')));
                        }
                        break;

                    case 8: // copy
                        if (asCommandArray.length != 3) {
                            printWarningMsg("Usage: copy <source> <destination>");
                        } else {
                            String[] src = split(asCommandArray[1], '/');
                            String[] dst = split(asCommandArray[2], '/');
                            printInfoMsg(fileManager.copy(src, dst));
                        }
                        break;

                    case 9: // append
                        if (asCommandArray.length != 3) {
                            printWarningMsg("Usage: append <source> <destination>");
                        } else {
                            printInfoMsg(fileManager.append(split(asCommandArray[1], '/'), split(asCommandArray[2], '/')));
                        }
                        break;

                    case 10: // rename
                        if (asCommandArray.length != 3) {
                            printWarningMsg("Usage: rename <old file> <new file>");
                        } else {
                            printInfoMsg(fileManager.rename(split(asCommandArray[1], '/'), split(asCommandArray[2], '/')));
                        }
                        break;

                    case 11: // mkdir
                        if (asCommandArray.length != 2) {
							printWarningMsg("Usage: mkdir <directory name>");
                        } else {
                            printInfoMsg(fileManager.mkdir(split(asCommandArray[1], '/')));
                        }
                        break;

                    case 12: // cd
                        if (asCommandArray.length != 2) {
                            printWarningMsg("Usage: cd <path>");
                        } else {
                            printInfoMsg(fileManager.cd(split(asCommandArray[1], '/')));
                        }
                        break;

                    case 13: // pwd
                        if (asCommandArray.length != 1) {
                            printWarningMsg("Usage: pwd");
                        } else {
                            printInfoMsg(fileManager.pwd());
                        }
                        break;

                    case 14: // help
                        printHelp();
                        break;
                        
                    case 15: // load file
                        if (asCommandArray.length != 2) {
                            printWarningMsg("Usage: loadfile <real-file>");
                        } else {
                            printInfoMsg(fileManager.loadfile(asCommandArray[1]));
                        }
                        break;

                    default:
                        printWarningMsg("Unknown command " + asCommandArray[0]);
                }
            }
        }
    }

    private void printHelp() {
		setInfoColors();
        printer.println("OSD Disk Tool .oO Help Screen Oo.");
        printer.println("-----------------------------------------------------------------------------------");
        printer.println("* quit:                             Quit OSD Disk Tool");
        printer.println("* format;                           Formats disk");
        printer.println("* ls     <path>:                    Lists contents of <path>.");
        printer.println("* create <path>:                    Creates a file and stores contents in <path>");
        printer.println("* cat    <path>:                    Dumps contents of <file>.");
        printer.println("* save   <real-file>:               Saves disk to <real-file>");
        printer.println("* read   <real-file>:               Reads <real-file> onto disk");
        printer.println("* rm     <path>:                    Removes <file>");
        printer.println("* copy   <source>    <destination>: Copy <source> to <destination>");
        printer.println("* append <source>    <destination>: Appends contents of <source> to <destination>");
        printer.println("* rename <old-file>  <new-file>:    Renames <old-file> to <new-file>");
        printer.println("* mkdir  <directory>:               Creates a new directory called <directory>");
        printer.println("* cd     <directory>:               Changes current working directory to <directory>");
        printer.println("* pwd:                              Get current working directory");
        printer.println("* help:                             Prints this help screen");
		setNormalColors();
		printer.print("");
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
            printer.print(p_asArray[nIndex] + "->");
        }
        System.out.println();
    }
}
