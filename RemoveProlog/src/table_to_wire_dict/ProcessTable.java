
package table_to_wire_dict;

//  Usage: java -jar ../../java/jars/ProcessTable.jar  p8_tuleta_tables dict_results debug
//
//  PGM table_to_wire_dict.ProcessTable
//  Args   src/machine_config_tables/foo_machine_tables
//         dict_source
//         debug
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import utilities.FfdcControls;
import utilities.MachineXTableData;

/*
 *
 *
 *
 *   #################
 *   # ProcessTable  #        #####################
 *   #            -> # >>>>>> # FfdcControls      #
 *   #               #        #                   #
 *   #               #        # List list         #
 *   #               #        # List warnList     #
 *   #               #        #                   #
 *   #               #        #####################
 *   #               #
 *   #               #        #########################
 *   #            -> # >>>>>> # MachineXTableData     #
 *   #               #        #                       #
 *   #               #        # Map dataset           #
 *   #               #        # Map cellDataset       #
 *   #               #        # Map cellForceDataset  #
 *   #               #        #                       #
 *   #               #        #                       #
 *   #               #        #                       #
 *   #               #        #                       #
 *   #               #        #########################
 *   #  Map dataset  #
 *   #               #
 *   #################
 *
 *
 *
 *
 *  >>>>    ProcessTable
 *
 *   Consumes a defined file format of slave and master definition. Optionally the file may also contain cell information.
 *
 *   Throughout the code an FFDC class instance is used. This instance  is continually updated
 *   with mundane details and important warnings. The mundane are viewed by executing with the
 *   Third parm set to 1 or 3
 *   The warning messages are always displayed at the completion of the program execution.
 *
 *   FfdcControls.class
 *   The FFDC object can also be called with an the option of exiting the program. The
 *   user would operate this way to stop on first error while debugging a new input file.
 *   The program will exit in this manner when the third parm passed to ProcessTable includes 4
 *
 *  >>>>>    MachineXTabledata class
 *   During program execution the data set created from the input file is stored in the MachineXTabledata class.
 *   The class instance is used throughout the program. This class internally stores the data in
 *   a private dictionary format, based on a majorKey (cmp:).  This separate object is used as
 *   a placeholder for more data conversion. If a future data format is used this class would
 *   be replaced with a different behavior.
 *    All information in these Maps was retreived from the input dictionary_table file
 *        Map dataset           Map containing data representing each slave and master entry
 *        Map cellDataset       Map of cell information used when entries contain the cell_assign key.
 *        Map cellForceDataset  Map of cell information used on a component basis.
 *
 *
 *   >>>>>   ProcessTable.class
 *   Operation steps.
 *  1) validates the parm one input file line by line. The line by line processing uses validateRecordKeys to ensure required keys are present and
 *  incompatible keys are not present.
 *
 *    Part of this operation includes the processing by method do_load_dict. This method reads in dictionary files from two possible
 *  locations, the file contents are added to the users parm one input file.
 *
 *  The line information is used to create a dictionary entry, that entry is added to the MachineXTabledata instance:
 *     a) Line contains slave or master  the data is stored in the dataset.
 *     b)  Line with cell_detail stored in celldataset
 *     c)  Line with cell_force_cmp stored in cellForceDataset.
 *     d)  Line with cell_force_cmp_default stored in cellForceDataset.
 *
 *  2) Walk through the MachineXTabledata dictionary dataset, for each entry attempt to locate the master entry
 *       An i2c slave should be associated to an i2c master.
 *       Slaves that are not associated with a master will result in an FFDC entry
 *       Masters not associated with slaves are not logged as an FFDC entry
 *       Regardless of whether a master is located, each entry becomes a valid dictionary key/value.
 *       Slave with  Master associations also create a 'crossing' dictionary entry.
 *       Details stored in dictionary dataSet contained in class ProcessTable.
 *
 *  3) Walk the dictionary dataSet contained in class ProcessTable, creating a buffer of the complete dictionary that represent the
 *  masters, slaves and applicable crossing details.
 *
 *  4) The buffer is then copied in the file identified
 *  as parm two, this will create valid python dictionaries wire{} and cellInfo{}.
 *
 *
 * Uses this.dataSet to contain the near final dictionary source code.  These key/values use the keys required by
 *  the wireManager.
 *  Data   string(cmp) : { unique_name1:{key/value, key:value...}, unique_name1:{key/value, key:value...}}
 *
 * The term inner or inside map refers to  outer_name:{unique_name1:{key/value, key:value...}, unique_name1:{key/value, key:value...}}
 *
 * This data does not contain all the proper ' and colons so a final processing step is required.  This is comprised of a number
 * of string commands to morph the string into dictionary format.
 *
 */

public class ProcessTable {

    public ProcessTable() {
        this.dataSet = new TreeMap<String, Object>();
        HashMap<String, HashMap<String, String>> cloud = new HashMap<String, HashMap<String, String>>();
        this.dataSet.put("wiringBlock0", cloud); // by default create this major
                                                    // key for the crossing
                                                    // wires
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        // Logger logger = Logger.getLogger("ibm.simics.ProcessTable");
        /*
         * Properties props = new java.util.Properties(); FileInputStream fis =
         * new FileInputStream("table_wire.properties");
         *
         * props.load(fis);
         */
        LogManager lm = LogManager.getLogManager();
        Logger logger;

        logger = Logger.getLogger("LoggingExample1");
        lm.addLogger(logger);

        // lm.readConfiguration(new FileInputStream("table_wire.properties"));
        // at the present time a properties file is not used
        // logger.info("pwd " + System.getProperty("user.dir"));
        // System.out.println(props.getProperty(".level"));

        // name package process_table
        // project wiring_utils
        // class .table_to_wire_dict.ProcessTable
        // src/machine_config_tables/foo_machine_tables dict_source none 1 2 or
        // 3
        logger.info("enter: ProcessTable");
        if (args.length < 2) {
            String strBuf = new String();
            strBuf += ("Usage: inputFile,  outputFile,  ffdcLevel \n");
            strBuf += ("inputFile: contains the configuration data \n");
            strBuf += ("outputFile:  Will contain the resultant dictionary source \n");
            strBuf += ("ffdcLevel   NO_FFDC = 0,  Upon completion enterDetailsInfile=.ffdc_data.text : 1, Display FFDC data : 2, Exit when error encountered : 4, Immediate display : 8  \n");
            strBuf += (" java -jar ../../modules/fsp/java/jars/ProcessTable.jar  dictionary_table dict_results.py \n");
            logger.info(strBuf);
            return;
        }
        String ffdcControlLevel = "0";

        if (args.length > 2) { // user told us the ffdc level to use This is
                                // parm number three and is optional
            ffdcControlLevel = args[2];
        }
        String inputfile = args[0];
        String outDictSource = args[1];
        String shadow_name = inputfile;
        int last_slash = shadow_name.lastIndexOf("/");
        String first_part = shadow_name.substring(0, last_slash + 1);
        String last_part = shadow_name.substring(last_slash + 1,
                shadow_name.length());
        String final_shadow_name = first_part + "." + last_part;
        logger.info(final_shadow_name);
        try {
            File file = new File(final_shadow_name);
            file.delete(); // delete the old hidden file
        } catch (Exception e) {
            e.printStackTrace();
        }
        PrintWriter shadow_dict_table = new PrintWriter(new FileWriter(
                final_shadow_name));
        shadow_dict_table.println("\n # Expanded dictionary_table " + inputfile
                + "\n");

        FfdcControls ffdc = new FfdcControls(ffdcControlLevel);
        logger.info("Parms: input " + inputfile + " outFile " + outDictSource
                + "  ffdc level   " + ffdcControlLevel);
        ffdc.addFfdcEntry(("Parms: input " + inputfile + " outFile "
                + outDictSource + "  ffdc level   " + ffdcControlLevel), logger);
        ProcessTable thisClass = new ProcessTable();
        MachineXTableData dict_out = new MachineXTableData(outDictSource);
        HashMap<String, String> detailedMap = new HashMap<String, String>();
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(inputfile)); // parm number one....
        } catch (Exception e) {
            logger.info("Exception reading file: " + e.getMessage() + " file: "
                    + inputfile);
            logger.info("Failed reading file: last line read  " + inputfile);
            ffdc.addFfdcEntryConfigWarning(
                    ("Failed reading file: " + inputfile), logger);
        }

        String word = "default";
        ffdc.addFfdcEntry("Starting operation of reading file", logger);
        List<String> list = new ArrayList<String>();
        List<String> load_list = new ArrayList<String>(); // contain records
                                                            // pulled from other
                                                            // dictionary files
        while (scanner.hasNextLine()) {
            String list_word = scanner.nextLine();
            list.add(list_word + "\n");
        }
        scanner.close();
        try {
            load_list = thisClass.do_load_dict(list, "load_cmp", logger, ffdc);
            list.addAll(load_list);

            load_list = thisClass
                    .do_load_dict(list, "load_local", logger, ffdc);
            list.addAll(load_list);

            // now we have the complete dictionary table so process each
            // line....
            Iterator<String> iter = list.iterator();
            while (iter.hasNext()) {
                boolean docEntryFound = false;
                String shadow = "";
                String savedDoc = "";
                word = iter.next(); // scanner.nextLine();
                String wordTemp = word;
                wordTemp = wordTemp.replaceAll(" ", ""); // remove space
                wordTemp = wordTemp.replaceAll("\\s", ""); // remove white space
                // see if a doc: entry exist, if so, save the contents to
                // replace them after other data morphing has completed
                // this is done or else the documentation value looses it spaces
                // and usefulness
                if ((wordTemp.indexOf("|doc:") != -1)
                        || (wordTemp.indexOf("doc:") != -1)) {
                    docEntryFound = true;
                    int docStart = word.indexOf("doc");
                    String docPlus = word.substring(docStart);
                    String cleanDoc = docPlus.replaceAll(" ", "");
                    if (cleanDoc.indexOf("doc:") != -1) {
                        int bar = docPlus.indexOf("|");
                        if (bar == -1) {
                            bar = docPlus.indexOf(">");
                        }
                        savedDoc = word.substring(docStart + 5, docStart + bar);
                        savedDoc = savedDoc.replaceAll(" ", "-");
                    }
                }
                // commas and single quotes. Map each of these to an oddball
                // character so we can restore them later
                // see the line is a comment and we should ignore or we must
                // load another dictionary file

                if (wordTemp.indexOf('#') == 0) {
                    logger.finer("Do not strip characters word: " + word);
                } else {
                    logger.finer("1 strip characters word: " + word);
                    word = word.replaceAll(" ", ""); // remove space
                    word = word.replaceAll("\\s", ""); // remove white space
                    word = word.replaceAll("\'", "#"); // ' become #
                    word = word.replaceAll(",", "&"); // , become &
                    logger.finer("2 strip characters word: " + word);
                }
                logger.fine("word " + word);

                if (word.isEmpty()) {
                    continue;
                }
                if (wordTemp.indexOf('#') == 0) { // test the temporary input
                                                    // line in which we removed
                                                    // spaces
                    ffdc.addFfdcEntry(("Comment in input file :" + word),
                            logger);
                } else {
                    detailedMap = thisClass.partitionLine(word, logger, ffdc); // return
                                                                                // dictionary
                                                                                // of
                                                                                // key/values
                                                                                // gleaned
                                                                                // from
                                                                                // the
                                                                                // line
                                                                                // read
                                                                                // from
                                                                                // the
                                                                                // input
                                                                                // file
                    String key = thisClass.createRecordKey(detailedMap, logger,
                            ffdc); // you guessed it, compose a unique key for
                                    // putting the data into the internal
                                    // dictionary.
                    Boolean tested = thisClass.validateRecordKeys(detailedMap,
                            logger, ffdc); // test the required key exist and no
                                            // conflicting key exist
                    if (tested) {
                        // put any saved doc entry record into the detailed
                        // entry
                        if (docEntryFound) {
                            detailedMap.put("doc", savedDoc);
                        }
                        // if this is a cell info record, place it in a unique
                        // dictionary
                        if (detailedMap.containsKey("cell_detail")) {
                            for (Map.Entry<String, String> entry : detailedMap
                                    .entrySet()) {
                                shadow += " | " + entry.getKey() + " : "
                                        + entry.getValue();
                            }
                            thisClass.updateCellDict(key, detailedMap,
                                    dict_out, logger, ffdc); // store data in
                                                                // the
                                                                // MachineXTableData
                                                                // object
                        } else if (detailedMap.containsKey("cell_force_cmp")) {
                            for (Map.Entry<String, String> entry : detailedMap
                                    .entrySet()) {
                                shadow += " | " + entry.getKey() + " : "
                                        + entry.getValue();
                            }
                            thisClass.updateCellForceDict(key, detailedMap,
                                    dict_out, logger, ffdc); // store data in
                                                                // the
                                                                // MachineXTableData
                                                                // object
                        } else if (detailedMap
                                .containsKey("cell_force_cmp_default")) {
                            for (Map.Entry<String, String> entry : detailedMap
                                    .entrySet()) {
                                shadow += " | " + entry.getKey() + " : "
                                        + entry.getValue();
                            }
                            thisClass.updateCellForceDict(key, detailedMap,
                                    dict_out, logger, ffdc); // store data in
                                                                // the
                                                                // MachineXTableData
                                                                // object
                            thisClass.updateCellForceDictDefault(key,
                                    detailedMap, dict_out, logger, ffdc); // store
                                                                            // data
                                                                            // in
                                                                            // the
                                                                            // MachineXTableData
                                                                            // object
                        } else if (detailedMap.containsKey("load_cmp")
                                || detailedMap
                                        .containsKey("load_dictionary_table")) {
                            ffdc.addFfdcEntry(
                                    ("Skip this entry, do not add to MachineXTableData   " + detailedMap),
                                    logger);
                        } else if (detailedMap.containsKey("dup")) { // create
                                                                        // new
                                                                        // records
                                                                        // based
                                                                        // on
                                                                        // the
                                                                        // current
                                                                        // detailedMap
                            String stringDup = detailedMap.get("dup");
                            try {
                                int intDup = Integer.parseInt(stringDup);
                                for (int c = 0; c < intDup; c++) {
                                    String cmpName = detailedMap.get("cmp");
                                    String cmpNum = Integer.toHexString(c);
                                    if (cmpName.contains("*")) {
                                        cmpName = cmpName.replace("*", cmpNum);
                                    } else {
                                        cmpName = cmpName + cmpNum;
                                    }
                                    shadow += " | " + "cmp : " + cmpName;
                                    HashMap<String, String> detailedMapCopy = new HashMap<String, String>();
                                    for (Map.Entry<String, String> entry : detailedMap
                                            .entrySet()) {
                                        detailedMapCopy.put(entry.getKey(),
                                                entry.getValue());
                                        if ((entry.getKey().compareTo("dup") != 0)
                                                && (entry.getKey().compareTo(
                                                        "cmp") != 0)) {
                                            shadow += " | " + entry.getKey()
                                                    + " : " + entry.getValue();
                                        }
                                    }
                                    if (detailedMapCopy.containsKey("far_cmp")) {
                                        String farCmpStr = detailedMapCopy
                                                .get("far_cmp");
                                        if (farCmpStr.contains("*")) {
                                            farCmpStr = farCmpStr.replace("*",
                                                    cmpNum);
                                        } else {
                                            farCmpStr = farCmpStr + cmpNum;
                                        }
                                        shadow += " | " + "far_cmp : "
                                                + farCmpStr;
                                        detailedMapCopy.put("far_cmp",
                                                farCmpStr);
                                    }
                                    if (shadow.isEmpty() == false) {
                                        if (shadow.indexOf('|') == 1) {
                                            shadow = shadow.substring(2);
                                        }
                                        shadow_dict_table.println("< " + shadow
                                                + " > \n");
                                        shadow = "";
                                    }
                                    detailedMapCopy.put("cmp", cmpName);
                                    String newKey = cmpName; // use the name
                                                                // that
                                                                // optionally
                                                                // used '*'
                                    thisClass.updateDict(newKey,
                                            detailedMapCopy, dict_out, logger,
                                            ffdc); // store data in the
                                                    // MachineXTableData object
                                    // update shadow

                                }
                            } catch (NumberFormatException nfe) {
                                logger.info("NumberFormatException: "
                                        + nfe.getMessage());
                                ffdc.addFfdcEntryConfigWarning(
                                        ("NumberFormatException: "
                                                + nfe.getMessage()
                                                + "\n entries " + detailedMap),
                                        logger);
                                ffdc.errorExit(50, logger);
                            }
                        } else {
                            thisClass.updateDict(key, detailedMap, dict_out,
                                    logger, ffdc); // store data in the
                                                    // MachineXTableData object
                            // regular entry, add to shadow
                            for (Map.Entry<String, String> entry : detailedMap
                                    .entrySet()) {
                                shadow += " | " + entry.getKey() + " : "
                                        + entry.getValue();
                            }
                        }
                    } else { // validation failed
                        if (!detailedMap.isEmpty()) {
                            ffdc.addFfdcEntryConfigWarning(
                                    ("validation failed for  " + detailedMap),
                                    logger);
                            ffdc.errorExit(51, logger);
                        }
                    }
                    if (shadow.indexOf('|') == 1) {
                        shadow = shadow.substring(2);
                    }
                    if (shadow.isEmpty() == false) {
                        shadow_dict_table.println("< " + shadow + " > \n");
                    }
                }
            }
        } catch (Exception e) {
            // Print out the exception that occurred
            logger.info("Exception reading file: " + e.getMessage());
            logger.info("Failed reading file: last line read  " + word);
            ffdc.addFfdcEntryConfigWarning(
                    ("Failed reading file: last line read  " + word), logger);
            ffdc.errorExit(52, logger);
        }
        // dict_out.display(logger, ffdc);
        // / now shuffle the data contained in the MachineXTableData to create
        // the dictionary information
        thisClass.createDictData(thisClass, dict_out, logger, ffdc);
        thisClass.showResults(logger, ffdc);
        String dataToProcess = "" + thisClass.dataSet;
        try {
            PrintWriter sourceFile = new PrintWriter(new FileWriter(
                    outDictSource));
            thisClass.getDictCodeFormat("wire=", dataToProcess, sourceFile,
                    logger, ffdc);
            // now get the cell information and create its own dictionary and
            // add to the file
            String cellForceDataToProcess = ""
                    + dict_out.get_cell_force_entries(logger, ffdc);
            thisClass.getDictCodeFormat("cellInfo=", cellForceDataToProcess,
                    sourceFile, logger, ffdc);
            sourceFile.close();
        } catch (Exception e) {
            // Print out the exception that occurred
            logger.info("ERROR: Unable to accomplish the file update   "
                    + e.getMessage());
            ffdc.addFfdcEntryConfigWarning(
                    ("ERROR: Unable to accomplish the file update   " + e
                            .getMessage()), logger);
            ffdc.errorExit(53, logger);

        }
        shadow_dict_table.close();
        ffdc.displayFfdcEntries(logger);
        ffdc.displayFfdcConfigWarningEntries(logger);
        logger.info("Processing completed...");
        // scanner.close( );
    }

    // Read trough all lines of the users dictionary_tabl file. If any line is a
    // command to load a
    // dictionary file local to the target just copy in the file.
    // If the command requests loading a components file, it is found in the
    // directory ../comp_dictionary_tables
    // (simics/targets/comp_dictionary_tables/blah) AND the users load_cmp : X
    // value is used in the cmp: ?? replacement data
    // When the user request load_local, the file is expected within the same
    // target diectory
    // returns a list of the line from the new files. The caller is responsible
    // to add these lines to the
    // complete list of dictionary details.
    private List<String> do_load_dict(List<String> list, String key_name,
            Logger logger, FfdcControls ffdc) {
        List<String> load_list = new ArrayList<String>(); // contain records
                                                            // pulled from other
                                                            // dictionary files

        ffdc.addFfdcEntry(this.dataSet.toString(), logger);
        Iterator<String> load_iter = list.iterator();
        boolean found_key = false;
        while (load_iter.hasNext()) {
            String load_savedLoad = "";
            String savedCmp = "";
            String load_word = load_iter.next(); // scanner.nextLine();
            String load_wordTemp = load_word;
            load_wordTemp = load_wordTemp.replaceAll(" ", ""); // remove space
            load_wordTemp = load_wordTemp.replaceAll("\\s", ""); // remove white
                                                                    // space
            // If a comment line igore it, if a specialload line, it begins and
            // with < ......>
            // and the format <key_name or |key_name
            if (load_wordTemp.indexOf('#') == 0) {
                ffdc.addFfdcEntry(" comment line  : " + load_wordTemp + "\n",
                        logger);
            } else if (((load_wordTemp.indexOf("<") == 0) && (load_wordTemp
                    .indexOf(">") == load_wordTemp.length() - 1))
                    && ((load_wordTemp.indexOf("|" + key_name + ":") != -1) || (load_wordTemp
                            .indexOf("<" + key_name) != -1))) {
                found_key = true;
                ffdc.addFfdcEntry(" Search found key : " + key_name, logger);
                int loadStart = load_word.indexOf("load_dictionary_table");
                String loadPlus = load_word.substring(loadStart);
                String cleanLoad = loadPlus.replaceAll(" ", "");
                if (cleanLoad.indexOf("load_dictionary_table:") != -1) {
                    int bar = loadPlus.indexOf("|");
                    if (bar == -1) {
                        bar = loadPlus.indexOf(">");
                    }
                    load_savedLoad = load_word.substring(loadStart + 23,
                            loadStart + bar);
                    load_savedLoad = load_savedLoad.replaceAll(" ", "");
                    ffdc.addFfdcEntry(
                            ("Found key load_dictionary_table for file " + load_savedLoad),
                            logger);
                } else {
                    ffdc.addFfdcEntryConfigWarning(
                            ("Failed to Find key load_dictionary_table "),
                            logger);
                }
                // now get the component name
                int cmpStart = load_word.indexOf(key_name);
                String cmpPlus = load_word.substring(cmpStart);
                String cleanCmp = cmpPlus.replaceAll(" ", "");
                if (cleanCmp.indexOf(key_name) != -1) {
                    int cmpBar = cmpPlus.indexOf("|");
                    if (cmpBar == -1) {
                        cmpBar = cmpPlus.indexOf(">");
                    }
                    savedCmp = load_word.substring(cmpStart + key_name.length()
                            + 2, cmpStart + cmpBar);
                    savedCmp = savedCmp.replaceAll(" ", "");
                }
                ffdc.addFfdcEntry(("Found key " + key_name
                        + " for replacement use: " + savedCmp), logger);
                String currentDir = System.getProperty("user.dir");
                // "/gsa/ausgsa/home/t/a/taarts/dev2000/aix/sand_dev2000/simics/targets/conventions_example";
                // // System.getProperty("user.dir");
                String load_file = currentDir
                        + "/targets/comp_dictionary_tables/" + load_savedLoad;
                if (key_name.equals("load_local")) {
                    load_file = currentDir + "/" + load_savedLoad;
                }
                try {
                    Scanner load_scanner = new Scanner(new File(load_file)); // parm
                                                                                // number
                                                                                // one....
                    // Scanner load_scanner =
                    ffdc.addFfdcEntry("Starting operation of reading file "
                            + load_file, logger);
                    while (load_scanner.hasNextLine()) {
                        String load_word_data = load_scanner.nextLine();
                        // ok, so find the cmp: key and replace its value with
                        // savedCmp
                        if (key_name.equals("load_cmp")) {
                            load_word_data = load_word_data.replace("REPLACE",
                                    savedCmp);
                        }
                        load_list.add(load_word_data + "\n");
                    }
                    load_scanner.close();
                } catch (Exception e) {
                    // Print out the exception that occurred
                    logger.info("Exception reading file: " + e.getMessage()
                            + " file: " + load_file);
                    logger.info("Failed reading file: last line read  "
                            + load_word);
                    ffdc.addFfdcEntryConfigWarning(("Failed reading file: "
                            + load_file + " last line read  " + load_word),
                            logger);
                    ffdc.errorExit(152, logger);
                }
            }
        }
        if (found_key == false) {
            ffdc.addFfdcEntry("Did not find do_load_dict entry for : "
                    + key_name, logger);
        }
        return (load_list);
    }

    private int showResults(Logger logger, FfdcControls ffdc) {
        int rc = 0;
        ffdc.addFfdcEntry(this.dataSet.toString(), logger);
        return (rc);
    }

    // Last processing of the contained dictionary this.dataSet. At this point
    // the dictionary
    // contains the required keys, but the syntax is not correct as far as
    // 'punctuation'. This
    // method will flip = to :, commas around string etc.....
    // Sorry but the use of REGEX was required and its syntax certainly is
    // different.
    //
    private String getDictCodeFormat(String dictName, String thisData,
            PrintWriter out, Logger logger, FfdcControls ffdc) {
        ffdc.addFfdcEntry(("enter finalFormatFile file  " + out), logger);
        String beginingData = thisData;
        String rString = "";
        // remove all spaces
        // transform all = to :
        // After every { put a '
        // After every : put a '
        // Around every , ','
        // Before every : put a '
        // Before every } put a '
        // pattern }, become },'
        // This leaves a faulty ' name:{'name became name:'{'name
        // so pattern :'{ becomes :{
        // how to handle user data ???????????
        // logger.info("1 begin " + beginingData);
        beginingData = beginingData.replaceAll(" ", ""); // remove space
        beginingData = beginingData.replaceAll("=", ":"); // = become :

        beginingData = beginingData.replaceAll(":'\\{", ":\\{"); // remove extra
                                                                    // quote :'{
                                                                    // becomes
                                                                    // :{
        // inject returns and comassssssss
        // also, seems our java REGEX fails to work for alphaNumberics in one
        // group, so I broke that into alpha and numeric groups
        try {
            beginingData = beginingData.replaceAll("([0-9]{1,})\\}", "$1'\\}"); // alpaNum}
                                                                                // alphaNum'}
            beginingData = beginingData.replaceAll("([a-zA-Z]{1,})\\}",
                    "$1'\\}"); // alpaNum} alphaNum'}
            beginingData = beginingData.replaceAll("\\{([0-9]{1,})", "\\{'$1"); // {alpaNum
                                                                                // {'alphaNum
            beginingData = beginingData.replaceAll("\\{([a-zA-Z]{1,})",
                    "\\{'$1"); // {alpaNum {'alphaNum

            beginingData = beginingData
                    .replaceAll("\\},([0-9]{1,})", "\\},'$1"); // },alpaNum
                                                                // },'alphaNum
            beginingData = beginingData.replaceAll("\\},([a-zA-Z]{1,})",
                    "\\},'$1"); // },alpaNum },'alphaNum
            beginingData = beginingData.replaceAll("([0-9]{1,}):", "$1':"); // alpaNum:
                                                                            // alphaNum':
            beginingData = beginingData.replaceAll("([a-zA-Z]{1,}):", "$1':"); // alpaNum:
                                                                                // alphaNum':
            beginingData = beginingData.replaceAll(":([0-9]{1,})", ":'$1"); // :alpaNum
                                                                            // :'alphaNum
            beginingData = beginingData.replaceAll(":([a-zA-Z]{1,})", ":'$1"); // :alpaNum
                                                                                // :'alphaNum
            beginingData = beginingData.replaceAll("([0-9]{1,}),([0-9]{1,})",
                    "$1','$2"); // alphaNum,alphaNum alphaNum,alphaNum
            beginingData = beginingData.replaceAll(
                    "([a-zA-Z]{1,}),([a-zA-Z]{1,})", "$1','$2"); // alphaNum,alphaNum
                                                                    // alphaNum,alphaNum
            beginingData = beginingData.replaceAll(
                    "([0-9]{1,}),([a-zA-Z]{1,})", "$1','$2"); // alphaNum,alphaNum
                                                                // alphaNum,alphaNum
            beginingData = beginingData.replaceAll(
                    "([a-zA-Z]{1,}),([0-9]{1,})", "$1','$2"); // alphaNum,alphaNum
                                                                // alphaNum,alphaNum

            beginingData = beginingData.replaceAll(
                    "([a-zA-Z0-9]{1,})'\\}\\},'([a-zA-Z0-9]{1,})",
                    "$1'\\},\\},  \n           '$2"); // alphaNum'}'},alphaNum
                                                        // alphaNum'},}, \n
                                                        // alphaNum
            // logger.info("1 begin " + beginingData);

            beginingData = beginingData.replaceAll("([0-9]{1,}),(\\[{1,})",
                    "$1',$2"); // alphaNum,[ alphaNum,'[ ta_v4
            beginingData = beginingData.replaceAll("([a-zA-Z]{1,}),(\\[{1,})",
                    "$1',2"); // alphaNum,[ alphaNum,'[ ta_v4
            beginingData = beginingData.replaceAll("(\\]{1,}),([a-zA-Z]{1,})",
                    "$1','$2"); // ],alphaNum ]','alphaNum ta_v4
            beginingData = beginingData.replaceAll("(\\]{1,}),([0-9]{1,})",
                    "$1','$2"); // ],alphaNum ]','alphaNum ta_v4
            beginingData = beginingData.replaceAll(
                    "(\\]{1,},),('[a-zA-Z]{1,})", "$1','$2"); // ],'alphaNum
                                                                // ]','alphaNum
                                                                // ta_v4
            beginingData = beginingData.replaceAll("(\\]{1,},),('[0-9]{1,})",
                    "$1','$2"); // ],'alphaNum ]','alphaNum ta_v4
            // logger.info("1.5 begin " + beginingData);
            beginingData = beginingData.replaceAll(":\\[", ":'\\["); // :[
                                                                        // becomes
                                                                        // :'[,
            beginingData = beginingData.replaceAll("&", ","); // & become ,
            beginingData = beginingData.replaceAll("#", ""); // # become ,
            beginingData = beginingData.replaceAll(":%", ":'%"); // :% become
                                                                    // :'%

            // logger.info("3 begin " + beginingData);

        } catch (Exception e) {
            // Print out the exception that occurred
            logger.info("Unable to accomplish the replace   " + e.getMessage());
            ffdc.addFfdcEntryConfigWarning(
                    ("Unable to accomplish the replace  " + beginingData),
                    logger);
            ffdc.errorExit(55, logger);

        }
        // walk the data and find minor key records, insert \n between them
        beginingData = beginingData.replaceAll("'},'",
                "'},   \n                            '"); // break minor keys to
                                                            // individual lines
        beginingData = dictName + beginingData;
        rString = beginingData;
        try {
            String warning = "#########################################################################################################################\n"
                    + "#     ######            #     #                 #######                                                                  \n"
                    + "#     #     #  ####     ##    #  ####  #####    #       #####  # #####                                                   \n"
                    + "#     #     # #    #    # #   # #    #   #      #       #    # #   #                                                     \n"
                    + "#     #     # #    #    #  #  # #    #   #      #####   #    # #   #                                                     \n"
                    + "#     #     # #    #    #   # # #    #   #      #       #    # #   #                                                     \n"
                    + "#     #     # #    #    #    ## #    #   #      #       #    # #   #                                                     \n"
                    + "#     ######   ####     #     #  ####    #      ####### #####  #   #                                                     \n"
                    + "#    \n"
                    + "#    \n"
                    + "#    \n"
                    + "#    #     #                                          #####                                                              \n"
                    + "#    ##   ##   ##    ####  #    # # #    # ######    #     # ###### #    # ###### #####    ##   ##### ###### #####       \n"
                    + "#    # # # #  #  #  #    # #    # # ##   # #         #       #      ##   # #      #    #  #  #    #   #      #    #      \n"
                    + "#    #  #  # #    # #      ###### # # #  # #####     #  #### #####  # #  # #####  #    # #    #   #   #####  #    #      \n"
                    + "#    #     # ###### #      #    # # #  # # #         #     # #      #  # # #      #####  ######   #   #      #    #      \n"
                    + "#    #     # #    # #    # #    # # #   ## #         #     # #      #   ## #      #   #  #    #   #   #      #    #      \n"
                    + "#    #     # #    #  ####  #    # # #    # ######     #####  ###### #    # ###### #    # #    #   #   ###### #####       \n"
                    + "#########################################################################################################################\n";

            out.println("\n" + warning);
            out.println("\n" + rString);
            ffdc.addFfdcEntry("Final dictionary source code  \n" + rString,
                    logger);
            logger.finer("Final dictionary source code  \n" + rString);
        } catch (Exception e) {
            // Print out the exception that occurred
            System.out.println("Unable to update " + out + ": "
                    + e.getMessage());
        }
        return (rString);

    }

    // Walk the data contained in the MachineXTableData creating the dictionary
    // entry for each slave and master.
    // When possible find the master associated with each slave. When the master
    // is found, also create the dictionary entry for that pairs crossing wires.
    // The created dictionary code is stored in the instances this.dataSet as
    // dictionary entries.
    //
    // NOTE: for the time being, as a master is added into temp_map, check
    // whether it already exist,,,, log sever and possibly exist
    @SuppressWarnings("unchecked")
    private void createDictData(ProcessTable thisClass, MachineXTableData data,
            Logger logger, FfdcControls ffdc) {
        ffdc.addFfdcEntry(("     Enter createDictData "), logger);
        String strBuf = new String();
        strBuf += "\n";
        // procees major_keys, example cmp:toaster would be a major_key called
        // toaster. We will process all
        // the records there are contained in the same major_key
        HashMap<String, HashMap<String, String>> this_map = data
                .getNextMajorKeyData(logger, ffdc); // about to walk the
                                                    // MachineXTableData,
                                                    // major_key (cmp) at a time
        while (this_map != null) {
            for (Map.Entry<String, HashMap<String, String>> entry : this_map
                    .entrySet()) {
                strBuf = "  \n";
                // examine the inside map data, figure out the characteristics
                // of the other end of the connection and see if they are found
                // If found, then create the wiring dictionary details.
                HashMap<String, String> inside_map = entry.getValue();
                HashMap<String, String> farData = thisClass.getFarEndData(
                        thisClass, data, inside_map, logger, ffdc, true); // will
                                                                            // ultimately
                                                                            // search
                                                                            // the
                                                                            // MachineXTableData
                // attempt to update the cell details in this record
                ffdc.addFfdcEntry(
                        (" this_map.entry " + entry + "\n farData " + farData),
                        logger);
                strBuf += ("Entry " + entry.getKey() + "  \n");
                String nearDocKey = "";
                String nearMajorKey = "";
                String linkName = "";
                String backPlane = ""; // entry may identify a back plane of
                                        // crossing wires other than the default
                                        // wiringBlock0
                HashMap<String, String> inCellInfo = new HashMap<String, String>();
                ;
                HashMap<String, String> outCellInfo = new HashMap<String, String>();
                HashMap<String, String> assignCellInfo = new HashMap<String, String>();
                HashMap<String, String> forceCellInfo = new HashMap<String, String>();
                Boolean isCharm = false;
                for (Map.Entry<String, String> innerEntry : inside_map
                        .entrySet()) {
                    nearMajorKey = inside_map.get("cmp");
                    linkName = inside_map.get("link");
                    if (inside_map.containsKey("backplane")) {
                        backPlane = inside_map.get("backplane");
                    }
                    if (inside_map.containsKey("slave_name")) { // possible
                                                                // minor key is
                                                                // a master and
                                                                // we will not
                                                                // obtain
                                                                // farEndData.
                                                                // Create a
                                                                // usable key
                                                                // for map
                                                                // insertion
                        nearDocKey = nearMajorKey + "_"
                                + inside_map.get("slave_name");
                        strBuf += ("slave entry : " + inside_map + "\n ");
                        strBuf += ("nearMajorKey  " + nearMajorKey
                                + " near_doc_key " + nearDocKey + " \n entry " + innerEntry);
                        strBuf += ("          nearData Key = "
                                + innerEntry.getKey() + ", Value = "
                                + innerEntry.getValue() + "\n");
                        // find any 'in_cell_info' and update entry with the
                        // actual data
                        if (inside_map.containsKey("in_cell_info")) {
                            inCellInfo = data.get_cell_entry(
                                    inside_map.get("in_cell_info"), logger,
                                    ffdc);
                            for (Map.Entry<String, String> inCellEntry : inCellInfo
                                    .entrySet()) {
                                inside_map.put("in_" + inCellEntry.getKey(),
                                        inCellEntry.getValue());
                            }
                            strBuf += (" slave inCellInfo " + inCellInfo
                                    + " \n   inside_map " + inside_map + "\n");
                        }
                        // determine whether this record is for a CHARM entry,
                        // if so, no crossing data will be created
                        if (inside_map.containsKey("charm")) {
                            if (inside_map.get("charm").equals("yes")) {
                                isCharm = true;
                                strBuf += (" Entry is a CHARM record  \n");
                            }
                        }
                        // find any 'assign_cell_info' and update entry with the
                        // actual data
                        if (inside_map.containsKey("cell_assign")) {
                            assignCellInfo = data
                                    .get_cell_entry(
                                            inside_map.get("cell_assign"),
                                            logger, ffdc);
                            for (Map.Entry<String, String> assignCellEntry : assignCellInfo
                                    .entrySet()) {
                                inside_map.put(
                                        "assign_" + assignCellEntry.getKey(),
                                        assignCellEntry.getValue());
                            }
                            strBuf += (" slave assignCellInfo "
                                    + assignCellInfo + " \n   inside_map "
                                    + inside_map + "\n");
                        }
                        if (inside_map.containsKey("cmp")) { // see if there is
                                                                // a cell force
                                                                // record for
                                                                // this
                                                                // entry
                            forceCellInfo = data.get_cell_force_entry(
                                    inside_map.get("cmp"), logger, ffdc);
                            if (forceCellInfo.isEmpty() == false) {
                                inCellInfo = forceCellInfo;
                                // logger.info(" slave inCellInfo " +
                                // forceCellInfo +"\n");
                                for (Map.Entry<String, String> forceCellInfoEntry : forceCellInfo
                                        .entrySet()) {
                                    inside_map
                                            .put("in_"
                                                    + forceCellInfoEntry
                                                            .getKey(),
                                                    forceCellInfoEntry
                                                            .getValue());
                                }
                            }
                            // logger.info(" slave forceCellInfo " +
                            // forceCellInfo + " \n   inside_map " + inside_map
                            // +"\n");
                            strBuf += (" slave forceCellInfo " + forceCellInfo
                                    + " \n   inside_map " + inside_map + "\n");
                        }
                        break; // break out as the iterator was wrecked by this
                                // put
                    } else { // Master_name
                        nearDocKey = nearMajorKey + "_"
                                + inside_map.get("master_name");
                        strBuf += ("master entry : " + inside_map + " \n ");
                        strBuf += ("nearMajorKey  " + nearMajorKey
                                + " near_doc_key " + nearDocKey + " \n entry " + innerEntry);
                        strBuf += ("          nearData Key = "
                                + innerEntry.getKey() + ", Value = "
                                + innerEntry.getValue() + "\n");
                        if (inside_map.containsKey("out_cell_info")) {
                            outCellInfo = data.get_cell_entry(
                                    inside_map.get("out_cell_info"), logger,
                                    ffdc);
                            for (Map.Entry<String, String> outCellEntry : outCellInfo
                                    .entrySet()) {
                                inside_map.put("out_" + outCellEntry.getKey(),
                                        outCellEntry.getValue());
                            }
                            strBuf += (" master outCellInfo" + outCellInfo
                                    + " \n   inside_map " + inside_map + "\n");
                        }
                        if (inside_map.containsKey("cmp")) { // see if there is
                                                                // a cell force
                                                                // record for
                                                                // this
                                                                // entry
                            forceCellInfo = data.get_cell_force_entry(
                                    inside_map.get("cmp"), logger, ffdc);
                            if (forceCellInfo.isEmpty() == false) {
                                outCellInfo = forceCellInfo;
                                // logger.info(" master outCellInfo " +
                                // forceCellInfo +"\n");
                                for (Map.Entry<String, String> forceCellInfoEntry : forceCellInfo
                                        .entrySet()) {
                                    inside_map.put(
                                            "out_"
                                                    + forceCellInfoEntry
                                                            .getKey(),
                                            forceCellInfoEntry.getValue());
                                }
                            }
                            // logger.info(" master forceCellInfo " +
                            // forceCellInfo + " \n   inside_map " + inside_map
                            // +"\n");
                            strBuf += (" master forceCellInfo " + forceCellInfo
                                    + " \n   inside_map " + inside_map + "\n");
                        }
                        break; // break out as the iterator was wrecked by this
                                // put
                    }
                }

                if (farData != null) { // far END data was found. far end is the
                                        // master
                    for (Map.Entry<String, String> far_entry : farData
                            .entrySet()) {
                        String farMajorKey = farData.get("cmp");
                        String farBackPlane = "wiringBlock0";
                        if (farData.containsKey("backplane")) {
                            farBackPlane = farData.get("backplane");
                        }
                        String farDocKey = farMajorKey + "_"
                                + farData.get("master_name");
                        ffdc.addFfdcEntry(("farMajorKey  " + farMajorKey
                                + " farDocKey " + farDocKey), logger);
                        strBuf += ("          farData key = "
                                + far_entry.getKey() + ", value = "
                                + far_entry.getValue() + "\n");
                        // find any 'out_cell_info' and update entry with the
                        // actual data
                        if (farData.containsKey("out_cell_info")) {
                            outCellInfo = data.get_cell_entry(
                                    farData.get("out_cell_info"), logger, ffdc);
                        }
                        if (inside_map.containsKey("cmp")) { // see if there is
                                                                // a cell force
                                                                // record for
                                                                // this
                                                                // entry
                            forceCellInfo = data.get_cell_force_entry(
                                    farData.get("cmp"), logger, ffdc);
                            if (forceCellInfo.isEmpty() == false) {
                                outCellInfo = forceCellInfo;
                            }
                        }

                        // so make the entry for the near connection
                        // nearMajorkey is a major key from the
                        // machineXTabledata. check if that key was already
                        // created into dataSet in the ProcessTable class. If
                        // not create it.
                        // Then see if the inner key was already added to the
                        // map. It should not be found
                        if (this.dataSet.containsKey(nearMajorKey)) {
                            HashMap<String, HashMap<String, String>> temp_map = (HashMap<String, HashMap<String, String>>) this.dataSet
                                    .get(nearMajorKey);
                            if (temp_map.containsKey(nearDocKey)) {
                                if (temp_map.containsValue(inside_map)) {
                                    ffdc.addFfdcEntry(
                                            (" (1.1)key already in map and is same entry  "
                                                    + nearDocKey
                                                    + " \n        "
                                                    + inside_map
                                                    + " \n       temp_map       " + temp_map),
                                            logger);
                                } else {
                                    ffdc.addFfdcEntryConfigWarning(
                                            (" (1.2)key already in map, associated with different value  "
                                                    + nearDocKey
                                                    + " \n        "
                                                    + inside_map
                                                    + " \n       temp_map       " + temp_map),
                                            logger);
                                    ffdc.errorExit(01, logger);
                                }
                            }
                            temp_map.put(nearDocKey, inside_map);
                        } else {
                            HashMap<String, HashMap<String, String>> new_major = new HashMap<String, HashMap<String, String>>();
                            this.dataSet.put(nearMajorKey, new_major);
                            HashMap<String, HashMap<String, String>> temp_map = (HashMap<String, HashMap<String, String>>) this.dataSet
                                    .get(nearMajorKey);
                            if (temp_map.containsKey(nearDocKey)) {
                                ffdc.addFfdcEntry(
                                        (" (2)key already in map " + nearDocKey
                                                + " \n          " + inside_map
                                                + " \n       temp_map       " + temp_map),
                                        logger);
                                ffdc.addFfdcEntryConfigWarning(
                                        (" (2)key already in map " + nearDocKey
                                                + " \n          " + inside_map
                                                + " \n       temp_map       " + temp_map),
                                        logger);
                                ffdc.errorExit(02, logger);
                            }
                            temp_map.put(nearDocKey, inside_map);
                        }
                        // now make the entry for the far connection. Do same
                        // check to make sure it was not already added
                        if (this.dataSet.containsKey(farMajorKey)) {
                            HashMap<String, HashMap<String, String>> temp_map = (HashMap<String, HashMap<String, String>>) this.dataSet
                                    .get(farMajorKey);
                            if (temp_map.containsKey(farDocKey)) {
                                HashMap<String, String> dataSet = temp_map
                                        .get(farDocKey);
                                if (dataSet.containsKey("slave_name")) { // it
                                                                            // is
                                                                            // possible
                                                                            // to
                                                                            // add
                                                                            // the
                                                                            // same
                                                                            // master
                                                                            // multiple
                                                                            // time,
                                                                            // but
                                                                            // slaves
                                                                            // only
                                                                            // once
                                    ffdc.addFfdcEntry(
                                            (" (3)Slave key already in map  farDocKey "
                                                    + farDocKey
                                                    + " \n         "
                                                    + farData
                                                    + " \n      temp_map           " + temp_map),
                                            logger);
                                    ffdc.addFfdcEntryConfigWarning(
                                            (" (3)key already in map  nearDocKey "
                                                    + farDocKey
                                                    + " \n         "
                                                    + farData
                                                    + " \n      temp_map           " + temp_map),
                                            logger);
                                    ffdc.errorExit(03, logger);
                                }
                            }
                            ffdc.addFfdcEntry((" \n\n  map  farDocKey "
                                    + farDocKey + " \n "), logger);
                            temp_map.put(farDocKey, farData);
                        } else {
                            HashMap<String, HashMap<String, String>> new_major = new HashMap<String, HashMap<String, String>>();
                            this.dataSet.put(farMajorKey, new_major);
                            HashMap<String, HashMap<String, String>> temp_map = (HashMap<String, HashMap<String, String>>) this.dataSet
                                    .get(farMajorKey);
                            if (temp_map.containsKey(farDocKey)) {
                                HashMap<String, String> dataSet = temp_map
                                        .get(farDocKey);
                                if (dataSet.containsKey("slave_name")) { // it
                                                                            // is
                                                                            // possible
                                                                            // to
                                                                            // add
                                                                            // the
                                                                            // same
                                                                            // master
                                                                            // multiple
                                                                            // time,
                                                                            // but
                                                                            // slaves
                                                                            // only
                                                                            // once
                                    ffdc.addFfdcEntry(
                                            (" (4)Slave key already in map  farDocKey "
                                                    + farDocKey
                                                    + " \n         "
                                                    + farData
                                                    + " \n      temp_map           " + temp_map),
                                            logger);
                                    ffdc.addFfdcEntryConfigWarning(
                                            (" (3)key already in map  nearDocKey "
                                                    + farDocKey
                                                    + " \n         "
                                                    + farData
                                                    + " \n      temp_map           " + temp_map),
                                            logger);
                                    ffdc.errorExit(03, logger);
                                }
                            }
                            temp_map.put(farDocKey, farData);
                        }
                        this.dataSet.get(farMajorKey);
                        // temp_map.put("cloud", farData+inside_map);
                        HashMap<String, String> crossData = new HashMap<String, String>();
                        // now update the crossing wires
                        crossData.put("master_name", nearDocKey);
                        crossData.put("link", linkName);
                        crossData.put("slave_name", farDocKey);
                        crossData.put("crossCmp", farBackPlane);
                        // not obvious but... The incell data pertains to the
                        // slave , the outCell data is the far master.
                        // device chassis-crossingWires device
                        // remember Master [ slave ---- master ] slave
                        // So the master device cell is used as the crossing
                        // wire slave cell.
                        // So... master's out_ data become the crossing slave
                        // in_ data
                        // HOWEVER if the IN and OUT data refer to the same
                        // cell_info, then create an ASSIGN entry, not an IO or
                        // OUT
                        boolean isAssign = false;
                        if (inCellInfo.containsKey("cell_info")
                                && outCellInfo.containsKey("cell_info")) {
                            if (inCellInfo.get("cell_info").equals(
                                    outCellInfo.get("cell_info"))) {
                                isAssign = true;
                            }
                        }
                        if (isAssign == false) {
                            for (Map.Entry<String, String> inCellEntry : inCellInfo
                                    .entrySet()) {
                                crossData.put("out_" + inCellEntry.getKey(),
                                        inCellEntry.getValue());
                            }
                            for (Map.Entry<String, String> outCellEntry : outCellInfo
                                    .entrySet()) {
                                crossData.put("in_" + outCellEntry.getKey(),
                                        outCellEntry.getValue());
                            }
                        } else { // compose the entry for assign cell, you can
                                    // use either in or out cell data as we now
                                    // know they are equal
                            for (Map.Entry<String, String> outCellEntry : outCellInfo
                                    .entrySet()) {
                                crossData.put(
                                        "assign_" + outCellEntry.getKey(),
                                        outCellEntry.getValue());
                            }
                        }
                        if (backPlane.isEmpty() == false) { // update record to
                                                            // say which
                                                            // backplane will
                                                            // contain this
                                                            // crossing data
                            crossData.put("backplane", backPlane);
                        }
                        if (this.dataSet.containsKey(farBackPlane)) {
                            HashMap<String, HashMap<String, String>> cloudMap = (HashMap<String, HashMap<String, String>>) this.dataSet
                                    .get(farBackPlane);
                            if (isCharm == false) {
                                cloudMap.put(nearDocKey + "_" + farDocKey,
                                        crossData);
                            }
                            ffdc.addFfdcEntry((strBuf), logger);
                            break;
                        } else {
                            HashMap<String, HashMap<String, String>> new_major = new HashMap<String, HashMap<String, String>>();
                            this.dataSet.put(farBackPlane, new_major);
                            HashMap<String, HashMap<String, String>> insideMap = (HashMap<String, HashMap<String, String>>) this.dataSet
                                    .get(farBackPlane);
                            if (isCharm == false) {
                                insideMap.put(nearDocKey + "_" + farDocKey,
                                        crossData);
                            }
                            break;
                        }
                    }
                } else { // no FAR end data was found
                    if (inside_map.containsKey("master_name")) {
                        strBuf += ("entry is a master so only add to map ");
                    } else {
                        // special rest for the key skip_ffdc. Needed as the
                        // c-link entries have no 'master'
                        if (entry.getValue().containsKey("skip_ffdc") == false) {
                            ffdc.addFfdcEntryConfigWarning(
                                    (" Slave had  No  master farData available   " + entry),
                                    logger);
                            strBuf += ("    No  master farData was available    " + entry);
                            ffdc.errorExit(05, logger);
                        }
                    }
                    if (this.dataSet.containsKey(nearMajorKey)) {
                        HashMap<String, HashMap<String, String>> temp_map = (HashMap<String, HashMap<String, String>>) this.dataSet
                                .get(nearMajorKey);
                        if (temp_map.containsKey(nearDocKey)) {
                            ffdc.addFfdcEntry(
                                    (" (5)Allowable key already in map "
                                            + nearDocKey + " \n        "
                                            + inside_map
                                            + " \n      temp_map      " + temp_map),
                                    logger);
                        } else {
                            temp_map.put(nearDocKey, inside_map);
                        }
                    } else {
                        HashMap<String, HashMap<String, String>> new_major = new HashMap<String, HashMap<String, String>>();
                        this.dataSet.put(nearMajorKey, new_major);
                        HashMap<String, HashMap<String, String>> temp_map = (HashMap<String, HashMap<String, String>>) this.dataSet
                                .get(nearMajorKey);
                        if (temp_map.containsKey(nearDocKey)) {
                            ffdc.addFfdcEntry(
                                    (" (6)Allowable key already in map "
                                            + nearDocKey + " \n        "
                                            + inside_map
                                            + " \n      temp_map      " + temp_map),
                                    logger);
                        } else {
                            temp_map.put(nearDocKey, inside_map);
                        }
                    }
                }
                ffdc.addFfdcEntry(strBuf, logger);
            }
            this_map = data.getNextMajorKeyData(logger, ffdc);

        }
        ffdc.addFfdcEntry(("exit this.dataSet" + this.dataSet), logger);
        return;
    }

    // boolean used, if true, the far end record will be indicate usage.
    private HashMap<String, String> getFarEndData(ProcessTable thisClass,
            MachineXTableData data, HashMap<String, String> inside_map,
            Logger logger, FfdcControls ffdc, boolean used) {
        ffdc.addFfdcEntry(
                ("     Enter getFarEndData process data  " + inside_map),
                logger);
        HashMap<String, String> farData = null;
        String farMajorKey = "";
        String linkValue = null;
        String mSName = null;
        String farDevNameValue = null;
        String comboLinkValue = null; // used to process an odd-ball connection
                                        // where the 'link' values differ in
                                        // name.
                                        // This is needed for legacy harrier
        for (Map.Entry<String, String> innerEntry : inside_map.entrySet()) {
            if (innerEntry.getKey().equals("combo_link")) {
                comboLinkValue = innerEntry.getValue();
            }
            if (innerEntry.getKey().equals("link")) {
                linkValue = innerEntry.getValue();
            }
            if (innerEntry.getKey().equals("cmp")) {
                innerEntry.getValue();
            }
            if (innerEntry.getKey().equals("far_cmp")) {
                farMajorKey = innerEntry.getValue();
            }
            if (innerEntry.getKey().equals("master_name")) {
                innerEntry.getValue();
                mSName = "slave_name";
            }
            if (innerEntry.getKey().equals("slave_name")) {
                innerEntry.getValue();
                mSName = "master_name";
            }
            if (innerEntry.getKey().equals("far_master_name")) {
                farDevNameValue = innerEntry.getValue();
                mSName = "master_name";
            }

        }
        // examine the master or slave name,
        String farDeviceName = farDevNameValue; // nearMSNameValue; // both near
                                                // and far use the same name of
                                                // the wiring objects
        if (mSName.equals("master_name")) {
            farData = data.getThisEntry(farMajorKey, linkValue, comboLinkValue,
                    mSName, farDeviceName, logger, ffdc, used);
            ffdc.addFfdcEntry(("farData " + farData), logger);
        }
        ffdc.addFfdcEntry((" far data " + farData), logger);
        return (farData);
    }

    private int updateDict(String key, HashMap<String, String> detailedMap,
            MachineXTableData dict_out, Logger logger, FfdcControls ffdc) {
        int rc = 0;
        ffdc.addFfdcEntry(
                ("Enter updateDict detailedMap size of " + detailedMap.size()),
                logger);
        rc = dict_out.add_entry(key, detailedMap, logger, ffdc);
        return (rc);
    }

    private int updateCellDict(String key, HashMap<String, String> detailedMap,
            MachineXTableData dict_out, Logger logger, FfdcControls ffdc) {
        int rc = 0;
        ffdc.addFfdcEntry(
                ("Enter updateCellDict detailedMap size of " + detailedMap
                        .size()), logger);
        rc = dict_out.add_cell_entry(key, detailedMap, logger, ffdc);
        return (rc);
    }

    private int updateCellForceDict(String key,
            HashMap<String, String> detailedMap, MachineXTableData dict_out,
            Logger logger, FfdcControls ffdc) {
        int rc = 0;
        ffdc.addFfdcEntry(
                ("Enter updateCellForceDict detailedMap size of " + detailedMap
                        .size()), logger);
        rc = dict_out.add_cell_force_entry(key, detailedMap, logger, ffdc);
        return (rc);
    }

    private int updateCellForceDictDefault(String key,
            HashMap<String, String> detailedMap, MachineXTableData dict_out,
            Logger logger, FfdcControls ffdc) {
        int rc = 0;
        ffdc.addFfdcEntry(
                ("Enter updateCellForceDictDefault detailedMap size of " + detailedMap
                        .size()), logger);
        rc = dict_out.add_cell_force_entry_default(key, detailedMap, logger,
                ffdc);
        return (rc);
    }

    // Record
    // <CMP:cmpNameIsFooOne|TYPE:typeIsI2cOne|USE:useIsMasterOne|NAME:nameIsBarOne>
    // Break the line into a dictionary, the key was located to the left off the
    // '|', the value to the right
    // Return the dictionary of these supposed key/values
    private HashMap<String, String> partitionLine(String data_line,
            Logger logger, FfdcControls ffdc) {
        ffdc.addFfdcEntry(("Enter partitionLine  :" + data_line), logger);

        HashMap<String, String> ret_map = new HashMap<String, String>();
        // pick apart the data_line into Map entries
        // First remove characters that identify the start and end
        String tempDataLine = data_line.trim();
        tempDataLine = tempDataLine.replaceAll(" ", ""); // remove space
        tempDataLine = tempDataLine.replaceAll("\\s", ""); // remove white space

        if ((tempDataLine.indexOf('>') != (tempDataLine.length() - 1))
                || (tempDataLine.length() == 0) || tempDataLine.startsWith("#")) {
            ffdc.addFfdcEntryConfigWarning(
                    ("None specific input, no data will be returned :"
                            + data_line + " temp " + tempDataLine), logger);
            return (ret_map);
        }
        String delims2 = "[<>]+";
        String[] tokens2 = data_line.split(delims2);
        if (tokens2.length <= 1) {
            ffdc.addFfdcEntryConfigWarning(
                    ("partitionLine() Invalid input data :" + data_line),
                    logger);
            ffdc.errorExit(40, logger);
            return (ret_map);
        }
        String raw_data = tokens2[1];
        logger.fine("raw_data  :" + raw_data);
        if (raw_data.length() == 0) {
            ffdc.addFfdcEntryConfigWarning(
                    ("partitionLine() Invalid input data :" + data_line
                            + " created token " + raw_data), logger);
            return (ret_map);
        }
        // search for user data. it is possible the user data is more than a
        // simple value.

        String pieces_delims = "[|]+";
        String[] token_pieces = raw_data.split(pieces_delims);
        // now find the component name to prepend to all data
        for (int c = 0; c < token_pieces.length; c++) {
            String k_v_delims = "[:]+";
            String[] k_v_pieces = token_pieces[c].split(k_v_delims);
            k_v_pieces[0].trim();
            k_v_pieces[1].trim();
            k_v_pieces[1] = k_v_pieces[1].replaceAll(" ", ""); // remove space
            k_v_pieces[1] = k_v_pieces[1].replaceAll("\\s", ""); // remove white
                                                                    // space
            if (ret_map.get(k_v_pieces[0]) == null) {
                ret_map.put(k_v_pieces[0], k_v_pieces[1]);
            } else {
                ffdc.addFfdcEntryConfigWarning(
                        ("partitionLine() ERROR: Key already in map "
                                + k_v_pieces[0] + " \n        " + raw_data),
                        logger);
                ffdc.errorExit(10, logger);
            }

        }
        ffdc.addFfdcEntry(("ret map " + ret_map), logger);
        return (ret_map);
    }

    // Record
    // <CMP:cmpNameIsFooOne|TYPE:typeIsI2cOne|USE:useIsMasterOne|NAME:nameIsBarOne>
    // OR < cell_detail:info_cse_X_1 | cell_cfg_name:cse_cell_name | delay:10 |
    // append1:%d | append2:1 | detail_name:info_cse_X_1 >
    private String createRecordKey(HashMap<String, String> data, Logger logger,
            FfdcControls ffdc) {
        ffdc.addFfdcEntry(("Enter createRecordKey :" + data), logger);
        String recordName = "";
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (entry.getKey().equals("cmp")
                    || entry.getKey().equals("cell_detail")
                    || entry.getKey().equals("cell_force_cmp")) {
                recordName = entry.getValue();
                break;
            }
            if (entry.getKey().equals("cell_force_cmp_default")) {
                recordName = "DEFAULT";
                break;
            }
        }
        ffdc.addFfdcEntry(("recordName :" + recordName), logger);
        return (recordName);
    }

    // The following validates required keys in the records.
    // Some keys if present require another key also be present, while other key
    // are mutually exclusive
    // initialzed to binary values we can mask off

    enum Key_Name {
        NONE(0), CMP(1), LINK(2), SLAVE_NAME(4), MASTER_NAME(8), CELL_INFO(16), DOC(
                32), FAR_CMP(64), FAR_MASTER_NAME(128), CELL_DETAIL(256), CELL_CFG_NAME(
                512), CELL_FORCE_NAME(1024), LOAD_DICTIONARY_TABLE(2048), LOAD_CMP(
                4096), LOAD_LOCAL(8192), CELL_FORCE_DFAULT_NAME(16384);

        private int value;

        private Key_Name(int c) {
            this.value = c;
        }

        public String get_string() {
            String r_str = null;
            if (this.value == 0) {
                r_str = "none";
            } else if (this.value == 1) {
                r_str = "cmp";
            } else if (this.value == 2) {
                r_str = "link";
            } else if (this.value == 4) {
                r_str = "slave_name";
            } else if (this.value == 8) {
                r_str = "master_name";
            } else if (this.value == 16) {
                r_str = "cell_info";
            } else if (this.value == 32) {
                r_str = "doc";
            } else if (this.value == 64) {
                r_str = "far_cmp";
            } else if (this.value == 128) {
                r_str = "far_master_name";
            } else if (this.value == 256) {
                r_str = "cell_detail";
            } else if (this.value == 512) {
                r_str = "cell_cfg_name";
            } else if (this.value == 1024) {
                r_str = "cell_force_cmp";
            } else if (this.value == 2048) {
                r_str = "load_dictionary_table";
            } else if (this.value == 4096) {
                r_str = "load_cmp";
            } else if (this.value == 8192) {
                r_str = "load_local";
            } else if (this.value == 16384) {
                r_str = "cell_force_cmp_default";
            }
            return (r_str);
        }

        public String get_string(int which) {
            String r_str = null;
            if (which == 0) {
                r_str = "none";
            } else if (which == 1) {
                r_str = "cmp";
            } else if (which == 2) {
                r_str = "link";
            } else if (which == 4) {
                r_str = "slave_name";
            } else if (which == 8) {
                r_str = "master_name";
            } else if (which == 16) {
                r_str = "cell_info";
            } else if (which == 32) {
                r_str = "doc";
            } else if (which == 64) {
                r_str = "far_cmp";
            } else if (which == 128) {
                r_str = "far_master_name";
            } else if (which == 256) {
                r_str = "cell_detail";
            } else if (which == 512) {
                r_str = "cell_cfg_name";
            } else if (which == 1024) {
                r_str = "cell_force_cmp";
            } else if (which == 2048) {
                r_str = "load_dictionary_table";
            } else if (which == 4096) {
                r_str = "load_cmp";
            } else if (which == 8192) {
                r_str = "load_local";
            } else if (which == 16384) {
                r_str = "cell_force_cmp_default";
            }
            return (r_str);
        }

        public int key_value() {
            return this.value;
        }

    }

    public static final int NOT_REQUIRED_KEY = 0;
    public static final int REQUIRED_KEY = 1;
    public static final int NOT_ALLOWED = 2;
    public static final int OPTIONAL_KEY = 0;
    public static final int INITIAL_COUNT = 0;
    public static final int NOT_VALIDATED = 0;
    public static final int VALIDATED = 1;

    // int[] array offsets
    public static final int VAL_OFFSET_REQUIRE = 0;
    public static final int VAL_OFFSET_COUNT = 1;
    public static final int VAL_OFFSET_EXCLUSIVE = 2;
    public static final int VAL_OFFSET_VALIDATED = 3;

    // test data
    // Map keys are valid wiring manager keys ie" CMP LINK SLAVE_NAME
    // MASTER_NAME CELL_INFO etc
    // int[] data [0] required 0 no, 1 yes
    // [1] occurence of this data record found 0 .. n If count > 1 error
    // [2] exclusive if enum key_name exists, error enum key_name
    // [3] validated 0 no, 1 yes

    private boolean validateRecordKeys(HashMap<String, String> data,
            Logger logger, FfdcControls ffdc) {
        ffdc.addFfdcEntry(("Enter validateRecordKeys :" + data), logger);
        HashMap<String, int[]> testData = new HashMap<String, int[]>();
        boolean rval = true;
        int[] tempData;

        if (data.isEmpty()) {
            logger.finer("No records to validate:");
            rval = false;
            return (rval);
        }
        if (data.containsKey("cell_detail")) {
            // now handle the cell information, a record is either a wire
            // definition or cell details
            tempData = new int[] { REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.NONE.key_value(), NOT_VALIDATED };
            testData.put("cell_detail", tempData);
            // now handle the cell information, a record is either a wire
            // definition or cell details
            tempData = new int[] { REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.NONE.key_value(), NOT_VALIDATED };
            testData.put("cell_cfg_name", tempData);
        } else if (data.containsKey("cell_force_cmp")) {
            // now handle the cell information, a record is either a wire
            // definition or cell details
            tempData = new int[] { NOT_REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.NONE.key_value(), NOT_VALIDATED };
            testData.put("cell_force_cmp", tempData);
            // now handle the cell information, a record is either a wire
            // definition or cell details
            tempData = new int[] { REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.NONE.key_value(), NOT_VALIDATED };
            testData.put("cell_cfg_name", tempData);
        } else if (data.containsKey("cell_force_cmp_default")) {
            // now handle the cell information, a record is either a wire
            // definition or cell details
            tempData = new int[] { NOT_REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.NONE.key_value(), NOT_VALIDATED };
            testData.put("cell_force_cmp_default", tempData);
            // now handle the cell information, a record is either a wire
            // definition or cell details
            tempData = new int[] { REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.NONE.key_value(), NOT_VALIDATED };
            testData.put("cell_cfg_name", tempData);
        } else if ((data.containsKey("load_cmp"))
                && (data.containsKey("load_dictionary_table"))) {
            tempData = new int[] { REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.NONE.key_value(), NOT_VALIDATED };
            testData.put("load_cmp", tempData);
            // now handle the file to load
            tempData = new int[] { REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.NONE.key_value(), NOT_VALIDATED };
            testData.put("load_dictionary_table", tempData);
            // keys not allowed
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.LOAD_LOCAL.key_value(), NOT_VALIDATED };
            testData.put("load_local", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.MASTER_NAME.key_value(), NOT_VALIDATED };
            testData.put("master_name", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.LINK.key_value(), NOT_VALIDATED };
            testData.put("link", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.CMP.key_value(), NOT_VALIDATED };
            testData.put("cmp", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.SLAVE_NAME.key_value(), NOT_VALIDATED };
            testData.put("slave_name", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.CELL_INFO.key_value(), NOT_VALIDATED };
            testData.put("cell_info", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.FAR_CMP.key_value(), NOT_VALIDATED };
            testData.put("far_cmp", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.FAR_MASTER_NAME.key_value(), NOT_VALIDATED };
            testData.put("far_master_name", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.CELL_DETAIL.key_value(), NOT_VALIDATED };
            testData.put("cell_detail", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.CELL_CFG_NAME.key_value(), NOT_VALIDATED };
            testData.put("cell_cfg_name", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.CELL_FORCE_NAME.key_value(), NOT_VALIDATED };
            testData.put("cell_force_name", tempData);

        } else if ((data.containsKey("load_local"))
                && (data.containsKey("load_dictionary_table"))) {
            tempData = new int[] { REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.NONE.key_value(), NOT_VALIDATED };
            testData.put("load_local", tempData);
            // now handle the file to load
            tempData = new int[] { REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.NONE.key_value(), NOT_VALIDATED };
            testData.put("load_dictionary_table", tempData);
            // keys not allowed
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.LOAD_CMP.key_value(), NOT_VALIDATED };
            testData.put("load_cmp", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.MASTER_NAME.key_value(), NOT_VALIDATED };
            testData.put("master_name", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.LINK.key_value(), NOT_VALIDATED };
            testData.put("link", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.CMP.key_value(), NOT_VALIDATED };
            testData.put("cmp", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.SLAVE_NAME.key_value(), NOT_VALIDATED };
            testData.put("slave_name", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.CELL_INFO.key_value(), NOT_VALIDATED };
            testData.put("cell_info", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.FAR_CMP.key_value(), NOT_VALIDATED };
            testData.put("far_cmp", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.FAR_MASTER_NAME.key_value(), NOT_VALIDATED };
            testData.put("far_master_name", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.CELL_DETAIL.key_value(), NOT_VALIDATED };
            testData.put("cell_detail", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.CELL_CFG_NAME.key_value(), NOT_VALIDATED };
            testData.put("cell_cfg_name", tempData);
            //
            tempData = new int[] { NOT_ALLOWED, INITIAL_COUNT,
                    Key_Name.CELL_FORCE_NAME.key_value(), NOT_VALIDATED };
            testData.put("cell_force_name", tempData);

        } else if (data.containsKey("cmp")) {
            tempData = new int[] { REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.NONE.key_value(), NOT_VALIDATED };
            testData.put("cmp", tempData);
            //
            tempData = new int[] { REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.NONE.key_value(), NOT_VALIDATED };
            testData.put("link", tempData);
            //
            tempData = new int[] { REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.MASTER_NAME.key_value(), NOT_VALIDATED };
            testData.put("slave_name", tempData);
            //
            tempData = new int[] { REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.SLAVE_NAME.key_value(), NOT_VALIDATED };
            testData.put("master_name", tempData);
            //
            tempData = new int[] { NOT_REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.NONE.key_value(), NOT_VALIDATED };
            testData.put("cell_info", tempData);
            //
            tempData = new int[] { NOT_REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.NONE.key_value(), NOT_VALIDATED };
            testData.put("doc", tempData);
            //
            tempData = new int[] { NOT_REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.MASTER_NAME.key_value(), NOT_VALIDATED };
            testData.put("far_cmp", tempData);
            // Key_Name.MASTER_NAME
            // key_Name.NONE .get_string("master_name");
            // key_Name.NONE.get_string("master_name") |
            // key_Name.NONE.get_string("cell_info")
            tempData = new int[] { NOT_REQUIRED_KEY, INITIAL_COUNT,
                    Key_Name.MASTER_NAME.key_value(), NOT_VALIDATED };
            testData.put("far_master_name", tempData);
        } else {
            ffdc.addFfdcEntry(("ERROR invalid user data "), logger);
            ffdc.addFfdcEntryConfigWarning(("ERROR invalid user data "), logger);
            ffdc.errorExit(20, logger);
            rval = false;
        }

        // see if the record to test contains a key we are not tracking in our
        // test data.
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (testData.containsKey(entry.getKey()) == false) {
                ffdc.addFfdcEntry(("User defined Key  " + entry.getKey()),
                        logger);
            }
        }
        ffdc.addFfdcEntry(
                ("complete data set used in validation  " + data.entrySet()),
                logger);
        // now verify the needed entries exist and any exclusive requirements
        // are meet
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (testData.containsKey(entry.getKey()) == false) { // allow keys
                                                                    // the user
                                                                    // defined
                continue;
            }
            int[] values = testData.get(entry.getKey()); // data pertainent to
                                                            // this particular
                                                            // entry
            values[VAL_OFFSET_COUNT] += 1;
            testData.put(entry.getKey(), values);
            int[] value_data = testData.get(entry.getKey());
            int isRequired = value_data[VAL_OFFSET_REQUIRE];
            int count = value_data[VAL_OFFSET_COUNT];
            int exclusive = value_data[VAL_OFFSET_EXCLUSIVE];
            ffdc.addFfdcEntry(
                    ("process  values : " + isRequired + ", " + count + ", "
                            + exclusive + ", " + value_data[VAL_OFFSET_VALIDATED]),
                    logger);

            int[] exclusive_class = new int[] { NOT_REQUIRED_KEY,
                    INITIAL_COUNT, Key_Name.NONE.key_value() };
            if (exclusive > Key_Name.NONE.key_value()) {
                exclusive_class = testData.get(Key_Name.NONE
                        .get_string(exclusive));
            }

            // if required and count > 0, if exclusive make sure other Key_Name
            // was NOT used..
            // if required and count == 0, if exclusive make sure other Key_Name
            // was used..
            if ((isRequired == REQUIRED_KEY) && (count > INITIAL_COUNT)
                    && exclusive != Key_Name.NONE.key_value()) {
                if (exclusive_class[VAL_OFFSET_COUNT] != INITIAL_COUNT) {
                    ffdc.addFfdcEntry(
                            ("ERROR has exclusive  with counts non-zero "),
                            logger);
                    ffdc.addFfdcEntryConfigWarning(
                            ("ERROR has exclusive  with counts non-zero "),
                            logger);
                    ffdc.errorExit(21, logger);
                    rval = false;
                    break;
                } else {
                    exclusive_class[VAL_OFFSET_VALIDATED] = VALIDATED;
                    testData.put(Key_Name.NONE.get_string(exclusive),
                            exclusive_class);
                    int[] finalValues = testData.get(entry.getKey()); // data
                                                                        // pertainent
                                                                        // to
                                                                        // this
                                                                        // particular
                                                                        // entry
                    finalValues[VAL_OFFSET_VALIDATED] = VALIDATED;
                    testData.put(entry.getKey(), finalValues);
                }
            } else if ((isRequired == NOT_ALLOWED) && (count > INITIAL_COUNT)) {
                ffdc.addFfdcEntry(
                        ("ERROR has NOT_ALLOWED  with counts non-zero "),
                        logger);
                ffdc.addFfdcEntryConfigWarning(
                        ("ERROR has NOT_ALLOWED  with counts non-zero "),
                        logger);
                ffdc.errorExit(22, logger);
                rval = false;
                break;
            } else if ((isRequired == REQUIRED_KEY) && (count > INITIAL_COUNT)
                    || (isRequired == NOT_REQUIRED_KEY)
                    && (count > INITIAL_COUNT)) {
                int[] finalValues = testData.get(entry.getKey()); // data
                                                                    // pertainent
                                                                    // to this
                                                                    // particular
                                                                    // entry
                finalValues[VAL_OFFSET_VALIDATED] = VALIDATED;
                testData.put(entry.getKey(), finalValues);
            }
            ffdc.addFfdcEntry(
                    ("key " + entry.getKey() + " final values : REQ "
                            + values[VAL_OFFSET_REQUIRE] + ",COUNT "
                            + values[VAL_OFFSET_COUNT] + ",EXCL "
                            + values[VAL_OFFSET_EXCLUSIVE] + ",VALID " + values[VAL_OFFSET_VALIDATED]),
                    logger);
        }

        // now see if all required entries are validated
        for (Map.Entry<String, int[]> entry_val : testData.entrySet()) {
            int[] valueDataVal = entry_val.getValue();
            int isRequiredVal = valueDataVal[VAL_OFFSET_REQUIRE];
            int isValidVal = valueDataVal[VAL_OFFSET_VALIDATED];
            ffdc.addFfdcEntry(
                    ("Validate key " + entry_val.getKey()
                            + " final values : REQ " + isRequiredVal
                            + ",VALID " + isValidVal), logger);
            if ((isRequiredVal == REQUIRED_KEY)
                    && (isValidVal == NOT_VALIDATED)) {
                ffdc.addFfdcEntry(("ERROR REQUIRED entry is NOT_VALIDATED key "
                        + entry_val.getKey() + " final values : REQ "
                        + isRequiredVal + ",VALID " + isValidVal), logger);
                ffdc.addFfdcEntryConfigWarning(
                        ("ERROR REQUIRED entry is NOT_VALIDATED key "
                                + entry_val.getKey() + " final values : REQ "
                                + isRequiredVal + ",VALID " + isValidVal),
                        logger);
                rval = false;
                ffdc.errorExit(23, logger);
                break;
            }
        }

        ffdc.addFfdcEntry(("return value  :" + rval), logger);
        return (rval);
    }

    private final Map<String, Object> dataSet;
}