package server;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Server {
    static final Logger log = LogManager.getLogger(Server.class);
    private Vector<ClientHandler> clients;

    public Server() {
        // создаем список клиентов
        clients = new Vector<>();
        ServerSocket server = null;
        Socket socket = null;
        try {
            // запускаем сервер
            AuthService.connect();
            server = new ServerSocket(8189);
            log.info("Server run. Waiting for clients");
            //System.out.println("сервер запущен");
            while (true) {
                // слушаем сокет и подключаем клиентов
                socket = server.accept();
                log.info("Client connected");
                //System.out.println("клиент подключился");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Unable to run server");
        } finally {
            try {
                socket.close();
                log.warn("Socket closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                server.close();
                log.warn("Server closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
            // гасим сервер
            AuthService.disconnect();
        }
    }

    //персональное сообщение
    public void sendPersonalMsg(ClientHandler from, String nickTo, String msg) {
        for (ClientHandler o : clients) {
            if (o.getNick().equals(nickTo)) {
                o.sendMsg("from " + from.getNick() + " : " + msg);
                log.info("PERSONAL from " + from.getNick() + " to " + nickTo + " : " + msg);
                return;
            }
        }
        from.sendMsg("Клиент с ником " + nickTo + " не найден в чате");
    }

    //в общий чат
    public void broadcastMsg(ClientHandler from, String msg) {
        for (ClientHandler o : clients) {
            if (!o.checkBlackList(from.getNick())) {
                o.sendMsg(msg);
                log.info(from.getNick() + " : " + msg);
            }
        }
    }

    //проверяем ник на доступность при входе
    public boolean isNickBusy(String nick) {
        for (ClientHandler o : clients) {
            if (o.getNick().equals(nick)){
                log.warn("client try to connect with connected nick " + nick);
                return true;
            }
        }
        return  false;
    }

    // отдаем список клиентов
    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder();
        sb.append("/clientslist ");
        for (ClientHandler o : clients) {
            sb.append(o.getNick() + " ");
        }
        String out = sb.toString();
        for (ClientHandler o : clients) {
            o.sendMsg(out);
            log.info("clients : " + out);
        }
    }

    // вводим клиента в чат
    public void subscribe(ClientHandler client) {
        clients.add(client);
        log.info(client.getNick() + " connected");
        broadcastClientList();
    }

    // выводим клиента из чата
    public void unsubscribe(ClientHandler client) {
        clients.remove(client);
        log.info(client.getNick() + " disconnected");
        broadcastClientList();
    }
}
