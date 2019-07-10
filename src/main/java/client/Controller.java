package client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    Button loginButton, historyButton, settingsButton, sendMessage, setWinButton, tryToRegButton, deleteUser;

    @FXML
    TextArea chatArea;

    @FXML
    ListView<String> clientList;

    @FXML
    TextField messageField, ipField, portField, loginField, passField, nicknameField;

    private String ipAddress;
    private int port;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private Stage newWindow;
    private Scene settingsScene;
    private Scene loginScene;

    private boolean isAutorized;
    List<TextArea> textAreas;

    static final Logger log = LogManager.getLogger(Controller.class);

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setAuthorized(false);
        textAreas = new ArrayList<>();
        textAreas.add(chatArea);
        System.out.println(isAutorized);
    }

    public void setAuthorized(boolean isAutorized) {

        this.isAutorized = isAutorized;
    }

    public void connect() {
        try {
            try(FileReader reader = new FileReader("config.conf")) {
                BufferedReader br = new BufferedReader(reader);
                String info = br.readLine();
                String[] tokens = info.split(" ");
                this.ipAddress = tokens[0];
                this.port = Integer.parseInt(tokens[1]);
                br.close();
            }
            socket = new Socket(ipAddress, port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            setAuthorized(false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //пытаемся залогиниться
                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/authok")) {
                                setAuthorized(true);
                                System.out.println(isAutorized);
                                break;
                            } else {
//                                for (TextArea o : textAreas) {
//                                    o.appendText(str + "\n");
//                                }
                            }
                        }
                        // служебные сигналы СерверНедоступен и СписокКлиентов
                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/")) {
                                if (str.equals("/serverclosed")) break;

                                if (str.startsWith("/clientslist")) {
                                    final String clist = str;
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            getClientList(clist);
                                        }
                                    });

                                }
                            } else {

                                chatArea.appendText(str + "\n");
                                //setAreaText(str);
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
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // окно авторизации и регистрации
    public void login() throws IOException {
        Parent logScene = FXMLLoader.load(getClass().getResource("/fxml/LoginScene.fxml"));
        loginScene = new Scene(logScene);
        loginScene.getStylesheets().add("/styles/loginStyles.css");
        newWindow = new Stage();
        newWindow.setTitle("Login / Registration");
        newWindow.setScene(loginScene);
        newWindow.initModality(Modality.APPLICATION_MODAL);
        newWindow.show();
    }

    // окно с историей сообщений
    public void history() {
//        try {
//            out.writeUTF("/history");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    // окно настроек
    public void settings() throws IOException {
        Parent secondScene = FXMLLoader.load(getClass().getResource("/fxml/SettingsScene.fxml"));
        settingsScene = new Scene(secondScene);
        newWindow = new Stage();
        newWindow.setTitle("Settings");
        newWindow.setScene(settingsScene);
        newWindow.initModality(Modality.APPLICATION_MODAL);
        newWindow.showAndWait();
    }

    // отправка сообщений
    public void sendMsg() {
        if (isAutorized) {
            try {
                out.writeUTF(messageField.getText());
                messageField.clear();
                messageField.requestFocus();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // сохранение настроек IP и порта
    public void setPrefs() {
        // записать IP и порт в файл конфига !!!!!!!!!!!!!
        ipAddress = ipField.getText();
        port = Integer.parseInt(portField.getText());
        saveConfig(ipAddress, port);
        Stage stage = (Stage) setWinButton.getScene().getWindow();
        stage.close();
    }

    // авторизация пользователя
    public void tryToAuth() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passField.getText());
            loginField.clear();
            passField.clear();
            Stage stage = (Stage) tryToRegButton.getScene().getWindow();
            stage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // регистрация пользователя
    public void tryToReg() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/addUser " + loginField.getText() + " " + passField.getText() + " " +
                    nicknameField.getText());
            loginField.clear();
            passField.clear();
            nicknameField.clear();
            Stage stage = (Stage) tryToRegButton.getScene().getWindow();
            stage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Удаление учетной записи
    public void deleteUser() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/delete " + loginField.getText() + " " + passField.getText() + " " + nicknameField.getText());
            loginField.clear();
            passField.clear();
            nicknameField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveConfig(String ipAddress, int port) {
        try(FileWriter file = new FileWriter("config.conf", false)) {
            String info = ipAddress + " " + port;
            file.write(info);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateChat(String str) {
        chatArea.appendText(str + "\n");
    }

    public void getClientList(String str) {
        log.info(str);
        String[] tokens = str.split(" ");

        clientList.getItems().clear();
        for (int i = 1; i < tokens.length; i++) {
            clientList.getItems().add(tokens[i]);
        }
    }
}
