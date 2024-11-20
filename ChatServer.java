import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Serwer uruchomiony na porcie " + port);

            new Thread(this::listenForCommands).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                InetAddress clientAddress = clientSocket.getInetAddress();
                int clientPort = clientSocket.getPort();
                System.out.println("Nowy klient połączony (" + clientAddress.getHostAddress() + ":" + clientPort + ")");

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            System.out.println("Błąd podczas uruchamiania serwera: " + e.getMessage());
        }
    }

    private void listenForCommands() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                String command = scanner.nextLine();
                if (command.startsWith("KICK ")) {
                    String usernameToKick = command.substring(5).trim();
                    kickUser(usernameToKick);
                } else if (command.startsWith("SEND ")) {
                    String message = command.substring(4).trim();
                    sendServerMessage(message);
                }
            }
        }
    }

    private void sendServerMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage("Pan Admin: " + message);
        }
        System.out.println("Wiadomość od serwera wysłana do wszystkich klientów: " + message);
    }

    private void kickUser(String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername() != null && client.getUsername().equals(username)) {
                client.disconnect();
                System.out.println("Użytkownik " + username + " został rozłączony przez administratora.");
                break;
            }
        }
    }

    private boolean verifyUserCredentials(String username, String hashedPassword) {
        try (BufferedReader br = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] userData = line.split(":");
                if (userData.length == 2 && userData[0].equals(username) && userData[1].equals(hashedPassword)) {
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
        private boolean isDisconnected = false;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                this.input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                this.output = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String getUsername() {
            return username;
        }

        @Override
        public void run() {
            try {
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
                disconnect();
            }
        }

        private void sendMessage(String message) {
            output.println(message);
        }

        public void disconnect() {
            if (!isDisconnected) {
                isDisconnected = true;
                try {
                    if (output != null) {
                        output.println("Zostałeś rozłączony przez administratora.");
                        output.flush();
                    }
                    clients.remove(this);
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        clientSocket.close();
                    }
                    System.out.println("Klient " + username + " rozłączony.");
                } catch (IOException e) {
                    System.out.println("Błąd podczas zamykania gniazda klienta: " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.startServer(50000);
    }
}
