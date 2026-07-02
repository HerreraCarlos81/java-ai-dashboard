package com.aiusage.ui;

import com.aiusage.model.DashboardData;
import com.aiusage.util.CostFormatter;
import com.aiusage.util.TokenFormatter;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

public class KeyDetailPane extends VBox {
    private final DashboardData data;

    public KeyDetailPane(DashboardData data) {
        this.data = data;
        setPadding(new Insets(5, 15, 10, 30));
        setSpacing(3);
        getStyleClass().add("key-detail-pane");
        buildContent();
    }

    private void buildContent() {
        List<DashboardData.KeySummary> summaries = data.getKeySummaries();
        if (summaries == null || summaries.isEmpty()) {
            getChildren().add(new Label("No API keys configured"));
            return;
        }
        for (DashboardData.KeySummary ks : summaries) {
            VBox keyBox = new VBox(2);
            keyBox.setPadding(new Insets(4, 0, 4, 0));
            keyBox.getStyleClass().add("key-entry");

            Label headerLabel = new Label(ks.getKeyLabel() + "  --  " + CostFormatter.format(ks.getCost()));
            headerLabel.getStyleClass().add("key-header");

            Label detailLabel = new Label("Tokens: " + TokenFormatter.format(ks.getTokens())
                + "  |  Requests: " + ks.getRequests());
            detailLabel.getStyleClass().add("key-detail");
            detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

            keyBox.getChildren().addAll(headerLabel, detailLabel);

            if (ks.hasError()) {
                Label errLabel = new Label("Error: " + ks.getErrorMessage());
                errLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10px;");
                keyBox.getChildren().add(errLabel);
            }

            getChildren().add(keyBox);
        }
    }
}
