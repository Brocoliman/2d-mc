import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.Math;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Random;


public class GamePanel extends JPanel implements ActionListener {
    Timer timer;

    // Game variables
    int frame = 0;
    final int DELAY = 10;
    final int WIDTH = 768;
    final int HEIGHT = 768;
    int block_size = 1;
    int middle = 600;

    int octaves = 8;
    int size = 1024;

    int min = 1;
    int range = 75;
    ArrayList<Float> list;
    ArrayList<Integer> res_list;
    Random random = new Random();
    NoiseGenerator noise;

    GamePanel() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT)); // minimum size for the component to display correctly
        this.setBackground(Color.black);
        this.setFocusable(true);
        this.addKeyListener(new KeyAdapter());
        this.addMouseListener(new MouseAdapter());

        startGame();
    }

    public void startGame() {
        list = new ArrayList<>();
        res_list = new ArrayList<>();
        for(int i = 0; i < size; i++) {
            list.add(random.nextFloat());
        }
        makeNoise();
        timer = new Timer(DELAY, this);
        timer.start();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.GREEN);
        for(int i = 0; i < size; i++) {
            int value = res_list.get(i);
            int value2 = (int)(list.get(i)*range+min);
            g.fillRect(i*block_size, middle-value*block_size, block_size, value*block_size);
            g.fillRect(i*block_size, middle-300-value2*block_size, block_size, value2*block_size);
        }

        g.drawString("Frame: " + frame, 50, 50);
        g.drawString("Octaves: " + octaves, 150, 50);

        makeNoise();
        frame++;
    }

    public void makeNoise() {
        noise = new NoiseGenerator(this.size, this.list, range, min);
        noise.PerlinNoise(this.octaves, 1.3f);
        res_list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            res_list.add(noise.perlin_noise.get(i));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    ///////////// Mouse and Key Events /////////////

    public class KeyAdapter extends java.awt.event.KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            // Priority keychecks (active when deathscreen/etc)
            switch (e.getKeyCode()) {
                case KeyEvent.VK_DOWN:
                    octaves --;
                    octaves = Math.max(0, octaves);
                    break;
                case KeyEvent.VK_UP:
                    octaves ++;
                    octaves = Math.min((int)(Math.log(size)/Math.log(2)), octaves);
                    break;
                case KeyEvent.VK_R:
                    startGame();
                default:
                    break;
            }

            makeNoise();
        }
    }

    public class MouseAdapter extends java.awt.event.MouseAdapter {
        @Override
        public void mousePressed (MouseEvent e) {
        }
    }
}
