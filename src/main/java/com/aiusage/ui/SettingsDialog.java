package com.aiusage.ui;

import com.aiusage.config.AppConfig;
import com.aiusage.config.ConfigManager;
import com.aiusage.config.ModelConfig;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SettingsDialog extends Dialog<Void> {
    private final ConfigManager configManager;
    private final AppConfig workingConfig;
    private final ObservableList<ModelConfig> modelList;
    private final ListView<ModelConfig> modelListView;
    private final ObservableList<ModelConfig.ApiKeyConfig> keyList;
    private final TableView<ModelConfig.ApiKeyConfig> keyTable;
    private final TextField keyLabelField;
    private final PasswordField keyValueField;
    private final CheckBox adminCheckBox;
    private final Button addKeyBtn;
    private final Label statusLabel;

    public SettingsDialog(ConfigManager configManager) {
        this.configManager = configManager;
        this.workingConfig = copyConfig(configManager.getCurrentConfig());
        this.modelList = FXCollections.observableArrayList();
        this.keyList = FXCollections.observableArrayList();

        setTitle("Settings");
        setHeaderText(null);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        modelListView = new ListView<>(modelList);
        modelListView.setPrefWidth(280);
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

        Button addModelBtn = new Button("Add");
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
        leftSide.setPrefWidth(300);

        keyTable = new TableView<>(keyList);
        keyTable.setPrefHeight(150);
        keyTable.setPlaceholder(new Label("No keys — add one below"));

        TableColumn<ModelConfig.ApiKeyConfig, String> labelCol = new TableColumn<>("Label");
        labelCol.setCellValueFactory(new PropertyValueFactory<>("label"));
        labelCol.setPrefWidth(120);

        TableColumn<ModelConfig.ApiKeyConfig, String> keyCol = new TableColumn<>("Key");
        keyCol.setCellValueFactory(cellData -> {
            String key = cellData.getValue().getKey();
            return new javafx.beans.property.SimpleStringProperty(maskKey(key));
        });
        keyCol.setPrefWidth(180);

        TableColumn<ModelConfig.ApiKeyConfig, String> adminCol = new TableColumn<>("Admin");
        adminCol.setCellValueFactory(cellData -> {
            boolean admin = cellData.getValue().isAdmin();
            return new javafx.beans.property.SimpleStringProperty(admin ? "Yes" : "");
        });
        adminCol.setPrefWidth(50);

        TableColumn<ModelConfig.ApiKeyConfig, Void> actionCol = new TableColumn<>();
        actionCol.setPrefWidth(50);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button delBtn = new Button("Del");
            {
                delBtn.setOnAction(e -> {
                    ModelConfig.ApiKeyConfig item = getTableView().getItems().get(getIndex());
                    ModelConfig model = modelListView.getSelectionModel().getSelectedItem();
                    if (model != null) {
                        model.getApiKeys().remove(item);
                    }
                    keyList.remove(item);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : delBtn);
            }
        });

        keyTable.getColumns().addAll(labelCol, keyCol, adminCol, actionCol);

        keyLabelField = new TextField();
        keyLabelField.setPromptText("e.g. Production");
        keyValueField = new PasswordField();
        keyValueField.setPromptText("sk-...");

        adminCheckBox = new CheckBox("Admin");
        adminCheckBox.setTooltip(new Tooltip("Admin key can fetch all org usage data (OpenAI only)"));

        addKeyBtn = new Button("Add Key");
        addKeyBtn.setOnAction(e -> addKey());

        HBox inlineAdd = new HBox(5,
            new Label("Label:"), keyLabelField,
            new Label("Key:"), keyValueField,
            adminCheckBox,
            addKeyBtn
        );

        VBox rightSide = new VBox(5,
            new Label("API Keys:"),
            keyTable,
            inlineAdd
        );
        rightSide.setPrefWidth(380);

        HBox mainContent = new HBox(15, leftSide, rightSide);
        mainContent.setPadding(new Insets(10));

        statusLabel = new Label();
        statusLabel.setPadding(new Insets(0, 0, 0, 10));

        VBox content = new VBox(10, mainContent, statusLabel);
        getDialogPane().setContent(content);
        getDialogPane().setPrefSize(730, 420);

        loadModels();

        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                saveConfig();
            }
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
        keyLabelField.clear();
        keyValueField.clear();
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
            statusLabel.setText("Added: " + model.getDisplayName());
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
            statusLabel.setText("Updated: " + updated.getDisplayName());
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
            statusLabel.setText("Deleted: " + selected.getDisplayName());
        }
    }

    private void addKey() {
        ModelConfig selected = modelListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a model first");
            return;
        }
        String label = keyLabelField.getText().trim();
        String key = keyValueField.getText().trim();
        if (label.isEmpty() || key.isEmpty()) return;

        ModelConfig.ApiKeyConfig k = new ModelConfig.ApiKeyConfig();
        k.setId(UUID.randomUUID().toString());
        k.setLabel(label);
        k.setKey(key);
        k.setEnabled(true);
        k.setAdmin(adminCheckBox.isSelected());

        if (selected.getApiKeys() == null) {
            selected.setApiKeys(new ArrayList<>());
        }
        selected.getApiKeys().add(k);
        keyList.add(k);
        keyLabelField.clear();
        keyValueField.clear();
        adminCheckBox.setSelected(false);
        statusLabel.setText("Key added: " + label);
    }

    private void saveConfig() {
        try {
            AppConfig current = configManager.getCurrentConfig();
            current.setModels(workingConfig.getModels());
            configManager.saveConfig();
            statusLabel.setText("Saved.");
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                "Failed to save: " + e.getMessage());
            alert.showAndWait();
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
