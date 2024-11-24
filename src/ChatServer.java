import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Klasa reprezentująca serwer czatu.
 * Obsługuje wielu klientów, pozwala na przesyłanie wiadomości, weryfikację użytkowników
 * oraz zarządzanie użytkownikami przez administratora.
 */
public class ChatServer {

    /**
     * Gniazdo serwera do akceptowania połączeń klientów.
     */
    protected ServerSocket serverSocket;

    /**
     * Lista obsługiwanych klientów.
     */
    protected List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    /**
     * Uruchamia serwer na podanym porcie.
     *
     * @param port Port, na którym serwer będzie nasłuchiwał.
     */
    public void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Serwer uruchomiony na porcie " + port);

            // Wątek do obsługi komend administratora
            new Thread(this::listenForCommands).start();

            // Akceptowanie nowych połączeń klientów
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nowy klient połączony: " + clientSocket.getRemoteSocketAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            if (serverSocket.isClosed()) {
                System.out.println("Serwer został zamknięty.");
            } else {
                System.out.println("Błąd podczas uruchamiania serwera: " + e.getMessage());
            }
        } finally {
            stopServer();
        }
    }

    /**
     * Zatrzymuje serwer i rozłącza wszystkich klientów.
     */
    public void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientHandler client : clients) {
                client.disconnect();
            }
            clients.clear();
            System.out.println("Serwer został pomyślnie zamknięty.");
        } catch (IOException e) {
            System.out.println("Błąd podczas zamykania serwera: " + e.getMessage());
        }
    }

    /**
     * Nasłuchuje komendy administratora (STOP, KICK, SEND).
     */
    protected void listenForCommands() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                String command = scanner.nextLine();
                if ("STOP".equalsIgnoreCase(command)) {
                    stopServer();
                    break;
                } else if (command.startsWith("KICK ")) {
                    String usernameToKick = command.substring(5).trim();
                    kickUser(usernameToKick);
                } else if (command.startsWith("SEND ")) {
                    String message = command.substring(5).trim();
                    sendServerMessage(message);
                }
            }
        }
    }

    /**
     * Wysyła wiadomość serwera do wszystkich klientów.
     *
     * @param message Wiadomość do wysłania.
     */
    protected void sendServerMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage("Pan Admin: " + message);
        }
        System.out.println("Wiadomość od serwera wysłana do wszystkich klientów: " + message);
    }

    /**
     * Rozłącza użytkownika o podanej nazwie.
     *
     * @param username Nazwa użytkownika do rozłączenia.
     */
    protected void kickUser(String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername() != null && client.getUsername().equals(username)) {
                client.disconnect();
                System.out.println("Użytkownik " + username + " został rozłączony przez administratora.");
                break;
            }
        }
    }

    /**
     * Weryfikuje dane logowania użytkownika na podstawie zapisanych danych.
     *
     * @param username       Nazwa użytkownika.
     * @param hashedPassword Zhashowane hasło użytkownika.
     * @return True, jeśli dane logowania są poprawne, w przeciwnym razie false.
     */
    protected boolean verifyUserCredentials(String username, String hashedPassword) {
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

    /**
     * Rozsyła wiadomość od jednego klienta do pozostałych.
     *
     * @param message Wiadomość do wysłania.
     * @param sender  Klient, który wysłał wiadomość.
     */
    protected void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Klasa reprezentująca pojedynczego klienta.
     * Odpowiada za obsługę połączenia z klientem, odbieranie i wysyłanie wiadomości.
     */
    protected class ClientHandler extends Thread {

        /**
         * Gniazdo połączeniowe klienta.
         */
        protected Socket clientSocket;

        /**
         * Strumień wejściowy do odbierania wiadomości od klienta.
         */
        protected BufferedReader input;

        /**
         * Strumień wyjściowy do wysyłania wiadomości do klienta.
         */
        protected PrintWriter output;

        /**
         * Nazwa użytkownika klienta.
         */
        protected String username;

        /**
         * Flaga wskazująca, czy klient został rozłączony.
         */
        protected boolean isDisconnected = false;

        /**
         * Konstruktor klasy ClientHandler.
         *
         * @param socket Gniazdo połączeniowe klienta.
         */
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                this.input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                this.output = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Zwraca nazwę użytkownika klienta.
         *
         * @return Nazwa użytkownika.
         */
        public String getUsername() {
            return username;
        }

        /**
         * Główna pętla wątku obsługująca komunikację z klientem.
         */
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
                if (e.getMessage().contains("Socket closed")) {
                    System.out.println("Gniazdo zamknięte przez klienta: " + username);
                } else {
                    System.out.println("Błąd w obsłudze klienta: " + e.getMessage());
                }
            } finally {
                disconnect();
            }
        }

        /**
         * Wysyła wiadomość do klienta.
         *
         * @param message Wiadomość do wysłania.
         */
        protected void sendMessage(String message) {
            output.println(message);
        }

        /**
         * Rozłącza klienta i zamyka jego zasoby.
         */
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

    /**
     * Punkt wejścia aplikacji.
     *
     * @param args Argumenty wejściowe.
     */
    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.startServer(50000);
    }
}
