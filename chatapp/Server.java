import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private static final int PORT = 12345;
    private final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    private JPanel chatContainer;
    private JScrollPane scrollPane;
    private JTextField messageField;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fallback
        }
        SwingUtilities.invokeLater(Server::new);
    }

    public Server() {
        setupGUI();
        startServer();
    }

    private void setupGUI() {
        JFrame frame = new JFrame("Chat Server Instance");
        frame.setSize(420, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(Color.WHITE);

        chatContainer = new JPanel();
        chatContainer.setLayout(new GridBagLayout());
        chatContainer.setBackground(Color.WHITE);
        chatContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        scrollPane = new JScrollPane(chatContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        messageField = new JTextField();
        messageField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 230, 230), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JButton sendButton = new JButton("Send");
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        sendButton.setOpaque(true);
        sendButton.setBorderPainted(false);
        sendButton.setContentAreaFilled(true);
        sendButton.setBackground(new Color(0, 149, 246));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);

        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendServerMessage());
        messageField.addActionListener(e -> sendServerMessage());

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                appendSystemMessage("Server active on port " + PORT);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                }
            } catch (IOException e) {
                appendSystemMessage("Server error: " + e.getMessage());
            }
        }).start();
    }

    private void sendServerMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            broadcastMessage("Server: " + message);
            appendChatBubble("You: " + message, true);
            messageField.setText("");
        }
    }

    protected void broadcastMessage(String message) {
        if (message.startsWith("[System]")) {
            appendSystemMessage(message.replace("[System]: ", ""));
        } else {
            appendChatBubble(message, false);
        }
        
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    protected void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    private void appendChatBubble(String message, boolean isRightAligned) {
        SwingUtilities.invokeLater(() -> {
            Color bubbleColor = isRightAligned ? new Color(0, 132, 255) : new Color(241, 240, 240);
            Color textColor = isRightAligned ? Color.WHITE : Color.BLACK;

            ChatBubble bubble = new ChatBubble(message, bubbleColor, textColor);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = chatContainer.getComponentCount();
            gbc.weightx = 1.0;
            gbc.insets = new Insets(4, 2, 4, 2);
            gbc.anchor = isRightAligned ? GridBagConstraints.LINE_END : GridBagConstraints.LINE_START;
            gbc.fill = GridBagConstraints.NONE;

            chatContainer.add(bubble, gbc);
            chatContainer.revalidate();
            chatContainer.repaint();
            
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        });
    }

    private void appendSystemMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            JLabel label = new JLabel(text);
            label.setFont(new Font("SansSerif", Font.ITALIC, 11));
            label.setForeground(Color.GRAY);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = chatContainer.getComponentCount();
            gbc.weightx = 1.0;
            gbc.insets = new Insets(6, 2, 6, 2);
            gbc.anchor = GridBagConstraints.CENTER;

            chatContainer.add(label, gbc);
            chatContainer.revalidate();
            chatContainer.repaint();
            
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        });
    }

    // Dedicated structural component class for rendering chat bubbles flawlessly
    private static class ChatBubble extends JPanel {
        private final Color backgroundColor;

        public ChatBubble(String text, Color bg, Color fg) {
            this.backgroundColor = bg;
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

            JLabel label = new JLabel(text);
            label.setFont(new Font("SansSerif", Font.PLAIN, 13));
            label.setForeground(fg);
            add(label, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(backgroundColor);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2d.dispose();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final Server server;
        private PrintWriter out;
        private BufferedReader in;
        private String nickname;

        public ClientHandler(Socket socket, Server server) {
            this.socket = socket;
            this.server = server;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

                nickname = in.readLine();
                if (nickname == null || nickname.trim().isEmpty()) {
                    nickname = "Client_" + socket.getPort();
                }
                
                server.broadcastMessage("[System]: " + nickname + " entered the space.");

                String message;
                while ((message = in.readLine()) != null) {
                    server.broadcastMessage(nickname + ": " + message);
                }
            } catch (IOException e) {
                // Handoff drop connection context safely
            } finally {
                server.removeClient(this);
                if (nickname != null) {
                    server.broadcastMessage("[System]: " + nickname + " disconnected.");
                }
                closeResources();
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        private void closeResources() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}