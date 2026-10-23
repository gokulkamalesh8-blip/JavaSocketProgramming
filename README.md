# Java Multi-Client Chat Application

A real-time desktop chat application built using Java, JavaFX, and TCP socket programming. The application allows multiple clients to communicate through a central server, exchange private messages, and share files.

## Features

- **Multi-client communication:** Multiple clients can connect to a single server.
- **Username management:** Rejects duplicate usernames while the original user is connected.
- **Public messaging:** Send text messages to all connected clients.
- **Private messaging:** Send direct messages to a specific username.
- **Emoji support:** Include emojis in chat messages.
- **File sharing:** Transfer supported files, including PDF documents and PNG images.
- **File downloads:** Received files are saved to the recipient's Downloads folder.
- **File-size validation:** Enforces a 25 MB file-size limit.
- **Connection handling:** Detects client disconnections and allows usernames to be reused after disconnection.
- **JavaFX interface:** Provides a graphical interface for chatting and file sharing.

## Technologies Used

- Java 21
- JavaFX
- TCP Socket Programming
- Multithreading
- Maven
- FXML
- IntelliJ IDEA

## Project Structure

```text
JavaSocketProgramming/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/
        │   └── org/
        │       └── example/
        │           ├── ClientInitializer.java
        │           ├── ClientController.java
        │           ├── ServerInitializer.java
        │           └── ServerController.java
        └── resources/
            ├── client.fxml
            └── server.fxml
```

*The structure above is illustrative; adjust the filenames if your project differs.*

## Prerequisites

- JDK 21
- IntelliJ IDEA
- Maven support
- JavaFX dependencies configured in `pom.xml`

## How to Run

1. Clone or download the repository.
2. Open the project in IntelliJ IDEA.
3. Allow Maven to import the dependencies.
4. Run `ServerInitializer.java` to start the server.
5. Run `ClientInitializer.java` to open a client window.
6. Enter a username when prompted.
7. Launch additional client instances to chat with other users.

The server listens on port `3000`. Clients connect to `127.0.0.1`, so this configuration is intended for testing on the same computer.

## Private Messaging

Use the following command in the chat input:

```text
/pm Client2 Hello! How are you?
```

Replace `Client2` with the username of the intended recipient.

## File Sharing

1. Open a connected client.
2. Click the Send File button.
3. Choose a supported file that is within the configured size limit.
4. Other connected clients can receive the file and save it locally.

## Concepts Demonstrated

- Object-oriented programming
- Client-server architecture
- TCP socket communication
- Multithreading
- Concurrent collections
- Input/output streams
- JavaFX event handling
- File input/output
- Exception handling

## Future Improvements

- Add secure authentication.
- Encrypt messages and file transfers.
- Support configurable server addresses for different computers.
- Improve delivery status and connection error messages.
- Add chat history and user-friendly file previews.
- Test large files, interrupted transfers, and additional file formats.

## Author

**Gokul N.**

B.Sc. Information Technology | 2026 Graduate

Java Developer | Interested in Backend Development and Software Engineering
