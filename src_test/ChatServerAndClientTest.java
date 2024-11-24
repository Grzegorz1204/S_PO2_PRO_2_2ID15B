import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.net.*;

/**
 * Klasa testowa dla serwera i klienta czatu.
 * Obejmuje testy poprawności logowania, przesyłania wiadomości oraz zarządzania użytkownikami.
 */
public class ChatServerAndClientTest {

    /**
     * Instancja serwera czatu używana do testów.
     */
    private ChatServer server;

    /**
     * Wątek uruchamiający serwer.
     */
    private Thread serverThread;

    /**
     * Przygotowanie środowiska testowego przed każdym testem.
     * Inicjalizuje serwer i uruchamia go w osobnym wątku.
     */
    @BeforeEach
    void setUp() {
        server = new ChatServer();
        serverThread = new Thread(() -> server.startServer(50000));
        serverThread.start();
        try {
            Thread.sleep(500); // Czas na uruchomienie serwera
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Czyszczenie środowiska testowego po każdym teście.
     * Zatrzymuje serwer i zamyka wątek serwera.
     */
    @AfterEach
    void tearDown() throws IOException {
        if (server.serverSocket != null && !server.serverSocket.isClosed()) {
            server.serverSocket.close();
        }
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }
    }

    /**
     * Test logowania klienta z poprawnymi danymi.
     * Sprawdza, czy serwer akceptuje poprawne dane logowania.
     */
    @Test
    @DisplayName("Test logowania klienta z poprawnymi danymi")
    void testClientLoginSuccess() throws Exception {
        try {
            try (PrintWriter writer = new PrintWriter(new FileWriter("users.txt"))) {
                String hashedPassword = hashPassword("testPassword");
                writer.println("testUser:" + hashedPassword);
            }

            try (Socket socket = new Socket("localhost", 50000);
                 BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                output.println("testUser:" + hashPassword("testPassword"));

                String response = input.readLine();
                assertEquals("Logowanie udane", response, "Oczekiwano udanej odpowiedzi serwera po poprawnym logowaniu.");
                System.out.println("Test logowania klienta z poprawnymi danymi zakończony sukcesem: " + response);
            }
        } catch (Exception e) {
            System.out.println("Test logowania klienta z poprawnymi danymi nie powiódł się: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Test logowania klienta z niepoprawnymi danymi.
     * Sprawdza, czy serwer odrzuca niepoprawne dane logowania.
     */
    @Test
    @DisplayName("Test logowania klienta z niepoprawnymi danymi")
    void testClientLoginFailure() throws Exception {
        try {
            try (Socket socket = new Socket("localhost", 50000);
                 BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                output.println("wrongUser:wrongPassword");

                String response = input.readLine();
                assertEquals("Nieprawidłowy login lub hasło", response, "Oczekiwano odpowiedzi o nieudanym logowaniu.");
                System.out.println("Test logowania klienta z niepoprawnymi danymi zakończony sukcesem: " + response);
            }
        } catch (Exception e) {
            System.out.println("Test logowania klienta z niepoprawnymi danymi nie powiódł się: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Test przesyłania wiadomości między dwoma klientami.
     * Sprawdza, czy wiadomości są poprawnie przesyłane między klientami za pośrednictwem serwera.
     */
    @Test
    @DisplayName("Test przesyłania wiadomości między klientami")
    void testMessageBroadcast() throws Exception {
        try {
            try (PrintWriter writer = new PrintWriter(new FileWriter("users.txt"))) {
                writer.println("user1:" + hashPassword("password1"));
                writer.println("user2:" + hashPassword("password2"));
            }

            try (Socket client1 = new Socket("localhost", 50000);
                 BufferedReader input1 = new BufferedReader(new InputStreamReader(client1.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter output1 = new PrintWriter(new OutputStreamWriter(client1.getOutputStream(), StandardCharsets.UTF_8), true);

                 Socket client2 = new Socket("localhost", 50000);
                 BufferedReader input2 = new BufferedReader(new InputStreamReader(client2.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter output2 = new PrintWriter(new OutputStreamWriter(client2.getOutputStream(), StandardCharsets.UTF_8), true)) {

                output1.println("user1:" + hashPassword("password1"));
                assertEquals("Logowanie udane", input1.readLine());

                output2.println("user2:" + hashPassword("password2"));
                assertEquals("Logowanie udane", input2.readLine());

                output1.println("Hello, user2!");

                String receivedMessage = input2.readLine();
                assertEquals("user1: Hello, user2!", receivedMessage, "Oczekiwano poprawnej wiadomości od klienta 1.");
                System.out.println("Test przesyłania wiadomości między klientami zakończony sukcesem: " + receivedMessage);
            }
        } catch (Exception e) {
            System.out.println("Test przesyłania wiadomości między klientami nie powiódł się: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Test wyrzucenia klienta przez administratora.
     * Sprawdza, czy klient otrzymuje odpowiednią wiadomość i jest rozłączony.
     */
    @Test
    @DisplayName("Test wyrzucenia klienta przez administratora")
    void testKickUser() throws Exception {
        try {
            try (PrintWriter writer = new PrintWriter(new FileWriter("users.txt"))) {
                writer.println("user1:" + hashPassword("password1"));
            }

            try (Socket client1 = new Socket("localhost", 50000);
                 BufferedReader input1 = new BufferedReader(new InputStreamReader(client1.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter output1 = new PrintWriter(new OutputStreamWriter(client1.getOutputStream(), StandardCharsets.UTF_8), true)) {

                output1.println("user1:" + hashPassword("password1"));
                assertEquals("Logowanie udane", input1.readLine());

                server.kickUser("user1");

                String disconnectMessage = input1.readLine();
                assertEquals("Zostałeś rozłączony przez administratora.", disconnectMessage, "Klient powinien otrzymać wiadomość o rozłączeniu.");
                System.out.println("Test wyrzucenia klienta przez administratora zakończony sukcesem: " + disconnectMessage);
            }
        } catch (Exception e) {
            System.out.println("Test wyrzucenia klienta przez administratora nie powiódł się: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Hashuje hasło przy użyciu algorytmu SHA-256.
     *
     * @param password Hasło do zhashowania.
     * @return Zhashowane hasło w formacie szesnastkowym.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported");
        }
    }
}
