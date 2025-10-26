package application;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ServerApplication extends Application {

	@Override
	public void start(Stage stage) {		

		try {

			Parent root = FXMLLoader.load(getClass().getResource("/view/server.fxml"));
			Scene scene = new Scene(root);
			scene.setFill(Color.TRANSPARENT);

			stage.setScene(scene);
			stage.setTitle("Hệ thống Server");
			stage.initStyle(StageStyle.TRANSPARENT);
			stage.show();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		launch(args);
	}
}
