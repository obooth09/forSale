import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Swing GUI for Buy & Sell Auction Game
 * Provides a graphical interface with buttons, panels, and interactive controls.
 */
public class BuySellGameGUI extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private GameState gameState;

    public BuySellGameGUI() {
        setTitle("Buy & Sell Auction Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setResizable(false);

        gameState = new GameState();
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(new SetupPanel(this, gameState), "setup");
        mainPanel.add(new AuctionPanel(this, gameState), "auction");
        mainPanel.add(new SellPanel(this, gameState), "sell");
        mainPanel.add(new ResultsPanel(this, gameState), "results");

        add(mainPanel);
        showSetupScreen();
        System.out.println("✓ BuySellGameGUI initialized and visible");
    }

    public void showSetupScreen() {
        cardLayout.show(mainPanel, "setup");
    }

    public void showAuctionScreen() {
        ((AuctionPanel) mainPanel.getComponent(1)).refresh();
        cardLayout.show(mainPanel, "auction");
    }

    public void showSellScreen() {
        ((SellPanel) mainPanel.getComponent(2)).refresh();
        cardLayout.show(mainPanel, "sell");
    }

    public void showResultsScreen() {
        ((ResultsPanel) mainPanel.getComponent(3)).initializeResults();
        cardLayout.show(mainPanel, "results");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BuySellGameGUI frame = new BuySellGameGUI();
            frame.setVisible(true);
            frame.toFront();
            frame.requestFocus();
            System.out.println("✓ Window is now visible and focused");
        });
    }
}

/**
 * Manages the game state shared between GUI panels.
 */
class GameState {
    private static final int STARTING_BALANCE = 18000;
    private List<Player> players = new ArrayList<>();
    private List<String> activityLog = new ArrayList<>();
    private List<Integer> currentChecks = new ArrayList<>();
    private List<SaleResult> lastSaleResults = new ArrayList<>();
    private int roundNumber = 1;
    private int saleRoundNumber = 1;
    private static final int TOTAL_ROUNDS = 5;
    private RoundManager currentRound;
    private Random random = new Random();

    public GameState() {}

    public void initializePlayers(String[] names, boolean[] isHuman, String[] aiDifficulties) {
        players.clear();
        for (int i = 0; i < names.length; i++) {
            Player player = new Player(names[i], STARTING_BALANCE, isHuman[i]);
            player.setAiDifficulty(aiDifficulties[i]);
            players.add(player);
        }
        activityLog.clear();
        currentChecks.clear();
        lastSaleResults.clear();
        roundNumber = 1;
        saleRoundNumber = 1;
        startNextRound();
    }

    public void startNextRound() {
        if (roundNumber <= TOTAL_ROUNDS && playersHaveFunds()) {
            currentRound = new RoundManager(roundNumber, TOTAL_ROUNDS, players, null, activityLog);
            currentRound.initializeForGui();
            roundNumber++;
        }
    }

    private boolean playersHaveFunds() {
        for (Player p : players) {
            if (p.getBalance() > 0) return true;
        }
        return false;
    }

    public List<Player> getPlayers() { return players; }
    public List<String> getActivityLog() { return activityLog; }
    public List<Integer> getCurrentChecks() { return new ArrayList<>(currentChecks); }
    public List<SaleResult> getLastSaleResults() { return new ArrayList<>(lastSaleResults); }
    public int getSaleRoundNumber() { return saleRoundNumber; }
    public int getRoundNumber() { return roundNumber - 1; }
    public int getTotalRounds() { return TOTAL_ROUNDS; }
    public RoundManager getCurrentRound() { return currentRound; }
    public boolean hasMoreRounds() { return roundNumber <= TOTAL_ROUNDS && playersHaveFunds(); }
    public void startSellingPhase() {
        saleRoundNumber = 1;
        lastSaleResults.clear();
        generateChecks();
    }

    public boolean isFinalSaleRound() { return saleRoundNumber >= TOTAL_ROUNDS; }

    public void advanceSaleRound() {
        saleRoundNumber++;
        generateChecks();
    }

    public boolean hasCardsToSell() {
        for (Player player : players) {
            if (!player.getAllCards().isEmpty()) return true;
        }
        return false;
    }

    public void processSaleRound(Property humanCard) {
        List<SaleChoice> choices = new ArrayList<>();
        lastSaleResults.clear();
        Player humanPlayer = players.get(0);
        if (humanCard != null && humanPlayer.getAllCards().contains(humanCard)) {
            humanPlayer.removeCard(humanCard);
            choices.add(new SaleChoice(humanPlayer, humanCard));
        }

        for (int i = 1; i < players.size(); i++) {
            Player player = players.get(i);
            Property card = chooseAiSaleCard(player);
            if (card != null) {
                player.removeCard(card);
                choices.add(new SaleChoice(player, card));
            }
        }

        choices.sort(Comparator.comparingInt(choice -> choice.card.getValue()));
        List<Integer> checks = new ArrayList<>(currentChecks);
        Collections.sort(checks);
        for (int i = 0; i < choices.size() && i < checks.size(); i++) {
            SaleChoice choice = choices.get(i);
            int check = checks.get(i);
            choice.player.addCheck(check);
            lastSaleResults.add(new SaleResult(choice.player.getName(), choice.card.getName(), choice.card.getValue(), check));
            activityLog.add(choice.player.getName() + " sells " + choice.card.getName() + " for $" + check);
        }
    }

    private Property chooseAiSaleCard(Player player) {
        List<Property> cards = player.getAllCards();
        if (cards.isEmpty()) return null;
        cards.sort(Comparator.comparingInt(Property::getValue));
        if (currentChecks.isEmpty()) return cards.get(0);

        List<Integer> checks = new ArrayList<>(currentChecks);
        Collections.sort(checks);
        int highestCheck = checks.get(checks.size() - 1);
        if (highestCheck >= 8000) return cards.get(cards.size() - 1);
        if (highestCheck >= 5000) return cards.get(cards.size() / 2);
        return cards.get(0);
    }

    private void generateChecks() {
        currentChecks.clear();
        List<Integer> values = new ArrayList<>();
        for (int value = 1000; value <= 10000; value += 1000) {
            values.add(value);
        }
        Collections.shuffle(values, random);
        for (int i = 0; i < 4; i++) {
            currentChecks.add(values.get(i));
        }
        Collections.sort(currentChecks);
    }

    private static class SaleChoice {
        private Player player;
        private Property card;

        SaleChoice(Player player, Property card) {
            this.player = player;
            this.card = card;
        }
    }

    public static class SaleResult {
        private String playerName;
        private String cardName;
        private int cardValue;
        private int checkAmount;

        SaleResult(String playerName, String cardName, int cardValue, int checkAmount) {
            this.playerName = playerName;
            this.cardName = cardName;
            this.cardValue = cardValue;
            this.checkAmount = checkAmount;
        }

        public String getPlayerName() { return playerName; }
        public String getCardName() { return cardName; }
        public int getCardValue() { return cardValue; }
        public int getCheckAmount() { return checkAmount; }
    }
}

/**
 * Setup screen for entering player names and AI selection.
 */
class SetupPanel extends JPanel {
    private JTextField[] playerNames;
    private JCheckBox[] isAiCheckboxes;
    private JComboBox<String>[] aiDifficultyBoxes;
    private BuySellGameGUI frame;
    private GameState gameState;

    public SetupPanel(BuySellGameGUI frame, GameState gameState) {
        this.frame = frame;
        this.gameState = gameState;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(new Color(240, 240, 240));

        // Title
        JLabel titleLabel = new JLabel("Buy & Sell - Player Setup");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        add(titleLabel, BorderLayout.NORTH);

        // Player setup panel
        JPanel setupPanel = new JPanel();
        setupPanel.setLayout(new GridLayout(4, 2, 10, 15));
        setupPanel.setBackground(new Color(240, 240, 240));

        playerNames = new JTextField[4];
        isAiCheckboxes = new JCheckBox[4];
        aiDifficultyBoxes = new JComboBox[4];
        String[] defaultNames = {"You", "Ada", "Boris", "Cleo"};

        for (int i = 0; i < 4; i++) {
            JLabel nameLabel = new JLabel("Player " + (i + 1) + " Name:");
            nameLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            playerNames[i] = new JTextField(defaultNames[i], 15);
            playerNames[i].setFont(new Font("Arial", Font.PLAIN, 14));

            JPanel playerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            playerPanel.setBackground(new Color(240, 240, 240));
            playerPanel.add(nameLabel);
            playerPanel.add(playerNames[i]);

            JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            checkPanel.setBackground(new Color(240, 240, 240));
            isAiCheckboxes[i] = new JCheckBox("AI Player", i > 0);
            isAiCheckboxes[i].setFont(new Font("Arial", Font.PLAIN, 14));
            isAiCheckboxes[i].setBackground(new Color(240, 240, 240));
            if (i == 0) isAiCheckboxes[i].setEnabled(false);
            checkPanel.add(isAiCheckboxes[i]);

            aiDifficultyBoxes[i] = new JComboBox<>(new String[]{"Easy", "Greedy"});
            aiDifficultyBoxes[i].setFont(new Font("Arial", Font.PLAIN, 14));
            aiDifficultyBoxes[i].setSelectedItem(i > 0 ? "Easy" : "Easy");
            aiDifficultyBoxes[i].setEnabled(i > 0);
            checkPanel.add(aiDifficultyBoxes[i]);

            final int playerIndex = i;
            isAiCheckboxes[i].addActionListener(e -> aiDifficultyBoxes[playerIndex].setEnabled(isAiCheckboxes[playerIndex].isSelected()));

            setupPanel.add(playerPanel);
            setupPanel.add(checkPanel);
        }

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(240, 240, 240));
        centerPanel.add(setupPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        // Begin button
        JButton beginButton = new JButton("BEGIN GAME");
        beginButton.setFont(new Font("Arial", Font.BOLD, 18));
        beginButton.setBackground(new Color(34, 139, 34));
        beginButton.setForeground(Color.BLACK);
        beginButton.setPreferredSize(new Dimension(200, 60));
        beginButton.setFocusPainted(false);
        beginButton.setBorder(new LineBorder(Color.BLACK, 2));
        beginButton.addActionListener(e -> startGame());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        buttonPanel.setBackground(new Color(240, 240, 240));
        buttonPanel.add(beginButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void startGame() {
        String[] names = new String[4];
        boolean[] isHuman = new boolean[4];
        String[] aiDifficulties = new String[4];

        for (int i = 0; i < 4; i++) {
            names[i] = playerNames[i].getText().trim();
            if (names[i].isEmpty()) names[i] = (i == 0 ? "You" : "Player " + (i + 1));
            isHuman[i] = !isAiCheckboxes[i].isSelected();
            aiDifficulties[i] = ((String) aiDifficultyBoxes[i].getSelectedItem()).toLowerCase();
        }

        gameState.initializePlayers(names, isHuman, aiDifficulties);
        frame.showAuctionScreen();
    }
}

/**
 * Main auction screen showing cards, balances, and bid controls.
 */
class AuctionPanel extends JPanel {
    private BuySellGameGUI frame;
    private GameState gameState;
    private JLabel roundLabel;
    private JLabel propertiesLabel;
    private JLabel highestBidLabel;
    private JLabel noticeLabel;
    private JPanel playersPanel;
    private JPanel propertiesDisplayPanel;
    private JPanel yourHandPanel;
    private JLabel yourPropertiesLabel;
    private JTextArea activityLogArea;
    private JSpinner bidInputSpinner;
    private JButton bidButton;
    private JButton passButton;
    private javax.swing.Timer aiTurnTimer;
    private javax.swing.Timer noticeTimer;
    private int aiTurnsRemaining;

    public AuctionPanel(BuySellGameGUI frame, GameState gameState) {
        this.frame = frame;
        this.gameState = gameState;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);

        // Top: Round info
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(new Color(220, 220, 220));
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        roundLabel = new JLabel();
        roundLabel.setFont(new Font("Arial", Font.BOLD, 22));
        topPanel.add(roundLabel, BorderLayout.WEST);

        propertiesLabel = new JLabel();
        propertiesLabel.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(propertiesLabel, BorderLayout.CENTER);

        highestBidLabel = new JLabel();
        highestBidLabel.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(highestBidLabel, BorderLayout.EAST);

        JPanel headerPanel = new JPanel(new BorderLayout(0, 6));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.add(topPanel, BorderLayout.NORTH);

        noticeLabel = new JLabel(" ");
        noticeLabel.setHorizontalAlignment(JLabel.CENTER);
        noticeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        noticeLabel.setForeground(Color.BLACK);
        noticeLabel.setOpaque(true);
        noticeLabel.setBackground(new Color(255, 225, 150));
        noticeLabel.setVisible(false);
        noticeLabel.setBorder(new EmptyBorder(8, 10, 8, 10));
        headerPanel.add(noticeLabel, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        // Middle: Players and cards
        JPanel middlePanel = new JPanel(new BorderLayout(10, 10));
        middlePanel.setBackground(Color.WHITE);

        // Cards display
        propertiesDisplayPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        propertiesDisplayPanel.setBackground(new Color(240, 240, 255));
        propertiesDisplayPanel.setBorder(new TitledBorder("Available Cards"));
        middlePanel.add(propertiesDisplayPanel, BorderLayout.NORTH);

        playersPanel = new JPanel();
        playersPanel.setLayout(new GridLayout(1, 4, 10, 0));
        playersPanel.setBackground(Color.WHITE);
        playersPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        middlePanel.add(playersPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setPreferredSize(new Dimension(250, 0));

        yourPropertiesLabel = new JLabel("Your cards:");
        yourPropertiesLabel.setFont(new Font("Arial", Font.BOLD, 14));
        rightPanel.add(yourPropertiesLabel, BorderLayout.NORTH);

        activityLogArea = new JTextArea();
        activityLogArea.setEditable(false);
        activityLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        activityLogArea.setLineWrap(true);
        activityLogArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(activityLogArea);
        scrollPane.setBorder(new LineBorder(Color.LIGHT_GRAY));
        rightPanel.add(scrollPane, BorderLayout.CENTER);

        middlePanel.add(rightPanel, BorderLayout.EAST);
        add(middlePanel, BorderLayout.CENTER);

        JPanel bottomContainer = new JPanel(new BorderLayout(0, 8));
        bottomContainer.setBackground(Color.WHITE);

        yourHandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        yourHandPanel.setBackground(new Color(248, 248, 248));
        yourHandPanel.setBorder(new TitledBorder("Your Hand"));
        JScrollPane handScrollPane = new JScrollPane(yourHandPanel);
        handScrollPane.setPreferredSize(new Dimension(0, 105));
        handScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        handScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        handScrollPane.setBorder(null);
        bottomContainer.add(handScrollPane, BorderLayout.NORTH);

        // Bid controls
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(new LineBorder(Color.LIGHT_GRAY, 2, true));

        JLabel bidLabel = new JLabel("Your bid ($1000 increments):");
        bidLabel.setFont(new Font("Arial", Font.BOLD, 14));
        bottomPanel.add(bidLabel);

        SpinnerNumberModel bidModel = new SpinnerNumberModel(1000, 1000, 18000, 1000);
        bidInputSpinner = new JSpinner(bidModel);
        bidInputSpinner.setFont(new Font("Arial", Font.PLAIN, 16));
        bidInputSpinner.setPreferredSize(new Dimension(120, 40));
        JSpinner.DefaultEditor bidEditor = (JSpinner.DefaultEditor) bidInputSpinner.getEditor();
        JFormattedTextField bidTextField = bidEditor.getTextField();
        bidTextField.setForeground(Color.BLACK);
        bidTextField.setFont(new Font("Arial", Font.PLAIN, 16));
        bottomPanel.add(bidInputSpinner);

        bidButton = new JButton("BID");
        bidButton.setFont(new Font("Arial", Font.BOLD, 16));
        bidButton.setBackground(new Color(0, 102, 204));
        bidButton.setForeground(Color.BLACK);
        bidButton.setPreferredSize(new Dimension(120, 50));
        bidButton.setFocusPainted(false);
        bidButton.setBorder(new LineBorder(Color.BLACK, 2));
        bidButton.addActionListener(e -> handleBid());
        bottomPanel.add(bidButton);

        passButton = new JButton("PASS");
        passButton.setFont(new Font("Arial", Font.BOLD, 16));
        passButton.setBackground(new Color(204, 0, 0));
        passButton.setForeground(Color.BLACK);
        passButton.setPreferredSize(new Dimension(120, 50));
        passButton.setFocusPainted(false);
        passButton.setBorder(new LineBorder(Color.BLACK, 2));
        passButton.addActionListener(e -> handlePass());
        bottomPanel.add(passButton);

        bottomContainer.add(bottomPanel, BorderLayout.SOUTH);
        add(bottomContainer, BorderLayout.SOUTH);
    }

    public void refresh() {
        RoundManager round = gameState.getCurrentRound();
        if (round == null) {
            frame.showResultsScreen();
            return;
        }

        // Update round and property info
        roundLabel.setText("Round " + gameState.getRoundNumber() + " of " + gameState.getTotalRounds());
        
        List<Property> props = round.getAvailableProperties();
        propertiesLabel.setText("Cards Available: " + props.size());
        
        // Update cards display panel
        propertiesDisplayPanel.removeAll();
        
        for (int i = 0; i < props.size(); i++) {
            Property p = props.get(i);
            JPanel propCard = new JPanel();
            propCard.setLayout(new BoxLayout(propCard, BoxLayout.Y_AXIS));
            propCard.setBackground(new Color(255, 200, 124));
            propCard.setBorder(new LineBorder(Color.BLACK, 3, false));
            propCard.setPreferredSize(new Dimension(100, 120));
            
            JLabel numLabel = new JLabel("Card");
            numLabel.setFont(new Font("Arial", Font.BOLD, 14));
            numLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            JLabel valueLabel = new JLabel(String.valueOf(p.getValue()));
            valueLabel.setFont(new Font("Arial", Font.BOLD, 32));
            valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            propCard.add(Box.createVerticalStrut(10));
            propCard.add(numLabel);
            propCard.add(Box.createVerticalStrut(5));
            propCard.add(valueLabel);
            propCard.add(Box.createVerticalStrut(10));
            
            propertiesDisplayPanel.add(propCard);
        }
        propertiesDisplayPanel.revalidate();
        propertiesDisplayPanel.repaint();

        int highestBid = round.getCurrentHighestBid();
        Player bidder = round.getCurrentHighestBidder();
        String bidStr = "Current bid: $" + highestBid;
        if (bidder != null) bidStr += " by " + bidder.getName();
        highestBidLabel.setText(bidStr);

        // Update player cards
        playersPanel.removeAll();
        for (Player p : gameState.getPlayers()) {
            playersPanel.add(createPlayerCard(p));
        }

        // Update your cards
        Player humanPlayer = gameState.getPlayers().get(0);
        StringBuilder yourProps = new StringBuilder("<html>Your cards:<br>");
        for (Property p : humanPlayer.getPurchasedProperties()) {
            yourProps.append(p.getName()).append("<br>");
        }
        for (Property p : humanPlayer.getPassedProperties()) {
            yourProps.append(p.getName()).append("<br>");
        }
        if (humanPlayer.getPurchasedProperties().isEmpty() && humanPlayer.getPassedProperties().isEmpty()) {
            yourProps.append("(none)<br>");
        }
        yourProps.append("</html>");
        yourPropertiesLabel.setText(yourProps.toString());
        refreshYourHand(humanPlayer);

        // Update activity log (last 15 entries)
        StringBuilder log = new StringBuilder();
        int start = Math.max(0, gameState.getActivityLog().size() - 15);
        for (int i = start; i < gameState.getActivityLog().size(); i++) {
            log.append("• ").append(gameState.getActivityLog().get(i)).append("\n");
        }
        activityLogArea.setText(log.toString());
        activityLogArea.setCaretPosition(activityLogArea.getDocument().getLength());

        int minBid = round.getCurrentHighestBid() + 1000;
        int maxBid = gameState.getPlayers().get(0).getBalance();
        if (maxBid < minBid) {
            minBid = Math.max(1000, maxBid);
        }
        bidInputSpinner.setModel(new SpinnerNumberModel(minBid, minBid, Math.max(minBid, maxBid), 1000));
        JSpinner.DefaultEditor bidEditor = (JSpinner.DefaultEditor) bidInputSpinner.getEditor();
        JFormattedTextField bidTextField = bidEditor.getTextField();
        bidTextField.setForeground(Color.BLACK);
        bidTextField.setFont(new Font("Arial", Font.PLAIN, 16));
        bidInputSpinner.requestFocus();
        playersPanel.revalidate();
        playersPanel.repaint();
    }

    private void refreshYourHand(Player humanPlayer) {
        yourHandPanel.removeAll();
        List<Property> handCards = new ArrayList<>();
        handCards.addAll(humanPlayer.getPurchasedProperties());
        handCards.addAll(humanPlayer.getPassedProperties());
        handCards.sort(Comparator.comparingInt(Property::getValue));

        if (handCards.isEmpty()) {
            JLabel emptyLabel = new JLabel("No cards yet");
            emptyLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            yourHandPanel.add(emptyLabel);
        } else {
            for (Property card : handCards) {
                yourHandPanel.add(createSmallCard(card));
            }
        }

        yourHandPanel.revalidate();
        yourHandPanel.repaint();
    }

    private JPanel createSmallCard(Property property) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(255, 234, 186));
        card.setBorder(new LineBorder(Color.BLACK, 2, false));
        card.setPreferredSize(new Dimension(58, 72));

        JLabel cardLabel = new JLabel("Card");
        cardLabel.setFont(new Font("Arial", Font.BOLD, 11));
        cardLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel valueLabel = new JLabel(String.valueOf(property.getValue()));
        valueLabel.setFont(new Font("Arial", Font.BOLD, 26));
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalStrut(6));
        card.add(cardLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLabel);
        return card;
    }

    private JPanel createPlayerCard(Player p) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new LineBorder(Color.BLACK, 2, true));
        card.setBackground(new Color(200, 220, 255));
        card.setPreferredSize(new Dimension(140, 160));

        JLabel nameLabel = new JLabel(p.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String type = p.isHuman() ? "HUMAN" : "AI " + p.getAiDifficulty().toUpperCase();
        JLabel typeLabel = new JLabel(type);
        typeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        typeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel balanceLabel = new JLabel("$" + p.getBalance());
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 13));
        balanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel bidLabel = new JLabel("Bid: $" + p.getCurrentBid());
        bidLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        bidLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        int totalCards = p.getPurchasedProperties().size() + p.getPassedProperties().size();
        JLabel cardsLabel = new JLabel("Cards: " + totalCards);
        cardsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        cardsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalStrut(8));
        card.add(nameLabel);
        card.add(typeLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(balanceLabel);
        card.add(bidLabel);
        card.add(cardsLabel);
        card.add(Box.createVerticalStrut(8));

        return card;
    }

    private void handleBid() {
        try {
            bidInputSpinner.commitEdit();
            int bidAmount = (Integer) bidInputSpinner.getValue();
            RoundManager round = gameState.getCurrentRound();
            
            // Validate bid
            int minBid = round.getCurrentHighestBid() + 1000;
            if (bidAmount < minBid) {
                JOptionPane.showMessageDialog(this, "Bid must be at least $" + minBid, "Invalid Bid", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (bidAmount % 1000 != 0) {
                JOptionPane.showMessageDialog(this, "Bid must be in $1000 increments", "Invalid Bid", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (bidAmount > gameState.getPlayers().get(0).getBalance()) {
                JOptionPane.showMessageDialog(this, "Insufficient funds", "Invalid Bid", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Place the bid via the round manager
            round.placeBidGui(gameState.getPlayers().get(0), bidAmount);
            finishHumanMove();
        } catch (NumberFormatException | java.text.ParseException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number", "Invalid Input", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void handlePass() {
        RoundManager round = gameState.getCurrentRound();
        round.passPlayerGui(gameState.getPlayers().get(0));
        finishHumanMove();
    }

    private void finishHumanMove() {
        RoundManager round = gameState.getCurrentRound();
        if (round.isRoundComplete()) {
            advanceAfterCompletedRound();
        } else {
            refresh();
            runAiTurnsOneAtATime();
        }
    }

    private void runAiTurnsOneAtATime() {
        setBidControlsEnabled(false);
        aiTurnsRemaining = gameState.getCurrentRound().getActiveAiPlayerCount();
        if (aiTurnTimer != null && aiTurnTimer.isRunning()) {
            aiTurnTimer.stop();
        }

        aiTurnTimer = new javax.swing.Timer(900, e -> {
            RoundManager round = gameState.getCurrentRound();
            boolean moved = round.processNextAiTurnGui(gameState);
            if (moved) {
                aiTurnsRemaining--;
            }
            refresh();
            Player humanPlayer = gameState.getPlayers().get(0);
            boolean humanStillActive = round.isPlayerActive(humanPlayer);

            if (round.isRoundComplete()) {
                aiTurnTimer.stop();
                advanceAfterCompletedRound();
            } else if (!moved) {
                aiTurnTimer.stop();
                setBidControlsEnabled(humanStillActive);
            } else if (humanStillActive && aiTurnsRemaining <= 0) {
                aiTurnTimer.stop();
                setBidControlsEnabled(true);
                checkAutomaticHumanPass();
            } else if (!humanStillActive && aiTurnsRemaining <= 0) {
                aiTurnsRemaining = round.getActiveAiPlayerCount();
            }
        });
        aiTurnTimer.setInitialDelay(900);
        aiTurnTimer.start();
    }

    private void advanceAfterCompletedRound() {
        gameState.startNextRound();
        if (gameState.hasMoreRounds()) {
            setBidControlsEnabled(true);
            refresh();
            checkAutomaticHumanPass();
        } else {
            gameState.startSellingPhase();
            frame.showSellScreen();
        }
    }

    private void setBidControlsEnabled(boolean enabled) {
        bidInputSpinner.setEnabled(enabled);
        bidButton.setEnabled(enabled);
        passButton.setEnabled(enabled);
    }

    private void checkAutomaticHumanPass() {
        RoundManager round = gameState.getCurrentRound();
        if (round == null || round.isRoundComplete()) return;

        Player humanPlayer = gameState.getPlayers().get(0);
        int minBid = round.getCurrentHighestBid() + 1000;
        boolean cannotAffordNextBid = humanPlayer.getBalance() < minBid;
        boolean alreadyHighestBidder = round.getCurrentHighestBidder() == humanPlayer;

        if (round.isPlayerActive(humanPlayer) && cannotAffordNextBid && !alreadyHighestBidder) {
            showNotice("Not enough money - automatic pass");
            setBidControlsEnabled(false);
            new javax.swing.Timer(1000, e -> {
                ((javax.swing.Timer) e.getSource()).stop();
                round.passPlayerGui(humanPlayer);
                finishHumanMove();
            }).start();
        }
    }

    private void showNotice(String message) {
        noticeLabel.setText(message);
        noticeLabel.setVisible(true);
        revalidate();
        repaint();

        if (noticeTimer != null && noticeTimer.isRunning()) {
            noticeTimer.stop();
        }
        noticeTimer = new javax.swing.Timer(1000, e -> {
            noticeLabel.setVisible(false);
            ((javax.swing.Timer) e.getSource()).stop();
            revalidate();
            repaint();
        });
        noticeTimer.setRepeats(false);
        noticeTimer.start();
    }
}

class SellPanel extends JPanel {
    private BuySellGameGUI frame;
    private GameState gameState;
    private JLabel titleLabel;
    private JLabel statusLabel;
    private JPanel checksPanel;
    private JPanel handPanel;
    private JPanel playersPanel;
    private JTextArea activityLogArea;
    private JButton sellButton;
    private ButtonGroup cardButtonGroup;
    private Property selectedCard;
    private boolean showingRoundResults;

    public SellPanel(BuySellGameGUI frame, GameState gameState) {
        this.frame = frame;
        this.gameState = gameState;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(new Color(220, 220, 220));
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        titleLabel = new JLabel("Phase 2 - Sell Cards");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        topPanel.add(titleLabel, BorderLayout.WEST);

        statusLabel = new JLabel();
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(statusLabel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(Color.WHITE);

        checksPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        checksPanel.setBackground(new Color(235, 250, 240));
        checksPanel.setBorder(new TitledBorder("Checks"));
        centerPanel.add(checksPanel, BorderLayout.NORTH);

        playersPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        playersPanel.setBackground(Color.WHITE);
        playersPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        centerPanel.add(playersPanel, BorderLayout.CENTER);

        activityLogArea = new JTextArea();
        activityLogArea.setEditable(false);
        activityLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        activityLogArea.setLineWrap(true);
        activityLogArea.setWrapStyleWord(true);
        JScrollPane logPane = new JScrollPane(activityLogArea);
        logPane.setPreferredSize(new Dimension(250, 0));
        centerPanel.add(logPane, BorderLayout.EAST);

        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(Color.WHITE);

        handPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        handPanel.setBackground(new Color(248, 248, 248));
        handPanel.setBorder(new TitledBorder("Choose One Card To Sell"));
        JScrollPane handScrollPane = new JScrollPane(handPanel);
        handScrollPane.setPreferredSize(new Dimension(0, 115));
        handScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        handScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        bottomPanel.add(handScrollPane, BorderLayout.CENTER);

        sellButton = new JButton("SELL CARD");
        sellButton.setFont(new Font("Arial", Font.BOLD, 16));
        sellButton.setForeground(Color.BLACK);
        sellButton.setBackground(new Color(34, 139, 34));
        sellButton.setPreferredSize(new Dimension(150, 55));
        sellButton.setFocusPainted(false);
        sellButton.setBorder(new LineBorder(Color.BLACK, 2));
        sellButton.addActionListener(e -> handleSellButton());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(sellButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void refresh() {
        selectedCard = null;
        showingRoundResults = false;
        titleLabel.setText("Phase 2 - Sell Cards");
        statusLabel.setText("Sale Round " + gameState.getSaleRoundNumber() + " of " + gameState.getTotalRounds());
        refreshChecks();
        refreshPlayers();
        refreshHand();
        refreshLog();
    }

    private void refreshChecks() {
        checksPanel.removeAll();
        for (int check : gameState.getCurrentChecks()) {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(new Color(190, 235, 200));
            card.setBorder(new LineBorder(Color.BLACK, 3, false));
            card.setPreferredSize(new Dimension(120, 80));

            JLabel label = new JLabel("Check");
            label.setFont(new Font("Arial", Font.BOLD, 13));
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel amount = new JLabel("$" + check);
            amount.setFont(new Font("Arial", Font.BOLD, 22));
            amount.setAlignmentX(Component.CENTER_ALIGNMENT);

            card.add(Box.createVerticalStrut(10));
            card.add(label);
            card.add(Box.createVerticalStrut(6));
            card.add(amount);
            checksPanel.add(card);
        }
        checksPanel.revalidate();
        checksPanel.repaint();
    }

    private void refreshPlayers() {
        playersPanel.removeAll();
        for (Player player : gameState.getPlayers()) {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(new Color(220, 235, 255));
            card.setBorder(new LineBorder(Color.BLACK, 2, true));
            card.setPreferredSize(new Dimension(140, 150));

            JLabel name = new JLabel(player.getName());
            name.setFont(new Font("Arial", Font.BOLD, 14));
            name.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel checks = new JLabel("Checks: $" + player.getCheckTotal());
            checks.setFont(new Font("Arial", Font.BOLD, 13));
            checks.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel cards = new JLabel("Cards left: " + player.getAllCards().size());
            cards.setFont(new Font("Arial", Font.PLAIN, 12));
            cards.setAlignmentX(Component.CENTER_ALIGNMENT);

            card.add(Box.createVerticalStrut(18));
            card.add(name);
            card.add(Box.createVerticalStrut(10));
            card.add(checks);
            card.add(cards);
            playersPanel.add(card);
        }
        playersPanel.revalidate();
        playersPanel.repaint();
    }

    private void refreshHand() {
        handPanel.removeAll();
        handPanel.setBorder(new TitledBorder("Choose One Card To Sell"));
        cardButtonGroup = new ButtonGroup();
        List<Property> cards = gameState.getPlayers().get(0).getAllCards();
        cards.sort(Comparator.comparingInt(Property::getValue));

        if (cards.isEmpty()) {
            JLabel emptyLabel = new JLabel("No cards left");
            emptyLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            handPanel.add(emptyLabel);
            sellButton.setText("SELL ROUND");
        } else {
            sellButton.setText("SELL CARD");
            for (Property card : cards) {
                JToggleButton button = new JToggleButton(String.valueOf(card.getValue()));
                button.setFont(new Font("Arial", Font.BOLD, 24));
                button.setForeground(Color.BLACK);
                button.setBackground(new Color(255, 234, 186));
                button.setPreferredSize(new Dimension(82, 88));
                button.setFocusPainted(false);
                button.setBorder(new LineBorder(Color.BLACK, 2));
                button.addActionListener(e -> {
                    selectedCard = card;
                    updateSelectedCardButtons();
                });
                cardButtonGroup.add(button);
                handPanel.add(button);
            }
        }

        handPanel.revalidate();
        handPanel.repaint();
    }

    private void updateSelectedCardButtons() {
        for (Component component : handPanel.getComponents()) {
            if (component instanceof JToggleButton) {
                JToggleButton button = (JToggleButton) component;
                if (button.isSelected()) {
                    button.setText("<html><center>Selected<br>" + button.getActionCommand() + "</center></html>");
                    button.setBackground(new Color(255, 210, 90));
                    button.setBorder(new LineBorder(new Color(0, 120, 215), 5));
                } else {
                    button.setText(button.getActionCommand());
                    button.setBackground(new Color(255, 234, 186));
                    button.setBorder(new LineBorder(Color.BLACK, 2));
                }
            }
        }
        handPanel.repaint();
    }

    private void refreshLog() {
        StringBuilder log = new StringBuilder();
        int start = Math.max(0, gameState.getActivityLog().size() - 15);
        for (int i = start; i < gameState.getActivityLog().size(); i++) {
            log.append("• ").append(gameState.getActivityLog().get(i)).append("\n");
        }
        activityLogArea.setText(log.toString());
        activityLogArea.setCaretPosition(activityLogArea.getDocument().getLength());
    }

    private void sellSelectedCard() {
        if (!gameState.getPlayers().get(0).getAllCards().isEmpty() && selectedCard == null) {
            JOptionPane.showMessageDialog(this, "Choose a card to sell first", "Choose Card", JOptionPane.WARNING_MESSAGE);
            return;
        }

        gameState.processSaleRound(selectedCard);
        showRoundResults();
    }

    private void handleSellButton() {
        if (showingRoundResults) {
            if (gameState.isFinalSaleRound()) {
                frame.showResultsScreen();
            } else {
                gameState.advanceSaleRound();
                refresh();
            }
        } else {
            sellSelectedCard();
        }
    }

    private void showRoundResults() {
        showingRoundResults = true;
        selectedCard = null;
        titleLabel.setText("Sale Round " + gameState.getSaleRoundNumber() + " Results");
        statusLabel.setText(gameState.isFinalSaleRound() ? "Final sale round" : "Round complete");

        refreshPlayers();
        refreshLog();
        handPanel.removeAll();
        handPanel.setBorder(new TitledBorder("Round Results"));

        List<GameState.SaleResult> results = gameState.getLastSaleResults();
        results.sort((a, b) -> Integer.compare(b.getCheckAmount(), a.getCheckAmount()));
        if (results.isEmpty()) {
            JLabel emptyLabel = new JLabel("No cards were sold this round");
            emptyLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            handPanel.add(emptyLabel);
        } else {
            GameState.SaleResult winner = results.get(0);
            JLabel winnerLabel = new JLabel(winner.getPlayerName() + " won this round with " + winner.getCardName() + " and got $" + winner.getCheckAmount());
            winnerLabel.setFont(new Font("Arial", Font.BOLD, 16));
            handPanel.add(winnerLabel);

            for (GameState.SaleResult result : results) {
                JLabel resultLabel = new JLabel(result.getPlayerName() + ": " + result.getCardName() + " -> $" + result.getCheckAmount());
                resultLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                resultLabel.setBorder(new EmptyBorder(0, 12, 0, 12));
                handPanel.add(resultLabel);
            }
        }

        sellButton.setText(gameState.isFinalSaleRound() ? "FINAL LEADERBOARD" : "NEXT ROUND");
        handPanel.revalidate();
        handPanel.repaint();
    }
}

/**
 * Final results screen showing rankings and final scores.
 */
class ResultsPanel extends JPanel {
    private BuySellGameGUI frame;
    private GameState gameState;
    private JTable resultsTable;
    private JLabel winnerLabel;

    public ResultsPanel(BuySellGameGUI frame, GameState gameState) {
        this.frame = frame;
        this.gameState = gameState;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(Color.WHITE);

        // Title
        JLabel titleLabel = new JLabel("Final Results");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        add(titleLabel, BorderLayout.NORTH);

        // Placeholder for table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        add(tablePanel, BorderLayout.CENTER);

        // Winner announcement
        winnerLabel = new JLabel();
        winnerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        winnerLabel.setHorizontalAlignment(JLabel.CENTER);
        add(winnerLabel, BorderLayout.SOUTH);
    }

    public void initializeResults() {
        // Clear and rebuild
        removeAll();
        
        // Title
        JLabel titleLabel = new JLabel("Final Results");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        add(titleLabel, BorderLayout.NORTH);

        // Results table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);

        String[] columns = {"Rank", "Player", "Check Money", "Cards Left"};
        Object[][] data = calculateResults();

        resultsTable = new JTable(data, columns);
        resultsTable.setFont(new Font("Arial", Font.PLAIN, 12));
        resultsTable.setRowHeight(25);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        add(tablePanel, BorderLayout.CENTER);

        // Winner announcement
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(Color.WHITE);
        if (data.length > 0) {
            String winner = (String) data[0][1];
            winnerLabel = new JLabel("Winner: " + winner);
            winnerLabel.setFont(new Font("Arial", Font.BOLD, 16));
            winnerLabel.setHorizontalAlignment(JLabel.CENTER);
            bottomPanel.add(winnerLabel, BorderLayout.CENTER);
        }

        JButton playAgainButton = new JButton("PLAY AGAIN");
        playAgainButton.setFont(new Font("Arial", Font.BOLD, 16));
        playAgainButton.setForeground(Color.BLACK);
        playAgainButton.setBackground(new Color(34, 139, 34));
        playAgainButton.setFocusPainted(false);
        playAgainButton.setBorder(new LineBorder(Color.BLACK, 2));
        playAgainButton.setPreferredSize(new Dimension(160, 50));
        playAgainButton.addActionListener(e -> frame.showSetupScreen());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(playAgainButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
        
        revalidate();
        repaint();
    }

    private Object[][] calculateResults() {
        List<Player> players = gameState.getPlayers();
        List<Object[]> results = new ArrayList<>();

        for (Player p : players) {
            results.add(new Object[]{0, p.getName(), p.getCheckTotal(), p.getAllCards().size()});
        }

        results.sort((a, b) -> Integer.compare((Integer) b[2], (Integer) a[2]));

        Object[][] data = new Object[results.size()][];
        for (int i = 0; i < results.size(); i++) {
            data[i] = results.get(i);
            data[i][0] = i + 1;
        }

        return data;
    }
}
