
import java.util.ArrayList;

// Noise between 0 and 1, mapped to certain range
public class NoiseGenerator {
    public int size;
    public ArrayList<Float> random_noise;
    public ArrayList<Integer> perlin_noise;
    public ArrayList<Double> perlin_mapping;
    private float res_range;
    private float res_min;

    NoiseGenerator (int size, ArrayList<Float> random_noise, float res_range, float res_min) {
        this.size = size;
        this.random_noise = random_noise;
        this.perlin_noise = new ArrayList<>();
        this.perlin_mapping = new ArrayList<>();
        this.res_range = res_range;
        this.res_min = res_min;
    }

    public void PerlinNoise (int octaves, float scale_bias) {
        // Iterate over each noise point
        for (int i = 0; i < this.size; i++) {
            // Sum over interpolated values
            float noise = 0;
            float scale = 1f; // to accumulate octaves
            float scale_acc = 0f; // to normalize final noise

            // Each octave, find 2 points to interpolate
            for (int o = 0; o < octaves; o++) {
                int pitch = this.size >> o; // Distance between 2 source points
                int sample_1 = (i / pitch) * pitch; // "rounding" to nearest pitch
                int sample_2 = (sample_1 + pitch) % this.size; // mod wrap around

                // Linear interpolation
                float blend = (float)(i - sample_1) / (float)pitch; // interp percentage
                float sample = (1f - blend) * this.random_noise.get(sample_1) +
                        blend * this.random_noise.get(sample_2);

                // Accumulate noise
                scale_acc += scale;
                noise += scale * sample;
                scale /= scale_bias;
            }

            // Add to result
            this.perlin_noise.add((int)(this.res_range * noise / scale_acc + this.res_min));
            this.perlin_mapping.add((double) (noise / scale_acc));
        }

    }

}