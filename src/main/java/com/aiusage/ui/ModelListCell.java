package com.aiusage.ui;

import com.aiusage.model.DashboardData;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

public class ModelListCell extends ListCell<DashboardData> {
    @Override
    protected void updateItem(DashboardData item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            setStyle("");
        } else {
            StringBuilder display = new StringBuilder();
            display.append(item.getDisplayName()).append("  |  $")
                .append(String.format("%.2f", item.getTotalCost()));
            if (item.getMonthlyBudget() > 0) {
                double pct = item.getTotalCost() / item.getMonthlyBudget() * 100;
                display.append(" / $").append(String.format("%.2f", item.getMonthlyBudget()))
                    .append("  (").append(String.format("%.0f", pct)).append("%)");
            }
            display.append("  |  ").append(formatTokens(item.getTotalTokens()));

            Label mainLabel = new Label(display.toString());
            VBox box = new VBox(mainLabel);

            String bgColor = "";
            String textColor = "";
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

            if (!textColor.isEmpty()) {
                mainLabel.setStyle(textColor);
            }
            if (!bgColor.isEmpty()) {
                setStyle(bgColor);
            } else {
                setStyle("");
            }

            if (item.hasError()) {
                Label errLabel = new Label("Error: " + item.getLastError());
                errLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10;");
                box.getChildren().add(errLabel);
                if (textColor.isEmpty()) {
                    mainLabel.setStyle("-fx-text-fill: #e74c3c;");
                }
            }

            setGraphic(box);
        }
    }

    private String formatTokens(long tokens) {
        if (tokens < 1_000) return tokens + " tok";
        if (tokens < 1_000_000) return String.format("%.1fK tok", tokens / 1000.0);
        return String.format("%.1fM tok", tokens / 1_000_000.0);
    }
}
