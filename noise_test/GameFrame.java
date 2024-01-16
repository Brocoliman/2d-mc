import org.json.JSONException;

import javax.swing.*;

public class GameFrame extends JFrame {
    GameFrame() throws JSONException {
        this.add(new GamePanel());

        this.setTitle("Minecraft");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        this.pack(); // fit JFrame around every component
        this.setVisible(true);
        this.setLocationRelativeTo(null); // appear middle of computer
    }
}