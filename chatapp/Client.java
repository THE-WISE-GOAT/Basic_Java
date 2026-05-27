package all;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Client {
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
            // Fallback
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
        nickname = JOptionPane.showInputDialog(null, "Choose your unique identity handle:", "Instagram Chat Setup", JOptionPane.PLAIN_MESSAGE);
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = "Guest_" + (int)(Math.random() * 1000);
        } else {
            nickname = nickname.trim();
        }
    }

    private void setupGUI() {
        JFrame frame = new JFrame(nickname);
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

        JButton sendButton = new JButton("Push");
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        sendButton.setOpaque(true);
        sendButton.setBorderPainted(false);
        sendButton.setContentAreaFilled(true);
        sendButton.setBackground(new Color(0, 149, 246));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);

        typingLabel = new JLabel(" ");
        typingLabel.setForeground(Color.LIGHT_GRAY);
        typingLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        typingLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 4, 0));

        JPanel controlWrapper = new JPanel(new BorderLayout());
        controlWrapper.setBackground(Color.WHITE);
        
        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setBackground(Color.WHITE);
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
            SwingUtilities.invokeLater(() -> typingLabel.setText(" "));
        });
        typingTimer.setRepeats(false);
    }

    private void handleTypingActivity() {
        if (!isCurrentlyTyping) {
            isCurrentlyTyping = true;
            SwingUtilities.invokeLater(() -> typingLabel.setText("typing..."));
        }
        typingTimer.restart();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            out.println(nickname);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not hook connection stream.", "Network Failure", JOptionPane.ERROR_MESSAGE);
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
                appendSystemMessage("Disconnected structural route link from host.");
            } finally {
                closeResources();
            }
        }).start();
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            appendChatBubble("[" + getCurrentTime() + "] You: " + message, true);
            messageField.setText("");
            isCurrentlyTyping = false;
            typingLabel.setText(" ");
            typingTimer.stop();
        }
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
            
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        });
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("HH:mm").format(new Date());
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

    // Custom structural UI layer handles font rendering cleanly across platforms
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