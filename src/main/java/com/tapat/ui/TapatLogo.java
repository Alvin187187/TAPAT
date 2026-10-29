package com.tapat.ui;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Scale;

class TapatLogo {
    private TapatLogo() {
    }

    static HBox wordmark(double height, boolean onDark) {
        Pane mark = mark(height, Color.web(onDark ? "#ffffff" : "#1a3c63"));
        Label name = new Label("TAPAT");
        name.getStyleClass().add("brand");
        if (onDark) {
            name.setStyle("-fx-text-fill: white;");
        }
        HBox row = new HBox(Math.max(8, height * 0.28), mark, name);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    static Pane mark(double height, Color ink) {
        double s = height / 46.0;
        SVGPath tee = new SVGPath();
        tee.setContent(
                "M 6 8 H 31 Q 37 8 37 14 V 16 Q 37 22 31 22 H 24 V 37 Q 24 43 18 43 H 16 Q 10 43 10 37 V 22 H 6 Q 0 22 0 16 V 14 Q 0 8 6 8 Z "
                        + "M 10 13 H 27 V 17 H 21 V 37 H 13 V 17 H 10 Z");
        tee.setFillRule(FillRule.EVEN_ODD);
        tee.setFill(ink);

        Path check = new Path(
                new MoveTo(31, 11),
                new LineTo(35, 16),
                new LineTo(45, 4));
        check.setStroke(ink);
        check.setFill(null);
        check.setStrokeWidth(3.4);
        check.setStrokeLineCap(StrokeLineCap.ROUND);
        check.setStrokeLineJoin(StrokeLineJoin.ROUND);

        Group drawing = new Group(tee, check);
        drawing.getTransforms().add(new Scale(s, s, 0, 0));
        Pane box = new Pane(drawing);
        box.setMinSize(48 * s, 46 * s);
        box.setPrefSize(48 * s, 46 * s);
        box.setMaxSize(48 * s, 46 * s);
        box.setMouseTransparent(true);
        return box;
    }
}
