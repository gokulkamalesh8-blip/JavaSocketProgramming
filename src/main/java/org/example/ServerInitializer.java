package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class ServerInitializer extends Application {

    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage stage) throws IOException {
        stage.setTitle("Chat Server");

        URL fxmlUrl;
        fxmlUrl = ServerInitializer.class.getResource("/server.fxml");

        if (fxmlUrl == null) {
            throw new IOException("server.fxml file not found!");
        }

        stage.setScene(new Scene(FXMLLoader.load(fxmlUrl)));
        stage.show();
    }
}