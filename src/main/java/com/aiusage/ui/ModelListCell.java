package com.aiusage.ui;

import com.aiusage.model.DashboardData;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.BiConsumer;

public class ModelListCell extends ListCell<DashboardData> {
    private BiConsumer<String, Double> budgetClickHandler;

    public void setBudgetClickHandler(BiConsumer<String, Double> handler) {
        this.budgetClickHandler = handler;
    }

    @Override
    protected void updateItem(DashboardData item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            setStyle("");
        } else {
            Label mainLabel = new Label(item.getDisplayName() + "  |  "
                + String.format("$%.2f", item.getTotalCost()) + "  |  "
                + formatTokens(item.getTotalTokens()));

            String textColor = "";
            String bgColor = "";
            if (item.getMonthlyBudget() > 0) {
                double pct = item.getTotalCost() / item.getMonthlyBudget() * 100;
                if (pct >= 100) {
                    bgColor = "-fx-background-color: #5c1a1a;";
                    textColor = "-fx-text-fill: #ff6b6b; -fx-font-weight: bold;";
                } else if (pct >= 75) {
                    bgColor = "-fx-background-color: #5c3a1a;";
                    textColor = "-fx-text-fill: #ffb86b;";
                } else if (pct >= 50) {
                    bgColor = "-fx-background-color: #5c5a1a;";
                    textColor = "-fx-text-fill: #f1fa8c;";
                }
            }

            if (!textColor.isEmpty()) mainLabel.setStyle(textColor);
            if (!bgColor.isEmpty()) setStyle(bgColor);
            else setStyle("");

            VBox infoBox = new VBox(mainLabel);
            if (item.hasError()) {
                Label errLabel = new Label("Error: " + item.getLastError());
                errLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10;");
                infoBox.getChildren().add(errLabel);
                if (textColor.isEmpty()) mainLabel.setStyle("-fx-text-fill: #e74c3c;");
            }

            Label budgetBadge = buildBudgetBadge(item);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            HBox row = new HBox(10, infoBox, budgetBadge);
            row.setAlignment(Pos.CENTER_LEFT);
            setGraphic(row);
        }
    }

    private Label buildBudgetBadge(DashboardData item) {
        Label badge = new Label();
        badge.setPadding(new Insets(2, 8, 2, 8));
        badge.setStyle("-fx-border-color: #555; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-font-size: 11px; -fx-cursor: hand;");
        badge.setCursor(Cursor.HAND);

        if (item.getMonthlyBudget() <= 0) {
            badge.setText("\uD83D\uDCB0 Budget not set");
            badge.setStyle(badge.getStyle() + "-fx-text-fill: #888;");
        } else {
            double pct = item.getTotalCost() / item.getMonthlyBudget() * 100;
            badge.setText(String.format("\uD83D\uDCB0 $%.0f (%.0f%%)", item.getMonthlyBudget(), pct));
            if (pct >= 100) {
                badge.setStyle(badge.getStyle() + "-fx-text-fill: #ff6b6b; -fx-border-color: #ff6b6b;");
            } else if (pct >= 75) {
                badge.setStyle(badge.getStyle() + "-fx-text-fill: #ffb86b; -fx-border-color: #ffb86b;");
            } else if (pct >= 50) {
                badge.setStyle(badge.getStyle() + "-fx-text-fill: #f1fa8c; -fx-border-color: #f1fa8c;");
            } else {
                badge.setStyle(badge.getStyle() + "-fx-text-fill: #4caf50; -fx-border-color: #4caf50;");
            }
        }

        badge.setOnMouseClicked(e -> {
            if (budgetClickHandler != null) {
                budgetClickHandler.accept(item.getModelName(), item.getMonthlyBudget());
            }
        });

        return badge;
    }

    private String formatTokens(long tokens) {
        if (tokens < 1_000) return tokens + " tok";
        if (tokens < 1_000_000) return String.format("%.1fK tok", tokens / 1000.0);
        return String.format("%.1fM tok", tokens / 1_000_000.0);
    }
}
