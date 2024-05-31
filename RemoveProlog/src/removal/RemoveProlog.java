package removal;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Logger;

public class RemoveProlog {

    public RemoveProlog() {
        super();
        // TODO Auto-generated constructor stub
    }

    public RemoveProlog(Scanner openFile) {
        super();
        // TODO Auto-generated constructor stub
    }

    public RemoveProlog(Logger logger) {
        super();
        this.logger = logger;

    }

    public ArrayList<String> removeProlog(Scanner scanner, String fileName,
            String suffix) {
        ArrayList<String> newText = new ArrayList<String>();
        String line = "default";

        Boolean keepLine = true;
        Boolean inChangeLog = false;
        Boolean withInSlashSplat = false;
        Boolean inActivityLog = false;
        Boolean copyR = false;
        Boolean withInSlashSlash = false;
        logger.getFilter();
        String pLog = "";
        this.logger.info("entered removeProlog  file " + fileName + " suffix "
                + suffix);
        if ((suffix.endsWith("py")) || (fileName.equals("Makefile"))) {
            pLog = "#*************************************************************************************** \n"
                    + "#        Change Log:  \n"
                    + "# Flag  Date        Owner      Defect     Description \n"
                    + "# ----  ----------  ---------  --------   -------------------------------------------- \n"
                    + "#                                         New part                                       \n"
                    + "#  \n" + "#  \n" + "#  \n";
        } else if ((suffix.endsWith("c")) || (suffix.endsWith("C"))
                || (suffix.endsWith("cc")) || (suffix.endsWith("h"))
                || (suffix.endsWith("hh")) || (suffix.endsWith("H"))
                || (suffix.endsWith("dml"))) {
            pLog = "//*************************************************************************************** \n"
                    + "//        Change Log:  \n"
                    + "// Flag  Date        Owner      Defect     Description \n"
                    + "// ----  ----------  ---------  --------   -------------------------------------------- \n"
                    + "//                                         New part                                       \n"
                    + "//  \n" + "//  \n" + "//  \n";
        }
        // newText.add(pLog); // No longer add any change logs

        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            // this.logger.info("keepLine " + keepLine + " line is : " + line);
            if ((line.contains("IBM_PROLOG_BEGIN_TAG") || line
                    .contains("// Copyright")
                    && (line.contains("// Copyright 2005-2008 Virtutech AB") == false))) {
                // this.logger.fine("found start");
                keepLine = false;
                copyR = true;
            }
            if (line.contains("Change Log") && (inChangeLog == false)) {
                // this.logger.fine("found start of change log");
                keepLine = false;
                inChangeLog = true;
                if (line.contains("/*")) {
                    withInSlashSplat = true;
                }
                if (line.contains("//")) {
                    withInSlashSlash = true;
                }

            }
            if (line.contains("/* Change Activity: */")
                    && (inActivityLog == false)) {
                // this.logger.fine("found start of change log");
                keepLine = false;
                inActivityLog = true;
                if (line.contains("/*")) {
                    withInSlashSplat = true;
                }
                if (line.contains("//")) {
                    withInSlashSlash = true;
                }
            }

            /*
             * if ((line.contains("IBM_PROLOG_END_TAG")) ||
             * (line.contains("// End Copyright")) ){
             * //this.logger.info("found start"); keepLine = true; }
             */
            if ((withInSlashSlash == true) && (line.contains("//") == false)) {
                // this.logger.info("found start");
                keepLine = true;
                withInSlashSlash = false;
            }
            if ((copyR == false) && (withInSlashSplat == false)
                    && (inActivityLog == false)
                    && (line.contains("//") == false)
                    && (line.contains("#") == false)
                    && (line.contains("*") == false)
                    && (line.contains("Change Log") == false)
                    && (line.contains("End Change Log") == false)) {
                // this.logger.info("found start");
                keepLine = true;
                inChangeLog = false;
                line = line.replace("\t", "    ");
                newText.add(line);
            } else if (keepLine) {
                if ((line.contains("/* Change Activity: */") == false)
                        && (line.contains("/* End Change Activity */") == false)
                        && (line.contains("LAST_VERSION_FROM_CC") == false)
                        && (line.contains("// $Source:") == false)
                        && (line.contains("# $Source:") == false)) {
                    // remove any TABs
                    line = line.replace("\t", "    ");
                    newText.add(line);
                }
            }
            if (line.contains("/* End Change Activity */")) {
                // this.logger.info("found end change actttt");
                keepLine = true;
                inActivityLog = false;
                withInSlashSplat = false;
                withInSlashSlash = false;
            }
            if ((line.contains("IBM_PROLOG_END_TAG"))
                    || (line.contains("// End Copyright"))) {
                // this.logger.info("found start");
                keepLine = true;
                copyR = false;
            }
            if ((withInSlashSplat == true) && (line.contains("*/"))) {
                // this.logger.info("found start");
                keepLine = true;
                withInSlashSplat = false;
            }

        }

        return (newText);

    }

    private Logger logger;

}