
# 💬 SmoothChat - Messenger & Instagram Inspired Swing UI

A lightweight, high-performance, real-time Java Swing chat framework implementing client-server socket streams. Features an adaptive bubble-based user interface optimized for flat layouts and cross-platform native rendering behavior without graphic engine occlusion.

---

## 🚀 Architectural Design Specs

* **Dynamic Grid Layouts:** Replaced traditional linear text constraints with `GridBagLayout` anchors to guarantee smooth bubble alignment across variable viewport scaling.
* **Component-Level Graphics Isolation:** Custom background rendering handling occurs strictly inside isolated component lifecycles to prevent native font anti-aliasing overrides.
* **Typing Debounce Processing:** Implements event-driven timers that gracefully refresh typing states with low processing overhead.
* **Stream Safety Integration:** Utilizes explicit thread management wrappers (`SwingUtilities.invokeLater`) ensuring absolute runtime thread safety for UI-bound operations.

---

## 🎮 Interface Controls & HUD

| Component | Default Action | Layout Hook | Theme Color |
| :--- | :--- | :--- | :--- |
| **Input Field** | Message buffer storage | `BorderLayout.CENTER` | Hex `#E6E6E6` |
| **Push / Send Button** | Dispatches message queue | `BorderLayout.EAST` | Hex `#0095F6` |
| **Self Chat Bubble** | Aligns to right quadrant | `GridBagConstraints.LINE_END` | Hex `#0084FF` |
| **Peer Chat Bubble** | Aligns to left quadrant | `GridBagConstraints.LINE_START` | Hex `#F1F0F0` |

---

## 📂 Structural Overview

Keep your classes organized inside standard Java project root structures as defined below:

```text
├── Server.java            (Standalone host application)
└── all/
    └── Client.java        (Client package application container)

```

---

## ⚙️ Compilation & Runtime Execution

### Step 1: Initializing the Server Environment

1. Open a terminal path at your root folder location and compile the server component:
```bash
javac Server.java

```


2. Launch the server context instance:
```bash
java Server

```



### Step 2: Compiling & Launching Connected Clients

1. To run package-bound assets, compile directly from the root layout directory targeting the full folder path:
```bash
javac all/Client.java

```


2. Spin up separate client interface targets by declaring their fully-qualified package naming syntax:
```bash
java all.Client

```



---

## 🛠️ Deep Dive: The Graphic Truncation Fix

Previous iterations using standard `setParagraphAttributes` alongside explicit HTML layouts resulted in text truncation blocks on specific Unix/macOS window servers. The current release completely eliminates this by isolating layout calculations:

1. Setting explicit components to `setOpaque(false)`.
2. Encapsulating font vector rendering inside structural `BorderLayout` nodes layered cleanly *above* custom `paintComponent` overrides.

