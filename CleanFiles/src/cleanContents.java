

import java.util.ArrayList;
import java.util.Scanner;

public interface cleanContents {

    ArrayList<String> processFile(Scanner scanner, String fileName,
            String suffix);

    //
    boolean findTrailingSpaces(String line);

    //
    boolean findTabs(String line);

    // remove trailing spaces
    String removeTrailingSpaces(String line);

    // convert TAB to 4 spaces
    //
    String convertTabs(String line);

}