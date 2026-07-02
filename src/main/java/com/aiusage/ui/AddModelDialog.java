package com.aiusage.ui;

import com.aiusage.config.ModelConfig;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddModelDialog extends Dialog<ModelConfig> {
    private final TextField nameField;
    private final TextField displayNameField;
    private final ComboBox<String> providerField;
    private final TextField baseUrlField;
    private final ListView<ModelConfig.ApiKeyConfig> keyListView;
    private final List<ModelConfig.ApiKeyConfig> keys;
    private final boolean editMode;

    public AddModelDialog() {
        this(null);
    }

    public AddModelDialog(ModelConfig existing) {
        this.editMode = existing != null;
        setTitle(editMode ? "Edit AI Model" : "Add AI Model");
        setHeaderText(editMode ? "Update model configuration" : "Configure a new AI model service");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        nameField = new TextField();
        nameField.setPromptText("e.g. OpenAI GPT-4o");

        displayNameField = new TextField();
        displayNameField.setPromptText("e.g. GPT-4o");

        providerField = new ComboBox<>();
        providerField.getItems().addAll("openai", "anthropic");

        baseUrlField = new TextField();
        baseUrlField.setPromptText("https://api.openai.com/v1");

        keys = new ArrayList<>();
        keyListView = new ListView<>();
        keyListView.setPrefHeight(120);
        keyListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ModelConfig.ApiKeyConfig item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getLabel() + " - " + maskKey(item.getKey()));
                }
            }
        });

        Button addKeyBtn = new Button("+ Add Key");
        addKeyBtn.setOnAction(e -> showApiKeyDialog(null));

        VBox keySection = new VBox(5,
            new Label("API Keys:"),
            keyListView,
            addKeyBtn
        );

        if (editMode) {
            nameField.setText(existing.getName());
            displayNameField.setText(existing.getDisplayName());
            providerField.setValue(existing.getProvider());
            baseUrlField.setText(existing.getBaseUrl());
            if (existing.getApiKeys() != null) {
                keys.addAll(existing.getApiKeys());
                keyListView.getItems().addAll(existing.getApiKeys());
            }
        } else {
            providerField.setValue("openai");
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Display Name:"), 0, 1);
        grid.add(displayNameField, 1, 1);
        grid.add(new Label("Provider:"), 0, 2);
        grid.add(providerField, 1, 2);
        grid.add(new Label("Base URL:"), 0, 3);
        grid.add(baseUrlField, 1, 3);
        grid.add(keySection, 0, 4, 2, 1);

        getDialogPane().setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton != saveButtonType) return null;
            if (nameField.getText().isBlank()) return null;
            ModelConfig config = new ModelConfig();
            config.setName(nameField.getText().trim());
            config.setDisplayName(displayNameField.getText().trim());
            config.setProvider(providerField.getValue());
            config.setBaseUrl(baseUrlField.getText().trim());
            config.setEnabled(true);
            config.setApiKeys(new ArrayList<>(keys));
            return config;
        });
    }

    private void showApiKeyDialog(ModelConfig.ApiKeyConfig existing) {
        ApiKeyDialog dialog = new ApiKeyDialog(existing);
        dialog.showAndWait().ifPresent(key -> {
            if (existing != null) {
                int idx = keys.indexOf(existing);
                keys.set(idx, key);
                keyListView.getItems().set(idx, key);
            } else {
                keys.add(key);
                keyListView.getItems().add(key);
            }
        });
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) return key;
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}
