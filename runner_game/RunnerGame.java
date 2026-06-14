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

enum GameState {
    MENU, PLAYING, PAUSED, GAME_OVER
}

public class RunnerGame extends JPanel implements ActionListener {
    private final Timer gameTimer;
    private final int runnerX = 50;
    private int runnerY = 250;
    private final int floorY = 300;
    private static final int WIDTH = 801;
    private static final int HEIGHT = 401;
    private static final int GRAVITY = 2;
    private static final int JUMP_STRENGTH = -14;
    private static final int GAME_SPEED_INITIAL = 7;
    private static final int OBSTACLE_GAP = 221;
    private static final int PANEL_PADDING = 10;
    private static final int HUD_Y_OFFSET = 25;
    private static final int HUD_Y_STEP = 20;
    private static final int HUD_X = 15;
    private static final int MAX_LIVES = 3;
    private static final int INITIAL_SCORE = 0;
    private static final int RESET_RUNNER_Y_OFFSET = 50;
    private static final int RUNNER_SIZE = 51;
    private static final int GROUND_HEIGHT = 101;
    private static final int INITIAL_OBSTACLE_X = 850;
    private static final int TIMER_DELAY = 19;
    private static final int DOUBLE_JUMP_THRESHOLD = 10;
    private static final double SPEED_INCREMENT = 0.005;

    private boolean isJumping = false;
    private int jumpCount = 0;
    private int yVelocity = 0;
    private int gameSpeed = GAME_SPEED_INITIAL;
    private GameState gameState = GameState.MENU;

    private Image runner, background, ground;
    private final ArrayList<Obstacle> obstacles;
    private int lives = MAX_LIVES;
    private int score = INITIAL_SCORE;
    private static int highScore = 0;
    private Clip gameSound;
    private boolean soundEnabled = true;

    private static final Font HUD_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font GAME_OVER_FONT_BIG = new Font("Arial", Font.BOLD, 36);
    private static final Font GAME_OVER_FONT_SMALL = new Font("Arial", Font.PLAIN, 18);
    private static final Color HUD_COLOR = Color.DARK_GRAY;
    private static final Color GAME_OVER_RED = Color.RED;
    private static final Color GAME_OVER_BLACK = Color.BLACK;
    private static final Color BG_COLOR = Color.WHITE;

    public RunnerGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(BG_COLOR);
        loadImages();
        playBackgroundMusic("all/hitter.wav");
        
        gameTimer = new Timer(TIMER_DELAY, this);
        obstacles = new ArrayList<>();
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleInput(e);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleInput(null);
            }
        });

        setFocusable(true);
    }

    private void handleInput(KeyEvent e) {
        boolean upPressed = e != null && (e.getKeyCode() == KeyEvent.VK_UP 
            || e.getKeyCode() == KeyEvent.VK_W 
            || e.getKeyCode() == KeyEvent.VK_SPACE);
        
        if (gameState == GameState.MENU || gameState == GameState.GAME_OVER) {
            startGame();
        } else if (gameState == GameState.PLAYING) {
            if ((upPressed || e == null) && !isJumping || jumpCount < 2) {
                jump();
            }
        } else if (gameState == GameState.PAUSED) {
            resumeGame();
        }
    }

    private void loadImages() {
        runner = new ImageIcon("all/screen.png").getImage().getScaledInstance(RUNNER_SIZE, RUNNER_SIZE, Image.SCALE_SMOOTH);
        background = new ImageIcon("all/back.jpg").getImage().getScaledInstance(WIDTH, HEIGHT, Image.SCALE_SMOOTH);
        ground = new ImageIcon("all/ground.png").getImage().getScaledInstance(WIDTH, GROUND_HEIGHT, Image.SCALE_SMOOTH);
    }

    private void playBackgroundMusic(String filepath) {
        try {
            File file = new File(filepath);
            if (file.exists()) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
                gameSound = AudioSystem.getClip();
                gameSound.open(audioStream);
                gameSound.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void jump() {
        isJumping = true;
        yVelocity = JUMP_STRENGTH;
        jumpCount++;
    }

    public void startGame() {
        gameState = GameState.PLAYING;
        if (!gameTimer.isRunning()) {
            gameTimer.start();
        }
        obstacles.clear();
        lives = MAX_LIVES;
        score = INITIAL_SCORE;
        runnerY = floorY - RESET_RUNNER_Y_OFFSET;
        yVelocity = 0;
        isJumping = false;
        jumpCount = 0;
        gameSpeed = GAME_SPEED_INITIAL;
        requestFocusInWindow();
    }

    public void pauseGame() {
        gameState = GameState.PAUSED;
        gameTimer.stop();
    }

    public void resumeGame() {
        gameState = GameState.PLAYING;
        gameTimer.start();
        requestFocusInWindow();
    }

    public void toggleSound() {
        soundEnabled = !soundEnabled;
        if (gameSound != null) {
            if (soundEnabled) {
                gameSound.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                gameSound.stop();
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == GameState.PLAYING) {
            if (isJumping) {
                runnerY += yVelocity;
                yVelocity += GRAVITY;

                if (runnerY >= floorY - RESET_RUNNER_Y_OFFSET) {
                    runnerY = floorY - RESET_RUNNER_Y_OFFSET;
                    isJumping = false;
                    jumpCount = 0;
                }
            }

            updateObstacles();
            checkCollisions();
            gameSpeed += SPEED_INCREMENT;
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

        if (obstacles.isEmpty() || obstacles.get(obstacles.size() - 1).getX() < WIDTH - OBSTACLE_GAP) {
            obstacles.add(new Obstacle(INITIAL_OBSTACLE_X, floorY));
        }
    }

    private void checkCollisions() {
        Rectangle runnerBounds = new Rectangle(runnerX + 5, runnerY + 5, RUNNER_SIZE - 10, RUNNER_SIZE - 10);
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle obs = obstacles.get(i);
            if (runnerBounds.intersects(obs.getBounds())) {
                lives--;
                obstacles.remove(i);
                if (lives <= 0) {
                    gameState = GameState.GAME_OVER;
                    gameTimer.stop();
                }
                break;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(background, 0, 0, null);
        g.drawImage(ground, 0, floorY, null);
        g.drawImage(runner, runnerX, runnerY, null);

        for (Obstacle obs : obstacles) {
            obs.draw(g);
        }

        g.setColor(HUD_COLOR);
        g.setFont(HUD_FONT);
        g.drawString("Lives: " + lives, HUD_X, HUD_Y_OFFSET);
        g.drawString("Score: " + score, HUD_X, HUD_Y_OFFSET + HUD_Y_STEP);
        g.drawString("High Score: " + highScore, HUD_X, HUD_Y_OFFSET + 2 * HUD_Y_STEP);

        if (gameState == GameState.GAME_OVER) {
            drawGameOverScreen(g);
        } else if (gameState == GameState.MENU) {
            drawMenuScreen(g);
        } else if (gameState == GameState.PAUSED) {
            drawPauseOverlay(g);
        }
    }

    private void drawGameOverScreen(Graphics g) {
        g.setColor(GAME_OVER_RED);
        g.setFont(GAME_OVER_FONT_BIG);
        FontMetrics metrics = g.getFontMetrics();
        String overText = "GAME OVER";
        String subText = "Click or press SPACE to replay!";
        
        int x1 = (WIDTH - metrics.stringWidth(overText)) / 2;
        g.drawString(overText, x1, 160);
        
        g.setFont(GAME_OVER_FONT_SMALL);
        g.setColor(GAME_OVER_BLACK);
        metrics = g.getFontMetrics();
        int x2 = (WIDTH - metrics.stringWidth(subText)) / 2;
        g.drawString(subText, x2, 200);
    }

    private void drawMenuScreen(Graphics g) {
        g.setColor(GAME_OVER_RED);
        g.setFont(GAME_OVER_FONT_BIG);
        FontMetrics metrics = g.getFontMetrics();
        String title = "ENDLESS RUNNER";
        
        int x = (WIDTH - metrics.stringWidth(title)) / 2;
        g.drawString(title, x, 140);
        
        g.setFont(GAME_OVER_FONT_SMALL);
        g.setColor(GAME_OVER_BLACK);
        String startText = "Click or press SPACE to start!";
        metrics = g.getFontMetrics();
        int x2 = (WIDTH - metrics.stringWidth(startText)) / 2;
        g.drawString(startText, x2, 200);
    }

    private void drawPauseOverlay(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        
        g2.setColor(Color.WHITE);
        g2.setFont(GAME_OVER_FONT_BIG);
        FontMetrics metrics = g2.getFontMetrics();
        String pauseText = "PAUSED";
        int x = (WIDTH - metrics.stringWidth(pauseText)) / 2;
        g2.drawString(pauseText, x, 160);
        
        g2.setFont(GAME_OVER_FONT_SMALL);
        String resumeText = "Click or press SPACE to resume";
        metrics = g2.getFontMetrics();
        int x2 = (WIDTH - metrics.stringWidth(resumeText)) / 2;
        g2.drawString(resumeText, x2, 200);
        g2.dispose();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Endless Runner Game");
        RunnerGame game = new RunnerGame();

        JPanel controlPanel = new JPanel();
        JButton startBtn = new JButton("Start");
        JButton pauseBtn = new JButton("Pause");
        JButton replayBtn = new JButton("Replay");
        JButton soundBtn = new JButton("Sound: ON");

        startBtn.addActionListener(e -> game.startGame());
        pauseBtn.addActionListener(e -> {
            if (game.gameState == GameState.PLAYING) {
                game.pauseGame();
            } else if (game.gameState == GameState.PAUSED) {
                game.resumeGame();
            }
        });
        replayBtn.addActionListener(e -> game.startGame());
        soundBtn.addActionListener(e -> {
            game.toggleSound();
            soundBtn.setText(game.soundEnabled ? "Sound: ON" : "Sound: OFF");
        });

        startBtn.setFocusable(false);
        pauseBtn.setFocusable(false);
        replayBtn.setFocusable(false);
        soundBtn.setFocusable(false);

        controlPanel.add(startBtn);
        controlPanel.add(pauseBtn);
        controlPanel.add(replayBtn);
        controlPanel.add(soundBtn);

        frame.add(game, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);
        
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        game.requestFocusInWindow();
    }
}
