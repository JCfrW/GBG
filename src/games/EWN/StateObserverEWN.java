package games.EWN;

import games.EWN.StateObserverHelper.Helper;
import games.EWN.StateObserverHelper.Player;
import games.EWN.StateObserverHelper.Token;
import games.EWN.config.ConfigEWN;
import games.EWN.config.StartingPositions;
import games.ObsNondetBase;
import games.StateObsNondeterministic;
import tools.ScoreTuple;
import tools.Types;
import tools.Types.ACTIONS;
import controllers.ExpectimaxNAgent;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static tools.Types.WINNER.*;

public class StateObserverEWN extends ObsNondetBase implements  StateObsNondeterministic {

    public static final long serialVersionUID = 12L;
    private static final double REWARD_NEGATIVE = -1, REWARD_POSITIVE = 1;
    private static Random random = new SecureRandom(); // using secure random and not reinitializing the random that often, should not cause this bug.
    private int numPlayers;
    public int count;
    private int player;
    private int size;
    public int turn = 0 ;
    private boolean isNextActionDeterministic;
    private ACTIONS nextNondeterministicAction;
    private Token[][] gameState;
    private ACTIONS rolledDice;
    private ArrayList<Player> players;
    private ArrayList<ACTIONS> availableActions;
    private ArrayList<ACTIONS> availableRandomActions;
    private int playerWin;      // Representing the winning player for faster evaluation
    private int playerLooses;   // Representing the loosing player for faster evaluation
    private ScoreTuple m_scoreTuple;


    public StateObserverEWN(){
        super();
        this.player =  0;
        this.rolledDice = null;
        this.numPlayers = ConfigEWN.NUM_PLAYERS;
        this.size = ConfigEWN.BOARD_SIZE;
        m_scoreTuple = new ScoreTuple(numPlayers);
        playerWin = -1;
        count = 0;
        playerLooses = -1;
        this.nextNondeterministicAction=null;
        this.isNextActionDeterministic=false;
        this.gameState = new Token[size][size];
        this.players = new ArrayList<>();
        this.availableActions = new ArrayList<>();
        this.availableRandomActions = new ArrayList<>();
        for(int i = 0; i < getRandomActionSize(size,numPlayers); i++){
            availableRandomActions.add( new ACTIONS(i));
        }
        for(int i = 0; i < numPlayers; i++){
            players.add(new Player(i,size));
        }
        if(ConfigEWN.RANDOM_POSITION){
            getRandomStartingPosition();
        }else {
            getFixedStartingPosition();
        }
        // Init empty board:
        advanceNondeterministic(); // roll the dice and setAvailableActions when we start a new state
    }



    public StateObserverEWN(StateObserverEWN other){
        super(other);		// copy members m_counter, lastMoves and stored*
                            // /WK/ ! was missing before 2021-09-10
        this.player = other.getPlayer();
        this.numPlayers = other.getNumPlayers();
        this.size = other.getSize();
        count = other.count;
        this.players = new ArrayList<>();
        this.nextNondeterministicAction = other.getNextNondeterministicAction();
        this.isNextActionDeterministic = other.isNextActionDeterministic();
        this.playerLooses = other.getPlayerLooses();
        this.playerWin = other.getPlayerWin();
        this.m_scoreTuple = new ScoreTuple(other.m_scoreTuple);
        this.availableActions = new ArrayList<ACTIONS>();
        this.availableRandomActions = other.getAvailableRandoms();
        this.gameState = new Token[size][size];
        this.turn = other.turn;
        this.rolledDice = other.rolledDice;
        // init players
        for(int i = 0; i < numPlayers; i++){
            this.players.add(new Player(i,size));
        }
        // init gamestate
        Token[][] gs = other.getGameState();
        for(int i = 0;  i <gs.length; i++){
            for(int k = 0; k < gs[i].length; k++){
                Token t = gs[i][k].copy();
                this.gameState[i][k] = t;
                if(t.getPlayer() >= 0){
                    players.get(t.getPlayer()).addToken(t);
                }
            }
        }
        if(this.availableActions == null) throw new RuntimeException("availableActions cannot be null");
        setAvailableActions();
    }

    private void getFixedStartingPosition(){
        int[][][] startingPosition = getStartingsPosition();
        for(int i = 0;i < startingPosition.length; i++){
            for(int k = 0; k < startingPosition[i].length; k++){
                int[] startPosEntry = startingPosition[i][k];
                Token token = new Token(i,k,startPosEntry[0],startPosEntry[1]);
                gameState[i][k] = token;
                if(startPosEntry[1] >= 0){
                    players.get(startPosEntry[1]).addToken(token);
                }
            }
        }
    }

    public void getRandomStartingPosition(){
        // Init empty field
        for(int x = 0; x < ConfigEWN.BOARD_SIZE; x++)
            for(int y = 0; y < ConfigEWN.BOARD_SIZE; y++)
                gameState[x][y] = new Token(x,y,-1,-1);

        //init player tokens and mirror for the opponent
        int[] values = getValues();
        int[] randomPositionIndex = getRandomPositionindices();
        for(int player = 0; player < ConfigEWN.NUM_PLAYERS; player++){
            for(int index = 0; index < values.length;index++){
                int position = rotate(randomPositionIndex[index],player);
                int j = position % ConfigEWN.BOARD_SIZE;
                int i = (position-j)/ConfigEWN.BOARD_SIZE;
                Token t = new Token(i,j,values[index],player);
                players.get(player).addToken(t);
                gameState[i][j]=t;
            }
        }
    }

    /**
     *           2nd player
     *  0 1 2    8 7 6
     *  3 4 5 => 5 4 3
     *  6 7 8    2 1 0
     *
     * @param index
     * @param player
     * @return
     */
    private int rotate(int index, int player){
        int n = ConfigEWN.BOARD_SIZE;
        int j = index % n;
        int i = (index-j) / n ;
        switch(player){
            case 0: return index;
            case 1: return (n*n-1) - ((i * n)+j);
            case 2: return (n-1-j)*n +i; // 0 => 30, 1 => 31
            case 3: return j*n+(n-1-i);
            default:throw new RuntimeException("Swap not implemented");
        }
    }


    private int[] getRandomPositionindices(){
        int[] randomIndices = getNormalPositionIndices();
        return shuffle(randomIndices);
    }

    private int[] shuffle(int[] array){
        int index;
        for(int i = array.length-1; i > 0; i--){
            //index = random.nextInt(i+1);
            index = ThreadLocalRandom.current().nextInt(i+1);
            if(index != i){
                array[index] ^= array[i];
                array[i] ^= array[index];
                array[index] ^= array[i];
            }
        }
        return array;
    }

    /**
     * @return the indices - in ascending order - that player 0's pieces occupy in the starting position
     */
    private int[] getNormalPositionIndices(){
        switch (ConfigEWN.BOARD_SIZE){
            case 3:return new int[]{0,1,3};
            case 4:return new int[]{0,1,4};
            case 5:return new int[]{0,1,2,5,6,10};
            case 6:return new int[]{0,1,2,6,7,12};
            default: throw new RuntimeException("Illegal board size");
        }
    }

    private int[] getValues(){
       if(ConfigEWN.BOARD_SIZE > 4) return StartingPositions.TOKENS_SIX;
       else return StartingPositions.TOKENS_THREE;
    }

    /**
     * Splitting the advancing step to deterministic and non-deterministic
     * @param action the action
     */
    @Override
    public void advance(ACTIONS action) {
        super.advanceBase(action);		//		includes addToLastMoves(action)
        if(isNextActionDeterministic) {
            if(action != null) {
                count = 0;
                advanceDeterministic(action);
            }
            else isNextActionDeterministic = false;
        }
        if(!isNextActionDeterministic) advanceNondeterministic();
        super.incrementMoveCounter();   // increment m_counter
    }

    /**
     * Advancing step, the deterministic part
     * <p>
     * For example, if we get action 706:
     * <ol>
     *     <li>Convert the action 706 to indices [from,to] =&gt; [7,6] {@link Helper}</li>
     *     <li>Split the indices of the array to their from and to coordinates</li>
     *     <li>if to_token is a players token remove it</li>
     *     <li>Changing the gameState</li>
     *     <li>Update Players token</li>
     * </ol>

     * @param action    the action
     */
    public void advanceDeterministic(ACTIONS action){
        if(!(action.toInt() == -1)){
            int[] convertedAction = Helper.getIntsFromAction(action); // [from, to] int array
            int to_x = convertedAction[1] % size;
            int to_y = (convertedAction[1] - to_x) / size;
            int from_x = convertedAction[0] % size;
            int from_y = (convertedAction[0] - from_x) / size;
            Token from_token = gameState[from_y][from_x];
            Token to_token = gameState[to_y][to_x];
            // check if the token is owned by a player or is empty
            if (to_token.getPlayer() >= 0) {
                players.get(to_token.getPlayer()).removeToken(to_token);
            }
            gameState[to_y][to_x] = from_token; // Setting token to new index;
            players.get(player).updateToken(from_token, to_token);
            gameState[from_y][from_x] = new Token(from_y, from_x, -1, -1); // Remove old token
        }
        turn++;
        boolean isOver = this.isGameOver();
        if(!isOver){
            this.player = (this.player + 1) % numPlayers; //next player
            availableActions.clear();
        }
        isNextActionDeterministic = false;      // /WK/ this was missing, I think
    }

    @Override
    public ACTIONS advanceNondeterministic(){
        if(isNextActionDeterministic){
            throw new RuntimeException("ACTION IS DETERMINISTIC must be NON");
        }
        //int actIndex = random.nextInt(availableRandomActions.size());
        int actIndex = ThreadLocalRandom.current().nextInt(availableRandomActions.size());
        advanceNondeterministic(availableRandomActions.get(actIndex));  // this sets also isNextActionDeterministic
        if(this.availableActions.size() == 0){
            this.availableActions.add(new ACTIONS(-1)); // Add empty action
        }
        return nextNondeterministicAction;
    }



    @Override
    public ACTIONS advanceNondeterministic(ACTIONS action) {
        nextNondeterministicAction = action;
        this.setAvailableActions();
        this.isNextActionDeterministic = true;

        return action;

    }




    private int getRandomActionSize(int size, int player){
        if(player == 2) return size == 5 ? 6 : 3;
        else if(player == 3) return 6;
        return size == 6 ? 6:3;
    }

    private int[][][] getStartingsPosition(){
        if(numPlayers == 2) {
            return ConfigEWN.BOARD_SIZE == 3 ? StartingPositions.S3P2 : ConfigEWN.BOARD_SIZE == 4 ? StartingPositions.S4P2 : StartingPositions.S5P2;
        }
        else if(numPlayers == 3) return StartingPositions.S6P3;
        else{
             return ConfigEWN.BOARD_SIZE == 6 ? StartingPositions.S6P4 : StartingPositions.S4P4;
        }
    }


    /**
     * Returns a new Object, with the same params as this one.
     * @return new StateObserverEWS();
     */
    public StateObserverEWN reset(){
        return new StateObserverEWN();
    }



    @Override
    public ArrayList<ACTIONS> getAllAvailableActions() {
        ArrayList<ACTIONS> ar = new ArrayList<>();
        for(int i = 0; i <size; i++){
            for(int k = 0; k < size; k++){
                ar.add(Helper.parseAction(i,k));
                ar.add(Helper.parseAction(k,i));
            }
            ar.add(new ACTIONS(i));
        }
        return ar;
    }

    @Override
    public void setPartialState(boolean p) {
        super.setPartialState(p);
    }

    @Override
    public ArrayList<ACTIONS> getAvailableActions() {
        return this.availableActions;
    }

    @Override
    public int getNumAvailableActions() {
        return this.availableActions.size();
    }

    /**
     * Helper for test routines
     * @param ranAct
     */
    public void setNextActionDeterministic(ACTIONS ranAct) {
        nextNondeterministicAction = ranAct;
        setAvailableActions();
    }

    @Override
    public void setAvailableActions() {
        Player p = players.get(player);
        p.setAvailableActions(nextNondeterministicAction.toInt());
        this.availableActions = p.getAvailableActions();
    }



    @Override
    public ACTIONS getAction(int i) {
        return this.availableActions.get(i);
    }



    @Override
    public int getPlayer() {
        return this.player;
    }

    @Override
    public int getNumPlayers() {
        return numPlayers;
    }

    private Types.WINNER getWinner2(int player) {
        if(playerWin >= 0) return playerWin == player ? PLAYER_WINS : PLAYER_LOSES;
        return playerLooses == player ? PLAYER_LOSES : PLAYER_WINS;

    }

    private Types.WINNER getWinner3(int player) {
            // Can only be player 2
            if(playerWin >= 0) {
                return player == 2 ? PLAYER_WINS : PLAYER_LOSES;
            }
            if(playerLooses >= 0){
                // if playerLooses == 2
                if(playerLooses == 2){
                    return player == 2 ? PLAYER_LOSES : PLAYER_WINS;
                }
                return player == 2 ? PLAYER_WINS : PLAYER_LOSES;
            }
            throw new RuntimeException("There must be a winner");
    }


    private Types.WINNER getWinner4(int player) {
        if(playerWin >= 0){
            // Check if the player is the winner
            if((playerWin == 0 || playerWin == 2) && (player == 0 || player == 2)){
                return PLAYER_WINS;
            }
            if((playerWin == 1 || playerWin == 3) && (player == 1 || player == 3))
            {
                return PLAYER_WINS;
            }
            return PLAYER_LOSES;
        }
        else if(playerLooses >= 0){

            if((playerLooses == 0 || playerLooses == 2) && (player == 0 || player == 2)){
                return PLAYER_LOSES;
            }
            if((playerLooses == 1 || playerLooses == 3) && (player == 1 || player == 3))
            {
                return PLAYER_LOSES;
            }
            return PLAYER_WINS;
        }
        throw new RuntimeException("There must be a winner");
    }

    private Types.WINNER getWinner(int player){
        switch (ConfigEWN.NUM_PLAYERS){
            case 2: return getWinner2(player);
            case 3: return getWinner3(player);
            case 4: return getWinner4(player);
            default: throw new RuntimeException("Number of players: "+player + " no allowed");
        }
    }


    @Override
    public ScoreTuple getGameScoreTuple() {
        for(int i = 0; i < ConfigEWN.NUM_PLAYERS; i++){
            m_scoreTuple.scTup[i] = getGameScore(i);
        }
        return m_scoreTuple;
    }

    @Override
    public double getGameScore(int player) {
        if(!isGameOver()) return 0.0;
        Types.WINNER winState = getWinner(player);
        switch (winState){
            case PLAYER_LOSES:
                return REWARD_NEGATIVE;
            case PLAYER_WINS:
                return REWARD_POSITIVE;
            case TIE:  throw new RuntimeException("invalid outcome of the game [wrong getGameScore]");
            }
        return 0.0;
    }


    @Override
    public double getReward(int player, boolean rewardIsGameScore) {
        // Check for the player
        return getGameScore(player);
    }

    @Override
    public double getMinGameScore() {
        return -1;
    }

    @Override
    public double getMaxGameScore() {
        return +1;
    }

    // --- obsolete, use Arena().getGameName() instead
//    public String getName() {return "EWN";}

    @Override
    public String stringDescr() {
        String str = "\n";
        for(int i = 0; i < gameState.length; i++){
            for(int k = 0; k < gameState[i].length; k++){
                Token t = gameState[i][k];
                int p = t.getPlayer();
                str += p== 0 ? "[X": p== 1 ? "[O":p==2 ? "[*" : p==3 ? "[#": "[ ";
                // note that the t.getValue() of non-empty fields is one smaller than the piece value displayed in GameBoard
                String val = String.valueOf(t.getValue());
                str += (t.getValue()>-1  ? val +"]" : " ]") + " ";
            }
            if (i==0) {
                // note that diceVal is one smaller than the "Dice: " displayed in GameBoard
                DecimalFormat frmAct = new DecimalFormat("0000");
                str += "    (diceVal:"+this.getNextNondeterministicAction().toInt()+",   ";
                str += "availActions:  ";
                for (ACTIONS act : this.getAvailableActions())
                    str += frmAct.format(act.toInt()) + " ";
                str += ")";
            }
            str += "\n";
        }
        str += "\n";
        return str;
    }

    /**
     * Method {@link #stringDescr()} is not unique since it omits the player to move. AND the 'availActions' string can
     * lead to different strings for the same state (depending on whether {@link #getAvailableActions()} returns
     * {@code null} or not). With {@code uniqueStringDescr()} we return a unique string for each state.
     *
     * @return a unique string description of the state
     *
     * @see ExpectimaxNAgent
     */
    @Override
    public String uniqueStringDescr() {
        String str = "\n";
        for(int i = 0; i < gameState.length; i++){
            for(int k = 0; k < gameState[i].length; k++){
                Token t = gameState[i][k];
                int p = t.getPlayer();
                str += p== 0 ? "[X": p== 1 ? "[O":p==2 ? "[*" : p==3 ? "[#": "[ ";
                // note that the t.getValue() of non-empty fields is one smaller than the piece value displayed in GameBoard
                String val = String.valueOf(t.getValue());
                str += (t.getValue()>-1  ? val +"]" : " ]") + " ";
            }
            if (i==0) {
                // note that diceVal is one smaller than the "Dice: " displayed in GameBoard
                DecimalFormat frmAct = new DecimalFormat("0000");
                str += "    (diceVal:"+this.getNextNondeterministicAction().toInt()+",   ";
                str += "player:  " +this.getPlayer();
                str += ")";
            }
            str += "\n";
        }
        str += "\n";
        return str;
    }


    @Override
    public ACTIONS getNextNondeterministicAction() {
        return nextNondeterministicAction;
    }

    @Override
    public boolean isNextActionDeterministic(){
        return this.isNextActionDeterministic;
    }

    @Override
    public ArrayList<ACTIONS> getAvailableRandoms() {
        return availableRandomActions;
    }

    @Override
    public int getNumAvailableRandoms() {
        return availableRandomActions.size();
    }

    @Override
    public double getProbability(ACTIONS action) {
        return 1.0/availableRandomActions.size();
    }

    @Override
    public void setPlayer(int p ){
        this.player = p;
    }

    @Override
    public StateObsNondeterministic copy() {
        return new StateObserverEWN(this);
    }



    /**
     * Game Over conditions
     *
     * 2 or 4 Player:
     *  One player does not have any tokens left.
     *  One Token reaches the opposite corner
     *
     * 3 Player:
     *  One player does not have any tokens left
     *  The 2-player team needs to capture all tokens of the single-team player
     *
     *
     * @return
     */
    @Override
    public boolean isGameOver() {
       switch(numPlayers){
           case 2:  return gameOverTwoPlayer();
           case 3: return gameOverThreePlayer();
           case 4: return gameOverFourPlayer();
           default: throw new RuntimeException("numPlayer "+ numPlayers+ " is not not implemented yet");
       }
    }



    private boolean gameOverFourPlayer(){
            int[] winningPositions = {size*size-1, 0,size-1,(size*size)-size};
            //Check for winning position
            for(Player p : players){
                int winPosition = winningPositions[p.getPlayer()]; // get the corresponding item of the array
                for(Token t: p.getTokens()){
                    if(t.getIndex() == winPosition){
                        playerWin = p.getPlayer();
                        return true;
                    }
                }
                if(p.getTokens().size() == 0)
                {
                    playerLooses = p.getPlayer();
                    return true;
                }
            }

            return false;
        }

    private boolean gameOverThreePlayer(){
        int winPosition = ConfigEWN.BOARD_SIZE-1;
        for (Token t : players.get(2).getTokens()) {
            if (t.getIndex() == winPosition) {
                playerWin = 2;
                return true;
            }
        }
        for(Player p : players) {
            if (p.getTokens().size() == 0) {
                playerLooses = p.getPlayer();
                return true;
            }
        }
        return false;
    }

    private boolean gameOverTwoPlayer(){
        int[] winningPositions = {size*size-1,0};
        for (Player p : players) {
            for (Token t : p.getTokens()) {
                if (t.getIndex() == winningPositions[t.getPlayer()]) {
                    playerWin = p.getPlayer();
                    return true;
                }
            }
            if (p.getTokens().size() == 0) {
                playerLooses = p.getPlayer(); // setting player for getWinner()
                return true;
            }

        }
        return false;
    }

    @Override
    public boolean isDeterministicGame() {
        return false;
    }

    @Override
    public boolean isFinalRewardGame() {
        return true;
    }

    public boolean isLegalAction(ACTIONS act){
        return availableActions.contains(act);
    }
    @Override
    public boolean isLegalState() {
        return true;
    }

    public int getSize(){
        return size;
    }

    public Token[][] getGameState(){
        return gameState;
    }

    public ArrayList<Player> getPlayers(){
        return players;
    }

    public int getPlayerWin(){
        return playerWin;
    }

    public int getPlayerLooses(){
        return playerLooses;
    }

    public ScoreTuple getM_scoreTuple(){
        return this.m_scoreTuple;
    }
}