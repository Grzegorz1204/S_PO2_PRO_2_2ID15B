import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ChatClient extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;

    private BufferedReader reader;
    private PrintWriter writer;
    private Socket socket;

    public ChatClient(String serverAddress, int port) {
        setTitle("CHAT");
        setSize(300, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        loginPanel.add(new JLabel("U\u017Cytkownik:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        usernameField = new JTextField(20);
        loginPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        loginPanel.add(new JLabel("Has\u0142o:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        passwordField = new JPasswordField(20);
        loginPanel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        loginButton = new JButton("Zaloguj");
        loginPanel.add(loginButton, gbc);
        getRootPane().setDefaultButton(loginButton);

        setLayout(new BorderLayout());
        add(loginPanel, BorderLayout.CENTER);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                try {
                    socket = new Socket(serverAddress, port);
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                    writer.println(username + ":" + password);

                    String response = reader.readLine();
                    if ("Logowanie udane".equals(response)) {
                        JOptionPane.showMessageDialog(null, "Logowanie udane!");

                        loginPanel.setVisible(false);
                        initializeChatComponents();
                        setSize(400, 500);
                        revalidate();
                        repaint();

                        new Thread(new IncomingReader()).start();
                    } else {
                        JOptionPane.showMessageDialog(null, "Login failed. Please try again.");
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "B\u0142\u0105d po\u0142\u0105czenia z serwerem.");
                }
            }
        });
    }

    private void initializeChatComponents() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        messageField = new JTextField();
        sendButton = new JButton("Wy\u015Blij");

        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);

        add(chatScrollPane, BorderLayout.CENTER);
        add(messagePanel, BorderLayout.SOUTH);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            chatArea.append("Ty: " + message + "\n");
            writer.println(message);
            messageField.setText("");
        }
    }

    private class IncomingReader implements Runnable {
        @Override
        public void run() {
            String message;
            try {
                while ((message = reader.readLine()) != null) {
                    chatArea.append(message + "\n");
                }
            } catch (IOException ex) {
                chatArea.append("Connection to server lost.\n");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            frame.setUndecorated(true);
            frame.setSize(1, 1);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(true);

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JLabel("Wprowad\u017A adres serwera:"), BorderLayout.NORTH);
            JTextField serverField = new JTextField(20);
            panel.add(serverField, BorderLayout.CENTER);

            final JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            optionPane.setOptions(new Object[]{"OK", "Anuluj"});
            JDialog dialog = optionPane.createDialog(frame, "\u0141\u0105czenie z chatem");

            dialog.setModal(true);
            dialog.setVisible(true);

            Object selectedValue = optionPane.getValue();
            frame.dispose();

            if ("OK".equals(selectedValue)) {
                String serverAddress = serverField.getText();
                if (serverAddress == null || serverAddress.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Adres serwera nie zosta\u0142 podany.", "B\u0142\u0105d", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                JFrame portFrame = new JFrame();
                portFrame.setUndecorated(true);
                portFrame.setSize(1, 1);
                portFrame.setLocationRelativeTo(null);
                portFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                portFrame.setVisible(true);

                JPanel portPanel = new JPanel(new BorderLayout());
                portPanel.add(new JLabel("Wprowad\u017A port serwera:"), BorderLayout.NORTH);
                JTextField portField = new JTextField(10);
                portPanel.add(portField, BorderLayout.CENTER);

                final JOptionPane portOptionPane = new JOptionPane(portPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
                portOptionPane.setOptions(new Object[]{"OK", "Anuluj"});
                JDialog portDialog = portOptionPane.createDialog(portFrame, "\u0141\u0105czenie z chatem");

                portDialog.setModal(true);
                portDialog.setVisible(true);

                Object portSelectedValue = portOptionPane.getValue();
                portFrame.dispose();

                if ("OK".equals(portSelectedValue)) {
                    String portStr = portField.getText();
                    if (portStr == null || portStr.isEmpty()) {
                        JOptionPane.showMessageDialog(null, "Port serwera nie zosta\u0142 podany.", "B\u0142\u0105d", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    try {
                        int port = Integer.parseInt(portStr);
                        ChatClient client = new ChatClient(serverAddress, port);
                        client.setVisible(true);
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(null, "Port musi by\u0107 liczb\u0105.", "B\u0142\u0105d", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
    }
}
