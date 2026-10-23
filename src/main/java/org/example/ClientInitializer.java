
package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TextInputDialog;

import java.io.IOException;
import java.util.Optional;

public class ClientInitializer extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Chat Application");
        dialog.setHeaderText("Enter your username");
        dialog.setContentText("Username:");

        Optional<String> result = dialog.showAndWait();

        if (result.isEmpty() || result.get().trim().isEmpty()) {
            return;
        }

        String username = result.get().trim();

        FXMLLoader loader = new FXMLLoader(
                ClientInitializer.class.getResource("/client.fxml")
        );

        Scene scene = new Scene(loader.load());

        ClientController controller = loader.getController();
        controller.setUsername(username);

        stage.setTitle("Chat - " + username);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
