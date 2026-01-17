import javafx.animation.*;
import javafx.application.Application;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalTime;
import java.util.*;

public class MainApp extends Application {

    private ObservableList<Appliance> appliances = FXCollections.observableArrayList();
    private Map<String, SocketGroup> groups = new HashMap<>();

    private TableView<Appliance> table;

    private ListView<String> alerts = new ListView<>();
    private ListView<String> recommendations = new ListView<>();

    private List<HistoryEntry> history = new ArrayList<>();
    private ListView<String> historyView = new ListView<>();

    private Label totalLabel = new Label();
    private Label powerLabel = new Label();
    private Label houseStatus = new Label();

    private long lastDangerTime = 0;

    private double energy = 0;

    @Override
    public void start(Stage stage) {

        createData();

        table = createApplianceTable();

        VBox summary = createSummaryPanel();
        TabPane bottomTabs = createBottomTabs();

        ToolBar toolbar = createToolbar(stage);

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setLeft(table);
        root.setRight(summary);
        root.setBottom(bottomTabs);

        Timeline loop = new Timeline(new KeyFrame(Duration.seconds(1), e -> update()));
        loop.setCycleCount(Animation.INDEFINITE);
        loop.play();

        stage.setScene(new Scene(root, 1100, 600));
        stage.setTitle("Smart Home Load Monitor");
        stage.show();
    }

    private void createData() {
        add("Kettle", "Kitchen", "Kitchen", 10, Priority.NON_ESSENTIAL);
        add("Microwave", "Kitchen", "Kitchen", 8, Priority.NON_ESSENTIAL);
        add("Fridge", "Kitchen", "Kitchen", 4, Priority.ESSENTIAL);
        add("TV", "Living Room", "Living", 2, Priority.ESSENTIAL);
        add("Decoder", "Living Room", "Living", 1.5, Priority.NON_ESSENTIAL);
        add("Standing Fan", "Bedroom", "Bedroom", 1.5, Priority.ESSENTIAL);
        add("Iron", "Bedroom", "Bedroom", 10, Priority.NON_ESSENTIAL);
        add("Washing Machine", "Laundry", "Laundry", 8, Priority.NON_ESSENTIAL );

        add("AC", "Bedroom", "Hardwired", 12, Priority.NON_ESSENTIAL);
        add("Ceiling Lights", "Bedroom", "Hardwired", 3, Priority.ESSENTIAL);
        add("Ceiling Fan", "Bedroom", "Hardwired", 1.5, Priority.ESSENTIAL);
        add("AC", "Living Room", "Hardwired", 12, Priority.NON_ESSENTIAL);
        add("Ceiling Lights", "Living Room", "Hardwired", 3, Priority.ESSENTIAL);
        add("Ceiling Fan", "Living Room", "Hardwired", 1.5, Priority.ESSENTIAL);
    }

    private void add(String n, String l, String g, double m, Priority p) {
        Appliance a = new Appliance(n, l, g, m, p);
        appliances.add(a);
        groups.computeIfAbsent(g, SocketGroup::new).add(a);
    }

    private TableView<Appliance> createApplianceTable() {
        TableView<Appliance> t = new TableView<>(appliances);

        t.getColumns().add(col("Appliance", a -> a.getName()));
        t.getColumns().add(col("Location", a -> a.getLocation()));
        t.getColumns().add(col("Socket Group", a -> a.getSocketGroup()));
        t.getColumns().add(col("Current (A)", a -> String.format("%.2f", a.getCurrent())));
        t.getColumns().add(col("Priority", a -> a.getPriority().toString()));
        t.getColumns().add(col("Status", a -> a.getStatus().toString()));

        t.setPrefWidth(700);
        return t;
    }

    private VBox createSummaryPanel() {
        VBox v = new VBox(10,
                new Label("Summary"),
                totalLabel,
                powerLabel,
                houseStatus
        );
        v.setPadding(new Insets(10));
        return v;
    }

    private TabPane createBottomTabs() {
        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Alerts", alerts));
        tabs.getTabs().add(new Tab("Recommendations", recommendations));
        tabs.getTabs().add(new Tab("History", historyView));
        return tabs;
    }

    private ToolBar createToolbar(Stage stage) {
        Button settings = new Button("Settings");
        settings.setOnAction(e -> openSettings(stage));
        return new ToolBar(
                new Label("Smart Home Load Monitor"),
                new Separator(),
                settings
        );
    }

    private void update() {
        alerts.getItems().clear();
        recommendations.getItems().clear();

        double totalI = 0;

        for (Appliance a : appliances) {
            a.update(Simulator.generate(15));

            if (a.getStatus() == ReadingStatus.INVALID) {
                alert("Sensor fault on " + a.getName());
            }
            if (a.getStatus() == ReadingStatus.SURGE) {
                alert("Surge on " + a.getName());
            }
        }

        for (SocketGroup g : groups.values()) {
            totalI += g.getTotalCurrent();
            if (g.getStatus() == ReadingStatus.DANGER)
                alert(g.getName() + " socket overloaded");
        }

        double power = totalI * Settings.voltage;
        energy += power / 3600000;
        double cost = energy * Settings.tariff;

        totalLabel.setText("Total Current: " + String.format("%.2f A", totalI));
        powerLabel.setText("Power: " + String.format("%.0f W | Cost: ₵%.2f", power, cost));

        if (totalI > Settings.mainLimit) {
            houseStatus.setText("HOUSE STATUS: DANGER");
            recommendLoadShedding(totalI);
        } else {
            houseStatus.setText("HOUSE STATUS: OK");
        }

        HistoryEntry snapshot = new HistoryEntry(LocalTime.now(), totalI, power);

        if (LocalTime.now().getMinute() % 10 == 0 && LocalTime.now().getSecond() == 0) {
            history.add(snapshot);

            if (history.size() > 144) { // last 24 hours, every 10 mins
                history.remove(0);
            }

            historyView.getItems().setAll(
                    history.stream().map(HistoryEntry::toString).toList()
            );
        }

        long now = System.currentTimeMillis();

        if (now - lastDangerTime > 10 * 60 * 1000) { // 10 minutes
            if (new Random().nextInt(100) == 0) {
                alert("WARNING OVERLOAD");
                lastDangerTime = now;
            }
        }

        table.refresh();
    }


    private void recommendLoadShedding(double total) {
        appliances.stream()
                .filter(a -> a.getPriority() == Priority.NON_ESSENTIAL)
                .sorted(Comparator.comparingDouble(Appliance::getCurrent).reversed())
                .forEach(a -> recommendations.getItems().add("Switch off " + a.getName()));
    }

    private void alert(String msg) {
        alerts.getItems().add(LocalTime.now().withNano(0) + " - " + msg);
    }

    private void openSettings(Stage owner) {
        Stage s = new Stage();
        TextField limit = new TextField(String.valueOf(Settings.mainLimit));
        TextField tariff = new TextField(String.valueOf(Settings.tariff));

        Button save = new Button("Save");
        save.setOnAction(e -> {
            Settings.mainLimit = Double.parseDouble(limit.getText());
            Settings.tariff = Double.parseDouble(tariff.getText());
            s.close();
        });

        VBox v = new VBox(10,
                new Label("Main Limit (A)"), limit,
                new Label("Tariff"), tariff,
                save
        );
        v.setPadding(new Insets(10));
        s.setScene(new Scene(v));
        s.setTitle("Settings");
        s.show();
    }

    private <T> TableColumn<Appliance, String> col(String name, java.util.function.Function<Appliance, String> f) {
        TableColumn<Appliance, String> c = new TableColumn<>(name);
        c.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(f.apply(d.getValue())));
        return c;
    }

    public static void main(String[] args) {
        launch();
    }
}
