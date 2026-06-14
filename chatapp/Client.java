package all;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Client {
    private static final int WINDOW_WIDTH = 420;
    private static final int WINDOW_HEIGHT = 600;
    private static final int PORT = 12345;
    private static final String HOST = "localhost";
    private static final Color PRIMARY_BLUE = new Color(0, 132, 255);
    private static final Color CHAT_BUBBLE_OTHER = new Color(241, 240, 240);
    private static final Color SEND_BUTTON_BG = new Color(0, 149, 246);
    private static final Color BORDER_COLOR = new Color(230, 230, 230);
    private static final Color CHAT_BG = Color.WHITE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String nickname;

    private JPanel chatContainer;
    private JScrollPane scrollPane;
    private JTextField messageField;
    private JLabel typingLabel;
    private Timer typingTimer;
    private boolean isCurrentlyTyping = false;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        SwingUtilities.invokeLater(Client::new);
    }

    public Client() {
        requestNickname();
        setupGUI();
        setupTypingTimer();
        connectToServer();
        startListening();
    }

    private void requestNickname() {
        nickname = JOptionPane.showInputDialog(null, "Choose your unique identity handle:", "Chat Setup", JOptionPane.PLAIN_MESSAGE);
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = "Guest_" + (int)(Math.random() * 1000);
        } else {
            nickname = nickname.trim();
        }
    }

    private void setupGUI() {
        JFrame frame = new JFrame(nickname);
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(CHAT_BG);

        chatContainer = new JPanel();
        chatContainer.setLayout(new GridBagLayout());
        chatContainer.setBackground(CHAT_BG);
        chatContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        scrollPane = new JScrollPane(chatContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        messageField = new JTextField();
        messageField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JButton sendButton = new JButton("Push");
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        sendButton.setOpaque(true);
        sendButton.setBorderPainted(false);
        sendButton.setContentAreaFilled(true);
        sendButton.setBackground(SEND_BUTTON_BG);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);

        typingLabel = new JLabel(" ");
        typingLabel.setForeground(Color.LIGHT_GRAY);
        typingLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        typingLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 4, 0));

        JPanel controlWrapper = new JPanel(new BorderLayout());
        controlWrapper.setBackground(CHAT_BG);
        
        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setBackground(CHAT_BG);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        controlWrapper.add(typingLabel, BorderLayout.NORTH);
        controlWrapper.add(inputPanel, BorderLayout.SOUTH);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(controlWrapper, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                handleTypingActivity();
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void setupTypingTimer() {
        typingTimer = new Timer(1500, e -> {
            isCurrentlyTyping = false;
            typingLabel.setText(" ");
        });
        typingTimer.setRepeats(false);
    }

    private void handleTypingActivity() {
        if (!isCurrentlyTyping) {
            isCurrentlyTyping = true;
            typingLabel.setText("typing...");
        }
        typingTimer.restart();
    }

    private void connectToServer() {
        try {
            socket = new Socket(HOST, PORT);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            out.println(nickname);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not connect to server.", "Connection Failed", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void startListening() {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    final String incoming = message;
                    if (incoming.startsWith("[System]")) {
                        appendSystemMessage(incoming.replace("[System]: ", ""));
                    } else {
                        if (!incoming.startsWith(nickname + ":")) {
                            appendChatBubble(incoming, false);
                        }
                    }
                }
            } catch (IOException e) {
                appendSystemMessage("Disconnected from server.");
            } finally {
                closeResources();
            }
        }).start();
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            appendChatBubble("[" + LocalTime.now().format(TIME_FORMATTER) + "] You: " + message, true);
            messageField.setText("");
            isCurrentlyTyping = false;
            typingLabel.setText(" ");
            typingTimer.stop();
        }
    }

    private void appendChatBubble(String message, boolean isRightAligned) {
        SwingUtilities.invokeLater(() -> {
            Color bubbleColor = isRightAligned ? PRIMARY_BLUE : CHAT_BUBBLE_OTHER;
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
            scrollToBottom();
        });
    }

    private void appendSystemMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            JLabel label = new JLabel(text);
            label.setFont(new Font("SansSerif", Font.ITALIC, 11));
            label.setForeground(Color.LIGHT_GRAY);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = chatContainer.getComponentCount();
            gbc.weightx = 1.0;
            gbc.insets = new Insets(6, 2, 6, 2);
            gbc.anchor = GridBagConstraints.CENTER;

            chatContainer.add(label, gbc);
            chatContainer.revalidate();
            chatContainer.repaint();
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
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
}
