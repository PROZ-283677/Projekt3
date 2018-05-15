package game;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;

public class Main extends Application{
	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("GameWindow.fxml"));
			AnchorPane root = fxmlLoader.load();
			Scene scene = new Scene(root);
			primaryStage.setScene(scene);
			Game controller = fxmlLoader.getController();
			primaryStage.setOnHiding(e -> controller.sendLeaveMessage());
			primaryStage.setTitle("TicTacToe Game");
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
