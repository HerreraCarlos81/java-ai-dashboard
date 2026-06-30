package com.aiusage.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class AddModelDialog extends Dialog<String[]> {
    public AddModelDialog() {
        setTitle("Add AI Model");
        setHeaderText("Configure a new AI model service");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. OpenAI GPT-4o");

        TextField displayNameField = new TextField();
        displayNameField.setPromptText("e.g. GPT-4o");

        ComboBox<String> providerField = new ComboBox<>();
        providerField.getItems().addAll("openai", "anthropic");
        providerField.setValue("openai");

        TextField baseUrlField = new TextField();
        baseUrlField.setPromptText("https://api.openai.com/v1");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Display Name:"), 0, 1);
        grid.add(displayNameField, 1, 1);
        grid.add(new Label("Provider:"), 0, 2);
        grid.add(providerField, 1, 2);
        grid.add(new Label("Base URL:"), 0, 3);
        grid.add(baseUrlField, 1, 3);

        getDialogPane().setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new String[]{
                    nameField.getText(),
                    displayNameField.getText(),
                    providerField.getValue(),
                    baseUrlField.getText()
                };
            }
            return null;
        });
    }
}
