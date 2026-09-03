import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.File;

public class ModernAuthApp extends Application {

    private static byte[] secureSecret;
    private int attemptsLeft = 3;

    private TextField inputField;
    private Button verifyButton;
    private Label statusLabel;
    private ProgressBar timeProgress;

    // Securely pass state from Main before launching
    public static void setSecret(byte[] secret) {
        secureSecret = secret;
    }

    public static void launchApp(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        HBox mainLayout = new HBox(40);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setStyle("-fx-background-color: #121212; -fx-padding: 40;");

        // ------------------ LEFT: QR Code Viewer ------------------
        VBox qrBox = new VBox(15);
        qrBox.setAlignment(Pos.CENTER);

        Label scanHeader = new Label("Set up 2FA");
        scanHeader.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', sans-serif;");

        Image qrImage = new Image(new File("totp_qr.png").toURI().toString());
        ImageView qrView = new ImageView(qrImage);
        qrView.setFitWidth(200);
        qrView.setPreserveRatio(true);

        qrBox.getChildren().addAll(scanHeader, qrView);

        // ------------------ RIGHT: Secure Input Form ------------------
        VBox loginBox = new VBox(18);
        loginBox.setAlignment(Pos.CENTER);

        Label promptLabel = new Label("Enter 6-digit Authenticator code");
        promptLabel.setStyle("-fx-text-fill: #a1a1aa; -fx-font-size: 14px; -fx-font-family: 'Segoe UI', sans-serif;");

        // Timer Progress Bar
        timeProgress = new ProgressBar(1.0);
        timeProgress.setPrefWidth(260);
        timeProgress.setStyle("-fx-accent: #2563eb; -fx-control-inner-background: #27272a; -fx-background-insets: 0;");

        inputField = new TextField();
        inputField.setPromptText("123456");
        inputField.setMaxWidth(260);
        inputField.setAlignment(Pos.CENTER);
        inputField.setStyle("-fx-background-color: #27272a; -fx-text-fill: white; -fx-prompt-text-fill: #71717a; -fx-font-size: 20px; -fx-font-family: 'Monospaced'; -fx-padding: 10; -fx-background-radius: 8;");

        verifyButton = new Button("Verify OTP");
        verifyButton.setPrefWidth(260);
        verifyButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");

        statusLabel = new Label("Attempts remaining: 3");
        statusLabel.setStyle("-fx-text-fill: #a1a1aa; -fx-font-size: 13px;");

        verifyButton.setOnAction(e -> handleVerification());
        inputField.setOnAction(e -> handleVerification()); // Enter key support

        loginBox.getChildren().addAll(promptLabel, timeProgress, inputField, verifyButton, statusLabel);
        mainLayout.getChildren().addAll(qrBox, loginBox);

        // ------------------ FRONTEND TIMER ANIMATION ------------------
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Strictly calculates visual progress. No crypto data accessed.
                long millis = System.currentTimeMillis();
                double msInWindow = millis % 30000.0;
                double progress = 1.0 - (msInWindow / 30000.0);
                timeProgress.setProgress(progress);
            }
        };
        timer.start();

        Scene scene = new Scene(mainLayout, 640, 360);
        stage.setTitle("Authentication Portal");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void handleVerification() {
        String entered = inputField.getText().trim();

        // Validation occurs securely via backend fxns module
        if (fxns.check(entered, secureSecret)) {
            statusLabel.setText("Authentication Successful \u2713");
            statusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 15px; -fx-font-weight: bold;");
            verifyButton.setDisable(true);
            inputField.setDisable(true);
        } else {
            attemptsLeft--;
            if (attemptsLeft <= 0) {
                statusLabel.setText("Login Failed. Account Locked.");
                statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 15px; -fx-font-weight: bold;");
                verifyButton.setDisable(true);
                inputField.setDisable(true);
            } else {
                statusLabel.setText("Invalid Code. Attempts remaining: " + attemptsLeft);
                statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px;");
                inputField.clear();
            }
        }
    }
}