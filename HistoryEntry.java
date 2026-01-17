import java.time.LocalTime;

public class HistoryEntry {
    private LocalTime time;
    private double totalCurrent;
    private double totalPower;

    public HistoryEntry(LocalTime time, double totalCurrent, double totalPower) {
        this.time = time;
        this.totalCurrent = totalCurrent;
        this.totalPower = totalPower;
    }

    public LocalTime getTime() { return time; }
    public double getTotalCurrent() { return totalCurrent; }
    public double getTotalPower() { return totalPower; }

    @Override
    public String toString() {
        return time.withNano(0) + " | Current: "
                + String.format("%.2f", totalCurrent) + "A | Power: "
                + String.format("%.0f", totalPower) + "W";
    }
}
