
package removal;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.logging.Logger;

public class FileUtils {
    public FileUtils(Logger logger) {
        super();
        this.logger = logger;
    }

    public boolean walkPath(String inputfile) {
        boolean rval = false;
        RemoveProlog rmvPlogClass = new RemoveProlog(logger);
        Scanner scanner = null;
        ArrayList<String> newText = new ArrayList<String>();
        logger.info("walkPath : " + inputfile);
        File folder = new File(inputfile);
        String fileName = "";
        String suffix = "";
        if (folder.isFile()) {
            logger.info(" folder " + folder);
            scanner = this.openFile(folder.getAbsolutePath());
            fileName = folder.getAbsoluteFile().getName();
            int startSuffix = fileName.indexOf('.');
            if (startSuffix > 0) {
                suffix = fileName.substring(fileName.indexOf('.') + 1,
                        fileName.length());
            }
            newText = rmvPlogClass.removeProlog(scanner, fileName, suffix);
            rval = this.updateFile(folder.getAbsolutePath(), newText);
            if (rval == false) {
                logger.severe("\n\n Update result Failed " + rval);
            }
        } else {
            File[] listOfFiles = folder.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    fileName = listOfFiles[i].getAbsoluteFile().getName();
                    int startSuffix = fileName.indexOf('.');
                    if (startSuffix > 0) {
                        suffix = fileName.substring(fileName.indexOf('.') + 1,
                                fileName.length());
                    }
                    scanner = this.openFile(inputfile
                            + listOfFiles[i].getName());
                    newText = rmvPlogClass.removeProlog(scanner, fileName,
                            suffix);
                    rval = this.updateFile(
                            inputfile + listOfFiles[i].getName(), newText);
                    if (rval == false) {
                        logger.severe("\n\n Update result Failed " + rval);
                    }
                } else if (listOfFiles[i].isDirectory()) {
                    logger.warning("Process subdirs "
                            + listOfFiles[i].getName());
                    rval = this
                            .walkPath(listOfFiles[i].getAbsolutePath() + "/");
                }
            }
        }

        return (rval);
    }

    public Scanner openFile(String inputfile) {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(inputfile)); // parm number one....
        } catch (Exception e) {
            // Print out the exception that occurred
            this.logger.warning("Unable to open existing file   "
                    + e.getMessage());
        }
        this.logger.info("Opened file " + inputfile);
        return (scanner);
    }

    public boolean updateFile(String inputfile, ArrayList<String> newText) {
        boolean rval = true;
        PrintWriter writer = null;
        Iterator<String> itr = newText.iterator();
        // open new file
        try {
            // writer = new PrintWriter(inputfile+primt, "UTF-8");
            writer = new PrintWriter(inputfile);
        } catch (Exception e) {
            // Print out the exception that occurred
            logger.info("Unable to create new file   " + e.getMessage());
            rval = false;
        }

        while (itr.hasNext()) {
            Object element = itr.next();
            // logger.fine(element.toString());
            writer.println(element.toString());
        }
        writer.close();

        return (rval);

    }

    private final Logger logger;

}