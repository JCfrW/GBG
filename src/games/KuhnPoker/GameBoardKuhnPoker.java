package games.KuhnPoker;

import controllers.PlayAgent;
import games.Arena;
import games.GameBoard;
import games.StateObservation;
import tools.Types;

import java.util.ArrayList;
import java.util.Random;

public class GameBoardKuhnPoker implements GameBoard {

	protected Arena  m_Arena;		// a reference to the Arena object, needed to
									// infer the current taskState
	protected Random rand;
	//private transient GameBoardPokerGui m_gameGui = null;
	private transient GameBoardKuhnPokerGui m_gameGui = null;

	protected StateObserverKuhnPoker m_so;
	private boolean arenaActReq=false;

	private boolean waitAtEndOfRound = true;

	public GameBoardKuhnPoker(Arena pokerGame) {
		initGameBoard(pokerGame);
	}

	public GameBoardKuhnPoker() {
		initGameBoard(null);
	}
	
    @Override
    public void initialize() {}

    private void initGameBoard(Arena arGame) 
	{
		m_Arena		= arGame;
		m_so		= new StateObserverKuhnPoker();
        rand 		= new Random(System.currentTimeMillis());	
        if (m_Arena!=null&&m_Arena.hasGUI() && m_gameGui==null) {
			m_gameGui = new GameBoardKuhnPokerGui(this);
        }

	}

	/**
	 * update game-specific parameters from {@link Arena}'s param tabs
	 */
	@Override
	public void updateParams() {}

	@Override
	public void clearBoard(boolean boardClear, boolean vClear) {
		if (boardClear) {
			m_so = new StateObserverKuhnPoker();
			if (m_Arena!=null&&m_Arena.hasGUI() && m_gameGui!=null) {
				m_gameGui.resetLog();
			}
		}
	}

	/**
	 * Update the play board and the associated values (labels).
	 * 
	 * @param so	the game state
	 * @param withReset  if true, reset the board prior to updating it to state so
	 * @param showValueOnGameboard	if true, show the game values for the available actions
	 * 				(only if they are stored in state {@code so}).
	 */
	@Override
	public void updateBoard(StateObservation so, 
							boolean withReset, boolean showValueOnGameboard) {
		if(so!=null) {
			StateObserverKuhnPoker soT = (StateObserverKuhnPoker) so;
			this.m_so = soT;
			if (m_gameGui != null)
				m_gameGui.updateBoard(soT, withReset, showValueOnGameboard);

		}
	}

	/**
	 * @return  true: if an action is requested from Arena or ArenaTrain
	 * 			false: no action requested from Arena, next action has to come 
	 * 			from GameBoard (e.g. user input / human move) 
	 */
	@Override
	public boolean isActionReq() {
		return arenaActReq;
	}

	/**
	 * @param	actReq true : GameBoard requests an action from Arena 
	 * 			(see {@link #isActionReq()})
	 */
	@Override
	public void setActionReq(boolean actReq) {
		arenaActReq=actReq;
	}


	protected void inspectMove(int x){
		Types.ACTIONS act = Types.ACTIONS.fromInt(x);
		assert m_so.isLegalAction(act) : "Desired action is not legal";
		m_so.advance(act);
		arenaActReq = true;
	}

	// Human Game Move
	protected void HGameMove(int x)
	{
		Types.ACTIONS act = Types.ACTIONS.fromInt(x);
		assert m_so.isLegalAction(act) : "Desired action is not legal";
		//m_Arena.roundOverWait = true;
		m_so.advance(act);
		//m_Arena.roundOverWait = m_so.isRoundOver();

		if(m_Arena!=null)
			(m_Arena.getLogManager()).addLogEntry(act, m_so, m_Arena.getLogSessionID());

		//arenaActReq = !m_so.isRoundOver();
		arenaActReq = true;
		//if(m_so.isRoundOver()){
		//	updateBoard(m_so,false,false);
		//}
	}


	public StateObservation getStateObs() {
		return m_so;
	}

	/**
	 * @return the 'empty-board' start state
	 */
	@Override
	public StateObservation getDefaultStartState() {
		clearBoard(true, true);
		return m_so;
	}

	/**
	 * @return a start state which is with probability 0.5 the default start state 
	 * 		start state and with probability 0.5 one of the possible one-ply 
	 * 		successors
	 */
	@Override
	public StateObservation chooseStartState() {
		getDefaultStartState();			// m_so is in default start state 
		if (rand.nextDouble()>0.5) {
			// choose randomly one of the possible actions in default 
			// start state and advance m_so by one ply
			ArrayList<Types.ACTIONS> acts = m_so.getAvailableActions();
			int i = rand.nextInt(acts.size());
			m_so.advance(acts.get(i));
		}
		return m_so;
	}

	@Override
    public StateObservation chooseStartState(PlayAgent pa) {
    	return chooseStartState();
    }

	@Override
	public String getSubDir() {
		return null;
	}
	
    @Override
    public Arena getArena() {
        return m_Arena;
    }
    
	@Override
	public void enableInteraction(boolean enable) {
		if (m_gameGui!=null)
			m_gameGui.enableInteraction(enable);
	}

	@Override
	public void showGameBoard(Arena pokerGame, boolean alignToMain) {
		if (m_gameGui!=null)
			m_gameGui.showGameBoard(pokerGame, alignToMain);
	}

	@Override
	public void toFront() {
		if (m_gameGui!=null)
			m_gameGui.toFront();
	}

	@Override
	public void destroy() {
		if (m_gameGui!=null)
			m_gameGui.destroy();
	}

	/**
	 * Pass through the number of players in the game
	 * @return number of players
	 */
	public int getNumPlayers(){
		return m_so.getNumPlayers();
	}

	public boolean getWaitAtEndOfRound(){
		return waitAtEndOfRound;
	}
}
