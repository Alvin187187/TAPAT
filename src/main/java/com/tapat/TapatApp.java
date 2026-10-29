package com.tapat;

import com.tapat.ui.MainShell;
import com.tapat.ui.Session;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public class TapatApp extends Application {
    private AppContext context;

    @Override
    public void start(Stage stage) {
        loadFonts();
        context = AppContext.open(Path.of("data"));
        Session session = new Session();
        MainShell shell = new MainShell(context, session);
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1360, Math.max(1100, bounds.getWidth() - 80));
        double height = Math.min(860, Math.max(720, bounds.getHeight() - 80));
        Scene scene = new Scene(shell.getRoot(), width, height);
        scene.getStylesheets().add(TapatApp.class.getResource("/css/tapat.css").toExternalForm());
        stage.setTitle("TAPAT — Community Financial Assistance and Transparency Management System");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.setX(bounds.getMinX() + Math.max(0, (bounds.getWidth() - width) / 2));
        stage.setY(bounds.getMinY() + Math.max(0, (bounds.getHeight() - height) / 2));
        stage.show();
        shell.start();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }

    private void loadFonts() {
        for (String name : List.of(
                "OpenSans-Regular.ttf",
                "OpenSans-SemiBold.ttf",
                "Montserrat-SemiBold.ttf",
                "Montserrat-Bold.ttf")) {
            InputStream stream = TapatApp.class.getResourceAsStream("/fonts/" + name);
            if (stream != null) {
                Font.loadFont(stream, 14);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
