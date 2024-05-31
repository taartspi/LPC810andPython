
package utilities;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

public class FfdcControls {

    public static final int NO_FFDC = 0;
    public static final int FILE_FFDC = 1;
    public static final int DISPLAY_FFDC = 2;
    public static final int ERROR_EXIT = 4;
    public static final int SHOW_ALL = 8;

    public static final int MAX_ENTRY_LEN = 512;

    public FfdcControls() {
        super();
        this.list = new ArrayList<String>();
        this.warnList = new ArrayList<String>();
        this.errorControlValue = NO_FFDC;
    }

    public FfdcControls(int errorControl) {
        super();
        this.list = new ArrayList<String>();
        this.warnList = new ArrayList<String>();
        this.errorControlValue = errorControl;
    }

    public FfdcControls(String errorControl) {
        super();
        this.list = new ArrayList<String>();
        this.warnList = new ArrayList<String>();
        this.errorControlValue = Integer.parseInt(errorControl);
    }

    public int errorExit(int errorCode, Logger logger) {
        int rc = 0;
        if ((this.errorControlValue & ERROR_EXIT) > 0) {
            logger.severe("Exit, dump FFDC logs");
            displayFfdcEntries(logger);
            displayFfdcConfigWarningEntries(logger);
            logger.severe("Exit with return value " + errorCode);
            System.exit(errorCode);
        } else {
            ; // addFfdcEntryConfigWarning((
              // "ErrorExit condition.  Error Value " + errorCode ), logger);
        }

        return (rc);
    }

    public int addFfdcEntry(String entry, Logger logger) {
        int rc = 0;
        String finalEntry = "";
        if (entry.length() > MAX_ENTRY_LEN) {
            finalEntry = entry.substring(0, 256) + " truncated";
        } else {
            finalEntry = entry;
        }
        this.list.add(finalEntry);
        if ((this.errorControlValue & SHOW_ALL) > 0) {
            logger.info(finalEntry);
        }

        return (rc);
    }

    // Intent is capture logs that show configuration errors. Thing like there
    // is no master found for a slave entry. Such
    // things that spell out an error in the users input files.
    // A separate display method will dump only these logs
    // displayFfdcConfigWarningEntries
    public int addFfdcEntryConfigWarning(String entry, Logger logger) {
        int rc = 0;
        this.warnList.add(entry);
        return (rc);
    }

    public int displayFfdcConfigWarningEntries(Logger logger) {
        int rc = 0;
        Iterator<String> itr = this.warnList.iterator();
        String strBuf = new String();
        strBuf += "Iterating through FFDC Configuration Warning elements..<level: " + this.errorControlValue +">\n";
        while (itr.hasNext()) {
            strBuf += ("  Entry " + itr.next() + "  \n");
        }
        logger.info(strBuf);
        return (rc);
    }

    public int displayFfdcEntries(Logger logger) {
        int rc = 0;
        String finalFormatFile = ".ffdc_data.text";
        String strBuf = new String();
        strBuf += "Iterating through FFDC elements..<level: " + this.errorControlValue +">\n";
        int count = 0;
        try {
            if ((this.errorControlValue & FILE_FFDC) > 0) {
                PrintWriter out = new PrintWriter(new FileWriter(
                        finalFormatFile));
                Iterator<String> itr = this.list.iterator();
                count = 0;
                while (itr.hasNext()) {
                    strBuf += ("  Entry " + itr.next() + "  \n");
                    count++;
                    if (count > 200) {
                        count = 0;
                        out.println("\n" + strBuf);
                        strBuf = "\n";
                    }
                }
                out.println("\n" + strBuf);
                // now add in the warning list information
                itr = this.warnList.iterator();
                count = 0;
                strBuf += ("\n\nIterating through FFDC entries elements...\n");
                while (itr.hasNext()) {
                    strBuf += ("  Entry " + itr.next() + "  \n");
                    count++;
                    if (count > 200) {
                        count = 0;
                        out.println("\n" + strBuf);
                        strBuf = "\n";
                    }
                }
                out.println("\n" + strBuf);

                out.close();
            }
        } catch (IOException e) {
            // Print out the exception that occurred
            System.out.println("Unable to create " + finalFormatFile + ": "
                    + e.getMessage());
        }
        if ((this.errorControlValue & DISPLAY_FFDC) > 1) {
            strBuf += "\n";
            logger.info("Iterating through FFDC entries elements...");
            count = 0;
            Iterator<String> itr = this.list.iterator();
            while (itr.hasNext()) {
                strBuf += ("  Entry " + itr.next() + "  \n");
                count++;
                if (count > 200) {
                    count = 0;
                    logger.info(strBuf);
                    strBuf = "\n";
                }
            }
            logger.info(strBuf);
        } else {
            logger.fine("Display disabled...");
        }
        return (rc);
    }

    public int setFfdcControl(int control, Logger logger) {
        int rc = 0;
        this.errorControlValue = control;
        return (rc);
    }

    private int errorControlValue; // 0 none, 1 log details when so requested,
                                   // 2
                                   // exit on error, 3 log details AND exit on
                                   // error
    private final ArrayList<String> list;
    private final ArrayList<String> warnList;

}