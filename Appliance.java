public class Appliance {

    private String name;
    private String location;
    private String socketGroup;
    private double current;
    private double previousCurrent;
    private double maxCurrent;
    private Priority priority;
    private ReadingStatus status = ReadingStatus.OK;

    public Appliance(String name, String location, String socketGroup,
                     double maxCurrent, Priority priority) {
        this.name = name;
        this.location = location;
        this.socketGroup = socketGroup;
        this.maxCurrent = maxCurrent;
        this.priority = priority;
    }

    public void update(double newCurrent) {

        if (newCurrent <= 0 || newCurrent > maxCurrent) {
            status = ReadingStatus.INVALID;
            return;
        }

        previousCurrent = current;
        current = newCurrent;

        double delta = current - previousCurrent;
        if (delta >= Settings.surgeThreshold) {
            status = ReadingStatus.SURGE;
        } else {
            status = ReadingStatus.OK;
        }
    }


    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getSocketGroup() { return socketGroup; }
    public double getCurrent() { return current; }
    public Priority getPriority() { return priority; }
    public ReadingStatus getStatus() { return status; }
}
