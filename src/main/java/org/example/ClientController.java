package org.example;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.io.File;
import java.nio.file.Files;

public class ClientController implements Initializable {

    private static final long MAX_FILE_SIZE = 25L * 1024 * 1024;

    @FXML private Button client_send_btn;
    @FXML private TextArea client_text_area;
    @FXML private TextField client_text_field;
    @FXML private Button emoji_btn;
    @FXML private Button send_file_btn;

    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private String username;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        client_text_area.setStyle("-fx-font-family: 'Segoe UI Emoji';");
    }
    public void setUsername(String username) {
        this.username = username;
        setClient();
    }



    private Socket remoteSocket;

    private void setClient() {
        try {
            // Keep the socket open while the client is connected
            remoteSocket = new Socket("127.0.0.1", 3000);

            dataOutputStream =
                    new DataOutputStream(remoteSocket.getOutputStream());

            dataInputStream =
                    new DataInputStream(remoteSocket.getInputStream());

            // Send username as the first message
            dataOutputStream.writeUTF(username);
            dataOutputStream.flush();

            Thread receiverThread = new Thread(() -> {
                try {
                    while (!remoteSocket.isClosed()) {
                        byte messageType = dataInputStream.readByte();

                        if (messageType == 0) {
                            String message = dataInputStream.readUTF();
                            toChat(message);

                        } else if (messageType == 4) {
                            String errorMessage = dataInputStream.readUTF();

                            toChat("[Connection rejected] " + errorMessage);

                            Platform.runLater(() -> {
                                client_text_field.setDisable(true);
                                client_send_btn.setDisable(true);
                                send_file_btn.setDisable(true);
                            });

                            remoteSocket.close();
                            break;

                        } else if (messageType == 1 || messageType == 2 || messageType == 3) {
                            // Keep your existing file reception code here,
                            // starting from:
                            int filenameLen = dataInputStream.readInt();

                            if (filenameLen <= 0 || filenameLen > 4096) {
                                throw new IOException("Invalid filename length");
                            }


                            byte[] filenameBytes =
                                    dataInputStream.readNBytes(filenameLen);

                            if (filenameBytes.length != filenameLen) {
                                throw new IOException("Incomplete filename received");
                            }

                            String filename = new String(
                                    filenameBytes,
                                    java.nio.charset.StandardCharsets.UTF_8
                            );

                            long fileSize = dataInputStream.readLong();

                            if (fileSize < 0 || fileSize > MAX_FILE_SIZE) {
                                throw new IOException("File exceeds the 25 MB limit");
                            }

                            byte[] fileData =
                                    dataInputStream.readNBytes((int) fileSize);

                            if (fileData.length != (int) fileSize) {
                                throw new IOException("Incomplete file received");
                            }

                            File outFile = new File(
                                    System.getProperty("user.home")
                                            + "/Downloads/"
                                            + filename.replace("/", "_")
                                            .replace("\\", "_")
                            );

                            Files.write(outFile.toPath(), fileData);

                            toChat("[File received: " + filename
                                    + " saved to " + outFile.getAbsolutePath() + "]");
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Server disconnected: " + e.getMessage());
                }
            });

            receiverThread.setDaemon(true);
            receiverThread.start();

        } catch (IOException e) {
            System.err.println("Client connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    public void sendMessage() {
        String msg = client_text_field.getText().trim();

        if (msg.isEmpty()) {
            return;
        }

        if (remoteSocket == null
                || remoteSocket.isClosed()
                || !remoteSocket.isConnected()) {
            toChat("Not connected to the server. Please reconnect.");
            return;
        }

        try {
            // Send type byte 0 for text messages
            synchronized (dataOutputStream) {
                dataOutputStream.writeByte(0);
                dataOutputStream.writeUTF(msg);
                dataOutputStream.flush();
            }

            if (!msg.startsWith("/pm ")) {
                toChat("You: " + msg);
            }

            client_text_field.clear();

        } catch (IOException e) {
            toChat("Message could not be sent. Connection may be closed.");
            client_text_field.setDisable(true);
            System.err.println("Send failed: " + e.getMessage());
        }
    }


    @FXML
    public void showEmojiPicker() {
        String[] emojis = {"😀", "😂", "❤️", "👍", "🎉", "🔥", "😎", "🤔"};

        StringBuilder picker = new StringBuilder();
        for (String e : emojis) {
            picker.append(e).append(" ");
        }

        client_text_field.setText(
                client_text_field.getText() + picker
        );
    }

    @FXML
    public void sendFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Send");
        File file = fileChooser.showOpenDialog(send_file_btn.getScene().getWindow());
        if (file == null) return;

        new Thread(() -> {
            try {
                long fileSize = Files.size(file.toPath());

                if (fileSize > MAX_FILE_SIZE) {
                    toChat("[Error: Maximum file size is 25 MB.]");
                    return;
                }

                byte[] fileData = Files.readAllBytes(file.toPath());
                String filename = file.getName();
                String ext = filename.toLowerCase();

                // Determine type based on file extension
                byte type;
                if (ext.endsWith(".png") || ext.endsWith(".jpg") || ext.endsWith(".jpeg") || ext.endsWith(".gif")) {
                    type = 2;
                } else if (ext.endsWith(".mp4") || ext.endsWith(".avi") || ext.endsWith(".mov")) {
                    type = 3;
                } else {
                    type = 1;
                }

                // Send binary frame: type, filename, size, data
                byte[] filenameBytes = filename.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                );
                synchronized (dataOutputStream) {
                    dataOutputStream.writeByte(type);
                    dataOutputStream.writeInt(filenameBytes.length);
                    dataOutputStream.write(filenameBytes);
                    dataOutputStream.writeLong(fileData.length);
                    dataOutputStream.write(fileData);
                    dataOutputStream.flush();
                }

                toChat("[You sent: " + filename + "]");
            } catch (IOException e) {
                toChat("[Error sending file: " + e.getMessage() + "]");
            }
        }).start();
    }

    // Safely update the TextArea from any thread
    private void toChat(String txt) {
        Platform.runLater(() -> client_text_area.appendText(txt + "\n"));
    }
}