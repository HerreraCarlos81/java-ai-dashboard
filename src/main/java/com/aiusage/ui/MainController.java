package com.aiusage.ui;

import com.aiusage.config.ConfigManager;
import com.aiusage.model.AiModel;
import com.aiusage.model.DashboardData;
import com.aiusage.service.CacheService;
import com.aiusage.service.UsageService;
import com.aiusage.util.CostFormatter;
import com.aiusage.util.DateUtils;
import com.aiusage.util.TokenFormatter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

public class MainController {
    private final ConfigManager configManager;
    private final UsageService usageService;
    private final CacheService cacheService;
    private final ObservableList<DashboardData> modelData;
    private final ListView<DashboardData> modelListView;
    private final Label headerLabel;
    private final Label totalCostLabel;
    private final Label totalTokensLabel;
    private final Label statusLabel;
    private boolean showingKeyDetails;
    private final Button refreshBtn;
    private final ProgressIndicator spinner;

    public MainController() {
        this.configManager = new ConfigManager();
        this.cacheService = new CacheService();
        this.usageService = new UsageService(cacheService);
        this.modelData = FXCollections.observableArrayList();
        this.modelListView = createListView();
        this.headerLabel = new Label();
        this.totalCostLabel = new Label();
        this.totalTokensLabel = new Label();
        this.statusLabel = new Label();
        this.statusLabel.getStyleClass().add("status-label");
        this.statusLabel.setManaged(false);
        this.showingKeyDetails = false;
        this.refreshBtn = new Button("Refresh");
        this.spinner = new ProgressIndicator();
        this.spinner.setPrefSize(18, 18);
        this.spinner.setVisible(false);
    }

    public VBox buildUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.getStyleClass().add("root");

        VBox header = buildHeader();
        VBox summaryBar = buildSummaryBar();
        VBox listContainer = buildListContainer();
        HBox buttonBar = buildButtonBar();

        root.getChildren().addAll(header, summaryBar, listContainer, buttonBar, statusLabel);
        VBox.setVgrow(listContainer, Priority.ALWAYS);
        return root;
    }

    private VBox buildHeader() {
        VBox header = new VBox(5);
        header.setPadding(new Insets(5, 0, 10, 0));

        Label titleLabel = new Label("AI Usage Dashboard");
        titleLabel.getStyleClass().add("dashboard-title");

        headerLabel.getStyleClass().add("dashboard-subtitle");
        headerLabel.setText(DateUtils.formatMonth(LocalDate.now()));

        header.getChildren().addAll(titleLabel, headerLabel);
        return header;
    }

    private VBox buildSummaryBar() {
        VBox bar = new VBox(3);
        bar.setPadding(new Insets(10));
        bar.getStyleClass().add("summary-bar");

        HBox statsRow = new HBox(30);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        totalCostLabel.getStyleClass().add("stat-value");
        totalTokensLabel.getStyleClass().add("stat-value");

        Label costTitle = new Label("Total Spent:");
        costTitle.getStyleClass().add("stat-label");
        HBox costBox = new HBox(5, costTitle, totalCostLabel);

        Label tokensTitle = new Label("Total Tokens:");
        tokensTitle.getStyleClass().add("stat-label");
        HBox tokensBox = new HBox(5, tokensTitle, totalTokensLabel);

        statsRow.getChildren().addAll(costBox, tokensBox);
        bar.getChildren().add(statsRow);
        return bar;
    }

    private VBox buildListContainer() {
        VBox container = new VBox(5);
        Label listTitle = new Label("Configured AI Models");
        listTitle.getStyleClass().add("section-title");

        modelListView.setPrefHeight(400);
        modelListView.setCellFactory(lv -> {
            ModelListCell cell = new ModelListCell();
            cell.setBudgetClickHandler((modelName, currentBudget) -> {
                String prompt = currentBudget > 0
                    ? "Current budget: $" + String.format("%.2f", currentBudget)
                    : "No budget set. Enter a monthly budget (e.g. 50.00):";
                TextInputDialog input = new TextInputDialog(currentBudget > 0 ? String.format("%.2f", currentBudget) : "");
                input.setTitle("Monthly Budget");
                input.setHeaderText("Set budget for " + modelName);
                input.setContentText(prompt);
                Label iconLabel = new Label("\uD83D\uDCB0");
                iconLabel.setStyle("-fx-font-size: 24px;");
                input.getDialogPane().setGraphic(iconLabel);
                input.showAndWait().ifPresent(val -> {
                    String trimmed = val.trim();
                    if (!Pattern.matches("^\\d+(\\.\\d{1,2})?$", trimmed)) {
                        showAlert("Invalid Format",
                            "Enter a number with up to 2 decimal places (e.g. 50.00).");
                        return;
                    }
                    try {
                        double newBudget = Double.parseDouble(trimmed);
                        configManager.getCurrentConfig().getModels().stream()
                            .filter(m -> m.getName().equals(modelName))
                            .findFirst()
                            .ifPresent(m -> m.setMonthlyBudget(newBudget));
                        configManager.saveConfig();
                        refreshData();
                    } catch (Exception ex) {
                        showAlert("Error", "Failed to save budget: " + ex.getMessage());
                    }
                });
            });
            return cell;
        });
        modelListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, selected) -> {
                if (selected != null && !showingKeyDetails) {
                    showingKeyDetails = true;
                    showKeyDetails(selected);
                    showingKeyDetails = false;
                }
            }
        );

        container.getChildren().addAll(listTitle, modelListView);
        return container;
    }

    private HBox buildButtonBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(10, 0, 0, 0));
        bar.setAlignment(Pos.CENTER_RIGHT);

        refreshBtn.getStyleClass().add("action-button");
        refreshBtn.setOnAction(e -> refreshData());

        Button settingsBtn = new Button("Settings");
        settingsBtn.getStyleClass().add("action-button");
        settingsBtn.setOnAction(e -> showSettingsDialog());

        bar.getChildren().addAll(refreshBtn, spinner, settingsBtn);
        return bar;
    }

    public void loadData() {
        try {
            configManager.loadConfig();
            refreshData();
        } catch (Exception e) {
            showAlert("Error", "Failed to load config: " + e.getMessage());
        }
    }

    private void refreshData() {
        refreshBtn.setDisable(true);
        spinner.setVisible(true);
        cacheService.invalidateCache();
        statusLabel.setManaged(false);

        new Thread(() -> {
            try {
                List<AiModel> models = configManager.getModels();
                if (models.isEmpty()) {
                    Platform.runLater(() -> {
                        modelData.clear();
                        totalCostLabel.setText("$0.00");
                        totalTokensLabel.setText("0");
                        statusLabel.setText("No models configured. Click Settings to add one.");
                        statusLabel.setManaged(true);
                        refreshBtn.setDisable(false);
                        spinner.setVisible(false);
                    });
                    return;
                }
                List<DashboardData> dashboard = usageService.refreshDashboard(models);
                Platform.runLater(() -> {
                    modelData.setAll(dashboard);
                    updateSummary(dashboard);
                    String globalError = dashboard.stream()
                        .filter(DashboardData::hasError)
                        .map(d -> d.getDisplayName() + ": " + d.getLastError())
                        .collect(java.util.stream.Collectors.joining(" | "));
                    if (!globalError.isEmpty()) {
                        statusLabel.setText(globalError);
                        statusLabel.setManaged(true);
                        statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    } else {
                        statusLabel.setText("Last refresh: "
                            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                        statusLabel.setManaged(true);
                        statusLabel.setStyle("-fx-text-fill: #4caf50;");
                    }
                    refreshBtn.setDisable(false);
                    spinner.setVisible(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to refresh data: " + e.getMessage());
                    refreshBtn.setDisable(false);
                    spinner.setVisible(false);
                });
            }
        }).start();
    }

    private void updateSummary(List<DashboardData> dashboard) {
        double totalCost = usageService.getTotalCost(dashboard);
        long totalTokens = usageService.getTotalTokens(dashboard);
        totalCostLabel.setText(CostFormatter.format(totalCost));
        totalTokensLabel.setText(TokenFormatter.format(totalTokens));
        modelListView.setItems(modelData);
    }

    private void showKeyDetails(DashboardData selected) {
        if (selected == null) return;
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(selected.getDisplayName() + " - Key Details");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ScrollPane scroll = new ScrollPane(new KeyDetailPane(selected));
        scroll.setFitToWidth(true);
        scroll.setPrefSize(500, 300);
        dialog.getDialogPane().setContent(scroll);
        dialog.setOnHidden(e -> Platform.runLater(() -> modelListView.requestFocus()));
        dialog.showAndWait();
    }

    private void showSettingsDialog() {
        SettingsDialog dialog = new SettingsDialog(configManager);
        dialog.showAndWait().ifPresent(r -> {
            try {
                configManager.loadConfig();
                refreshData();
            } catch (Exception e) {
                showAlert("Error", "Failed to reload config: " + e.getMessage());
            }
        });
    }

    private ListView<DashboardData> createListView() {
        ListView<DashboardData> lv = new ListView<>();
        lv.setItems(modelData);
        return lv;
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
