# forSale

A Java auction game with both console and Swing GUI versions.

## Overview

- `BuySellGame.java`: Console-based auction gameplay with 4 players, AI opponents, bidding rounds, and final scoring.
- `BuySellGameGUI.java`: Swing GUI version with player setup, live auction screen, property cards, and a results screen.

## How to run

From the project folder, compile and run either version with:

```bash
javac BuySellGame.java
java BuySellGame
```

or

```bash
javac BuySellGameGUI.java
java BuySellGameGUI
```

## Notes

- The GUI uses a `CardLayout` to switch between setup, auction, sell, and results screens.
- The game starts with four players and a fixed number of auction rounds.
- Player names and AI settings can be configured in the GUI setup screen.
