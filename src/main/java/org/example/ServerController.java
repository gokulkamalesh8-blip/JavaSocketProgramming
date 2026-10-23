package org.example;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

public class ServerController implements Initializable {

    private static final long MAX_FILE_SIZE = 25L * 1024 * 1024;

    @FXML private Button server_send_btn;
    @FXML private TextArea server_text_area;
    @FXML private TextField server_text_field;

    // Stores the output stream for every connected client
    private final Map<String, DataOutputStream> clientMap =
            new ConcurrentHashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Disable input controls until the server is ready
        server_text_area.setEditable(false);
        server_text_field.setDisable(true);
        server_send_btn.setDisable(true);
        startServer();
    }


    private void startServer() {
        // Run the server on a background thread
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(3000)) {
                appendMessage("Server started. Waiting for clients...");

                // Enable the GUI controls
                Platform.runLater(() -> {
                    server_text_field.setDisable(false);
                    server_send_btn.setDisable(false);
                });

                // Accept multiple client connections
                while (true) {
                    Socket clientSocket = serverSocket.accept();

                    appendMessage("Client connected!");

                    // Handle each client on a separate thread
                    Thread clientThread = new Thread(
                            () -> handleClient(clientSocket)
                    );

                    clientThread.setDaemon(true);
                    clientThread.start();
                }

            } catch (IOException e) {
                appendMessage("Server error: " + e.getMessage());
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();
    }




    private void handleClient(Socket clientSocket) {
        String username = null;
        DataOutputStream out = null;
        boolean registered = false;

        try {
            DataInputStream in =
                    new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            // First message contains the username
            username = in.readUTF().trim();

            if (username.isEmpty()) {
                out.writeByte(4);
                out.writeUTF("Username cannot be empty.");
                out.flush();
                return;
            }

// Reject duplicate usernames atomically
            if (clientMap.putIfAbsent(username, out) != null) {
                appendMessage("Duplicate username rejected: " + username);

                out.writeByte(4);
                out.writeUTF("Username already taken. Please choose another username.");
                out.flush();
                return;
            }

            registered = true;

            appendMessage(username + " has joined.");
            broadcastText(username + " has joined the chat.");

            while (true) {
                byte messageType = in.readByte();

                if (messageType == 0) {
                    String message = in.readUTF();

                    if (message.startsWith("/pm ")) {
                        String[] parts = message.split(" ", 3);

                        if (parts.length == 3) {
                            String targetUsername = parts[1];
                            String privateMessage = parts[2];

                            DataOutputStream targetOut =
                                    clientMap.get(targetUsername);

                            if (targetOut != null) {
                                synchronized (targetOut) {
                                    targetOut.writeByte(0);
                                    targetOut.writeUTF(
                                            "[PM from " + username + "]: "
                                                    + privateMessage
                                    );
                                    targetOut.flush();
                                }

                                synchronized (out) {
                                    out.writeByte(0);
                                    out.writeUTF(
                                            "[PM to " + targetUsername + "]: "
                                                    + privateMessage
                                    );
                                    out.flush();
                                }
                            } else {
                                synchronized (out) {
                                    out.writeByte(0);
                                    out.writeUTF(
                                            "User '" + targetUsername
                                                    + "' not found."
                                    );
                                    out.flush();
                                }
                            }
                        }
                    } else {
                        appendMessage(username + ": " + message);
                        broadcastText(username + ": " + message);
                    }

                } else {
                    // Binary file, image, or video
                    int filenameLen = in.readInt();
                    if (filenameLen < 0 || filenameLen > 1_000_000) {
                        throw new IOException("Invalid filename length");
                    }

                    byte[] filenameBytes = in.readNBytes(filenameLen);
                    if (filenameBytes.length != filenameLen) {
                        throw new IOException("Incomplete filename");
                    }

                    String filename = new String(
                            filenameBytes,
                            java.nio.charset.StandardCharsets.UTF_8
                    );
                    long fileSize = in.readLong();
                    if (fileSize < 0 || fileSize > MAX_FILE_SIZE) {
                        throw new IOException("File exceeds the 25 MB limit");
                    }

                    byte[] fileData = in.readNBytes((int) fileSize);
                    if (fileData.length != (int) fileSize) {
                        throw new IOException("Incomplete file data");
                    }

                    appendMessage(username + " sent a file: " + filename);
                    broadcastFile(username, messageType, filename, fileData);
                }
            }

        } catch (IOException e) {
            if (registered) {
                System.err.println(
                        "Client connection ended: " + e.getMessage()
                );
            }

        } finally {
            if (registered && username != null && out != null) {
                boolean removed = clientMap.remove(username, out);

                if (removed) {
                    appendMessage(username + " has left the chat.");
                    broadcastText(username + " has left the chat.");
                }
            }

            try {
                clientSocket.close();
            } catch (IOException ignored) {
                // Socket may already be closed
            }
        }
    }



    private void broadcastText(String message) {
        for (DataOutputStream out : clientMap.values()) {
            try {
                synchronized (out) {
                    out.writeByte(0);
                    out.writeUTF(message);
                    out.flush();
                }
            } catch (IOException e) {
                System.err.println("Text broadcast failed: " + e.getMessage());
            }
        }
    }

    private void broadcastFile(String sender, byte type,
                               String filename, byte[] data) {
        byte[] filenameBytes = (sender + "/" + filename)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);

        for (DataOutputStream out : clientMap.values()) {
            try {
                synchronized (out) {
                    out.writeByte(type);
                    out.writeInt(filenameBytes.length);
                    out.write(filenameBytes);
                    out.writeLong(data.length);
                    out.write(data);
                    out.flush();
                }
            } catch (IOException e) {
                System.err.println("File broadcast failed: " + e.getMessage());
            }
        }
    }

    private void broadcast(String message) {
        broadcastText(message);
    }

    @FXML
    public void sendMessage() {
        String msg = server_text_field.getText().trim();
        if (msg.isEmpty()) return;

        // Prefix with "Server:" so clients know who sent it
        broadcast("Server: " + msg);
        appendMessage("Server (You): " + msg);
        server_text_field.clear();
    }
    private void appendMessage(String message) {
        Platform.runLater(() -> server_text_area.appendText(message + "\n"));
    }
}
