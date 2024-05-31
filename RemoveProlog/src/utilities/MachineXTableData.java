
package utilities;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

/*
 * Stores internal dictionary
 *  This dictionary is a partially processed input file. The data
 * very specific to the ProcessTable class.
 *
 *  Although, this class could if needed do further processing of the data
 *
 * Data   string(cmp) : { unique_name1:{key/value, key:value...}, unique_name1:{key/value, key:value...}}
 *
 * The term inner or inside map refers to  unique_name1:{key/value, key:value...}, unique_name1:{key/value, key:value...}
 *
 * Also, the cell details are in their own dictionary
 * Data   string(name_from_the _table) : {{key/value, key:value...}}}
 *
 *
 */

public class MachineXTableData {
    public MachineXTableData() {
        this.instName = "";
        this.dataSet = new TreeMap<String, Object>();
        this.cellDataSet = new TreeMap<String, Object>();
        this.cellForceDataSet = new TreeMap<String, Object>();
        this.lastMajorOffset = 0;
        this.cellDefaultData = new HashMap<String, String>();
    }

    public MachineXTableData(String instanceName) {
        this.instName = instanceName;
        this.dataSet = new TreeMap<String, Object>();
        this.cellDataSet = new TreeMap<String, Object>();
        this.cellForceDataSet = new TreeMap<String, Object>();
        this.lastMajorOffset = 0;
        this.cellDefaultData = new HashMap<String, String>();
    }

    @SuppressWarnings("unchecked")
    public int add_entry(String key, HashMap<String, String> value,
            Logger logger, FfdcControls ffdc) {
        ffdc.addFfdcEntry(("enter add_entry   key " + key + " value " + value),
                logger);
        int rc = 1;

        String recordName = "";
        // create a new inner map of this particular master or slave entry
        // the key into the inner map is the master or slave name value
        HashMap<String, HashMap<String, String>> inner_map = new HashMap<String, HashMap<String, String>>();
        for (Map.Entry<String, String> entry : value.entrySet()) {
            if ((entry.getKey().equals("master_name"))
                    || (entry.getKey().equals("slave_name"))) {
                recordName = entry.getValue();
                inner_map.put(recordName, value);
                break;
            }
        }
        if (this.dataSet.get(key) == null) {
            this.dataSet.put(key, inner_map);
        } else {
            HashMap<String, HashMap<String, String>> insideMap = (HashMap<String, HashMap<String, String>>) this.dataSet
                    .get(key);
            if ((insideMap.containsKey(recordName))) {
                if (value.containsKey("charm")) {
                    if (value.get("charm").equals("yes")) {
                        ffdc.addFfdcEntry(
                                (" add_entry, this record already in map allowed as this is a CHARM record "
                                        + recordName + " \n full record" + value),
                                logger);
                    } else {
                        ffdc.addFfdcEntryConfigWarning(
                                (" add_entry, this record already in map, suspect duplicate entry in input file. master/slave name "
                                        + recordName + " \n full record" + value),
                                logger);
                        ffdc.addFfdcEntry(
                                (" add_entry, this record already in map, suspect duplicate entry in input file. master/slave name "
                                        + recordName + " \n full record" + value),
                                logger);
                        ffdc.errorExit(101, logger);
                    }
                }
            } else {
                ffdc.addFfdcEntry((" add_entry, this record  " + recordName
                        + " \n full record" + value), logger);
                insideMap.put(recordName, value);
                this.dataSet.put(key, insideMap);
            }
        }
        ffdc.addFfdcEntry(("add_entry exit map " + inner_map + " \n full map "
                + this.dataSet + "\n\n"), logger);
        return (rc);
    }

    public int display(Logger logger, FfdcControls ffdc) {
        int rc = 0;
        List<String> keys = new ArrayList<String>(this.dataSet.keySet());
        // write the results to the output file
        String strBuf = new String();
        strBuf += ("     Enter display: data for instance name " + instName + "\n");
        for (String key : keys) {
            strBuf += ("Major key " + key + " : " + this.dataSet.get(key));
            for (Map.Entry<String, Object> entry : this.dataSet.entrySet()) {
                if (entry.getKey().equals(key)) {
                    strBuf += ("  CMP Key = " + entry.getKey() + "\n");
                    @SuppressWarnings("unchecked")
                    HashMap<String, HashMap<String, String>> insideMap = (HashMap<String, HashMap<String, String>>) entry
                            .getValue();
                    for (Entry<String, HashMap<String, String>> inner_entry : insideMap
                            .entrySet()) {
                        strBuf += ("                  " + inner_entry.getKey()
                                + ", Value = " + inner_entry.getValue() + "\n");
                    }
                }
            }
        }
        ffdc.addFfdcEntry((strBuf), logger);
        return (rc);
    }

    public HashMap<String, HashMap<String, String>> getNextMajorKeyData(
            Logger logger, FfdcControls ffdc) {
        ffdc.addFfdcEntry(
                ("     Enter getNextMajorKeyData offset  " + this.lastMajorOffset),
                logger);
        HashMap<String, HashMap<String, String>> insideMap = null;
        List<String> keys = new ArrayList<String>(this.dataSet.keySet());
        int counter = 0;
        for (String key : keys) {
            ffdc.addFfdcEntry(("Major key " + key + " : " + keys), logger);
            if (this.lastMajorOffset == counter) {
                this.lastMajorOffset++;
                ffdc.addFfdcEntry(
                        ("Major key " + key + " : " + this.dataSet.get(key)
                                + " offset " + this.lastMajorOffset
                                + " counter " + counter), logger);
                insideMap = getInnerMap(key, logger, ffdc);
                break;
            } else {
                counter++;
            }
        }
        ffdc.addFfdcEntry((" exit getNextMajorKeyData " + insideMap), logger);
        return (insideMap);
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, HashMap<String, String>> getInnerMap(String key,
            Logger logger, FfdcControls ffdc) {
        ffdc.addFfdcEntry(("     Enter getInnerMap  for  major Key" + key),
                logger);

        HashMap<String, HashMap<String, String>> insideMap = null;
        for (Map.Entry<String, Object> entry : this.dataSet.entrySet()) {
            if (entry.getKey().equals(key)) { // found the inner map
                insideMap = (HashMap<String, HashMap<String, String>>) entry
                        .getValue();
                ffdc.addFfdcEntry(("inner map  " + insideMap), logger);
            }
        }

        return (insideMap);
    }

    public int putInnerMap(String key,
            HashMap<String, HashMap<String, String>> map, Logger logger,
            FfdcControls ffdc) {
        ffdc.addFfdcEntry(("     Enter getInnerMap  for  major Key" + key),
                logger);
        int rCode = 0;
        HashMap<String, HashMap<String, String>> insideMap = null;
        for (Map.Entry<String, Object> entry : this.dataSet.entrySet()) {
            if (entry.getKey().equals(key)) { // found the inner map
                entry.setValue(map);
                ffdc.addFfdcEntry(("inner map  " + insideMap), logger);
            }
        }

        return (rCode);
    }

    // odd-ball parm comboLinkValue: used to process an connections where the
    // 'link' values differ in name. This is needed for legacy harrier
    // used -- mark entry as in use
    public HashMap<String, String> getThisEntry(String key, String linkValue,
            String comboLinkValue, String mSName, String mSNameValue,
            Logger logger, FfdcControls ffdc, boolean used) {
        ffdc.addFfdcEntry(("     Enter getThisEntry for  major Key  " + key
                + " link type " + linkValue + " comboLinkValue "
                + comboLinkValue + " MASTER/SLAVE " + mSName
                + " MASTER/SLAVE value " + mSNameValue), logger);
        HashMap<String, String> rval = null;
        HashMap<String, HashMap<String, String>> inner_map = null;
        inner_map = getInnerMap(key, logger, ffdc);
        ffdc.addFfdcEntry((" inner_map : \n " + inner_map), logger);
        if (inner_map == null) { // no far end records exist for the major key
            ffdc.addFfdcEntry((" No far end map exist for major key " + key),
                    logger);
            return (rval);
        }
        for (Map.Entry<String, HashMap<String, String>> entry : inner_map
                .entrySet()) {
            HashMap<String, String> insideMap = entry.getValue();
            ffdc.addFfdcEntry((" insideMap  " + insideMap), logger);
            boolean comboIsEqual = false; // ta_v4 some harrier entries use
                                            // different links between the far
                                            // ends (fpga..)
            if ((comboLinkValue != null)
                    && (insideMap.containsKey("combo_link"))) {
                comboIsEqual = insideMap.get("combo_link").equals(
                        comboLinkValue);
            }
            if (insideMap.containsKey(mSName)) { // check we have the required
                                                    // master or slave entry
                if (((insideMap.get(mSName).equals(mSNameValue)) && (insideMap
                        .get("link").equals(linkValue)))
                        || ((insideMap.get(mSName).equals(mSNameValue)) && (comboIsEqual))) { // &&
                                                                                                // (innerBackPlane.equals(backPlane))
                    if (used) {
                        setThisEntryProcessed(key, linkValue, mSName,
                                mSNameValue, logger, ffdc);
                    }
                    rval = insideMap;
                    break;
                }
            }
        }
        return (rval);
    }

    public int setThisEntryProcessed(String key, String linkValue,
            String mSName, String mSNameValue, Logger logger, FfdcControls ffdc) {
        ffdc.addFfdcEntry(("     Enter setThisEntryProcessed for  major Key  "
                + key + " link type " + linkValue + " MASTER/SLAVE " + mSName
                + " MASTER/SLAVE value " + mSNameValue), logger);
        int rc = 0;
        Integer count = 0;
        HashMap<String, HashMap<String, String>> inner_map = null;
        inner_map = getInnerMap(key, logger, ffdc);

        for (Map.Entry<String, HashMap<String, String>> entry : inner_map
                .entrySet()) {
            HashMap<String, String> insideMap = entry.getValue();
            ffdc.addFfdcEntry((" insideMap  " + insideMap), logger);
            if (insideMap.containsKey(mSName)) { // check we have the required
                                                    // master or slave entry
                if ((insideMap.get(mSName).equals(mSNameValue))
                        && (insideMap.get("link").equals(linkValue))) {
                    rc = 1;
                    boolean ffdc_required = true;
                    insideMap.put("processed", "yes");
                    if (insideMap.containsKey("skip_ffdc")) {
                        ffdc_required = false;
                    }
                    if (insideMap.containsKey("use_count")) {
                        count = Integer.decode(insideMap.get("use_count"));
                        count += 1;
                        insideMap.put("use_count", count.toString());
                    } else {
                        insideMap.put("use_count", "1");
                    }
                    // only the signal and i2c_link support fannot to mutliple
                    // slaves, all other link type log error details
                    if (!insideMap.get("link").equals("signal")
                            && !insideMap.get("link").equals("i2c_link")
                            && (count > 1) && (ffdc_required)) {
                        ffdc.addFfdcEntry(
                                (" ERROR do not allow multi use master cmp  entry:  " + insideMap),
                                logger);
                        ffdc.addFfdcEntryConfigWarning(
                                (" ERROR do not allow multi use master cmp entry: " + insideMap),
                                logger);
                        ffdc.errorExit(190, logger);
                    } else {
                        ffdc.addFfdcEntry(
                                (" Allow multi use master cmp entry : " + insideMap),
                                logger);
                    }
                    inner_map.put(entry.getKey(), insideMap);
                    putInnerMap(key, inner_map, logger, ffdc);
                    break;
                }
            }
        }
        return (rc);
    }

    public int add_cell_entry(String key, HashMap<String, String> value,
            Logger logger, FfdcControls ffdc) {
        ffdc.addFfdcEntry(
                ("enter add_cell_entry   key " + key + " value " + value),
                logger);
        int rc = 1;
        if (this.cellDataSet.get(key) == null) {
            this.cellDataSet.put(key, value);
        } else {
            if (this.cellDataSet.containsKey(key)) {
                ffdc.addFfdcEntryConfigWarning(
                        (" add_cell_entry, this record already in map key "
                                + key + " \n full record" + value), logger);
                ffdc.errorExit(160, logger);
            } else {
                this.cellDataSet.put(key, value);
            }
        }

        ffdc.addFfdcEntry(
                ("add_cell_entry exit map  full map " + this.cellDataSet),
                logger);
        return (rc);
    }

    public int add_cell_force_entry_default(String key,
            HashMap<String, String> value, Logger logger, FfdcControls ffdc) {
        ffdc.addFfdcEntry(("enter add_cell_force_entry_default   key " + key
                + " value " + value), logger);
        int rc = 1;
        if (this.cellDefaultData.isEmpty()) {
            ffdc.addFfdcEntry(
                    (" add_cell_force_entry_default, was empty,set to  " + value),
                    logger);
            this.cellDefaultData = value;
        } else {
            ffdc.addFfdcEntry(
                    (" add_cell_force_entry_default, replace existing record with  " + value),
                    logger);
            this.cellDefaultData = value;
        }

        ffdc.addFfdcEntry(("add_cell_force_entry_default "), logger);
        return (rc);
    }

    public HashMap<String, String> get_cell_force_entry_default(String key,
            HashMap<String, String> value, Logger logger, FfdcControls ffdc) {
        ffdc.addFfdcEntry(("enter get_cell_force_entry_default "), logger);
        if (this.cellDefaultData.isEmpty()) {
            ffdc.addFfdcEntry((" get_cell_force_entry_default, was empty  "),
                    logger);
        } else {
            ffdc.addFfdcEntry(
                    (" get_cell_force_entry_default record   " + this.cellDefaultData),
                    logger);
        }

        ffdc.addFfdcEntry(("get_cell_force_entry_default "), logger);
        return (this.cellDefaultData);
    }

    public int add_cell_force_entry(String key, HashMap<String, String> value,
            Logger logger, FfdcControls ffdc) {
        ffdc.addFfdcEntry(("enter add_cell_force_entry   key " + key
                + " value " + value), logger);
        int rc = 1;
        if (this.cellForceDataSet.get(key) == null) {
            this.cellForceDataSet.put(key, value);
        } else {
            if (this.cellForceDataSet.containsKey(key)) {
                ffdc.addFfdcEntryConfigWarning(
                        (" add_cell_force_entry, this record already in map key "
                                + key + " \n full record" + value), logger);
                ffdc.errorExit(170, logger);
            } else {
                this.cellForceDataSet.put(key, value);
            }
        }

        ffdc.addFfdcEntry(
                ("add_cell_force_entry exit map  full map " + this.cellForceDataSet),
                logger);
        return (rc);
    }

    // If the requested key is not found, if a default exist return the default.
    @SuppressWarnings("unchecked")
    public HashMap<String, String> get_cell_entry(String key, Logger logger,
            FfdcControls ffdc) {
        ffdc.addFfdcEntry(("enter get_cell_entry   key " + key), logger);
        HashMap<String, String> value = new HashMap<String, String>();
        if (this.cellDataSet.get(key) == null) {
            if (this.cellForceDataSet.get(key) == null) {
                ffdc.addFfdcEntry(
                        (" get_cell_entry, Allow for legacy Harrier, this record NOT in either map " + key),
                        logger); // not a problem as this is an optional feature
                // ffdc.addFfdcEntryConfigWarning((
                // " get_cell_entry, this record NOT in either map " + key ),
                // logger);
                // ffdc.errorExit(180, logger);
                // Thread.dumpStack();
                if (this.cellDefaultData.isEmpty() == false) {
                    ffdc.addFfdcEntry(
                            (" get_cell_entry, return default cell data "),
                            logger); // not a problem as this is an optional
                                        // feature
                    value = this.cellDefaultData;
                }
            } else {
                value = (HashMap<String, String>) this.cellForceDataSet
                        .get(key);
            }
        } else {
            value = (HashMap<String, String>) this.cellDataSet.get(key);
        }
        ffdc.addFfdcEntry(("get_cell_entry exit map detailed " + value
                + " \n   full map " + this.cellDataSet), logger);
        return (value);
    }

    // ### Force these components into a specific cell....
    // < cell_force_cmp : foo | | out_cell_info : info_fsp | in_cell_info :
    // info_fsp >
    // < cell_force_cmp : bar | | out_cell_info : info_x_ite | in_cell_info :
    // info_x_ite >
    // If the requested force key is not found, if a default exist return the
    // default.
    @SuppressWarnings("unchecked")
    public HashMap<String, String> get_cell_force_entry(String key,
            Logger logger, FfdcControls ffdc) {
        ffdc.addFfdcEntry(("enter get_cell_forece_entry   key " + key), logger);
        HashMap<String, String> value = new HashMap<String, String>();
        if (this.cellForceDataSet.get(key) == null) {
            if (this.cellDataSet.get(key) == null) {
                ffdc.addFfdcEntry(
                        (" get_cell_entry, Allow for legacy Harrier, this record NOT in either map " + key),
                        logger); // not a problem as this is an optional feature
                // ffdc.addFfdcEntryConfigWarning((
                // " get_cell_force_entry, this record NOT in either map " + key
                // ), logger);
                // ffdc.errorExit(190, logger);
                // Thread.dumpStack();
                if (this.cellDefaultData.isEmpty() == false) {
                    ffdc.addFfdcEntry(
                            (" get_cell_force_entry, return default cell data "),
                            logger); // not a problem as this is an optional
                                        // feature
                    value = this.cellDefaultData;
                }
            } else {
                value = (HashMap<String, String>) this.cellDataSet.get(key);
            }
        } else {
            value = (HashMap<String, String>) this.cellForceDataSet.get(key);
        }
        ffdc.addFfdcEntry(
                ("get_cell_force_entry exit map  full map " + this.cellForceDataSet),
                logger);

        return (value);
    }

    // return all force cell data
    public Map<String, Object> get_cell_force_entries(Logger logger,
            FfdcControls ffdc) {
        ffdc.addFfdcEntry(("enter get_cell_forece_entries "), logger);
        Map<String, Object> value = this.cellForceDataSet;
        ffdc.addFfdcEntry(
                ("get_cell_force_entries exit map  full map " + this.cellForceDataSet),
                logger);
        return (value);
    }

    private int lastMajorOffset;
    private final String instName;
    private final Map<String, Object> dataSet;
    private final Map<String, Object> cellDataSet;
    private final Map<String, Object> cellForceDataSet;
    private HashMap<String, String> cellDefaultData;
}