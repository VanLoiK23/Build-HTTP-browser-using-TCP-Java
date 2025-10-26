package controller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import controller.HTTPSocket.HttpClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import model.HttpMessage;

public class BrowserController implements Initializable {

	@FXML
	private ChoiceBox<String> choiceTypeRequest;

	@FXML
	private ScrollPane scrollPaneBody;

	@FXML
	private ScrollPane scrollPaneHeader;

	@FXML
	private TextField searchTextField;

	@FXML
	private VBox vBoxInScroll_Body;

	@FXML
	private VBox vBoxInScroll_Header;

	private HttpClient httpClient = new HttpClient();

	@FXML
	void submit(ActionEvent event) {
		String method = choiceTypeRequest.getValue();
		String urlStr = searchTextField.getText();

		if (urlStr.isEmpty()) {
			alertInfo(AlertType.WARNING, "Thiếu thông tin!!!", "Vui lòng nhập URL!");
			return;
		}

		try {
			URL url = new URL(urlStr);
			String host = url.getHost();
			int port = (url.getPort() == -1) ? 80 : url.getPort();
			String path = url.getPath().isEmpty() ? "/" : url.getPath();

			HttpMessage httpMessage = new HttpMessage();
			httpMessage.setHost(host);
			httpMessage.setMethod(method);
			httpMessage.setPath(path);
			httpMessage.setPort(port);

			httpClient.sendUrlRequest(httpMessage);

			vBoxInScroll_Body.getChildren().clear();
			vBoxInScroll_Header.getChildren().clear();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void analyzeHTML(String html) throws IOException {
		StringWriter stringWriter = new StringWriter();
		BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);

		int pCount = countTags(html, "p");
		int divCount = countTags(html, "div");
		int spanCount = countTags(html, "span");
		int imgCount = countTags(html, "img");

		bufferedWriter.write("Số lượng <p>: " + pCount + "\n");
		bufferedWriter.write("Số lượng <div>: " + divCount + "\n");
		bufferedWriter.write("Số lượng <span>: " + spanCount + "\n");
		bufferedWriter.write("Số lượng <img>: " + imgCount + "\n");
		bufferedWriter.write("Chiều dài HTML: " + html.length() + " ký tự\n");

		bufferedWriter.flush();

		Label headerLabel = new Label("\n----- PHÂN TÍCH HTML -----\n");
		Label bodLabel = new Label(stringWriter.toString());
		vBoxInScroll_Header.getChildren().addAll(headerLabel, bodLabel);

		bufferedWriter.close();
	}

	private static int countTags(String html, String tag) {
		Matcher matcher = Pattern.compile("<" + tag + "\\b", Pattern.CASE_INSENSITIVE).matcher(html);
		int count = 0;
		while (matcher.find())
			count++;
		return count;
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		vBoxInScroll_Header.heightProperty().addListener((obs, oldVal, newVal) -> {
			scrollPaneHeader.setVvalue(1.0);
		});
		vBoxInScroll_Body.heightProperty().addListener((obs, oldVal, newVal) -> {
			scrollPaneBody.setVvalue(1.0);
		});

		choiceTypeRequest.getItems().addAll("GET", "POST", "HEAD");
		choiceTypeRequest.setValue("GET");

		// HEADER
		httpClient.setHeaderHandler(header -> {
			Platform.runLater(() -> {
				Label headerLabel = new Label("=== HEADER ===\n" + header);
				headerLabel.setStyle("-fx-font-family: Consolas; -fx-text-fill: blue;");
				vBoxInScroll_Header.getChildren().add(headerLabel);
			});
		});

		// BODY
		httpClient.setBodyHandler(body -> {
			if (body != null && !body.isEmpty()) {
				System.out.println("=== BODY ===");
				System.out.println(body);

				Platform.runLater(() -> {
					if (body.trim().startsWith("<!DOCTYPE") || body.contains("<html")) {
						// if html load to webview

						try {
							analyzeHTML(body);

							WebView webview = new WebView();
							webview.getEngine().loadContent(body);

							webview.getEngine().documentProperty().addListener((obs, oldDoc, newDoc) -> {
								if (newDoc != null) {
									webview.getEngine().executeScript("""
											    var body = document.body, html = document.documentElement;
											    Math.max(body.scrollHeight, body.offsetHeight,
											             html.clientHeight, html.scrollHeight, html.offsetHeight);
											""");
								}
							});

							webview.setPrefHeight(600);
							vBoxInScroll_Body.getChildren().add(webview);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						Label bodyLabel = new Label("=== BODY ===\n" + body);
						bodyLabel.setStyle("-fx-font-family: Consolas; -fx-text-fill: black;");
						vBoxInScroll_Body.getChildren().add(bodyLabel);
					}
				});
			}
		});

		// ERROR
		httpClient.setErrorHandler(err -> {
			Platform.runLater(() -> {
				alertInfo(AlertType.ERROR, "Can't search with URL", err);
			});
		});
	}

	private void alertInfo(AlertType alertType, String title, String message) {
		Alert alert = new Alert(alertType);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

}
