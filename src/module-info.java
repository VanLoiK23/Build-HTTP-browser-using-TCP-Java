module BrowserHTTP {
	requires javafx.controls;
	requires javafx.fxml;
	requires fontawesomefx;
	requires javafx.web;
	
	opens application to javafx.graphics, javafx.fxml;
	opens controller to javafx.fxml; // 👈 cho phép FXMLLoader truy cập
	opens controller.HTTPSocket to javafx.fxml; 

	exports model;
	exports controller; // nếu bạn muốn gói này public cho module khác
	exports controller.HTTPSocket;
}
