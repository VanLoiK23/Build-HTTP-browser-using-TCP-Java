package controller;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import controller.HTTPSocket.HttpServer;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class ServerController implements Initializable {
	private static Integer PORT = null;
	private static Integer MAX_CONNECT = null;
	private volatile boolean isRunning = false;
	private ExecutorService pool;
	private ServerSocket serverSocket;

	@FXML
	private TextField maxNumberClientTextFileld;

	@FXML
	private TextField portNumberTextField;

	@FXML
	private ScrollPane scrollLog;

	@FXML
	private Button start;

	@FXML
	private Button stop;

	@FXML
	private VBox vBoxInScrollLog;

	private Boolean checkValidTextField(TextField textField) {
		return (textField.getText() != null && !textField.getText().isEmpty());
	}

	private boolean isValidNumber(String input) {
		if (input == null || input.isEmpty())
			return false;
		try {
			Integer.parseInt(input);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private void alertInfo(AlertType alertType, String title, String message) {
		Alert alert = new Alert(alertType);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	@FXML
	void startServer(MouseEvent event) {
		if (checkValidTextField(portNumberTextField) && checkValidTextField(maxNumberClientTextFileld)) {

			if (isValidNumber(portNumberTextField.getText()) && checkValidTextField(maxNumberClientTextFileld)) {
				PORT = Integer.parseInt(portNumberTextField.getText());
				MAX_CONNECT = Integer.parseInt(maxNumberClientTextFileld.getText());
			}
		}

		if (PORT == null && MAX_CONNECT == null) {
			alertInfo(AlertType.WARNING, "Vui lòng điền các thông số cho Server",
					"Server không khởi động do việc thiếu các thông số cần thiết");
		} else {
			isRunning = true;
			start.setDisable(true);
			start.setOpacity(0.3);
			stop.setDisable(false);
			stop.setOpacity(1);
			startServer();
			alertInfo(AlertType.INFORMATION, "Server đã được khởi tạo", "Server chạy trên port " + PORT);

			renderLogging("Server chạy trên PORT " + PORT, "INFO");
		}
	}

	@FXML
	void stopServer(MouseEvent event) {
		if (!isRunning) {
			alertInfo(AlertType.WARNING, "Server chưa được khởi tạo", "Server chưa được khởi động");
		} else {

			isRunning = false;
			start.setDisable(false);
			start.setOpacity(1);
			stop.setDisable(true);
			stop.setOpacity(0.3);

			try {
				if (serverSocket != null && !serverSocket.isClosed()) {
					serverSocket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (pool != null && !pool.isShutdown()) {
				pool.shutdown();
				try {
					if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
						System.out.println("Force shutdown...");
						pool.shutdownNow(); // vẫn còn thì mới ép dừng
					}
				} catch (InterruptedException e) {
					pool.shutdownNow();
					Thread.currentThread().interrupt();
				}

			}

			alertInfo(AlertType.INFORMATION, "Server đã tắt", "Server đã ngừng chạy ");

			renderLogging("Server đã tắt", "INFO");
		}
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		HttpServer.setOnLogging(logging -> {
			Platform.runLater(() -> {
				renderLogging(logging.getMessage(), logging.getType());
			});

		});

		vBoxInScrollLog.heightProperty().addListener((obs, oldVal, newVal) -> {
			scrollLog.setVvalue(1.0);
		});

	}

	private void renderLogging(String message, String type) {

		// type "INFO", "ERROR", "WARNING"
		HBox logRow = new HBox(10);
		logRow.setAlignment(Pos.CENTER_LEFT);
		logRow.setPadding(new Insets(4));
		logRow.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 6;");

		FontAwesomeIcon icon = new FontAwesomeIcon();
		switch (type.toUpperCase()) {
		case "ERROR":
			icon.setGlyphName("EXCLAMATION_TRIANGLE");
			icon.setFill(Color.RED);
			break;
		case "WARNING":
			icon.setGlyphName("EXCLAMATION_CIRCLE");
			icon.setFill(Color.ORANGE);
			break;
		default:
			icon.setGlyphName("INFO_CIRCLE");
			icon.setFill(Color.LIGHTBLUE);
		}
		icon.setSize("16");

		String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
		Label timeLabel = new Label("[" + time + "]");
		timeLabel.setTextFill(Color.GRAY);
		timeLabel.setStyle("-fx-font-size: 12; -fx-font-family: Consolas;");

		// message
		Label msgLabel = new Label(message);
		msgLabel.setWrapText(true);
		msgLabel.setTextFill(Color.WHITE);
		msgLabel.setStyle("-fx-font-size: 13; -fx-font-family: 'Segoe UI';");

		logRow.getChildren().addAll(icon, timeLabel, msgLabel);

		Platform.runLater(() -> {
			vBoxInScrollLog.getChildren().add(logRow);
		});
	}

	private void startServer() {
		Thread serverThread = new Thread(() -> {
			pool = null;
			try {
				serverSocket = new ServerSocket(PORT);

				System.out.println("Server started on port " + PORT);
				isRunning = true;

				pool = Executors.newFixedThreadPool(MAX_CONNECT);

				while (isRunning) {

					try {
						Socket socket = serverSocket.accept();

						if (!isRunning) {
							socket.close();
							break;
						}

						if (socket.isClosed() || !socket.isConnected()) {
							System.out.println("Socket không hợp lệ, bỏ qua");
							continue;
						}

						pool.execute(new HttpServer(socket));
					} catch (IOException e) {
						if (!isRunning)
							break;
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				isRunning = false;
			}
		});
		serverThread.setDaemon(true);
		serverThread.start();
	}

}
