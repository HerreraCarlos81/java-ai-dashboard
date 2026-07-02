package com.aiusage.ui;

import com.aiusage.model.DashboardData;
import com.aiusage.util.CostFormatter;
import com.aiusage.util.TokenFormatter;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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
        if (data.getMonthlyBudget() > 0) {
            double pct = data.getTotalCost() / data.getMonthlyBudget();
            VBox budgetBox = new VBox(3);
            budgetBox.setPadding(new Insets(4, 0, 8, 0));

            Label budgetLabel = new Label("Budget: $"
                + String.format("%.2f", data.getTotalCost()) + " / $"
                + String.format("%.2f", data.getMonthlyBudget())
                + "  (" + String.format("%.0f", pct * 100) + "%)");
            budgetLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

            ProgressBar bar = new ProgressBar(Math.min(pct, 1.0));
            bar.setPrefWidth(300);
            String barColor;
            if (pct >= 1.0) {
                barColor = "-fx-accent: #e74c3c;";
            } else if (pct >= 0.75) {
                barColor = "-fx-accent: #e67e22;";
            } else if (pct >= 0.5) {
                barColor = "-fx-accent: #f1c40f;";
            } else {
                barColor = "-fx-accent: #2ecc71;";
            }
            bar.setStyle(barColor);

            budgetBox.getChildren().addAll(budgetLabel, bar);
            getChildren().add(budgetBox);
        }

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
