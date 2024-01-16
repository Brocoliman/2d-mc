class Block {
    public int chunk_idx;
    public String ore;
    public boolean passable = false;
    public int col; // index in chunk (i.e. not with pixel count)
    public int row;
    public int break_dur = 1000000;

    Block (int chunk_idx, int col, int row, String ore) { // new block generation
        this.ore = ore;
        this.col = col;
        this.row = row;
        this.chunk_idx = chunk_idx;
        this.passable = false;
    }

    Block (int chunk_idx, int col, int row) { // air
        this.ore = "air";
        this.col = col;
        this.row = row;
        this.chunk_idx = chunk_idx;
        this.passable = true;
        this.break_dur = 0;
    }

    public void initBreakDur(LoadExtData ext_data) {
        try {
            this.break_dur = ext_data.break_dur_data.get(this.ore);}
        catch (NullPointerException e) {
            System.out.println(this.ore);
        }
    }

    boolean equal(Block block_compare, int block_size, int chunk_width) {
        if (!(this.chunk_idx == block_compare.chunk_idx)) return false;
        return this.globalLocateXBlock(block_size, chunk_width) == block_compare.globalLocateXBlock(block_size, chunk_width)
                && this.globalLocateYBlock(block_size) == block_compare.globalLocateYBlock(block_size);
    }


    int globalLocateXBlock(int block_size, int chunk_width) {
        return chunk_width*block_size* chunk_idx + this.col*block_size;
    }

    int globalLocateYBlock(int block_size) {
        return this.row * block_size;
    }


}