	import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.logging.Logger;

import utilities.FfdcControls;


public class FileUtils {



	    public FileUtils(Logger logger, FfdcControls ffdc) {
	        super();
	        this.logger = logger;
	        this.ffdc = ffdc;
	    }

	    public boolean walkPath(String inputfile) {
	        boolean rval = false;
	        CleanTabSpcFile readClass = new CleanTabSpcFile(logger, ffdc);
	        Scanner scanner = null;
	        ArrayList<String> newText = new ArrayList<String>();
	        ffdc.addFfdcEntryConfigWarning(("walkPath : " + inputfile), logger);
	        File folder = new File(inputfile);
	        String fileName = "";
	        String suffix = "";
	        if (folder.isFile()) {
	            this.ffdc.addFfdcEntryConfigWarning(" folder " + folder,this.logger);
	            scanner = this.openFile(folder.getAbsolutePath());
	            fileName = folder.getAbsoluteFile().getName();
	            int startSuffix = fileName.indexOf('.');
	            if (startSuffix > 0) {
	                suffix = fileName.substring(fileName.indexOf('.') + 1,
	                        fileName.length());
	            }
	            newText = readClass.processFile(scanner, fileName, suffix);
	            rval = this.updateFile(folder.getAbsolutePath(), newText);
	            if (rval == false) {
	                this.ffdc.addFfdcEntry("\n\n Update result Failed " + rval, this.logger);
	                this.ffdc.errorExit(42, this.logger);
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
	                    newText = readClass.processFile(scanner, fileName, suffix);
	                    rval = this.updateFile(
	                            inputfile + listOfFiles[i].getName(), newText);
	                    if (rval == false) {
	                        this.ffdc.addFfdcEntry("\n\n Update result Failed " + rval, this.logger);
	                    }
	                } else if (listOfFiles[i].isDirectory()) {
	                    this.ffdc.addFfdcEntryConfigWarning("Process subdirs "+ listOfFiles[i].getName(),this.logger);
	                    rval = this.walkPath(listOfFiles[i].getAbsolutePath() + "/");
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
	            this.ffdc.addFfdcEntry("Unable to open existing file   "
	                    + e.getMessage(),this.logger);
	        }
	        this.ffdc.addFfdcEntryConfigWarning("Opened file " + inputfile, this.logger);
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
	            this.ffdc.addFfdcEntry("Unable to create new file   " + e.getMessage(), this.logger);
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
	    private final  FfdcControls ffdc;

	}
