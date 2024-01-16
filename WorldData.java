import java.util.ArrayList;
import java.util.Random;

public class WorldData
{
    // Metadata
    public ArrayList<Chunk> world_data;
    public int chunk_width;
    public int chunk_height;
    public int num_chunks;
    public int max_num_chunks;
    private LoadExtData ext_data;

    // Generation data
    Random generator_raw_noise;
    NoiseGenerator generator_perlin_noise;
    private ArrayList<Float> generation_raw_noise;

    // New World
    WorldData (int chunk_width, int chunk_height, int max_num_chunks, LoadExtData ext_data) {
        // Initialize variables
        this.num_chunks = 0;
        this.max_num_chunks = max_num_chunks;
        this.chunk_width = chunk_width;
        this.chunk_height = chunk_height;
        this.ext_data = ext_data;
        this.world_data = new ArrayList<> ();

        // Initialize generation
        this.generation_raw_noise = new ArrayList<>();
        this.generator_raw_noise = new Random();

        // Generate raw noise layer for entire world
        float random_height;
        String t = "Noise: ";
        for (int col = 0; col < chunk_width*this.max_num_chunks; col++) {
            random_height = generator_raw_noise.nextFloat();
            generation_raw_noise.add(random_height);
            t += (int)(123+30*random_height) + ", ";
        }
        generation_raw_noise.set(0, 53f);
        generation_raw_noise.set(num_chunks*chunk_width, 53f);
        System.out.println(t);
        t = "\nPerlin:";

        // Generate primary perlin noise layer
        generator_perlin_noise = new NoiseGenerator(
                this.max_num_chunks*this.chunk_width,
                generation_raw_noise, 2, 1);
        generator_perlin_noise.PerlinNoise(8, 2);
        for (int col = 0; col < chunk_width*this.max_num_chunks; col++) {
            t += (int)(0+generator_perlin_noise.perlin_mapping.get(col)) + ", ";
        }
        System.out.println(t);
    }

    // Generate Chunks
    /*
    sea_level=chunk_height/2
    Primary: noise layer; basic hills and stuff, using material and hill heights from biome info
    Secondary: features layer; mountains, ravines, river gulleys, using biome information and generation techniques
    Tertiary: complex morphing layer; inward bends and potentially caves
    */
    public void generateRight () {
        num_chunks++;
        int world_size = num_chunks*chunk_width; // in width

        // Prairie bounds; 123 to 153

        // Add leading information (height map for most recent chunk, just generated) to world data
        int[] chunk_perlin_noise = new int[chunk_width];
        for (int col = 0; col < chunk_width; col++) {
            chunk_perlin_noise[col] = generator_perlin_noise.perlin_noise.get(world_size-chunk_width+col);
        }

        Chunk next_chunk = new Chunk(this.num_chunks-1, this.chunk_width, this.chunk_height, this.ext_data,
                chunk_perlin_noise, "prairie");
        this.world_data.add(next_chunk);

    }
}

