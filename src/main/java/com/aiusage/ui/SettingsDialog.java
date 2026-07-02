package com.aiusage.ui;

import com.aiusage.config.AppConfig;
import com.aiusage.config.ConfigManager;
import com.aiusage.config.ModelConfig;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SettingsDialog extends Dialog<Void> {
    private final ConfigManager configManager;
    private final AppConfig workingConfig;
    private final ObservableList<ModelConfig> modelList;
    private final ListView<ModelConfig> modelListView;
    private final ListView<ModelConfig.ApiKeyConfig> keyListView;
    private final ObservableList<ModelConfig.ApiKeyConfig> keyList;
    private final Label statusLabel;

    public SettingsDialog(ConfigManager configManager) {
        this.configManager = configManager;
        this.workingConfig = copyConfig(configManager.getCurrentConfig());
        this.modelList = FXCollections.observableArrayList();
        this.keyList = FXCollections.observableArrayList();

        setTitle("Settings");
        setHeaderText("Manage AI models and API keys");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        modelListView = new ListView<>(modelList);
        modelListView.setPrefWidth(300);
        modelListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ModelConfig item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName() + " (" + item.getProvider() + ")");
                }
            }
        });
        modelListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, selected) -> showKeysForModel(selected)
        );

        Button addModelBtn = new Button("Add Model");
        addModelBtn.setOnAction(e -> addModel());
        Button editModelBtn = new Button("Edit");
        editModelBtn.setOnAction(e -> editModel());
        Button deleteModelBtn = new Button("Delete");
        deleteModelBtn.setOnAction(e -> deleteModel());

        HBox modelButtons = new HBox(5, addModelBtn, editModelBtn, deleteModelBtn);
        modelButtons.setPadding(new Insets(5, 0, 0, 0));

        VBox leftSide = new VBox(5,
            new Label("Models:"),
            modelListView,
            modelButtons
        );
        leftSide.setPrefWidth(320);

        keyListView = new ListView<>(keyList);
        keyListView.setPrefWidth(300);
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

        Button addKeyBtn = new Button("Add Key");
        addKeyBtn.setOnAction(e -> addKey());
        Button deleteKeyBtn = new Button("Delete");
        deleteKeyBtn.setOnAction(e -> deleteKey());

        HBox keyButtons = new HBox(5, addKeyBtn, deleteKeyBtn);
        keyButtons.setPadding(new Insets(5, 0, 0, 0));

        VBox rightSide = new VBox(5,
            new Label("API Keys:"),
            keyListView,
            keyButtons
        );
        rightSide.setPrefWidth(320);

        HBox mainContent = new HBox(15, leftSide, rightSide);
        mainContent.setPadding(new Insets(10));

        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-label");

        VBox content = new VBox(10, mainContent, statusLabel);
        getDialogPane().setContent(content);
        getDialogPane().setPrefSize(700, 450);

        loadModels();

        setResultConverter(dialogButton -> {
            if (dialogButton != saveButtonType) return null;
            if (saveConfig()) return null;
            return null;
        });
    }

    private void loadModels() {
        modelList.clear();
        if (workingConfig.getModels() != null) {
            modelList.addAll(workingConfig.getModels());
        }
        if (!modelList.isEmpty()) {
            modelListView.getSelectionModel().select(0);
        }
    }

    private void showKeysForModel(ModelConfig model) {
        keyList.clear();
        if (model != null && model.getApiKeys() != null) {
            keyList.addAll(model.getApiKeys());
        }
    }

    private void addModel() {
        AddModelDialog dialog = new AddModelDialog();
        dialog.showAndWait().ifPresent(model -> {
            if (workingConfig.getModels() == null) {
                workingConfig.setModels(new ArrayList<>());
            }
            workingConfig.getModels().add(model);
            modelList.add(model);
            modelListView.getSelectionModel().select(model);
            statusLabel.setText("Model added: " + model.getDisplayName());
        });
    }

    private void editModel() {
        ModelConfig selected = modelListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        AddModelDialog dialog = new AddModelDialog(selected);
        dialog.showAndWait().ifPresent(updated -> {
            int idx = workingConfig.getModels().indexOf(selected);
            workingConfig.getModels().set(idx, updated);
            modelList.set(idx, updated);
            modelListView.getSelectionModel().select(updated);
            statusLabel.setText("Model updated: " + updated.getDisplayName());
        });
    }

    private void deleteModel() {
        ModelConfig selected = modelListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete model \"" + selected.getDisplayName() + "\"?",
            ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            workingConfig.getModels().remove(selected);
            modelList.remove(selected);
            keyList.clear();
            statusLabel.setText("Model deleted: " + selected.getDisplayName());
        }
    }

    private void addKey() {
        ModelConfig selected = modelListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        ApiKeyDialog dialog = new ApiKeyDialog(null);
        dialog.showAndWait().ifPresent(key -> {
            if (selected.getApiKeys() == null) {
                selected.setApiKeys(new ArrayList<>());
            }
            selected.getApiKeys().add(key);
            keyList.add(key);
            statusLabel.setText("Key added: " + key.getLabel());
        });
    }

    private void deleteKey() {
        ModelConfig.ApiKeyConfig selectedKey = keyListView.getSelectionModel().getSelectedItem();
        ModelConfig selectedModel = modelListView.getSelectionModel().getSelectedItem();
        if (selectedKey == null || selectedModel == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete key \"" + selectedKey.getLabel() + "\"?",
            ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            selectedModel.getApiKeys().remove(selectedKey);
            keyList.remove(selectedKey);
            statusLabel.setText("Key deleted: " + selectedKey.getLabel());
        }
    }

    private boolean saveConfig() {
        try {
            AppConfig current = configManager.getCurrentConfig();
            current.setModels(workingConfig.getModels());
            configManager.saveConfig();
            statusLabel.setText("Configuration saved.");
            return true;
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                "Failed to save: " + e.getMessage());
            alert.showAndWait();
            return false;
        }
    }

    private AppConfig copyConfig(AppConfig original) {
        AppConfig copy = new AppConfig();
        copy.setVersion(original.getVersion());
        if (original.getModels() != null) {
            copy.setModels(original.getModels().stream()
                .map(this::copyModel)
                .collect(Collectors.toList()));
        }
        return copy;
    }

    private ModelConfig copyModel(ModelConfig m) {
        ModelConfig copy = new ModelConfig();
        copy.setName(m.getName());
        copy.setDisplayName(m.getDisplayName());
        copy.setProvider(m.getProvider());
        copy.setBaseUrl(m.getBaseUrl());
        copy.setEnabled(m.isEnabled());
        if (m.getApiKeys() != null) {
            copy.setApiKeys(m.getApiKeys().stream()
                .map(this::copyKey)
                .collect(Collectors.toList()));
        }
        return copy;
    }

    private ModelConfig.ApiKeyConfig copyKey(ModelConfig.ApiKeyConfig k) {
        ModelConfig.ApiKeyConfig copy = new ModelConfig.ApiKeyConfig();
        copy.setId(k.getId());
        copy.setLabel(k.getLabel());
        copy.setKey(k.getKey());
        copy.setEnabled(k.isEnabled());
        return copy;
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) return key;
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}
