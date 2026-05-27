# 🏃‍♂️ Endless Runner Game

A 2D endless runner side-scroller game built using Java Swing and AWT. Avoid obstacles, rack up high scores, and enjoy smooth physics-based jump mechanics!

## ✨ Features
* **Smooth Jump Physics:** Implements realistic gravity and vertical momentum curves.
* **Dynamic Obstacles:** Spawns random sized objects at varied intervals.
* **Audio Integration:** Looping background tracks for immersive gameplay.
* **Score Tracking:** Real-time calculation of your current run score and your historic High Score.

---

## 🎮 Controls

| Action | Control Option 1 | Control Option 2 |
| :--- | :--- | :--- |
| **Jump** | `W` Key / `UP` Arrow | Mouse Left-Click |
| **Alternative Jump** | `SPACEBAR` | — |

---

## 📂 Asset Structure
Before launching the game, ensure that you place your image and music assets inside an asset directory named `all/` relative to your runtime project directory:

```text
├── src/
│   └── RunnerGame.java
└── all/
    ├── screen.gif   (Player character)
    ├── back.jpg     (Game background)
    ├── ground.png   (Floor platform layer)
    ├── hitter.wav   (Background music)
    ├── ob1.png      (Obstacle variant 1)
    ├── ob2.png      (Obstacle variant 2)
    ├── ob33.png     (Obstacle variant 3)
    └── ob4.png      (Obstacle variant 4)
```

## 🚀 Getting Started
### Prerequisites
   -->Java Development Kit (JDK 8 or higher).

1. Compilation & Running
Open your terminal or command prompt inside the directory containing your source code.

2. Compile the game source file:
```bash
javac RunnerGame.java
```
3. Launch the compiled binary app:
```bash
java RunnerGame 
```
Click the Start button on the bottom control panel to begin playing!