class Chunk {
    // Settings
    public final int depth; // index of chunk in chunk array
    private final int cols;
    private final int rows;
    public LoadExtData ext_data;

    // Data
    public final Block[] data;

    // Generation metadata
    public String biome;
    public int[] height_map;
    public boolean stable; // can/cannot be a transition for another biome (e.x. a middle of mountain can't, but prairie can)

    // New Chunk with given generation data to display
    Chunk (int depth, int cols, int rows, LoadExtData ext_data, int[]height_map, String biome) {
        // Initialize data
        this.depth = depth;
        this.cols = cols;
        this.rows = rows;
        this.ext_data = ext_data;
        this.data = new Block[cols*rows];

        // Generation metadata
        this.biome = biome;
        this.stable = true;
        this.height_map = height_map;

        // Generate terrain (using base, then each biome chooses additional adjustments)
        this.generate();
    }

    void generate() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int height_level = height_map[j];
                if (i < height_level) {
                    this.data[i*cols+j] = new Block(this.depth, j, i, "stone");
                } else {
                    this.data[i*cols+j] = new Block(this.depth, j, i);
                }
            }
        }
    }

    Block localLocateBlockXY(int x, int y, int block_size) {
        return this.data[x/block_size+(int)Math.ceil(y/(float)block_size)*this.cols];
    }
}