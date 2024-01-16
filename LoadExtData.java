import dev.mccue.json.JsonObject;

import dev.mccue.json.Json;
import dev.mccue.json.JsonDecoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LoadExtData {
    // Asset images
    public HashMap<String, BufferedImage> assets = new HashMap<>(); // contain direct link to images
    String[] misc_asset_names; // names of non-item assets to load
    Json json_items; // for first level raw data management
    Map<String, JsonObject> items = new HashMap<>(); // final level data to directly retrieve from

    // Item datas
    public HashMap<String, Integer> break_dur_data = new HashMap<>();
    public HashMap<String, Integer> stack_size_data = new HashMap<>();

    // Ore generation datas
    Json ore_gen_prob_raw_data;
    public ArrayList<HashMap<String, Double>> ore_generation_probability_data = new ArrayList<>();

    // Font
    public Font minecraftia;

    LoadExtData()  {
        // Import item json data
        try (var fileReader = new FileReader("/Users/jinghuang/IdeaProjects/RealGame/src/item_meta_data.json")) {
            json_items = Json.read(fileReader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Convert items JSONObj to non-generic Map
        JsonObject value_dict;
        JsonObject json_items_as_map = JsonDecoder.object(json_items);
        for (String name: json_items_as_map.keySet()) {
            value_dict = JsonDecoder.object(json_items_as_map.get(name));
            this.items.put(name, value_dict);
        }

        // Import font
        try{
            this.minecraftia = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream(
                    "/Users/jinghuang/IdeaProjects/RealGame/res/font/Minecraftia-Regular.ttf"
            ));
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }
    }

    // Insert asset into the list, catching exceptions
    void insertAsset (String name) {
        try {
            assets.put(name, ImageIO.read(
                    new File ("/Users/jinghuang/IdeaProjects/RealGame/src/assets/" + name + ".png")
            ));
        } catch (IOException e) {
            System.out.println("/Users/jinghuang/IdeaProjects/RealGame/src/assets/" + name + ".png");
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Load image assets
    public void loadAssets() {
        // For assets that aren't in itemmetadata --- i.e. they aren't game items, and thus do not have stored names
        // so here is the list of their stored names
        misc_asset_names = new String[]{
                // Hearts
                "hearts/full_heart",
                "hearts/half_heart",
                "hearts/empty_heart",
                "hearts/flashing_lost_full_heart",
                "hearts/flashing_lost_half_heart",
                "hearts/flashing_lost_empty_heart",
                "hearts/flashing_full_heart",
                "hearts/flashing_half_heart",
                "hearts/flashing_empty_heart",

                // GUI
                "gui/hotbar",
                "gui/selected_slot",

                // Block destroy overlays
                "block_states/destroy_stage_0",
                "block_states/destroy_stage_1",
                "block_states/destroy_stage_2",
                "block_states/destroy_stage_3",
                "block_states/destroy_stage_4",
                "block_states/destroy_stage_5",
                "block_states/destroy_stage_6",
                "block_states/destroy_stage_7",
                "block_states/destroy_stage_8",
                "block_states/destroy_stage_9",
        };

        // Load item & block assets
        for(Object item_name: this.items.keySet()) {
            // Default load item
            insertAsset("items/" + item_name);
            // If it is a block, also load block texture
            if(JsonDecoder.field(this.items.get(item_name),
                    "block", JsonDecoder::boolean_)) {
                insertAsset("blocks/" + item_name);
            }
        }

        // Load miscellaneous
        for(String misc: misc_asset_names) {
            insertAsset(misc);
        }
    }

    public void loadOreGenerationProbabilityData() {

    }

    public void loadBlockDurData() {
        // Load block break data
        for( Object item_name: this.items.keySet()) {
            // Has to be a block
            if (JsonDecoder.field(this.items.get(item_name), "block", JsonDecoder::boolean_)) {
                break_dur_data.put((String)item_name,
                        JsonDecoder.field(this.items.get(item_name), "block_dur", JsonDecoder::int_)
                );
            }
        }
    }

    public void loadItemStackSizeData() {
        // Load block break data
        for( Object item_name: this.items.keySet()) {
            stack_size_data.put((String)item_name, JsonDecoder.field(this.items.get(item_name), "stack_size", JsonDecoder::int_));
        }
    }


}
