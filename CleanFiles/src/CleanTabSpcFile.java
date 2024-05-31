import java.util.ArrayList;



import java.util.Scanner;

import java.util.logging.Logger;

import utilities.FfdcControls;
public class CleanTabSpcFile implements cleanContents {

    public CleanTabSpcFile(Logger logger,  FfdcControls ffdc) {
        super();
        this.logger = logger;
        this.ffdc = ffdc;

    }

	  public ArrayList<String> processFile(Scanner scanner, String fileName, String suffix) {
	        ArrayList<String> newText = new ArrayList<String>();
	        String line = "default";
	        boolean tab = false;
	        boolean space = false;
	        
	        logger.getFilter();
	        this.ffdc.addFfdcEntryConfigWarning("entered processFile  file: " + fileName + " suffix: "
	                + suffix, this.logger);
	        while (scanner.hasNextLine()) {
	            line = scanner.nextLine();
	            // newText.add(line);
	            tab = false;
	            space = false;
	            if (this.findTabs(line)) {
	                // this.logger.info("call convert tabs");
	                line = this.convertTabs(line);
	                newText.add(line);
	                tab = true;
	            }
	            if (this.findTrailingSpaces(line)) {
	                // this.logger.info("call remove trailing spaces");
	                line = this.removeTrailingSpaces(line);
	                newText.add(line);
	                space = true;
	            }
	            if(!tab && !space){
	            	newText.add(line);
	            }	
	            this.ffdc.addFfdcEntryConfigWarning("text " + newText, this.logger);
	        }
	        return (newText);

	    }

	    private Logger logger;
	    private  FfdcControls ffdc;
	    public boolean findTrailingSpaces(String line) {
	        boolean rval = false;

	        if ((line.length() > 0)
	                && (line.lastIndexOf(" ") == (line.length() - 1))) {
	        	rval = true;
	        }
	        return rval;
	    }

	    public boolean findTabs(String line) {
	        boolean rval = line.contains("\t");
	        return rval;
	    }

	    public String removeTrailingSpaces(String line) {
	    	String rval = line;
	    	this.ffdc.addFfdcEntryConfigWarning("Line contains trailing space :" + line +"|", this.logger);
	    	while (line.lastIndexOf(" ") == (line.length() - 1)) {
	            line = line.substring(0, line.length()-1);
	        }
	        rval = line;
	        this.ffdc.addFfdcEntryConfigWarning("After cleanup of spaces      :" + rval+"|", this.logger); 
	        return rval;
	    }

	    public String convertTabs(String line) {
	    	this.ffdc.addFfdcEntryConfigWarning("Line contains tabs    :" + line+"|", this.logger);
	        String rval = line;
	        rval = line.replace("\t", "    ");
	    	this.ffdc.addFfdcEntryConfigWarning("After cleanup of tabs :" + rval+"|", this.logger); 
	    	return rval;
	    }
}
