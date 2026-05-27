import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;
import javax.swing.*;

class Obstacle {
    private int x, y;
    private final int width, height;
    private final Image image;
    private static final Random RANDOM = new Random();
    private static final String[] IMAGE_PATHS = {
        "all/ob1.png", "all/ob2.png", "all/ob33.png", "all/ob4.png"
    };

    public Obstacle(int startX, int floorY) {
        this.width = 30 + RANDOM.nextInt(20);
        this.height = 30 + RANDOM.nextInt(20);
        this.x = startX;
        this.y = floorY - this.height;

        String randomPath = IMAGE_PATHS[RANDOM.nextInt(IMAGE_PATHS.length)];
        this.image = new ImageIcon(randomPath).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }

    public void move(int speed) {
        x -= speed;
    }

    public boolean isOffScreen() { 
        return x + width < 0;
    }

    public int getX() {
        return x;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void draw(Graphics g) {
        g.drawImage(image, x, y, null);
    }
}

public class RunnerGame extends JPanel implements ActionListener {
    private final Timer gameTimer;
    private final int runnerX = 50;
    private int runnerY = 250;
    private final int floorY = 300;
    
    // Smooth Jump Physics variables
    private boolean isJumping = false;
    private int yVelocity = 0;
    private final int gravity = 1;
    private final int jumpStrength = -15;

    private Image runner, background, ground;
    private final ArrayList<Obstacle> obstacles;
    
    private int lives = 3;
    private boolean gameOver = false;
    private final int obstacleGap = 220;
    private final int gameSpeed = 6;
    private int score = 0;
    private static int highScore = 0;
    private Clip gameSound;

    public RunnerGame() {
        setPreferredSize(new Dimension(800, 400));
        setBackground(Color.WHITE);
        loadImages();
        playBackgroundMusic("all/hitter.wav");
        
        gameTimer = new Timer(20, this);
        obstacles = new ArrayList<>();
        
        // Key Controls
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if ((e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_SPACE) && !isJumping && !gameOver) {
                    jump();
                }
            }
        });

        // Mouse/Touch Controls
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isJumping && !gameOver) { 
                    jump();
                }
            }
        });

        setFocusable(true);
    }

    private void loadImages() {
        runner = new ImageIcon("all/screen.png").getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH); //can use a running animation by using a gif with multiple frames
        ground = new ImageIcon("all/ground.png").getImage().getScaledInstance(800, 100, Image.SCALE_SMOOTH);
    }

    private void playBackgroundMusic(String filepath) {
        try {
            File file = new File(filepath);
            if (file.exists()) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
                gameSound = AudioSystem.getClip();
                gameSound.open(audioStream);
                gameSound.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                System.out.println("Audio file not found: " + filepath);
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        } 
    }

    private void jump() {
        isJumping = true;
        yVelocity = jumpStrength;
    }

    public void startGame() {
        if (!gameTimer.isRunning()) {
            gameTimer.start();
        }
        obstacles.clear();
        lives = 3;
        gameOver = false;
        score = 0;
        runnerY = floorY - 50; // Reset position onto the floor
        yVelocity = 0;
        isJumping = false;
        requestFocusInWindow(); 
    }

    public void pauseGame() {
        gameTimer.stop();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            // Apply Gravity physics
            if (isJumping) {
                runnerY += yVelocity;
                yVelocity += gravity;

                // Check if landed
                if (runnerY >= floorY - 50) {
                    runnerY = floorY - 50;
                    isJumping = false;
                }
            }

            updateObstacles();
            checkCollisions();
            repaint();
        }
    }

    private void updateObstacles() {
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle obs = obstacles.get(i);
            obs.move(gameSpeed);
            
            if (obs.isOffScreen()) {
                obstacles.remove(i);
                i--;
                score++;
                if (score > highScore) {
                    highScore = score;
                }
            }
        }

        if (obstacles.isEmpty() || obstacles.get(obstacles.size() - 1).getX() < 800 - obstacleGap) {
            obstacles.add(new Obstacle(850, floorY));
        }
    }

    private void checkCollisions() {
        Rectangle runnerBounds = new Rectangle(runnerX, runnerY, 50, 50);
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle obs = obstacles.get(i);
            if (runnerBounds.intersects(obs.getBounds())) {
                lives--;
                obstacles.remove(i);
                if (lives <= 0) {
                    gameOver = true;
                    gameTimer.stop();
                }
                break;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw layers
        g.drawImage(background, 0, 0, null);
        g.drawImage(ground, 0, floorY, null);
        g.drawImage(runner, runnerX, runnerY, null);

        for (Obstacle obs : obstacles) {
            obs.draw(g);
        }

        // Draw UI HUD
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Lives: " + lives, 15, 25);
        g.drawString("Score: " + score, 15, 45);
        g.drawString("High Score: " + highScore, 15, 65);

        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            FontMetrics metrics = g.getFontMetrics();
            String overText = "GAME OVER";
            String subText = "Click 'Replay' to try again!";
            
            int x1 = (getWidth() - metrics.stringWidth(overText)) / 2;
            g.drawString(overText, x1, 160);
            
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            g.setColor(Color.BLACK);
            metrics = g.getFontMetrics();
            int x2 = (getWidth() - metrics.stringWidth(subText)) / 2;
            g.drawString(subText, x2, 200);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Endless Runner Game");
        RunnerGame game = new RunnerGame();

        JPanel controlPanel = new JPanel();
        JButton startBtn = new JButton("Start");
        JButton pauseBtn = new JButton("Pause");
        JButton replayBtn = new JButton("Replay");

        // UI button event bindings
        startBtn.addActionListener(e -> game.startGame());
        pauseBtn.addActionListener(e -> game.pauseGame());
        replayBtn.addActionListener(e -> game.startGame()); // Replay just uses start logic

        // Prevent buttons from stealing keyboard focus permanently
        startBtn.setFocusable(false);
        pauseBtn.setFocusable(false);
        replayBtn.setFocusable(false);

        controlPanel.add(startBtn);
        controlPanel.add(pauseBtn);
        controlPanel.add(replayBtn);

        frame.add(game, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);
        
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); // Centers window on screen
        frame.setVisible(true);
        
        // Let game start receiving key inputs immediately
        game.requestFocusInWindow();
    }
}