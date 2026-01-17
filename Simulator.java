import java.util.Random;

public class Simulator {

    private static final Random r = new Random();
    private static int time = 0;

    public static double generate(double max) {

        if (Settings.mode == SimulationMode.SCRIPTED) {
            time++;
            if (time == 5) return max * 1.4;
            if (time == 8) return -2;
            if (time == 12) return max;
        }

        if (r.nextDouble() < 0.1) return -1;
        if (r.nextDouble() < 0.1) return max * 1.3;

        return r.nextDouble() * max;
    }
}
