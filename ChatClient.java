import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String clientName;

    public ChatClient(String serverAddress, int port) {
        try {
            socket = new Socket(serverAddress, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Podaj swoją nazwę użytkownika: ");
            clientName = userInput.readLine();
            out.println(clientName);
            
            new Thread(new ServerListener()).start();

            String message;
            while ((message = userInput.readLine()) != null) {
                out.println(message);
            }
        } catch (IOException e) {
            System.err.println("Błąd połączenia z serverem: " + e.getMessage());
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("Błąd zamykania połączenia: " + e.getMessage());
            }
        }
    }

    private class ServerListener implements Runnable {
        public void run() {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println(serverMessage);
                }
            } catch (IOException e) {
                System.err.println("Błąd odczytu servera: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        Scanner connectServer = new Scanner(System.in);
        System.out.print("Wprowadź adres servera: ");
        String serverAddress = connectServer.nextLine();
        System.out.print("Wprowadź port servera: ");
        int port = connectServer.nextInt();
        
        new ChatClient(serverAddress, port);
        connectServer.close();
    }
}
