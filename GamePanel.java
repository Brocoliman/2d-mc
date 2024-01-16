import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.Math;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;


public class GamePanel extends JPanel implements ActionListener {
    Timer timer;

    // Game variables
    final int DELAY = 10;
    final int WIDTH = 768;
    final int HEIGHT = 768;
    int INIT_NUM_CHUNKS = 10;
    int MAX_NUM_CHUNKS = 50;
    int FRAMES_PER_SECOND = 1000/DELAY;
    int TICKS_PER_FRAME = 10; // default 10; how fast non-player related events go
    int frame; // 1 sec is 100 frames
    boolean running = false;

    // Loads
    LoadExtData ext_data = new LoadExtData();
    PlayerData player_data = PlayerData.fromFile("/Users/jinghuang/IdeaProjects/RealGame/src/playerdata.json", ext_data);
    Font MINECRAFTIA = ext_data.minecraftia;

    // Block variables
    final int BLOCK_SIZE = 32;
    final int CHUNK_HEIGHT = 256; // rows per chunk
    final int CHUNK_WIDTH = 16; // blocks per row
    WorldData world;
    Block air = new Block(0, 0, 0);

    // Player mechanic variables
    final int player_size = 25;
    final int PLAYER_SPAWN_X = (int)(INIT_NUM_CHUNKS/2.0* BLOCK_SIZE * CHUNK_WIDTH);
    final int PLAYER_SPAWN_Y = BLOCK_SIZE * CHUNK_HEIGHT /2 + player_size;
    final double GRAVITY = 9.81 / 200000.0 * DELAY * DELAY * BLOCK_SIZE;
    final int PLAYER_SPEED_X = 2;
    final double PLAYER_JUMP_STRENGTH = 4.5;
    double x_offset;
    double y_offset;
    int jump_point = 0;
    int PLAYER_FULL_HP = 20;
    int player_hp = PLAYER_FULL_HP;
    int max_height = 0; // calculate fall dmg

    // Healthbar animation and drawing variables
    final double HEALTHBAR_SCALE = 1.5;
    final int HEALTHBAR_X = WIDTH/10;
    final int HEALTHBAR_Y = HEIGHT*26/32;
    final int HEALTHBAR_SEP = 19;
    final int HEALTH_FRAMES_PER_FLASH = 75;
    final int HEALTH_ANIMATION_FLASHES = 7;
    final int HEALTH_ANIMATION_FRAME_LENGTH = 2 * HEALTH_ANIMATION_FLASHES * HEALTH_FRAMES_PER_FLASH;
    boolean suspend_normal_bar = false;
    int health_animation_frames_left = 0;
    int curr_dmg = 0;

    // Hotbar drawing variables
    final int HOTBAR_SCALE = 3;
    final int HOTBAR_FRAME = 3;
    final int HOTBAR_X = WIDTH/12;
    final int HOTBAR_Y = HEIGHT*28/32;
    final int HOTBAR_SLOT_SIZE = 20;
    final Font HOTBAR_INDICATOR_FONT = new Font("minecraftia", Font.PLAIN, 8* HOTBAR_SCALE);
    final FontMetrics HOTBAR_FONT_METRICS = getFontMetrics(HOTBAR_INDICATOR_FONT);

    // Death screen animation variables
    final int DEATH_ANIMATION_ALPHA = 156;
    int death_animation_frame = 0;
    double death_animation_value = 0;
    boolean respawn_signal = false;

    // Pointer variables
    final int POINT_DISPLAY_RADIUS = 18;
    final int POINT_DISPLAY_WIDTH = 2;
    int point_display_x;
    int point_display_y;
    int point_x;
    int point_y;

    // Camera variables
    double zoom = 1;
    int pan_x = 0;
    int pan_y = 0;

    // Placing & Breaking variables
    double break_time_multiplier = 0.1;
    boolean mouse_down = false;
    int break_start_frame = 0;
    Block target_block;
    Block prev_target_block;

    // Collision detection variables
    Block topleft = null;
    Block topright = null;
    Block botleft = null;
    Block botright = null;
    int collide_y = 0;
    int collide_x = 0;
    boolean hanging = false; // hanging under block
    boolean clinging = false; // clinging to side of a block

    // Player progress variables
    int player_x;
    int player_y; // positive y is down the screen, negative y is up
    int player_hotbar_idx = 0;
    Hotbar player_hotbar;

    // Background variables
    int TICKS_PER_DAYLIGHT_CYCLE = TICKS_PER_FRAME * FRAMES_PER_SECOND * 600;

    // Player Settings
    boolean allow_clinging = false;
    boolean allow_hanging = false;
    int reach_dist = 5; // taxicab dist

    GamePanel() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT)); // minimum size for the component to display correctly
        this.setBackground(Color.black);
        this.setFocusable(true);
        this.addKeyListener(new KeyAdapter());
        this.addMouseListener(new MouseAdapter());

        startGame();
    }

    public void startGame() {
        // Initialize world
        world = new WorldData(CHUNK_WIDTH, CHUNK_HEIGHT, MAX_NUM_CHUNKS, ext_data);
        for (int i = 0; i < INIT_NUM_CHUNKS; i++) {
            world.generateRight();
        }

        // Initialize variables to default values
        player_hotbar = new Hotbar(
                new String[]{null, null, "blocks/dirt", null, null, null, null, null, null},
                new int[]{0, 0, 10, 0, 0, 0, 0, 0, 0},
                ext_data
                );
        timer = new Timer(DELAY, this);
        timer.start();
        running = true;
        frame = 0;
        player_x = PLAYER_SPAWN_X;
        player_y = PLAYER_SPAWN_Y;

        // Load Player values to overlap default values
        player_x = player_data.player_x;
        player_y = player_data.player_y;
        player_hotbar = player_data.player_hotbar;

        // Load Assets
        ext_data.loadAssets();
        ext_data.loadBlockDurData();
        ext_data.loadItemStackSizeData();
    }

    public void paintComponent(Graphics g) {
        // Delay chunk initialize
        if (frame==10) {
            for (int i = 0; i < world.num_chunks; i++) {
                updateChunk(i);
            }
        }

        // Repaint everything
        super.paintComponent(g);

        // Updating
        if (running) {
            updatePointer();
            updateMovement();
            updateBreaking();
        }

        // Draw elements
        drawBkg(g);
        drawChunk(g, player_x/(BLOCK_SIZE * CHUNK_WIDTH));
        for (int dev = 1; dev < 4; dev++) {
            drawChunk(g, Math.min(player_x/(BLOCK_SIZE * CHUNK_WIDTH)+dev, world.num_chunks));
            drawChunk(g, Math.max(player_x/(BLOCK_SIZE * CHUNK_WIDTH)-dev, 0));
        }
        drawTargetedBlock(g);
        drawPlayer(g);
        drawHealthbar(g);
        drawHotbar(g);
        drawDebug(g);
        drawPointer(g);

        if(!running) {
            drawDeathScreen(g);
        }
        frame++;
    }

    ///////////// Handler Functions /////////////

    public int findDisplayX(int x) {
        return WIDTH/2 + (int)((x - player_x + pan_x) * zoom);
    }

    public int findDisplayY(int y) {
        return HEIGHT/2 + (int)((player_y - y + pan_y) * zoom);
    }

    public void takeDamage(int damage) {
        // Keys to activate dmg: set damage, suspend bar, set animation time & draw healthbar handles animation
        curr_dmg = damage;
        suspend_normal_bar = true;
        health_animation_frames_left = HEALTH_ANIMATION_FRAME_LENGTH;
        player_hp -= curr_dmg;

        // Health below 0
        if (player_hp <= 0) {
            // Initialize death screen
            running = false;
        }
    }

    public Block globalLocateBlockXY(int x, int y) {
        int chunk_idx = x/(BLOCK_SIZE * CHUNK_WIDTH);
        // Check if block is outside world
        if (! (chunk_idx >= 0 && chunk_idx < world.num_chunks)) {
            return air;
        }
        x %= BLOCK_SIZE * CHUNK_WIDTH;
        return world.world_data.get(chunk_idx).localLocateBlockXY(x, y, BLOCK_SIZE);
    }

    ///////////// Drawing Functions /////////////

    public void drawBkg(Graphics g) {
        Color color_a = new Color(0, 30, 72);
        Color color_b = new Color(0, 50, 175);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setPaint(new GradientPaint (
                WIDTH/2.0f + (int)(Math.cos(2 * Math.PI * frame*10.0/TICKS_PER_DAYLIGHT_CYCLE)*WIDTH/2),
                HEIGHT/2.0f + (int)(Math.sin(2 * Math.PI * frame*10.0/TICKS_PER_DAYLIGHT_CYCLE)*HEIGHT/2),
                color_a,
                WIDTH/2.0f - (int)(Math.cos(2 * Math.PI * frame*10.0/TICKS_PER_DAYLIGHT_CYCLE)*WIDTH*3/4),
                HEIGHT/2.0f - (int)(Math.sin(2 * Math.PI * frame*10.0/TICKS_PER_DAYLIGHT_CYCLE)*HEIGHT*3/4),
                color_b));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
    }

    public void drawChunk(Graphics g, int idx) {
        // Select chunk to draw
        Chunk chunk;
        try {
            chunk = world.world_data.get(idx);
        } catch (IndexOutOfBoundsException e) {
            return;
        }

        // Draw chunks
        g.setColor(Color.WHITE);
        for (int row = 0; row < CHUNK_HEIGHT; row++) {
            for (int col = 0; col < CHUNK_WIDTH; col++) {
                // Draw block
                Block draw_block = chunk.data[row * CHUNK_WIDTH + col];
                int draw_block_x = draw_block.globalLocateXBlock(BLOCK_SIZE, CHUNK_WIDTH);
                int draw_block_y = draw_block.globalLocateYBlock(BLOCK_SIZE);
                if (draw_block.ore.equals("air")) continue;
                BufferedImage curr_texture = ext_data.assets.get("blocks/" + draw_block.ore);
                g.drawImage( // draw texture
                        curr_texture,
                        findDisplayX(draw_block_x),
                        findDisplayY(draw_block_y),
                        (int)(BLOCK_SIZE * zoom)+1,
                        (int)(BLOCK_SIZE * zoom)+1,
                        null
                );
            }
        }
    }

    public void drawPlayer(Graphics g) {
        // Draw player
        if (clinging && allow_clinging) {
            g.setColor(Color.GREEN);
        } else {
            g.setColor(Color.WHITE);
        }
        g.fillRect(findDisplayX(player_x), findDisplayY(player_y), (int)(player_size*zoom), (int)(player_size*zoom));

    }

    public void drawPointer(Graphics g) {
        // Draw pointer
        g.setColor(Color.WHITE);
        g.fillRect(point_display_x - POINT_DISPLAY_RADIUS, point_display_y, POINT_DISPLAY_RADIUS, POINT_DISPLAY_WIDTH);
        g.fillRect(point_display_x, point_display_y - POINT_DISPLAY_RADIUS, POINT_DISPLAY_WIDTH, POINT_DISPLAY_RADIUS);
        g.fillRect(point_display_x, point_display_y, POINT_DISPLAY_RADIUS, POINT_DISPLAY_WIDTH);
        g.fillRect(point_display_x, point_display_y, POINT_DISPLAY_WIDTH, POINT_DISPLAY_RADIUS);
    }

    public void drawTargetedBlock(Graphics g) {
        // Draw targeted box
        target_block = globalLocateBlockXY(point_x, point_y);
        g.setColor(new Color(255, 255, 255, 32));
        g.fillRect(findDisplayX(target_block.globalLocateXBlock(BLOCK_SIZE, CHUNK_WIDTH)),
                findDisplayY(target_block.globalLocateYBlock(BLOCK_SIZE)),
                BLOCK_SIZE, BLOCK_SIZE);

        // Draw cracks
        if(mouse_down) {
            int frame_percentage = (int)((frame - break_start_frame)/(double)(target_block.break_dur * break_time_multiplier) * 10);
            if (frame - break_start_frame != 0) {
                g.drawImage(
                        ext_data.assets.get("block_states/destroy_stage_" + Math.max(frame_percentage, 0)),
                        findDisplayX(target_block.globalLocateXBlock(BLOCK_SIZE, CHUNK_WIDTH)),
                        findDisplayY(target_block.globalLocateYBlock(BLOCK_SIZE)),
                        (int)(BLOCK_SIZE * zoom)+1,
                        (int)(BLOCK_SIZE * zoom)+1,
                        null
                );
            }
        }
    }

    public void drawHealthbar(Graphics g) {
        // Alternating flash
        boolean flashing = (health_animation_frames_left % (HEALTH_FRAMES_PER_FLASH *2)) < HEALTH_FRAMES_PER_FLASH;
        BufferedImage draw_heart = null;
        for (int i = 0; i < (int) Math.ceil(PLAYER_FULL_HP / 2.0); i++) {
            // When taking damage and actively flashing
            if (suspend_normal_bar && flashing) {
                if (health_animation_frames_left == 0) {
                    suspend_normal_bar = false;
                    curr_dmg = 0;
                }
                int player_old_hp = player_hp + curr_dmg;
                if (player_old_hp <= 2 * i) { // empty heart
                    if (2 * i + 1 <= player_old_hp && 2 * i + 1 >= player_hp) { // was part of lost hearts
                        draw_heart = ext_data.assets.get("hearts/flashing_lost_empty_heart");
                    } else {
                        draw_heart = ext_data.assets.get("hearts/flashing_empty_heart");
                    }
                } else if (player_old_hp == 2 * i + 1) {
                    if (2 * i + 1 <= player_old_hp && 2 * i + 1 >= player_hp) { // was part of lost hearts
                        draw_heart = ext_data.assets.get("hearts/flashing_lost_half_heart");
                    } else {
                        draw_heart = ext_data.assets.get("hearts/flashing_half_heart");
                    }
                } else if (player_old_hp >= 2 * i) {
                    if (2 * i + 1 <= player_old_hp && 2 * i + 1 >= player_hp) { // was part of lost hearts
                        draw_heart = ext_data.assets.get("hearts/flashing_lost_full_heart");
                    } else {
                        draw_heart = ext_data.assets.get("hearts/flashing_full_heart");
                    }
                }
                health_animation_frames_left--;
            } else {
                // This else is a redirect of taking damage but not flashing --- so still need to continue animation
                if (suspend_normal_bar)health_animation_frames_left--;

                // Normal display
                if (player_hp <= 2 * i) {
                    draw_heart = ext_data.assets.get("hearts/empty_heart");
                } else if (player_hp == 2 * i + 1) {
                    draw_heart = ext_data.assets.get("hearts/half_heart");
                } else if (player_hp >= 2 * i) {
                    draw_heart = ext_data.assets.get("hearts/full_heart");
                }
            }
            // Draw
            g.drawImage(
                    draw_heart,
                    HEALTHBAR_X + (int)(i * HEALTHBAR_SEP * HEALTHBAR_SCALE),
                    HEALTHBAR_Y,
                    (int)(32 * HEALTHBAR_SCALE),
                    (int)(32 * HEALTHBAR_SCALE),
                    null
            );
        }
    }

    public void drawHotbar(Graphics g) {
        // Draw original hotbar
        g.drawImage(ext_data.assets.get("gui/hotbar"),
                HOTBAR_X - HOTBAR_SCALE * HOTBAR_FRAME, HOTBAR_Y - HOTBAR_SCALE * HOTBAR_FRAME,
                HOTBAR_SCALE *(9* HOTBAR_SLOT_SIZE +2),
                HOTBAR_SCALE *(HOTBAR_SLOT_SIZE +2), null);

        // Draw selected slot
        g.drawImage(ext_data.assets.get("gui/selected_slot"),
                HOTBAR_X + player_hotbar_idx * HOTBAR_SLOT_SIZE * HOTBAR_SCALE - 12,
                HOTBAR_Y-12,
                24* HOTBAR_SCALE, 24* HOTBAR_SCALE, null);

        // Draw items
        BufferedImage item_draw;
        for (int i = 0; i < 9; i++) {
            if (player_hotbar.amounts[i] == 0) continue;
            item_draw = ext_data.assets.get(player_hotbar.items[i]);
            g.drawImage(item_draw, HOTBAR_X +i* HOTBAR_SCALE * HOTBAR_SLOT_SIZE,
                    HOTBAR_Y, 16* HOTBAR_SCALE, 16* HOTBAR_SCALE, null);
        }

        // Draw indicator numbers
        for (int i = 0; i < 9; i++) {
            // Don't write 0 or 1's
            if (player_hotbar.amounts[i] <= 1) continue;
            int amount = player_hotbar.amounts[i];
            String amount_display = Integer.toString(amount);
            g.setColor(Color.WHITE);
            g.setFont(HOTBAR_INDICATOR_FONT);
            g.drawString(amount_display,
                    HOTBAR_X +((i+1)* HOTBAR_SLOT_SIZE -4)* HOTBAR_SCALE - HOTBAR_FONT_METRICS.stringWidth(amount_display), // center from right
                    HOTBAR_Y +20* HOTBAR_SCALE
            );
        }
    }

    public void drawDebug(Graphics g) {
        // Draw debug boxes
        int c = 10;

        /*
        g.setColor(topleft.is_air ? Color.GREEN : Color.RED);
        g.fillOval(findDisplayX(player_x+(int)x_offset-c/2), findDisplayY(player_y+(int)y_offset+c/2), c, c);
        g.setColor(topright.is_air ? Color.GREEN : Color.RED);
        g.fillOval(findDisplayX(player_x+player_size+(int)x_offset-c/2), findDisplayY(player_y+(int)y_offset+c/2), c, c);
        g.setColor(botleft.is_air ? Color.GREEN : Color.RED);
        g.fillOval(findDisplayX(player_x+(int)x_offset-c/2), findDisplayY(player_y-player_size+(int)y_offset+c/2), c, c);
        g.setColor(botright.is_air ? Color.GREEN : Color.RED);
        g.fillOval(findDisplayX(player_x+player_size+(int)x_offset-c/2), findDisplayY(player_y-player_size+(int)y_offset+c/2), c, c);
         */

        // Draw Text
        g.setColor(Color.WHITE);
        g.drawString("X, Y: " + player_x/BLOCK_SIZE + ", " + player_y/BLOCK_SIZE, 50, 50);
        g.drawString("Biome: " + world.world_data.get(player_x/(BLOCK_SIZE * CHUNK_WIDTH)).biome, 50, 75);
        g.drawString("Chunk: " + player_x/(BLOCK_SIZE*CHUNK_WIDTH), 50, 100);
        g.drawString("Frame: " + frame, 50, 125);
        g.drawString("CX, CY: " + point_x/BLOCK_SIZE + ", " + point_y/BLOCK_SIZE, 50, 150);
        g.drawString("Ore: " + target_block.ore, 50, 175);
    }

    public void drawDeathScreen(Graphics g) {
        // Check for respawn
        if (respawn_signal) {
            // Signal reset
            running = true;
            respawn_signal = false;
            death_animation_frame = 0;
            zoom = 1;
            pan_x = 0;
            pan_y = 0;

            // Player reset
            player_hp = PLAYER_FULL_HP;
            player_x = PLAYER_SPAWN_X;
            player_y = PLAYER_SPAWN_Y;
            x_offset = 0;
            y_offset = 0;
        }

        // Death Screen Red
        death_animation_value = Math.max(0.25d, 1.0d-Math.log(death_animation_frame/132.0+1));
        g.setColor(new Color((int)(255*death_animation_value), 0, 0, DEATH_ANIMATION_ALPHA));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Camera zoom & pan
        zoom = Math.min(Math.sqrt(death_animation_frame/250.0+1), 1.73); // stop zooming at 500 frames
        pan_y = (int)Math.min(Math.sqrt(5*death_animation_frame), 5* BLOCK_SIZE);

        // Decrease animation frames
        death_animation_frame++;

    }

    ///////////// Update Functions /////////////

    public void updateBreaking () {
        if (mouse_down) {
            // Distance must be within reach
            int dist = Math.abs(target_block.globalLocateXBlock(BLOCK_SIZE, CHUNK_WIDTH) - player_x) + Math.abs(target_block.globalLocateYBlock(BLOCK_SIZE) - player_y);
            if (dist > reach_dist * BLOCK_SIZE && !target_block.passable) {
                break_start_frame = frame;
            }

            // Check for changes on breaking block; if changed, restart
            boolean changed_target = (prev_target_block == null || target_block != prev_target_block);
            if (changed_target) {
                break_start_frame = frame;
            }

            // Check if block is break
            if (target_block != null && !target_block.passable && frame-break_start_frame >= break_time_multiplier * target_block.break_dur) {
                // Reset break frame
                break_start_frame = frame;

                // Collect block
                player_hotbar.insert("blocks/"+target_block.ore, 1);

                // Update block to air
                target_block.passable = true;
                target_block.ore = "air";
                updateChunk(target_block.chunk_idx);
            }

            // Set previous target
            prev_target_block = target_block;
        } else {
            break_start_frame = frame;
        }
    }

    // Only do when invoked; very time-consuming func
    // Often invoked with the targeted block
    public void updateChunk(int idx) {
        Chunk chunk = world.world_data.get(idx);
        // Self-referenced updating
        for (int row = 0; row < CHUNK_HEIGHT; row++) {
            for (int col = 0; col < CHUNK_WIDTH; col++) {
                Block update_block = chunk.data[row* CHUNK_WIDTH +col];
                int update_block_x = update_block.globalLocateXBlock(BLOCK_SIZE, CHUNK_WIDTH);
                int update_block_y = update_block.globalLocateYBlock(BLOCK_SIZE);

                // Dirt to grass
                if (update_block.ore.equals("dirt")) {
                    if (globalLocateBlockXY(update_block_x, update_block_y + BLOCK_SIZE / 2).passable) {
                        chunk.data[row * CHUNK_WIDTH + col] = new Block(update_block.chunk_idx, col, row, "grass");
                    }
                }

                // Grass to dirt
                if (update_block.ore.equals("grass")) {
                    if (!globalLocateBlockXY(update_block_x, update_block_y + BLOCK_SIZE / 2).passable) {
                        chunk.data[row * CHUNK_WIDTH + col] = new Block(update_block.chunk_idx, col, row, "dirt");
                    }
                }

                // Update durability property
                chunk.data[row * CHUNK_WIDTH + col].initBreakDur(ext_data);
            }
        }
    }

    // Find where pointer is
    public void updatePointer () {
        // Update pointer
        Point mouse_pos = MouseInfo.getPointerInfo().getLocation();
        Point window_pos = getLocationOnScreen();
        point_display_x = (int)(mouse_pos.getX()-window_pos.getX());
        point_display_y = (int)(mouse_pos.getY()-window_pos.getY());
        point_x = point_display_x - WIDTH/2 + player_x;
        point_y = HEIGHT/2 - point_display_y + player_y;
    }

    // Collision and movement system
    public void updateMovement () {
        // Test x-offsets
        topleft = air;
        topright = air;
        botleft = air;
        botright = air;
        if (x_offset < 0) { // if move left, test left side
            topleft = globalLocateBlockXY(player_x + (int)x_offset, player_y+1);
            botleft = globalLocateBlockXY(player_x + (int)x_offset, player_y-player_size+1);
            collide_x = -1;
        } else if (x_offset > 0) { // if move right, test right side
            topright = globalLocateBlockXY(player_x + player_size + (int)x_offset, player_y+1);
            botright = globalLocateBlockXY(player_x + player_size + (int)x_offset, player_y-player_size+1);
            collide_x = 1;
        }
        if (topleft.passable && topright.passable && botleft.passable && botright.passable) { // no collide on x
            player_x += x_offset;
            collide_x = 0;
        } else { // record collision on x for wall jump
            if(allow_clinging) {
                jump_point = 1;
            }
        }

        // Test y-offsets
        topleft = air;
        topright = air;
        botleft = air;
        botright = air;
        y_offset -= GRAVITY;
        if (y_offset > 0) { // if move up, test top
            topleft = globalLocateBlockXY(player_x, player_y + (int)y_offset);
            topright = globalLocateBlockXY(player_x + player_size, player_y + (int)y_offset);
            collide_y = -1; // collide up
        } else if (y_offset < 0) { // if move down, test bottom
            botleft = globalLocateBlockXY(player_x, player_y - player_size + (int)y_offset);
            botright = globalLocateBlockXY(player_x + player_size, player_y - player_size + (int)y_offset);
            collide_y = 1; // collide down
        }
        if (topleft.passable && topright.passable && botleft.passable && botright.passable) { // no collide on y
            player_y += y_offset;
            collide_y = 0; // if checked but no collision
        } else { // collide on y
            // Prevent hanging if setting not on
            if(y_offset > 0 && !allow_hanging) {
                y_offset = 0;
            } else if(y_offset < 0) {
                y_offset = 0;
            } else {
                y_offset = 0; // do not stack gravitation for next frame
            }

            // Give jump point for y collisions only
            jump_point = 1;

            // Handle fall damage
            if (max_height - player_y >= BLOCK_SIZE * 3) {
                takeDamage((max_height - player_y- BLOCK_SIZE *3)/ BLOCK_SIZE +1);
            }
            max_height = player_y;
        }

        // Update collision information
        // Do not give jump points after hanging if setting is not on
        if (!allow_hanging) { // if grapple not allowed, prevent jump points saving after stop hanging
            if (hanging && collide_y != -1) { // previously hanging and now not
                jump_point = 0;
            }
        }
        hanging = collide_y == -1; // when touching something above
        clinging = collide_y == 0 && // when touching something left or right and not touching block above or below
                (!globalLocateBlockXY(player_x - 5, player_y).passable ||
                        !globalLocateBlockXY(player_x + 5 + BLOCK_SIZE, player_y).passable);
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
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                respawn_signal = true;
            }
            if(!running) return;

            // Movement keys
            if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W) {
                // If on ground, jump point allows for jump
                // If clinging, then the setting will decide whether the player can jump
                // Handling hanging above
                if (!hanging && !(clinging && collide_y == 1) && jump_point > 0) {
                    y_offset = PLAYER_JUMP_STRENGTH;
                    jump_point -= 1;
                } else if (clinging && allow_clinging) {
                    y_offset = PLAYER_JUMP_STRENGTH;
                    jump_point -= 1;
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A) {
                x_offset = -PLAYER_SPEED_X;
            }
            if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D) {
                x_offset = PLAYER_SPEED_X;
            }
            if (e.getKeyCode() == KeyEvent.VK_K) {
                takeDamage(500);
            }

            // Inventory keys
            switch(e.getKeyCode()) {
                case KeyEvent.VK_1: player_hotbar_idx = 0; break;
                case KeyEvent.VK_2: player_hotbar_idx = 1; break;
                case KeyEvent.VK_3: player_hotbar_idx = 2; break;
                case KeyEvent.VK_4: player_hotbar_idx = 3; break;
                case KeyEvent.VK_5: player_hotbar_idx = 4; break;
                case KeyEvent.VK_6: player_hotbar_idx = 5; break;
                case KeyEvent.VK_7: player_hotbar_idx = 6; break;
                case KeyEvent.VK_8: player_hotbar_idx = 7; break;
                case KeyEvent.VK_9: player_hotbar_idx = 8; break;
                default: break;
            }

            // Load & Save keys
            if (e.getKeyCode() == KeyEvent.VK_Y) {
                player_data.toFile("/Users/jinghuang/IdeaProjects/RealGame/src/playerdata.json", player_x, player_y, player_hotbar);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if(!running) return;
            if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W
                    || e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
                // Prevent stopping midair or when hitting side of wall
                if (jump_point > 0 && collide_x == 0) {
                    y_offset = 0;
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT
                    || e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_D) {
                x_offset = 0;
            }
        }
    }

    public class MouseAdapter extends java.awt.event.MouseAdapter {
        @Override
        public void mousePressed (MouseEvent e) {
            if(!running) return;
            // distance must be within reach
            int dist = Math.abs(target_block.globalLocateXBlock(BLOCK_SIZE, CHUNK_WIDTH)-player_x) + Math.abs(target_block.globalLocateYBlock(BLOCK_SIZE)-player_y);
            if (e.getButton() == MouseEvent.BUTTON1) { // left button = break
                mouse_down = true; // triggers breaking
                break_start_frame = frame;
            }
            if (e.getButton() == MouseEvent.BUTTON3) { // right button = place
                if (target_block.passable && dist <= reach_dist * BLOCK_SIZE && dist > BLOCK_SIZE * 0.8) {
                    int target_x = target_block.globalLocateXBlock(BLOCK_SIZE, CHUNK_WIDTH);
                    int target_y = target_block.globalLocateYBlock(BLOCK_SIZE);
                    Block left = globalLocateBlockXY(target_x-BLOCK_SIZE, target_y);
                    Block right = globalLocateBlockXY(target_x+BLOCK_SIZE, target_y);
                    Block up = globalLocateBlockXY(target_x, target_y+BLOCK_SIZE);
                    Block down = globalLocateBlockXY(target_x, target_y-BLOCK_SIZE);
                    boolean adjacent = !(left.passable && right.passable && up.passable && down.passable);
                    if (adjacent && player_hotbar.amounts[player_hotbar_idx] > 0) {
                        // Pull identification of the inventory item
                        String[] block_name_dir = player_hotbar.items[player_hotbar_idx].split("/");

                        // Must be a block (1st specification)
                        if (!block_name_dir[0].equals("blocks")) return;

                        // Place block
                        world.world_data.get(target_block.chunk_idx).data[target_block.col + target_block.row * CHUNK_WIDTH] =
                                new Block(target_block.chunk_idx, target_block.col, target_block.row, block_name_dir[block_name_dir.length-1]);
                        world.world_data.get(target_block.chunk_idx).data[target_block.col + target_block.row * CHUNK_WIDTH].initBreakDur(ext_data);
                        updateChunk(target_block.chunk_idx);
                        player_hotbar.amounts[player_hotbar_idx]--;
                    }
                }
            }
        }
        public void mouseReleased (MouseEvent e) {
            mouse_down = false;
        }
    }
}