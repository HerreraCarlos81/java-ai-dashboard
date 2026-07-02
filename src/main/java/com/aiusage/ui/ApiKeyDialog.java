package com.aiusage.ui;

import com.aiusage.config.ModelConfig;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.UUID;

public class ApiKeyDialog extends Dialog<ModelConfig.ApiKeyConfig> {
    public ApiKeyDialog(ModelConfig.ApiKeyConfig existing) {
        boolean editMode = existing != null;
        setTitle(editMode ? "Edit API Key" : "Add API Key");
        setHeaderText(editMode ? "Update API key details" : "Add a new API key");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField labelField = new TextField();
        labelField.setPromptText("e.g. Production, Dev, Team A");

        PasswordField keyField = new PasswordField();
        keyField.setPromptText("sk-...");

        if (editMode) {
            labelField.setText(existing.getLabel());
            keyField.setText(existing.getKey());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Label:"), 0, 0);
        grid.add(labelField, 1, 0);
        grid.add(new Label("API Key:"), 0, 1);
        grid.add(keyField, 1, 1);

        getDialogPane().setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton != saveButtonType) return null;
            if (labelField.getText().isBlank() || keyField.getText().isBlank()) return null;
            ModelConfig.ApiKeyConfig key = new ModelConfig.ApiKeyConfig();
            key.setId(editMode ? existing.getId() : UUID.randomUUID().toString());
            key.setLabel(labelField.getText().trim());
            key.setKey(keyField.getText().trim());
            key.setEnabled(true);
            return key;
        });
    }
}
