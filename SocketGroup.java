import java.util.ArrayList;
import java.util.List;

public class SocketGroup {

    private String name;
    private List<Appliance> appliances = new ArrayList<>();

    public SocketGroup(String name) {
        this.name = name;
    }

    public void add(Appliance a) {
        appliances.add(a);
    }

    public double getTotalCurrent() {
        return appliances.stream()
                .filter(a -> a.getStatus() != ReadingStatus.INVALID)
                .mapToDouble(Appliance::getCurrent)
                .sum();
    }

    public ReadingStatus getStatus() {
        double i = getTotalCurrent();
        if (i < 10) return ReadingStatus.OK;
        if (i <= 13) return ReadingStatus.WARNING;
        return ReadingStatus.DANGER;
    }

    public List<Appliance> getAppliances() {
        return appliances;
    }

    public String getName() {
        return name;
    }
}
