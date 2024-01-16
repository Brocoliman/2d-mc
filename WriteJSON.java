import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.util.HashMap;

public class WriteJSON {
    String filename;
    HashMap<String, Object> data;

    WriteJSON (String filename, HashMap<String, Object> data) {
        this.filename = filename;
        this.data = data;
        // Convert regular arrays to JSONArray
        for (String name: this.data.keySet()) {
            if(this.data.get(name) instanceof Object[]) {
                this.data.put(name, this.data.get(name));
            }
        }
    }

    /*public void writeJSON() {
        try {
            FileWriter fw = new FileWriter("/Users/jinghuang/IdeaProjects/game3 copy/src/"+this.filename);

            // Write using own method
            String export_string = "";
            for (String key: this.data.keySet()) {
                Object value = this.data.get(key);
                // Different format for each data type
                if (value instanceof String[] s) {
                    String sub_export_string = "";
                    for (String str: s) {
                        sub_export_string += str + ", ";
                    }
                    // Cut off last comma
                    sub_export_string = sub_export_string.substring(0, sub_export_string.length()-2);
                    export_string += "[" + sub_export_string + "]";
                } else if (value instanceof Integer[] i) {
                    String sub_export_string = "";
                    for (Integer str: i) {
                        sub_export_string += str + ", ";
                    }
                    // Cut off last comma
                    sub_export_string = sub_export_string.substring(0, sub_export_string.length()-2);
                    export_string += "[" + sub_export_string + "]";
                } else if (value instanceof String) {
                    export_string += key + ": \"" + value +"\",\n\t";
                } else if (value instanceof Integer) {
                    export_string += key + ": " + value +",\n\t";
                }

            }
            fw.flush();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }*/

}