package removal;

import java.util.logging.LogManager;
import java.util.logging.Logger;

public class WalkFiles {
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        LogManager lm = LogManager.getLogManager();
        Logger logger;
        logger = Logger.getLogger("WalkFiles_Logging");
        FileUtils fUtil = new FileUtils(logger);
        lm.addLogger(logger);
        logger.entering("WalkFiles", "main");

        if (args.length != 1) { // user told us the ffdc level to use This is
                                // parm number three and is optional
            logger.exiting("walkFiles",
                    "main, requires single PARM: path to files");
            System.exit(0);
        }
        String inputfile = args[0];
        logger.info(" Directory " + inputfile);
        boolean updateRval = fUtil.walkPath(inputfile);
        if (updateRval == false) {
            logger.severe("\n\n Update result Failed " + updateRval);
        }
        logger.info("Exiting");

    }

}