package Model;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.lang.Math;

public class Grid implements PropertyChangeListener {
    private int maximumNumberOfTurns;
    private ArrayList<Patch> patches;
    private ArrayList<ColoredTrailsPlayer> players;
    private ArrayList<Token> allTokensInPlay;
    private HashMap<ColoredTrailsPlayer, ArrayList<Token>> tokens;
    private HashMap<ColoredTrailsPlayer, ArrayList<ArrayList<Token>>> offers;
    private String wayOfAssigningGoals;

    private enum STATE {
        INACTIVE, ACTIVE, WAITING_FOR_OFFER, WAITING_FOR_COUNTER_OFFER
    }
    private STATE gameState;

    /* Private Methods */

    /**
     * @return an ArrayList<Color> with random values from the Color enum
     */
    private ArrayList<Color> generateRandomColorFiveByFive() {
        ArrayList<Color> colors = new ArrayList();
        Random random = new Random();
        int numberOfColors = (Color.values()).length;
        for(int i = 0; i < 25; i++) {
            colors.add(Color.values()[random.nextInt(numberOfColors)]);
        }
        return colors;
    }

    /**
     * @param numberOfTurns represents the number of turns the negotiation had
     * @return the player corresponding to the number of turns the negotiation had
     * modulo the size of the players list
     */
    private ColoredTrailsPlayer getPlayer(int numberOfTurns) {
        return players.get(numberOfTurns % players.size());
    }

    /**
     * Sets the gameState to the given parameter
     * @param gameState
     */
    private void setGameState(STATE gameState) {
        this.gameState = gameState;
    }

    /**
     * defines the allowed indices in the five by five grid
     * @return a patch with an allowed index
     */
    private Patch pickRandomGoalFiveByFive() {
        Random random = new Random();
        ArrayList<Integer> allowedIndices = new ArrayList<>(Arrays.asList(0, 2, 3, 4, 5, 9, 15, 19, 20, 21, 23, 24));
        Integer randomIndex = (Integer) random.nextInt(25);
        while( ! allowedIndices.contains( randomIndex ) ) {
            randomIndex = (Integer) random.nextInt(25);
        }
        return patches.get(randomIndex);
    }

    /**
     * if the players and patches have been created, assigns 4 tokens randomly to each player
     */
    private void distributeTokensToPlayers() {
        if(players != null && patches != null) {
            Random random = new Random();
            ArrayList<Patch> patchesCopy = (ArrayList<Patch>) patches.clone();
            Collections.shuffle(patchesCopy);
            for(Patch patchToBeReceived : patchesCopy) {
                int numberOfDistributedTokens = patchesCopy.indexOf(patchToBeReceived);
                if(numberOfDistributedTokens < 4 * players.size()) {
                    ColoredTrailsPlayer playerToReceiveToken = players.get(numberOfDistributedTokens % players.size());
                    Token tokenToBeReceived = new Token(patchToBeReceived.getColor());
                    tokens.get(playerToReceiveToken).add(tokenToBeReceived);
                    allTokensInPlay.add(tokenToBeReceived);
                } else {
                    return;
                }

            }
        }
    }

    /**
     * creates 25 patches with the colors from generateRandomColorFiveByFive()
     */
    private void generatePatchesFiveByFive() {
        patches = new ArrayList<>();
        ArrayList<Color> colors = generateRandomColorFiveByFive();
        for(int i = 0; i < 25; i++) {
            patches.add(new Patch(colors.get(i)));
            patches.get(i).setState(Patch.State.ACTIVE);
        }
    }

    /**
     * assigns DIFFERENT goals to each player
     */
    private void assignDifferentRandomGoalsToPlayers() {
        ArrayList<Patch> assignedPatches = new ArrayList();
        Patch goal = pickRandomGoalFiveByFive();
        for(ColoredTrailsPlayer player : players) {
            while(assignedPatches.contains(goal)) {
                goal = pickRandomGoalFiveByFive();
            }
            player.setGoal(goal);
            assignedPatches.add(goal);
        }
    }

    /**
     * assigns the same goal to each player
     */
    private void assignSameRandomGoalsToPlayers() {
        for(ColoredTrailsPlayer player : players) {
            player.setGoal(pickRandomGoalFiveByFive());
        }
    }

    /**
     * The method lets the playerToBeAnnounced that the goal of its partner is goalOfPartner
     * @param playerToBeAnnounced: The player which is announced
     * @param goalOfPartner: The goal of the partner
     */
    private void announceGoal(ColoredTrailsPlayer playerToBeAnnounced, Patch goalOfPartner) {
        playerToBeAnnounced.listenToGoal(goalOfPartner);
    }

    /**
     * The method sends the offer of the playerToOffer to playerToReceive
     * @param playerToOffer: The player which made the offer
     * @param playerToReceive: The player receiving the offer
     */
    private void makeOffer(ColoredTrailsPlayer playerToOffer, ColoredTrailsPlayer playerToReceive) {
        // Clone the offer of the playerToOffer
        ArrayList<ArrayList<Token>> offer = (ArrayList<ArrayList<Token>>) offers.get(playerToOffer).clone();
        // Reverse the offer, so that the playerToReceive receives it with its offered hand at index 0
        Collections.reverse(offer);
        playerToReceive.receiveOffer(offer);
    }


    /**
     * The method assumes that tokens must be preserved within negotiations, i.e. the tokens in play
     * remain the same.
     * An offer is legal if all the players get a hand and the tokens offered
     * were in play, i.e. the players do not introduce new tokens in the game, and the tokens are preserved.
     * @param offer the offer consisting of an ArrayList with an ArrayList with tokens for each player,
     *              representing their offered hand
     * @return true if the offer is legal, false otherwise
     */
    private boolean isOfferLegal(ArrayList<ArrayList<Token>> offer) {
        if(offer.size() != players.size()) {    //check that the players offered something for all players
            return false;
        }
        ArrayList<Token> allTokensInOffer = new ArrayList<>();
        for(ArrayList<Token> hand : offer) {
            allTokensInOffer.addAll(hand);
        }
        return allTokensInPlay.equals(allTokensInOffer) || allTokensInPlay.size() != allTokensInOffer.size();
    }

    /**
     * An offer is an ArrayList that contains two ArrayLists, each representing the collection
     * of tokens of each player. The first one is the proposed collection of the player offering,
     * second one is the one of the one being offered. First reverses the posterior offer
     * to ensure that the collections checked belong to the same player. Checks if the offers are the same.
     * @param priorOffer    The initial offer
     * @param posteriorOffer    The counter-offer
     * @return true if the offers are the same, false otherwise
     */
    private boolean acceptedOffer(ArrayList<ArrayList<Token>> priorOffer, ArrayList<ArrayList<Token>> posteriorOffer) {
        ArrayList<ArrayList<Token>> posteriorOfferReversed = (ArrayList<ArrayList<Token>>) posteriorOffer.clone();
        Collections.reverse(posteriorOfferReversed);
        for(int index = 0; index < priorOffer.size(); index++) {
            if( priorOffer.get(index).size() != posteriorOffer.get(index).size()) {
                return false;
            }
            for(int indexInHands = 0; indexInHands < priorOffer.get(index).size(); indexInHands++){
                if(priorOffer.get(index).get(indexInHands).getColor() != posteriorOffer.get(index).get(indexInHands).getColor()) {
                    return false;
                }
            }
        }
        return true;
    }

    /* Public Methods */


    /**
     * Public constructor:
     * sets the state of the grid to inactive and, by default,
     * the distributionOfPatches is set to random and different
     */
    public Grid() {
        this.gameState = STATE.INACTIVE;
        this.wayOfAssigningGoals = "randDif";
        this.maximumNumberOfTurns = 10;
    }

    /**
     * @param player: The player which the tokens belong to
     * @return The tokens of the player
     */
    public ArrayList<Token> getTokens(ColoredTrailsPlayer player) {
        return tokens.get(player);
    }

    /**
     * adds a player to the list of players and sets its grid to this
     * @param player The player to be added
     */
    public void addPlayer(ColoredTrailsPlayer player) {
        players.add(player);
        player.setGrid(this);
    }

    /**
     * assigns the goals according to the wayOfAssigningGoals
     */
    public void assignGoalsToPlayers() {
        switch (wayOfAssigningGoals) {
            case "randDif":
                assignDifferentRandomGoalsToPlayers();
                break;
            case "randSame":
                assignSameRandomGoalsToPlayers();
                break;
        }
    }

    /**
     * sets up the game by distributing the tokens to players and assigning goals to players
     */
    public void setUp() {
        distributeTokensToPlayers();
        assignGoalsToPlayers();
    }

    /**
     * The method updates the offer associated to the player, according to the parameter offer.
     * @param player: The player which makes the offer
     * @param offer: The offer of the player
     */
    public void setOffer(ColoredTrailsPlayer player, ArrayList<ArrayList<Token>> offer) {
        offers.put(player, offer);
    }

    /**
     * Starts the negotiations. Ends when both players sent the same offer or the number of turns has reached the
     * maximumNumberOfTurns, initially set to 10
     * @return true if the negotiations ended because of agreement, false if it reached the maximumNumberOfTurns
     */
    public boolean start() {
        gameState = STATE.ACTIVE;                   //Initialize the state of the game with ACTIVE
        int numberOfTurns = 0;                      //Initialize the number of turns with 0
        ArrayList<ArrayList<Token>> currentOffer = null;   //Initialize the initial offer with null
        ArrayList<ArrayList<Token>> nextOffer = null;   //Initialize the initial offer with null
        while (gameState != STATE.INACTIVE && numberOfTurns < maximumNumberOfTurns) {
            ColoredTrailsPlayer currentPlayer = getPlayer(numberOfTurns);
            setGameState(STATE.WAITING_FOR_OFFER);
            //There are two possibilities: I could pass the grid to the players and let them manage the state of the grid
            //and the flow of the game, or do it classically in this loop. The latter would be a lot more interesting
            currentPlayer.makeOffer();


            //currentOffer = currentPlayer.makeOffer(tokens.get(currentPlayer), tokens.get(nextPlayer));!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            //reverse the order of the hands in nextOffer, so that the order of the hands aligns with the one of players
            //nextOffer = nextPlayer.makeOffer(currentOffer.get(1), currentOffer.get(0));!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            if(!isOfferLegal(currentOffer)) {
                currentOffer = null;
            }
            if(!isOfferLegal(nextOffer)) {
                nextOffer = null;
            }
            if (currentOffer != null && nextOffer != null) {
                if(acceptedOffer(currentOffer, nextOffer)) {
                    gameState = STATE.INACTIVE;
                }
            }
            numberOfTurns++;
        }
        return numberOfTurns < maximumNumberOfTurns;
    }

    /**
     * initial score calculation, should be improved to represent not only the distance.
     * @param player
     * @return utility
     */
    public int calculateFinalScore(ColoredTrailsPlayer player) {        //make this useful
        int position = player.getPlayerPosition();
        int playerY = position % 5;
        int playerX = position / 5;
        int goalPosition = player.getGoal().getPatchPosition();
        int goalY = goalPosition % 5;
        int goalX = goalPosition / 5;
        return 4 - (Math.abs(playerX-goalX) + Math.abs(playerY-goalY));
    }

    /**
     * @return patches
     */
    public ArrayList<Patch> getPatches() {
        return patches;
    }           // This can be problematic when sending the grid, send a copy instead, or delete the getter

    /**
     * sets the wayOfAssigningGoals to the given string
     * @param wayOfAssigningGoals a string between "randDif" or "randSame"
     */
    public void setWayOfAssigningGoals(String wayOfAssigningGoals) {
        this.wayOfAssigningGoals = wayOfAssigningGoals;
    }

    /**
     * Sets maximumNumberOfTurns to the given value
     * @param maximumNumberOfTurns
     */
    public void setMaximumNumberOfTurns(int maximumNumberOfTurns) {
        this.maximumNumberOfTurns = maximumNumberOfTurns;
    }


    /**
     * The method is intended to be used by players to change the state of the game. The state can only be changed if
     * the game is active (Here, we should think of way to allow it without messing the game, maybe think of a hierarchy
     * if commands, for instance, revealing the goal should be prioritized compared to offering)
     * @param state
     */
    public void setGameStateByPlayer(STATE state) {
        if(gameState == STATE.ACTIVE) {
            gameState = state;
        }
    }



    @Override
    public void propertyChange(PropertyChangeEvent evt) {

    }

    public static void main(String[] args) {
        Grid grid = new Grid();
        grid.generatePatchesFiveByFive();
        for(Patch patch : grid.getPatches()) {
            System.out.println(patch.getColor());
        }
    }


}