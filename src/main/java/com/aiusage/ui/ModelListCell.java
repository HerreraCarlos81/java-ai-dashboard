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
        } else {
            Label mainLabel = new Label(item.getDisplayName() + "  |  "
                + String.format("$%.2f", item.getTotalCost()) + "  |  "
                + formatTokens(item.getTotalTokens()));
            VBox box = new VBox(mainLabel);
            if (item.hasError()) {
                Label errLabel = new Label("Error: " + item.getLastError());
                errLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10;");
                box.getChildren().add(errLabel);
                mainLabel.setStyle("-fx-text-fill: #e74c3c;");
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
