import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * Buy & Sell Console - AP CSA style
 * Implements a console-only auction game with 4 players.
 * - Bids increase in $1000 increments
 * - Human player input validated
 * - AI players make simple logical decisions
 * - Passing awards the lowest available property at half current bid
 * - Winning bidder pays full bid and receives the highest available property
 *
 * Compile and run:
 * javac BuySellGame.java
 * java BuySellGame
 */
public class BuySellGame {
    public static void main(String[] args) {
        GameManager game = new GameManager();
        game.start();
    }
}

/**
 * Controls the entire game: players, rounds, and final scoring.
 */
class GameManager {
    private static final int STARTING_BALANCE = 10000; // as requested
    private static final int TOTAL_ROUNDS = 5;
    private final Scanner scanner = new Scanner(System.in);
    private final List<Player> players = new ArrayList<>();
    private final List<String> activityLog = new ArrayList<>();
    private int roundNumber = 1;

    public GameManager() {
        // Players will be created during setup
    }

    public void start() {
        printWelcome();
        setupPlayers();
        confirmGameStart();

        while (roundNumber <= TOTAL_ROUNDS && playersHaveFunds()) {
            RoundManager round = new RoundManager(roundNumber, TOTAL_ROUNDS, players, scanner, activityLog);
            round.playRound();
            roundNumber++;
            if (roundNumber <= TOTAL_ROUNDS) {
                promptEnterKey("Press Enter to continue to the next round...");
            }
        }

        printFinalResults();
    }

    private void printWelcome() {
        clearConsole();
        System.out.println("====================================================");
        System.out.println("        BUY & SELL AUCTION GAME - CONSOLE EDITION      ");
        System.out.println("====================================================");
        System.out.println("Four players compete in a property auction game.");
        System.out.println("Each player starts with $" + STARTING_BALANCE + " and bids on property cards.");
        System.out.println("Pass to receive a lower-value property at half of your bid.");
        System.out.println();
    }

    private void setupPlayers() {
        System.out.println("====================================================");
        System.out.println("                  PLAYER SETUP                      ");
        System.out.println("====================================================");

        // Human player (Player 1)
        System.out.print("\nEnter your player name (default: You): ");
        String humanName = scanner.nextLine().trim();
        if (humanName.isEmpty()) humanName = "You";
        players.add(new Player(humanName, STARTING_BALANCE, true));

        // AI players (Players 2, 3, 4)
        String[] defaultAiNames = {"Ada", "Boris", "Cleo"};
        for (int i = 0; i < 3; i++) {
            System.out.println();
            System.out.print("Player " + (i + 2) + " name (default: " + defaultAiNames[i] + "): ");
            String name = scanner.nextLine().trim();
            if (name.isEmpty()) name = defaultAiNames[i];

            System.out.print("Is " + name + " an AI player? (y/n, default: y): ");
            String aiInput = scanner.nextLine().trim().toLowerCase();
            boolean isAi = !aiInput.equals("n");

            players.add(new Player(name, STARTING_BALANCE, !isAi));
        }
    }

    private void confirmGameStart() {
        clearConsole();
        System.out.println("====================================================");
        System.out.println("                   PLAYER ROSTER                    ");
        System.out.println("====================================================");
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            String type = p.isHuman() ? "Human" : "AI";
            System.out.println((i + 1) + ". " + p.getName() + " (" + type + ") - Starting balance: $" + STARTING_BALANCE);
        }
        System.out.println();
        promptEnterKey("Press Enter to begin the game...");
    }

    private boolean playersHaveFunds() {
        for (Player player : players) {
            if (player.getBalance() > 0) return true;
        }
        return false;
    }

    private void promptEnterKey(String message) {
        System.out.println();
        System.out.print(message);
        scanner.nextLine();
        System.out.println();
    }

    private void printFinalResults() {
        clearConsole();
        System.out.println("====================================================");
        System.out.println("                    FINAL RESULTS                    ");
        System.out.println("====================================================");

        // Calculate score: money + sum of properties (purchased + passed)
        List<PlayerScore> scores = new ArrayList<>();
        for (Player p : players) {
            int propSum = 0;
            for (Property pr : p.getPurchasedProperties()) propSum += pr.getValue();
            for (Property pr : p.getPassedProperties()) propSum += pr.getValue();
            int total = p.getBalance() + propSum;
            scores.add(new PlayerScore(p.getName(), total, p.getBalance(), propSum));
        }

        Collections.sort(scores, Comparator.comparingInt(PlayerScore::getScore).reversed());

        System.out.printf("%-4s | %-8s | %10s | %14s\n", "Rank", "Player", "Money", "Property Value");
        System.out.println("----------------------------------------------------");
        int r = 1;
        for (PlayerScore ps : scores) {
            System.out.printf("%-4d | %-8s | $%8d | $%12d\n", r, ps.name, ps.money, ps.propertyValue);
            r++;
        }
        System.out.println("----------------------------------------------------");

        System.out.println("\nActivity log (recent):");
        int start = Math.max(0, activityLog.size() - 20);
        for (int i = start; i < activityLog.size(); i++) System.out.println(" - " + activityLog.get(i));

        System.out.println();
        System.out.println("Winner: " + scores.get(0).name);
    }

    private void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static class PlayerScore {
        final String name;
        final int score;
        final int money;
        final int propertyValue;

        PlayerScore(String name, int score, int money, int propertyValue) {
            this.name = name;
            this.score = score;
            this.money = money;
            this.propertyValue = propertyValue;
        }

        int getScore() { return score; }
    }
}

/**
 * Handles a single round's auction logic and presentation.
 */
class RoundManager {
    private static final int PROPERTY_COUNT = 4;
    private static final int MIN_PROPERTY_VALUE = 2000;
    private static final int MAX_PROPERTY_VALUE = 10000;
    private static final int BID_INCREMENT = 1000;

    private final int roundNumber;
    private final int totalRounds;
    private final List<Player> players;
    private final Scanner scanner;
    private final List<Property> properties = new ArrayList<>();
    private final List<Player> activePlayers = new ArrayList<>();
    private final List<Player> passedPlayers = new ArrayList<>();
    private final List<String> activityLog; // shared with GameManager
    private int currentHighestBid = 0;
    private Player currentHighestBidder = null;
    private final Random random = new Random();

    public RoundManager(int roundNumber, int totalRounds, List<Player> players, Scanner scanner, List<String> activityLog) {
        this.roundNumber = roundNumber;
        this.totalRounds = totalRounds;
        this.players = players;
        this.scanner = scanner;
        this.activityLog = activityLog;
    }

    public void playRound() {
        prepareRound();
        displayRoundState();

        while (activePlayers.size() > 1) {
            for (Player p : new ArrayList<>(activePlayers)) {
                if (activePlayers.size() <= 1) break;
                if (p.hasPassed()) continue;
                handlePlayerTurn(p);
                displayRoundState();
            }
        }

        concludeRound();
    }

    private void prepareRound() {
        generateProperties();
        activePlayers.clear();
        activePlayers.addAll(players);
        passedPlayers.clear();
        currentHighestBid = 0;
        currentHighestBidder = null;
        for (Player p : players) p.resetRoundState();
    }

    private void generateProperties() {
        properties.clear();
        for (int i = 0; i < PROPERTY_COUNT; i++) {
            int value = MIN_PROPERTY_VALUE + random.nextInt(MAX_PROPERTY_VALUE - MIN_PROPERTY_VALUE + 1);
            properties.add(new Property("Property " + (i+1), ((value + 499) / 1000) * 1000));
        }
        Collections.sort(properties, Comparator.comparingInt(Property::getValue));
    }

    private void displayProperties() {
        System.out.print("Available properties:");
        for (Property property : properties) {
            System.out.print(" [" + property.getName() + ": " + formatMoney(property.getValue()) + "]");
        }
        System.out.println();
    }

    private void handlePlayerTurn(Player player) {
        if (player.getBalance() <= 0) {
            passPlayer(player, "has no money left and is forced to pass");
            return;
        }
        if (player.isHuman()) humanTurn(player); else aiTurn(player);
    }

    private void humanTurn(Player player) {
        System.out.println("----------------------------------------------------");
        System.out.println("Your turn. Type your bid amount or 0 to PASS.");
        int choice = readBidAmount(player);
        if (choice == 0) { passPlayer(player, "passes"); return; }
        placeBid(player, choice);
    }

    private void aiTurn(Player player) {
        boolean isLeader = player == currentHighestBidder;
        Property highest = getHighestRemainingProperty();
        int maxWill = calculateAiWillingness(player, highest);

        if (player.getCurrentBid() == 0) {
            if (currentHighestBid == 0 || currentHighestBid < maxWill) {
                int bid = Math.min(player.getBalance(), Math.max(BID_INCREMENT, currentHighestBid + BID_INCREMENT));
                bid = ((bid + BID_INCREMENT - 1) / BID_INCREMENT) * BID_INCREMENT;
                placeBid(player, bid);
            } else passPlayer(player, "passes");
            return;
        }

        if (isLeader) {
            int next = currentHighestBid + BID_INCREMENT;
            if (next <= maxWill && next <= player.getBalance()) placeBid(player, next);
            else logActivity(player.getName() + " holds at " + formatMoney(currentHighestBid));
            return;
        }

        int minRaise = currentHighestBid + BID_INCREMENT;
        if (minRaise <= maxWill && minRaise <= player.getBalance()) placeBid(player, minRaise);
        else passPlayer(player, "passes");
    }

    private int calculateAiWillingness(Player player, Property highestProperty) {
        double aggr = 0.5 + random.nextDouble() * 0.4; // 0.5 - 0.9
        int target = (int)(highestProperty.getValue() * aggr);
        int buffered = Math.max(target, currentHighestBid + BID_INCREMENT);
        return Math.min(buffered, player.getBalance());
    }

    private void placeBid(Player player, int amount) {
        if (amount % BID_INCREMENT != 0) amount = (amount / BID_INCREMENT) * BID_INCREMENT;
        player.setCurrentBid(amount);
        currentHighestBid = amount;
        currentHighestBidder = player;
        logActivity(player.getName() + " bids " + formatMoney(amount));
    }

    private void passPlayer(Player player, String reason) {
        player.setHasPassed(true);
        activePlayers.remove(player);
        passedPlayers.add(player);
        Property prop = removeLowestProperty();
        int cost = player.getCurrentBid() / 2;
        player.adjustBalance(-cost);
        player.addPassedProperty(prop);
        String act = player.getName() + " " + reason + " and receives " + prop.getName() + " for " + formatMoney(cost);
        if (player.isHuman()) act = "You pass and receive " + prop.getName() + " for " + formatMoney(cost);
        logActivity(act);
        player.setCurrentBid(0);
        recomputeHighestBidder();
    }

    private void recomputeHighestBidder() {
        currentHighestBid = 0;
        currentHighestBidder = null;
        for (Player p : activePlayers) {
            if (p.getCurrentBid() > currentHighestBid) {
                currentHighestBid = p.getCurrentBid();
                currentHighestBidder = p;
            }
        }
    }

    private void concludeRound() {
        if (activePlayers.size() == 1) {
            Player winner = activePlayers.get(0);
            int bid = winner.getCurrentBid();
            Property p = removeHighestProperty();
            winner.adjustBalance(-bid);
            winner.addPurchasedProperty(p);
            String msg = winner.getName() + " wins the round with a bid of " + formatMoney(bid) + " and receives " + p.getName();
            if (winner.isHuman()) msg = "You win the round with a bid of " + formatMoney(bid) + " and receive " + p.getName();
            logActivity(msg);
        } else if (activePlayers.isEmpty()) {
            logActivity("All players passed. Remaining properties are not awarded this round.");
        } else {
            Player winner = currentHighestBidder != null ? currentHighestBidder : activePlayers.get(0);
            int bid = winner.getCurrentBid();
            Property p = removeHighestProperty();
            winner.adjustBalance(-bid);
            winner.addPurchasedProperty(p);
            String msg = winner.getName() + " wins the round with a final bid of " + formatMoney(bid) + " and receives " + p.getName();
            if (winner.isHuman()) msg = "You win the round with a final bid of " + formatMoney(bid) + " and receive " + p.getName();
            logActivity(msg);
        }
        displayRoundState();
    }

    private void displayRoundState() {
        clearConsole();
        System.out.println("====================================================");
        System.out.println("Round " + roundNumber + " of " + totalRounds + " - Auction Status");
        System.out.println("----------------------------------------------------");
        displayProperties();
        System.out.println("Current highest bid: " + formatMoney(currentHighestBid) + (currentHighestBidder == null ? "" : " by " + currentHighestBidder.getName()));
        System.out.println("----------------------------------------------------");
        System.out.printf("%-8s | %10s | %10s | %12s\n", "Player", "Balance", "Bid", "Owned Props");
        System.out.println("----------------------------------------------------");
        for (Player p : players) {
            System.out.printf("%-8s | $%9d | $%8d | %10d props\n", p.getName(), p.getBalance(), p.getCurrentBid(), p.getPurchasedProperties().size() + p.getPassedProperties().size());
        }
        System.out.println("----------------------------------------------------");

        // show human properties
        for (Player p : players) {
            if (p.isHuman()) {
                System.out.print("Your properties: ");
                List<String> names = new ArrayList<>();
                for (Property pr : p.getPurchasedProperties()) names.add(pr.getName() + "(" + formatMoney(pr.getValue()) + ")");
                for (Property pr : p.getPassedProperties()) names.add(pr.getName() + "(" + formatMoney(pr.getValue()) + ")");
                if (names.isEmpty()) System.out.println("(none)"); else System.out.println(String.join(", ", names));
                break;
            }
        }

        System.out.println("\nActivity log (recent):");
        int start = Math.max(0, activityLog.size() - 10);
        for (int i = start; i < activityLog.size(); i++) System.out.println(" - " + activityLog.get(i));
        System.out.println();
    }

    private int readBidAmount(Player player) {
        int minimum = currentHighestBid + BID_INCREMENT;
        System.out.print("Enter 0 to PASS, or bid at least " + formatMoney(minimum) + " (in " + formatMoney(BID_INCREMENT) + " increments): ");
        String in = scanner.nextLine().trim();
        try {
            int v = Integer.parseInt(in);
            if (v == 0) return 0;
            if (v < minimum) { System.out.println("Your bid must be at least " + formatMoney(minimum) + "."); return readBidAmount(player); }
            if (v % BID_INCREMENT != 0) { System.out.println("Bids must be in " + formatMoney(BID_INCREMENT) + " increments."); return readBidAmount(player); }
            if (v > player.getBalance()) { System.out.println("You do not have enough money for that bid."); return readBidAmount(player); }
            return v;
        } catch (NumberFormatException e) {
            System.out.println("Invalid entry. Please enter a whole number.");
            return readBidAmount(player);
        }
    }

    private Property removeLowestProperty() { return properties.remove(0); }
    private Property removeHighestProperty() { return properties.remove(properties.size() - 1); }
    private Property getHighestRemainingProperty() { if (properties.isEmpty()) return new Property("No Property", 0); return properties.get(properties.size() - 1); }

    private void logActivity(String action) { activityLog.add(action); }
    private String formatMoney(int amount) { return "$" + amount; }
    private void clearConsole() { System.out.print("\033[H\033[2J"); System.out.flush(); }
}

/**
 * Represents a player in the auction game.
 */
class Player {
    private final String name;
    private int balance;
    private final boolean human;
    private int currentBid;
    private boolean passed;
    private final List<Property> purchasedProperties = new ArrayList<>();
    private final List<Property> passedProperties = new ArrayList<>();

    public Player(String name, int balance, boolean human) {
        this.name = name;
        this.balance = balance;
        this.human = human;
    }

    public String getName() { return name; }
    public int getBalance() { return balance; }
    public boolean isHuman() { return human; }
    public int getCurrentBid() { return currentBid; }
    public List<Property> getPurchasedProperties() { return purchasedProperties; }
    public List<Property> getPassedProperties() { return passedProperties; }
    public boolean hasPassed() { return passed; }

    public void resetRoundState() { currentBid = 0; passed = false; }
    public void setCurrentBid(int bid) { currentBid = bid; }
    public void setHasPassed(boolean passed) { this.passed = passed; }
    public void adjustBalance(int amount) { balance += amount; }
    public void addPurchasedProperty(Property property) { purchasedProperties.add(property); }
    public void addPassedProperty(Property property) { passedProperties.add(property); }
}

/**
 * Represents a property card available for bidding.
 */
class Property {
    private final String name;
    private final int value;

    public Property(String name, int value) { this.name = name; this.value = value; }
    public String getName() { return name; }
    public int getValue() { return value; }
}
