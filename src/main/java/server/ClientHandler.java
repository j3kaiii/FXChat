package server;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler {
    static final Logger log = LogManager.getLogger(ClientHandler.class);
    private Server server;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String nick;
    private List<String> blackList;

    public ClientHandler(final Server server, final Socket socket) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.blackList = new ArrayList<>();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //блок регистрации и авторизации
                        while (true) {
                            String str = in.readUTF();
                            // авторизация
                            if (str.startsWith("/auth")) {
                                log.info("str - " + str);
                                String[] tokens = str.split(" ");
                                String newNick = AuthService.getNickByLoginAndPass(tokens[1], tokens[2]);
                                log.info("newNick - " + newNick);
                                if (newNick != null) {
                                    if (!server.isNickBusy(newNick)) {
                                        sendMsg("/authok");
                                        log.info("nick is not busy : /authok");
                                        nick = newNick;
                                        server.subscribe(ClientHandler.this);
                                        break;
                                    } else {
                                        sendMsg("Учетная запись уже используется");
                                    }
                                } else {
                                    sendMsg("Неверный логин/пароль");
                                }
                                // регистрация
                            } else if (str.startsWith("/addUser")) {
                                String[] tokens = str.split(" ");
                                if (AuthService.addNewUser(tokens[1], tokens[3], tokens[2])) {
                                    sendMsg("/regok");
                                } else {
                                    sendMsg("Такой пользователь существует");
                                }
                                // удаление
                            } else if (str.startsWith("/delete ")) {
                                String[] tokens = str.split(" ");
                                AuthService.deleteUser(tokens[1],tokens[2], tokens[3]);
                            }
                        }

                        // блок вспомогательных команд
                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/")) {
                                if(str.startsWith("/history")) {
                                    StringBuilder stringBuilder  = AuthService.getHistoryChat();
                                    out.writeUTF(stringBuilder.toString());
                                }
                                if (str.equals("/end")) {
                                    out.writeUTF("/serverclosed");
                                    break;
                                }
                                if (str.startsWith("/w ")) {
                                    String[] tokens = str.split(" ", 3);
                                    //String m = str.substring(tokens[1].length() + 4);
                                    server.sendPersonalMsg(ClientHandler.this, tokens[1], tokens[2]);
                                }
                                if (str.startsWith("/blacklist ")) {
                                    String[] tokens = str.split(" ");
                                    blackList.add(tokens[1]);
                                    sendMsg("Вы добавили пользователя " + tokens[1] + " в черный список");
                                }
                            } else {
                                AuthService.saveHistory(nick, str);
                                server.broadcastMsg(ClientHandler.this, nick + ": " + str);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        server.unsubscribe(ClientHandler.this);
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // получаем ник
    public String getNick() {
        return nick;
    }

    // отправляем сообщение
    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
            log.info("CH sendMsg : " + msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // проверяем черный список
    public boolean checkBlackList(String nick) {
        return blackList.contains(nick);
    }
}
