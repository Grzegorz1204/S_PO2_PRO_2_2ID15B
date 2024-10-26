import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static Set<ClientHandler> clientHandlers = new HashSet<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(50000)) {
            System.out.println("Serwer wystartował, oczekiwanie na klientów...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nowy klient połączony!");

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.add(clientHandler);

                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastMessage(String message, ClientHandler excludeClient) {
        for (ClientHandler client : clientHandlers) {
            if (client != excludeClient) {
                client.sendMessage(message);
            }
        }
    }

    public static void removeClient(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
        System.out.println(clientHandler.getClientName() + " opuścił czat.");
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            clientName = in.readLine();
            System.out.println(clientName + " dołączył do czatu.");

            ChatServer.broadcastMessage(clientName + " dołączył do czatu.", this);

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                System.out.println(clientName + ": " + clientMessage);
                ChatServer.broadcastMessage(clientName + ": " + clientMessage, this);
            }
        } catch (SocketException e) {
            System.out.println("Połączenie z " + clientName + " zostało przerwane.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ChatServer.removeClient(this);
            ChatServer.broadcastMessage(clientName + " opuścił czat.", this);
        }
    }

    public String getClientName() {
        return clientName;
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}
