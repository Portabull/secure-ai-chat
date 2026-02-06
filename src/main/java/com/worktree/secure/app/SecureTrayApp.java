package com.worktree.secure.app;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import netscape.javascript.JSObject;

public class SecureTrayApp extends Application {

    private double dragOffsetX;
    private double dragOffsetY;

    private static final double STEP = 40;
    private static final double MIN_WIDTH = 420;
    private static final double MIN_HEIGHT = 260;

    private static final String WINDOW_TITLE = "SECURE_HIDDEN_WINDOW";

    private JSObject jsWindow; // 🔥 cached JS window

    private JavaBridge bridge;

    /* ---------- Windows API ---------- */
    interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class);

        Pointer FindWindowA(String cls, String title);

        boolean SetWindowDisplayAffinity(Pointer hWnd, int affinity);
    }

    private static final int WDA_EXCLUDEFROMCAPTURE = 0x11;

    @Override
    public void start(Stage ignored) {

        System.out.println(">>> JavaFX Application STARTED <<<");

        Platform.setImplicitExit(false);


        System.setProperty("javafx.animation.pulse", "10");
        System.setProperty("prism.order", "sw");

        /* ---------- Hidden owner (no taskbar) ---------- */
        Stage owner = new Stage(StageStyle.UTILITY);
        owner.setOpacity(0);
        owner.setWidth(1);
        owner.setHeight(1);
        owner.show();
        System.out.println("Stage shown owner: " + owner.isShowing());

        owner.setIconified(true);

        /* ---------- Header ---------- */
        Button wPlus = new Button("W +");
        Button wMinus = new Button("W -");
        Button hPlus = new Button("H +");
        Button hMinus = new Button("H -");
        Button restart = new Button("RESTART");
        Button close = new Button("CLOSE");


        HBox header = new HBox(8, wMinus, wPlus, hMinus, hPlus, restart, close);
        header.setAlignment(Pos.CENTER_RIGHT);
        header.setPrefHeight(36);
        header.setStyle("-fx-background-color:#2c3e50;-fx-padding:4;");

        styleHeaderButton(wPlus);
        styleHeaderButton(wMinus);
        styleHeaderButton(hPlus);
        styleHeaderButton(hMinus);
        styleHeaderButton(restart);
        styleHeaderButton(close);

        /* ---------- WebView ---------- */
        WebView webView = new WebView();
        String page = ConfigManager.exists()
                ? "/secure.html"
                : "/setup.html";

        webView.getEngine().load(
                getClass().getResource(page).toExternalForm()
        );


        /* ---------- JS bridge injection ---------- */
        webView.getEngine().getLoadWorker().stateProperty().addListener(
                (obs, old, state) -> {
                    if (state == javafx.concurrent.Worker.State.SUCCEEDED) {

                        jsWindow = (JSObject)
                                webView.getEngine().executeScript("window");

                        this.bridge = new JavaBridge(webView, jsWindow);
                        jsWindow.setMember("java", this.bridge);

                        jsWindow.call("onJavaReady");
                        jsWindow.call("debug", "Java bridge READY");
                    }
                }
        );

        /* ---------- Layout ---------- */
        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(webView);

        Scene scene = new Scene(root, 500, 620);

        /* ---------- Window ---------- */
        Stage stage = new Stage(StageStyle.UNDECORATED);
        stage.initOwner(owner);
        stage.setTitle(WINDOW_TITLE);
        stage.setScene(scene);
        stage.show();
        System.out.println("Stage shown: " + stage.isShowing());

        stage.setAlwaysOnTop(true);

        /* ---------- Drag ---------- */
        header.setOnMousePressed(e -> {
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
        });

        header.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });

        /* ---------- Resize (FIXED & STABLE) ---------- */
        stage.widthProperty().addListener((o, a, b) -> {
            webView.setDisable(true);
            if (bridge != null) bridge.setResizing(true);
        });

        stage.heightProperty().addListener((o, a, b) -> {
            webView.setDisable(true);
            if (bridge != null) bridge.setResizing(true);
        });

        scene.setOnMouseReleased(e -> Platform.runLater(() -> {
            webView.setDisable(false);
            if (bridge != null) bridge.endResize();
        }));


        wPlus.setOnAction(e -> Platform.runLater(() -> {
            stage.setWidth(stage.getWidth() + STEP);
            if (bridge != null) bridge.endResize();
        }));

        wMinus.setOnAction(e -> Platform.runLater(() -> {
            stage.setWidth(Math.max(MIN_WIDTH, stage.getWidth() - STEP));
            if (bridge != null) bridge.endResize();
        }));

        hPlus.setOnAction(e -> Platform.runLater(() -> {
            stage.setHeight(stage.getHeight() + STEP);
            if (bridge != null) bridge.endResize();
        }));

        hMinus.setOnAction(e -> Platform.runLater(() -> {
            stage.setHeight(Math.max(MIN_HEIGHT, stage.getHeight() - STEP));
            if (bridge != null) bridge.endResize();
        }));


        restart.setOnAction(e -> AppControls.restartApplication());
        close.setOnAction(e -> Platform.exit());


        /* ---------- Hide from screen capture ---------- */
        Pointer hwnd = User32.INSTANCE.FindWindowA(null, WINDOW_TITLE);
        if (hwnd != null) {
            User32.INSTANCE.SetWindowDisplayAffinity(hwnd, WDA_EXCLUDEFROMCAPTURE);
        }
    }

    private void styleHeaderButton(Button b) {
        b.setStyle(
                "-fx-background-color:#455a64;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:12px;" +
                        "-fx-padding:4 10 4 10;"
        );
    }

    public static void main(String[] args) {
        launch(args);
    }
}
