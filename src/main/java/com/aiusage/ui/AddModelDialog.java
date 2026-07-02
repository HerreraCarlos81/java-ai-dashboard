package com.aiusage.ui;

import com.aiusage.config.ModelConfig;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.UUID;

public class AddModelDialog extends Dialog<ModelConfig> {
    private final TextField nameField;
    private final TextField displayNameField;
    private final ComboBox<String> providerField;
    private final TextField baseUrlField;
    private final TextField budgetField;
    private final TextField keyLabelField;
    private final PasswordField keyValueField;
    private final CheckBox adminCheckBox;
    private final ObservableList<ModelConfig.ApiKeyConfig> keys;
    private final TableView<ModelConfig.ApiKeyConfig> keyTable;

    public AddModelDialog() {
        this(null);
    }

    public AddModelDialog(ModelConfig existing) {
        boolean editMode = existing != null;
        setTitle(editMode ? "Edit AI Model" : "Add AI Model");
        setHeaderText(null);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        nameField = new TextField();
        nameField.setPromptText("e.g. OpenAI GPT-4o");
        displayNameField = new TextField();
        displayNameField.setPromptText("e.g. GPT-4o");
        providerField = new ComboBox<>();
        providerField.getItems().addAll("openai", "anthropic");
        providerField.setValue("openai");
        baseUrlField = new TextField();
        baseUrlField.setPromptText("https://api.openai.com/v1");
        budgetField = new TextField();
        budgetField.setPromptText("e.g. 50.00 (0 = no budget)");

        GridPane modelGrid = new GridPane();
        modelGrid.setHgap(10);
        modelGrid.setVgap(8);
        modelGrid.add(new Label("Name:"), 0, 0);
        modelGrid.add(nameField, 1, 0);
        modelGrid.add(new Label("Display Name:"), 0, 1);
        modelGrid.add(displayNameField, 1, 1);
        modelGrid.add(new Label("Provider:"), 0, 2);
        modelGrid.add(providerField, 1, 2);
        modelGrid.add(new Label("Base URL:"), 0, 3);
        modelGrid.add(baseUrlField, 1, 3);
        modelGrid.add(new Label("Monthly Budget ($):"), 0, 4);
        modelGrid.add(budgetField, 1, 4);

        TitledPane modelPane = new TitledPane("Model", modelGrid);
        modelPane.setCollapsible(false);

        keys = FXCollections.observableArrayList();

        keyLabelField = new TextField();
        keyLabelField.setPromptText("e.g. Production");
        keyValueField = new PasswordField();
        keyValueField.setPromptText("sk-...");

        adminCheckBox = new CheckBox("Admin");
        adminCheckBox.setTooltip(new Tooltip("Admin key can fetch all org usage data (OpenAI only)"));

        Button addKeyBtn = new Button("Add");
        addKeyBtn.setOnAction(e -> addKey());

        HBox inlineAdd = new HBox(5,
            new Label("Label:"), keyLabelField,
            new Label("Key:"), keyValueField,
            adminCheckBox,
            addKeyBtn
        );
        inlineAdd.setPadding(new Insets(5, 0, 5, 0));

        keyTable = new TableView<>(keys);
        keyTable.setPrefHeight(130);
        keyTable.setPlaceholder(new Label("No API keys added yet"));

        TableColumn<ModelConfig.ApiKeyConfig, String> labelCol = new TableColumn<>("Label");
        labelCol.setCellValueFactory(new PropertyValueFactory<>("label"));
        labelCol.setPrefWidth(120);

        TableColumn<ModelConfig.ApiKeyConfig, String> keyCol = new TableColumn<>("Key");
        keyCol.setCellValueFactory(cellData -> {
            String key = cellData.getValue().getKey();
            return new javafx.beans.property.SimpleStringProperty(maskKey(key));
        });
        keyCol.setPrefWidth(200);

        TableColumn<ModelConfig.ApiKeyConfig, String> adminCol = new TableColumn<>("Admin");
        adminCol.setCellValueFactory(cellData -> {
            boolean admin = cellData.getValue().isAdmin();
            return new javafx.beans.property.SimpleStringProperty(admin ? "Yes" : "");
        });
        adminCol.setPrefWidth(50);

        TableColumn<ModelConfig.ApiKeyConfig, Void> actionCol = new TableColumn<>();
        actionCol.setPrefWidth(60);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button delBtn = new Button("Del");
            {
                delBtn.setOnAction(e -> {
                    ModelConfig.ApiKeyConfig item = getTableView().getItems().get(getIndex());
                    keys.remove(item);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : delBtn);
            }
        });

        keyTable.getColumns().addAll(labelCol, keyCol, adminCol, actionCol);

        VBox keySection = new VBox(5,
            new Label("API Keys:"),
            inlineAdd,
            keyTable
        );
        TitledPane keyPane = new TitledPane("API Keys", keySection);
        keyPane.setCollapsible(false);

        if (editMode) {
            nameField.setText(existing.getName());
            displayNameField.setText(existing.getDisplayName());
            providerField.setValue(existing.getProvider());
            baseUrlField.setText(existing.getBaseUrl());
            if (existing.getMonthlyBudget() > 0) {
                budgetField.setText(String.format("%.2f", existing.getMonthlyBudget()));
            }
            if (existing.getApiKeys() != null) {
                keys.addAll(existing.getApiKeys());
            }
        }

        VBox content = new VBox(10, modelPane, keyPane);
        content.setPadding(new Insets(10));
        getDialogPane().setContent(content);
        getDialogPane().setPrefSize(550, 460);

        setResultConverter(dialogButton -> {
            if (dialogButton != saveButtonType) return null;
            if (nameField.getText().isBlank()) {
                showError("Name is required");
                return null;
            }
            ModelConfig config = new ModelConfig();
            config.setName(nameField.getText().trim());
            config.setDisplayName(displayNameField.getText().trim());
            config.setProvider(providerField.getValue());
            config.setBaseUrl(baseUrlField.getText().trim());
            config.setEnabled(true);
            config.setMonthlyBudget(parseBudget(budgetField.getText()));
            config.setApiKeys(new ArrayList<>(keys));
            return config;
        });
    }

    private void addKey() {
        String label = keyLabelField.getText().trim();
        String key = keyValueField.getText().trim();
        if (label.isEmpty() || key.isEmpty()) return;
        ModelConfig.ApiKeyConfig k = new ModelConfig.ApiKeyConfig();
        k.setId(UUID.randomUUID().toString());
        k.setLabel(label);
        k.setKey(key);
        k.setEnabled(true);
        k.setAdmin(adminCheckBox.isSelected());
        keys.add(k);
        keyLabelField.clear();
        keyValueField.clear();
        adminCheckBox.setSelected(false);
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) return key;
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

    private double parseBudget(String text) {
        if (text == null || text.isBlank()) return 0;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
