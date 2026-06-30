package com.aiusage.ui;

import com.aiusage.model.DashboardData;
import javafx.scene.control.ListCell;

public class ModelListCell extends ListCell<DashboardData> {
    @Override
    protected void updateItem(DashboardData item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(item.getDisplayName() + "  |  "
                + String.format("$%.2f", item.getTotalCost()) + "  |  "
                + formatTokens(item.getTotalTokens()));
        }
    }

    private String formatTokens(long tokens) {
        if (tokens < 1_000) return tokens + " tok";
        if (tokens < 1_000_000) return String.format("%.1fK tok", tokens / 1000.0);
        return String.format("%.1fM tok", tokens / 1_000_000.0);
    }
}
