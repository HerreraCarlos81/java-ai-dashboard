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
import java.util.List;

public class MainController {
    private final ConfigManager configManager;
    private final UsageService usageService;
    private final CacheService cacheService;
    private final ObservableList<DashboardData> modelData;
    private final ListView<DashboardData> modelListView;
    private final Label headerLabel;
    private final Label totalCostLabel;
    private final Label totalTokensLabel;

    public MainController() {
        this.configManager = new ConfigManager();
        this.cacheService = new CacheService();
        this.usageService = new UsageService(cacheService);
        this.modelData = FXCollections.observableArrayList();
        this.modelListView = createListView();
        this.headerLabel = new Label();
        this.totalCostLabel = new Label();
        this.totalTokensLabel = new Label();
    }

    public VBox buildUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.getStyleClass().add("root");

        VBox header = buildHeader();
        VBox summaryBar = buildSummaryBar();
        VBox listContainer = buildListContainer();
        HBox buttonBar = buildButtonBar();

        root.getChildren().addAll(header, summaryBar, listContainer, buttonBar);
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
        modelListView.setCellFactory(lv -> new ModelListCell());
        modelListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, selected) -> showKeyDetails(selected)
        );

        container.getChildren().addAll(listTitle, modelListView);
        return container;
    }

    private HBox buildButtonBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(10, 0, 0, 0));
        bar.setAlignment(Pos.CENTER_RIGHT);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("action-button");
        refreshBtn.setOnAction(e -> refreshData());

        Button settingsBtn = new Button("Settings");
        settingsBtn.getStyleClass().add("action-button");
        settingsBtn.setOnAction(e -> showSettingsDialog());

        bar.getChildren().addAll(refreshBtn, settingsBtn);
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
        try {
            cacheService.invalidateCache();
            List<AiModel> models = configManager.getModels();
            if (models.isEmpty()) {
                modelData.clear();
                totalCostLabel.setText("$0.00");
                totalTokensLabel.setText("0");
                return;
            }
            List<DashboardData> dashboard = usageService.refreshDashboard(models);
            modelData.setAll(dashboard);
            updateSummary(dashboard);
        } catch (Exception e) {
            showAlert("Error", "Failed to refresh data: " + e.getMessage());
        }
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
