package dirsizerecognizer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;

public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			SplitPane root = (SplitPane) FXMLLoader
					.load(getClass().getResource("/fxml/DirSizeRecognizer.fxml"));
			Scene scene = new Scene(root);
			scene.getStylesheets()
					.add(getClass().getResource("/css/yellowOnBlack.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("Dir Size Recognizer");
			primaryStage.show();
			primaryStage.setMinWidth(primaryStage.getWidth());
			primaryStage.setMinHeight(primaryStage.getHeight());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}