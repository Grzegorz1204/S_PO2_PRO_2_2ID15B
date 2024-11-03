import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();

    public void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Serwer uruchomiony na porcie " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                InetAddress clientAddress = clientSocket.getInetAddress();
                int clientPort = clientSocket.getPort();
                System.out.println("Nowy klient połączony(" + clientAddress.getHostAddress() + ":" + clientPort + ")");

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            System.out.println("Błąd podczas uruchamiania serwera: " + e.getMessage());
        }
    }

    private boolean verifyUserCredentials(String username, String password) {
        try (BufferedReader br = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] userData = line.split(":");
                if (userData.length == 2 && userData[0].equals(username) && userData[1].equals(password)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("Błąd podczas odczytu danych użytkownika: " + e.getMessage());
        }
        return false;
    }

    private void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private BufferedReader input;
        private PrintWriter output;
        private String username;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                output = new PrintWriter(clientSocket.getOutputStream(), true);

                // Odbieranie loginu i hasła
                String credentials = input.readLine();
                String[] userData = credentials.split(":");

                if (userData.length == 2 && verifyUserCredentials(userData[0], userData[1])) {
                    username = userData[0];
                    output.println("Logowanie udane");
                    System.out.println("Użytkownik " + username + " zalogował się pomyślnie.");

                    String message;
                    while ((message = input.readLine()) != null) {
                        System.out.println(username + ": " + message);
                        broadcastMessage(username + ": " + message, this);
                    }
                } else {
                    output.println("Nieprawidłowy login lub hasło");
                    System.out.println("Nieudana próba logowania.");
                    clientSocket.close();
                }

            } catch (IOException e) {
                System.out.println("Błąd w obsłudze klienta: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                    clients.remove(this);
                    System.out.println("Klient " + username + " rozłączony.");
                } catch (IOException e) {
                    System.out.println("Błąd podczas zamykania gniazda klienta: " + e.getMessage());
                }
            }
        }

        private void sendMessage(String message) {
            output.println(message);
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.startServer(50000); 
    }
}
