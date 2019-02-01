package com.stormbots;/**
 * Created by e on 1/18/19.
 */

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class GraphicalSimulator extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {



            FXMLLoader loader = new FXMLLoader(
                    Application.class.getClass().getResource(
                            "/com/stormbots/PID_Controller.fxml"
                    )
            );


            final Parent root;
            try {
                root = loader.load();
            } catch (IOException e) {
                //todo handle UI interaction here?
                e.printStackTrace();
                return;
            }
            GUI_Controller controller = loader.getController();


            primaryStage.setTitle("PID Simulator");
            root.setVisible(true);
            primaryStage.setScene(new Scene(root, 600, 650));
            primaryStage.show();
            //root.setVisible(true);
            //the last thing we do is pull down the loading message

    }
}
