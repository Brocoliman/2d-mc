import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;

public class ReadJSON {
    String filename;
    JSONObject data;
    JSONParser parser = new JSONParser();

    ReadJSON (String filename) {
        this.filename = filename;
    }

    public JSONObject readJSON() {
        try {
            Object raw_data = parser.parse(new FileReader("/Users/jinghuang/IdeaProjects/game3 copy/src/"+this.filename));
            this.data = (JSONObject) raw_data;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return this.data;
    }

}