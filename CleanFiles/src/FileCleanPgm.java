
import java.util.logging.LogManager;
import java.util.logging.Logger;

import utilities.FfdcControls;

// ARGS   /home/taarts/workspace/CleanFiles/Tests/TabSpaceResult

public class FileCleanPgm {
	   public static void main(String[] args) {
	        // TODO Auto-generated method stub

	        LogManager lm = LogManager.getLogManager();
	        Logger logger;
	        logger = Logger.getLogger("FileCleanPgm_Logging");

	        lm.addLogger(logger);
	        logger.entering("FileCleanPgm", "main");

	        if (args.length == 0) { // user told us the ffdc level to use This is
	                                // parm number three and is optional
	            logger.exiting("walkpath",
	                    "main, requires one PARM: full path to file");
	            System.exit(0);
	        }
	        String inputfile = args[0];
	        int ffdcControlLevel = 0;

	        if (args.length > 1) { // user told us the ffdc level to use This is
	                                // parm number three and is optional
	            ffdcControlLevel = Integer.parseInt(args[1]);
	        }
	        FfdcControls ffdc = new FfdcControls(ffdcControlLevel);
	        FileUtils fUtil = new FileUtils(logger, ffdc);
	        
	        ffdc.addFfdcEntryConfigWarning(" Directory " + inputfile, logger);
	        boolean updateRval = fUtil.walkPath(inputfile);
	        if (updateRval == false) {
	        	ffdc.addFfdcEntry("\n\n Update result Failed " + updateRval, logger);
	        }
	        ffdc.displayFfdcEntries(logger);
	        if(ffdcControlLevel > 1){
	        	ffdc.displayFfdcConfigWarningEntries(logger);
	        }
	        logger.exiting("Exiting"," 0");

	    }

	}

