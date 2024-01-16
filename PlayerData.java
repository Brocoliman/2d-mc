import dev.mccue.json.Json;
import dev.mccue.json.JsonDecodeException;
import dev.mccue.json.JsonDecoder;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class PlayerData {
    public int player_x;
    public int player_y;
    public Hotbar player_hotbar;

    public PlayerData(Json json, LoadExtData ext_data) {
        // Asserts the player_x and player_y value can be fetched accordingly with integer standards
        this.player_x = JsonDecoder.field(json, "player_x", JsonDecoder::int_);
        this.player_y = JsonDecoder.field(json, "player_y", JsonDecoder::int_);

        // Same for arrays
        var hotbarAmounts = JsonDecoder.field(json, "player_hotbar_amnts", JsonDecoder::array);
        var hotbarItems = JsonDecoder.field(json, "player_hotbar_items", JsonDecoder::array);

        if (hotbarAmounts.size() != 9) {
            throw JsonDecodeException.of("Expected exactly 9 hotbar amounts", hotbarAmounts);
        }

        if (hotbarItems.size() != 9) {
            throw JsonDecodeException.of("Expected exactly 9 hotbar items", hotbarAmounts);
        }

        this.player_hotbar = new Hotbar(ext_data);

        for (int i = 0; i < 9; i++) {
            // Now asserts parts of index and fetches
            player_hotbar.items[i] = JsonDecoder.index(hotbarItems, i, JsonDecoder::string);
            player_hotbar.amounts[i] = JsonDecoder.index(hotbarAmounts, i, JsonDecoder::int_);
        }
    }

    public static PlayerData fromFile(String path, LoadExtData ext_data) {
        try (var fileReader = new FileReader(path)) {
            return new PlayerData(Json.read(fileReader), ext_data);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Json toJson(int player_x, int player_y, Hotbar player_hotbar) {
        // Update object values
        this.player_x = player_x;
        this.player_y = player_y;
        this.player_hotbar = player_hotbar;

        // Essentially objectBuilder is the json export package
        var objectBuilder = Json.objectBuilder();
        objectBuilder.put("player_x", this.player_x);
        objectBuilder.put("player_y", this.player_y);

        List<Json> hotbarAmounts = new ArrayList<>();
        List<Json> hotbarItems = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            // Json.of turns to Json type of that data
            hotbarAmounts.add(Json.of(this.player_hotbar.amounts[i]));
            hotbarItems.add(Json.of(this.player_hotbar.items[i]));
        }

        // Push into the objectBuilder
        objectBuilder.put("player_hotbar_amnts", hotbarAmounts);
        objectBuilder.put("player_hotbar_items", hotbarItems);

        // Finishes build and converts to Json
        return objectBuilder.build();
    }

    public void toFile(String path, int player_x, int player_y, Hotbar player_hotbar) {
        try (var fileWriter = new FileWriter(path)) {
            Json.write(this.toJson(player_x, player_y, player_hotbar), fileWriter);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}