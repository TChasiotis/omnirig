package com.omnirig.controllers.builder;

import com.omnirig.services.BuildAnalyzer;
import com.omnirig.services.BuildAnalyzer.Resolution;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public final class PcBuilderReportView {

    private PcBuilderReportView() {}

    public static Node build(BuildAnalyzer.Report report, Resolution activeResolution, Consumer<Resolution> onResolutionChange) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14; -fx-border-color: #10B981; -fx-border-width: 1.5; -fx-border-radius: 14;");

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label header = new Label("Γνώμη Τεχνικού OmniRig");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox resSwitch = new HBox(6);
        resSwitch.setAlignment(Pos.CENTER_RIGHT);
        resSwitch.getChildren().add(createResButton(Resolution.FHD_1080P, activeResolution, onResolutionChange));
        resSwitch.getChildren().add(createResButton(Resolution.QHD_1440P, activeResolution, onResolutionChange));
        if (report.supports4K()) {
            resSwitch.getChildren().add(createResButton(Resolution.UHD_4K, activeResolution, onResolutionChange));
        }

        topRow.getChildren().addAll(header, sp, resSwitch);

        HBox scores = new HBox(18,
            createScorePill("Performance", report.performanceScore() + " / 10", "#2563EB"),
            createScorePill("Value / Money", report.valueScore() + " / 10", "#10B981"),
            createScorePill("Balance", report.balanceScore() + " / 10", "#8B5CF6")
        );

        Label verdict = new Label("Bottleneck Check: " + report.bottleneckVerdict());
        verdict.setWrapText(true);
        verdict.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-font-size: 14px;");

        BuildAnalyzer.FpsSet fps = report.getFpsFor(activeResolution);
        String resTag = activeResolution.getLabel();

        HBox fpsRow = new HBox(18,
            createFpsBox("Fortnite (" + resTag + ")", fps.fortnite() + " FPS"),
            createFpsBox("Cyberpunk 2077 (" + resTag + ")", fps.cyberpunk() + " FPS"),
            createFpsBox("Red Dead Redemption 2 (" + resTag + ")", fps.rdr2() + " FPS")
        );

        VBox notesBox = new VBox(6);
        for (String note : report.technicianNotes()) {
            Label nl = new Label("•  " + note);
            nl.setWrapText(true);
            nl.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
            notesBox.getChildren().add(nl);
        }

        card.getChildren().addAll(topRow, scores, verdict, new Separator(), fpsRow, new Separator(), notesBox);
        return card;
    }

    private static Button createResButton(Resolution res, Resolution active, Consumer<Resolution> onSelect) {
        boolean selected = active == res;
        Button btn = new Button(res.getLabel());
        btn.setStyle(selected
            ? "-fx-background-color: #2563EB; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 7; -fx-cursor: hand;"
            : "-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 7; -fx-cursor: hand;");
        btn.setOnAction(e -> onSelect.accept(res));
        return btn;
    }

    private static VBox createScorePill(String label, String value, String color) {
        VBox box = new VBox(2);
        box.setPadding(new Insets(10, 18, 10, 18));
        box.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10;");
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        box.getChildren().addAll(l, v);
        return box;
    }

    private static VBox createFpsBox(String game, String fps) {
        VBox box = new VBox(2);
        box.setPadding(new Insets(10, 16, 10, 16));
        box.setStyle("-fx-background-color: #0F172A; -fx-background-radius: 10;");
        Label g = new Label(game);
        g.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        Label f = new Label(fps);
        f.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 16px; -fx-font-weight: bold;");
        box.getChildren().addAll(g, f);
        return box;
    }
}