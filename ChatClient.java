import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    public void startClient(String address, int port) {
        try {
            socket = new Socket(address, port);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            Scanner scanner = new Scanner(System.in);
            System.out.print("Podaj login: ");
            String username = scanner.nextLine();
            System.out.print("Podaj hasło: ");
            String password = scanner.nextLine();

            output.println(username + ":" + password); 

            String response = input.readLine(); 
            System.out.println(response);

            if ("Logowanie udane".equals(response)) {
                System.out.println("Połączono z czatem!");

                new Thread(new MessageReceiver()).start();

                String message;
                while (true) {
                    //System.out.print("Ty: ");
                    message = scanner.nextLine();
                    output.println(message);
                }
            } else {
                System.out.println("Logowanie nieudane. Spróbuj ponownie.");
            }

        } catch (IOException e) {
            System.out.println("Błąd podczas łączenia z serwerem: " + e.getMessage());
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.out.println("Błąd podczas zamykania gniazda: " + e.getMessage());
            }
        }
    }

    private class MessageReceiver implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = input.readLine()) != null) {
                    System.out.println(message);
                }
            } catch (IOException e) {
                System.out.println("Błąd w odbieraniu wiadomości: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        Scanner connectServer = new Scanner(System.in);
        System.out.print("Wprowadź adres servera: ");
        String serverAddress = connectServer.nextLine();
        System.out.print("Wprowadź port servera: ");
        int port = connectServer.nextInt();


        ChatClient client = new ChatClient();
        client.startClient(serverAddress, port); 
        connectServer.close();

    }
}
