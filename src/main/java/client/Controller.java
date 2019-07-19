package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    TextField msgField;

    @FXML
    TextArea chatArea;

    @FXML
    HBox bottomPanel;

    @FXML
    GridPane upperPanel, settings;

    @FXML
    TextField loginField, ipField, nicknameField;

    @FXML
    PasswordField passwordField;

//    @FXML
//    private Label userName;

    @FXML
    ListView<String> clientsList;

    Socket socket;
    DataInputStream in;
    DataOutputStream out;

    //final String IP_ADDRESS = "localhost";
    private String ipAddress;
    final int PORT = 8189;

    private boolean isAuthorized;

    List<TextArea> textAreas;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthorized(false);
        if (Files.exists(Paths.get("settings.conf"))) {
            readIpFromFile();
        } else {
            readIpFromUser();
        }
        textAreas = new ArrayList<>();
        textAreas.add(chatArea);
    }

    private void readIpFromFile() {
        settings.setVisible(false);
        settings.setManaged(false);
        try(BufferedReader br = new BufferedReader(new FileReader("settings.conf"))) {
            ipAddress = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readIpFromUser() {
        upperPanel.setVisible(false);
        upperPanel.setManaged(false);
    }

    public void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
        if (!isAuthorized) {
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);
            bottomPanel.setVisible(false);
            bottomPanel.setManaged(false);
            clientsList.setVisible(false);
            clientsList.setManaged(false);
        } else {
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);
            bottomPanel.setVisible(true);
            bottomPanel.setManaged(true);
            clientsList.setVisible(true);
            clientsList.setManaged(true);
        }
    }

    public void connect() {
        try {
            //userName.setText("test");
            socket = new Socket(ipAddress, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            setAuthorized(false);
            Thread thread = new Thread(() -> {
                try {
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/deleteok")) {
                            chatArea.appendText("Пользователь удален");
                        }
                        else if (str.startsWith("/regok")) {
                            chatArea.appendText("Пользователь добавлен. Введите логин и пароль для входа.");
                        }
                        else if (str.startsWith("/authok")) {
                            setAuthorized(true);
                            break;
                        } else {
                            for (TextArea o : textAreas) {
                                o.appendText(str + "\n");
                            }
                        }
                    }
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals("/serverclosed")) break;
                            if (str.startsWith("/clientslist ")) {
                                String[] tokens = str.split(" ");
                                Platform.runLater(() -> {
                                    clientsList.getItems().clear();
                                    for (int i = 1; i < tokens.length; i++) {
                                        clientsList.getItems().add(tokens[i]);
                                    }
                                });
                            }
                        } else {
                            chatArea.appendText(str + "\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    setAuthorized(false);
                }
            });
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg() {
        try {
            out.writeUTF(msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToAuth() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passwordField.getText());
            loginField.clear();
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void selectClient(MouseEvent mouseEvent) {
        if(mouseEvent.getClickCount() == 2) {
            MiniStage ms = new MiniStage(clientsList.getSelectionModel().getSelectedItem(), out, textAreas);
            ms.show();
        }
    }

    public void setConnection() {
        settings.setVisible(false);
        settings.setManaged(false);
        upperPanel.setVisible(true);
        upperPanel.setManaged(true);
        try(BufferedWriter bw = new BufferedWriter(new FileWriter("settings.conf", false))) {
            ipAddress = ipField.getText();
            bw.write(ipAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToReg() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/addUser " + loginField.getText() + " " + nicknameField.getText() + " " + passwordField.getText());
            loginField.clear();
            nicknameField.clear();
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToDel() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/delete " + loginField.getText() + " " + passwordField.getText() + " " + nicknameField.getText());
            loginField.clear();
            nicknameField.clear();
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
