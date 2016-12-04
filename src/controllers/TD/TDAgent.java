package controllers.TD;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import params.TDParams;
import tools.Types;
import controllers.TD.TD_Lin;
import controllers.TD.TD_NNet;
import controllers.TD.TD_func;
import controllers.AgentBase;
import controllers.PlayAgent;
import controllers.PlayAgent.AgentState;
import games.Feature;
import games.StateObservation;
import games.TicTacToe.StateObserverTTT;
import games.TicTacToe.TDPlayerTTT;

/**
 * The TD-Learning {@link PlayAgent} for TicTacToe. It has either a linear net
 * {@link TD_Lin} or a BP neural net {@link TD_NNet} to model the value
 * function. The net follows closely the (pseudo-)code by [SuttonBonde93]. It
 * can be trained with different feature sets:
 * <ul>
 * <li>0: Levkovich's features
 * <li>1,2: thin, thick feature set (6 or 10 features)
 * <li>3: thick feature set + board position (19 features)
 * <li>4: extended = thick + extra features (13 features)
 * <li>9: raw = only board position (9 features)
 * </ul>
 * The internal learning rate ALPHA for the net input layer weights is alpha/n,
 * where n=(size of feature vector) and alpha is the constructors' first
 * parameter.
 * 
 * Some functionality is packed in the superclass {@link TicTDBase} and *its*
 * superclass {@link AgentBase} (gameNum, maxGameNum, AgentState)
 * 
 * @see PlayAgent
 * @see TicTDBase
 * @see AgentBase
 * 
 * @author Wolfgang Konen, TH K�ln, Nov'16
 */
abstract public class TDAgent extends AgentBase implements PlayAgent,Serializable {
	protected TD_func m_Net;
	private double m_epsilon = 0.1;
	private double m_EpsilonChangeDelta = 0.001;
	// size of feature input vector for each featmode
	// (featmode def'd in TicTDBase. If featmode==8, use
	// TicTDBase.getInputSize8())
	private int inpSize[] = { 6, 6, 10, 19, 13, 19, 0, 0, 0, 9 };
	protected int hiddenSize = 15; // size of hidden layer (only for TD_NNet)
	private Random rand;
//	private int[][] m_trainTable = null;
//	private double[][] m_deltaTable = null;
	private int numFinishedGames = 0;
	private boolean randomSelect = false;
	private boolean m_hasLinearNet;
	private boolean m_hasSigmoid;
	protected Feature m_feature;
	
	/**
	 * Member {@link #m_tdPar} is only needed for saving and loading the agent
	 * (to restore the agent with all its parameter settings)
	 */
	private TDParams m_tdPar;
	private static final long serialVersionUID = 1234L;
	
	//public int epiCount=0;

	/**
	 * Default constructor for TDPlayerTTT, needed for loading a serialized version
	 */
	public TDAgent() {
		super();
		TDParams tdPar = new TDParams();
		initNet(tdPar, 1000);
	}

	/**
	 * Construct new {@link TDPlayerTTT}, setting everything from tdPar and set default
	 * maxGameNum=1000
	 * 
	 * @param tdPar
	 */
	public TDAgent(String name, TDParams tdPar) {
		super(name);
		initNet(tdPar, 1000);
	}

	/**
	 * Construct new {@link TDPlayerTTT}, setting everything from tdPar and from maxGameNum
	 * 
	 * @param tdPar
	 * @param maxGameNum
	 */
	public TDAgent(String name, TDParams tdPar, int maxGameNum) {
		super(name);
		initNet(tdPar, maxGameNum);
	}

	/**
	 * 
	 * @param tdPar
	 * @param maxGameNum
	 */
	private void initNet(TDParams tdPar, int maxGameNum) {
		m_tdPar = new TDParams();
		m_tdPar.setFrom(tdPar);
		m_feature = makeFeatureClass(tdPar.getFeatmode());
		//super.setFeatmode(tdPar.getFeatmode());
		//super.setEpochMax(tdPar.getEpochs());
		if (m_feature.getFeatmode() > 9) {
			m_Net = null;
		} else {
			if (tdPar.hasLinearNet()) {
				m_Net = new TD_Lin(getInputSize(m_feature.getFeatmode()),
						tdPar.hasSigmoid());
			} else {
				m_Net = new TD_NNet(getInputSize(m_feature.getFeatmode()),
						hiddenSize, tdPar.hasSigmoid());
			}
			// set alpha,beta,gamma,lambda & epochMax,rpropLrn from the TDpars
			// tab
			this.setTDParams(tdPar, maxGameNum);
		}
		// m_EPS=eps;
		m_epsilon = tdPar.getEpsilon();
		m_EpsilonChangeDelta = (m_epsilon - tdPar.getEpsilonFinal())
				/ maxGameNum;
		m_hasSigmoid = tdPar.hasSigmoid();
		m_hasLinearNet = tdPar.hasLinearNet();
		rand = new Random(System.currentTimeMillis());
		setAgentState(AgentState.INIT);
	}

	/**
	 * Get the next best action and return it
	 * 
	 * @param sob			current game state (is returned unchanged)
	 * @param random		allow epsilon-greedy random action selection	
	 * @param VTable		the score for each available action (corresponding
	 * 						to sob.getAvailableActions())
	 * @param silent
	 * @return actBest		the best action. If several actions have the same
	 * 						score, break ties by selecting one of them at random 
	 * 						
	 * Side effect: sets member randomSelect (true: if action was selected 
	 * at random, false: if action was selected by agent).
	 * See {@link #wasRandomAction()}.
	 */
	public Types.ACTIONS getNextAction(StateObservation so, boolean random, double[] VTable, boolean silent) {
		int i, j;
		double MaxScore = -Double.MAX_VALUE;
		double CurrentScore = 0; 	// NetScore*Player, the quantity to be
									// maximized
		StateObservation NewSO;
		int count = 1; // counts the moves with same MaxScore
        Types.ACTIONS actBest = null;
        int iBest;
        
//        assert (sob instanceof StateObserverTTT)
//		: "StateObservation 'sob' is not an instance of StateObserverTTT";
//		StateObserverTTT so = (StateObserverTTT) sob;
		int player = so.getPlayerPM(); 	 
		//int[][] Table = so.getTable();
        randomSelect = false;
		if (random) {
			randomSelect = (rand.nextDouble() < m_epsilon);
		}
		
		// get the best (or eps-greedy random) action
        ArrayList<Types.ACTIONS> acts = so.getAvailableActions();
        Types.ACTIONS[] actions = new Types.ACTIONS[acts.size()];
        //VTable = new double[acts.size()];  
        // DON'T! The caller has to define VTable with the right length
        
        for(i = 0; i < actions.length; ++i)
        {
            actions[i] = acts.get(i);
            NewSO = so.copy();
            NewSO.advance(actions[i]);
			
			if (NewSO.isGameOver()) {
				// Fetch game score and normalize it to the range [0,1], since 
				// TD_NNet may build a value function with a sigmoid function
				// mapping to [0,1]. Then it can use only rewards in [0,1].
				switch (so.getNumPlayers()) {
				case 1: 
					CurrentScore = NewSO.getGameScore();
					break;
				case 2: 
					CurrentScore = (-player)*NewSO.getGameScore();
					// so.getGameScore() returns -1, if 'player', that is the
					// one who *made* the move to 'so', has won. If we multiply
					// this by (-player), we get a reward +1 for a X(player=+1)- 
					// win and a reward -1 for an O(player=-1)-win.
					// And a reward 0 for a tie.
					break;
				default: 
					throw new RuntimeException("TDPlayer.trainAgent not yet "+
							"implementing case so.getNumPlayers()>2");
				}
				// Normalize to +1 (X-win), 0.5 (tie), 0.0 (O-win) for 2-player game:
				CurrentScore = normalize(CurrentScore,so.getMinGameScore(),
								   		 so.getMaxGameScore(),0.0,1.0);
			}  else {
				CurrentScore = player * getScore(NewSO);
			}
			// ???? questionable: a) what happens in case of a tie and 
			//      b) shouldn't this be in range [-1,+1]? 
//				if (NewSO.win()) {
//					CurrentScore = player * (player + 1.0) / 2.0;   
//					// 0 / 1  version for O / X - win
//				}
			
			if (!silent)
				System.out.println(NewSO.toString()+", "+(2*CurrentScore*player-1));
				//print_V(Player, NewSO.getTable(), 2 * CurrentScore * Player - 1);
			if (randomSelect) {
				CurrentScore = rand.nextDouble();
			}
			VTable[i] = CurrentScore;
			if (MaxScore < CurrentScore) {
				MaxScore = CurrentScore;
				actBest = actions[i];
				iBest  = i; 
				count = 1;
			} else if (MaxScore == CurrentScore) {
				count++;	        
			}
        } // for
        if (count>1) {  // more than one action with MaxScore: 
        	// break ties by selecting one of them randomly
        	int selectJ = (int)(rand.nextDouble()*count);
        	for (i=0, j=0; i < actions.length; ++i) 
        	{
        		if (VTable[i]==MaxScore) {
        			if (j==selectJ) actBest = actions[i];
        			j++;
        		}
        	}
        }
        assert actBest != null : "Oops, no best action actBest";
		if (!silent) {
			System.out.print("---Best Move: ");
            NewSO = so.copy();
            NewSO.advance(actBest);
			System.out.println(NewSO.toString()+", "+(2*MaxScore*player-1));
			//print_V(Player, NewSO.getTable(), 2 * MaxScore * Player - 1);
		}			
		return actBest;
	}

	/**
	 * 
	 * @return	returns true/false, whether the action suggested by last call 
	 * 			to getNextAction() was a random action 
	 */
	public boolean wasRandomAction() {
		return randomSelect;
	}

	/**
	 * Return the agent's score for that after state.
	 * 
	 * @param so			the current game state;
	 * @return V(), the prob. that X (Player +1) wins from that after state.
	 *         Player*V() is the quantity to be maximized by getNextAction.
	 */
	public double getScore(StateObservation so) {
//		assert (sob instanceof StateObserverTTT)
//		: "StateObservation 'sob' is not an instance of StateObserverTTT";
//		StateObserverTTT so = (StateObserverTTT) sob;
//		int Player = -so.getPlayerPM(); 	// Player is the player who made the move 
//									 	// while so has the player who moves next 
//		int[][] Table = so.getTable();
		double score = m_Net.getScore(m_feature.prepareInputVector(so));
		return score;
	}


	/**
	 * Train the agent (the net) for one complete game episode. Side effect:
	 * AgentBase.incrementGameNum().
	 * 
	 * @param Player
	 *            +1 or -1, player who makes the next move. If Player=+1, the
	 *            initial board position is empty, if Player=-1, the initial
	 *            board position has an 'X' set at a random location (so X is
	 *            always the one who starts the game)
	 * @return true, if agent raised a stop condition (currently only CMAPlayer)
	 */
	public boolean trainAgent(StateObservation sob) {
		return trainAgent(sob, Integer.MAX_VALUE);
	}
	public boolean trainAgent(StateObservation so, int epiLength) {
		//int[][] table = new int[3][3];
		double[] VTable = null;
		double reward = 0.0;
		boolean randomMove;
		boolean finished = false;
		boolean wghtChange = false;
		boolean DEBG = false;
		double Input[], oldInput[];
		String S_old, I_old = null;   // only as debug info
		int player;
		Types.ACTIONS actBest;
		boolean isNtuplePlayer = (m_feature.getFeatmode() == 8
				|| this.getClass().getName().equals("TicTacToe.TD_NTPlayer"));

//		assert (sob instanceof StateObserverTTT) : "Input 'sob' is not of class StateObserverTTT";
//		StateObserverTTT so = (StateObserverTTT) sob;
		player = so.getPlayerPM();
		// ??? where is the setting of table (in the old version ??? 

		m_Net.resetElig(); // reset the elig traces before starting a new game
							// /WK/ NEW/02/2015
		oldInput = m_feature.prepareInputVector(so);
		S_old = so.toString();   
		//S_old = tableToString(-Player, table);
		if (!isNtuplePlayer)
			I_old = m_feature.stringRepr(oldInput);
		m_Net.calcScoresAndElig(oldInput);
		if (DEBG) {
			for (int i = 0; i < oldInput.length; ++i)
				System.out.print((int) ((3 + oldInput[i]) % 3));
			System.out.println();
		}
		int counter=0;		// count the number of moves
		while (true) {
			VTable = new double[so.getNumAvailableActions()+1];
			actBest = this.getNextAction(so, true, VTable, true);
			randomMove = this.wasRandomAction();
			so.advance(actBest);
			if (so.isGameOver()) {
				// Fetch a reward and normalize it to the range [0,1], since 
				// TD_NNet may build a value function with a sigmoid function
				// mapping to [0,1]. Then it can use only rewards in [0,1].
				switch (so.getNumPlayers()) {
				case 1: 
					reward = so.getGameScore();
					break;
				case 2: 
					reward = (-player)*so.getGameScore();
					// so.getGameScore() returns -1, if 'player', that is the
					// one who *made* the move to 'so', has won. If we multiply
					// this by (-player), we get a reward +1 for a X(player=+1)- 
					// win and a reward -1 for an O(player=-1)-win.
					// And a reward 0 for a tie.
					break;
				default: 
					throw new RuntimeException("TDPlayer.trainAgent not yet "+
							"implementing case so.getNumPlayers()>2");
				}
				// Normalize to +1 (X-win), 0.5 (tie), 0.0 (O-win) for 2-player game:
				reward = normalize(reward,so.getMinGameScore(),
								   so.getMaxGameScore(),0.0,1.0);
				finished = true;
			} else {
				reward = 0.0;
			}
			counter++;
			if (counter==epiLength) {
				reward=rewardEstimate(so);
				//epiCount++;
				finished = true; 
			}
			Input = m_feature.prepareInputVector(so);
			if (randomMove && !finished) {
				// no training, go to next move
				m_Net.calcScoresAndElig(Input); // calculate score, write it to
												// old_y[k] for
												// next pass & update
												// eligibilities (NEW
												// WK/02/2015)
				// only for diagnostics
				if (DEBG)
					System.out.println("random move");

			} else {
				// do one training step
				
				// this is the accumulation logic: if eMax>0, then form 
				// mini batches and apply the weight changes only at the end
				// of such mini batches
				int eMax = super.getEpochMax();
				if (eMax==0) {
					wghtChange=true;
				} else {
					if (finished) numFinishedGames++;
					wghtChange = (finished && (numFinishedGames % eMax) == 0);
				}
				
				// either no random move or game is finished >> target signal is
				// meaningful!
				m_Net.updateWeights(reward, Input, finished, wghtChange);
				// contains afterwards a m_Net.calcScoresAndElig(Input);

				oldInput = Input;
			}

			if (finished) {
				if (DEBG)
					if (randomMove)
						System.out.println("Terminated by random move");
				break;
			}
			if (DEBG) {
				for (int i = 0; i < Input.length; ++i)
					System.out.print((int) ((3 + Input[i]) % 3));
				System.out.println();
			}

			player = so.getPlayerPM();   // advance to the next player
		}
		m_Net.finishUpdateWeights(); // adjust learn params ALPHA & BETA
		m_epsilon = m_epsilon - m_EpsilonChangeDelta;
		incrementGameNum();
		return false;
	}
	
	public void setTDParams(TDParams tdPar, int maxGameNum) {
		m_Net.setLambda(tdPar.getLambda());
		m_Net.setGamma(tdPar.getGamma());
		if (m_feature.getFeatmode() == 8) {
			m_Net.setAlpha(tdPar.getAlpha());
		} else {
			// set ALPHA and BETA in TD_NNet (TD_Lin) inverse proportional to
			// the fan-in,
			// i.e. divide by the number of neurons on the input side of the
			// weights:
			m_Net.setAlpha( tdPar.getAlpha() / inpSize[m_feature.getFeatmode()] );
		}
		m_Net.setBeta(tdPar.getAlpha() / hiddenSize); 	// only relevant for
														// TD_NNet
		m_Net.setAlphaChangeRatio(Math.pow(
				tdPar.getAlphaFinal() / tdPar.getAlpha(), 1.0 / maxGameNum));
		//m_Net.setEpochs(tdPar.getEpochs());  // now we use epochs over whole games
		m_Net.setRpropLrn(tdPar.hasRpropLrn());
		m_Net.setRpropInitDelta( tdPar.getAlpha() / inpSize[m_feature.getFeatmode()] );
	}

	public void setAlpha(double alpha) {
		m_Net.setAlpha(alpha);
	}

	public double getAlpha() {
		// only for debug & testing
		// super.counStates(1);
		int verbose1 = 1; // 0: skip analyze_hmX, 1: one-line output, 2:
							// multi-line output in analyse_hmX
		//super.analyze_hmC(getGameNum(), verbose1);
		int verbose2 = 1; // 0: skip analyze_hmX, 1: one-line output, 2:
							// multi-line output in analyse_hmX
		//super.analyze_hmX(verbose2);

		return m_Net.getAlpha();
	}

	public double getEpsilon() {
		return m_epsilon;
	}
	
	public String stringDescr() {
		String cs = getClass().getName();
		String str = cs + ", " + (m_hasLinearNet?"LIN":"BP")
						+ ", " + (m_hasSigmoid?"with sigmoid":"w/o sigmoid")
						+ ", lambda:" + m_Net.getLambda()
						+ ", features:" + m_feature.getFeatmode();
		return str;
	}
	
	public String printTrainStatus() {
		DecimalFormat frm = new DecimalFormat("#0.0000");
		String cs = ""; //getClass().getName() + ": ";   // optional class name
		String str = cs + "alpha="+frm.format(m_Net.getAlpha()) 
				   + ", epsilon="+frm.format(getEpsilon())
				   //+ ", lambda:" + m_Net.getLambda()
				   + ", "+getGameNum() + " games";
		return str;
	}

	private int getInputSize(int featmode) {
			return inpSize[featmode];
	}

	public int getHiddenSize() {
		return hiddenSize;
	}

	public TDParams getTDParams() {
		return m_tdPar;
	}
	
	public int getFeatmode() {
		return m_feature.getFeatmode();
	}
	
	/**
	 * Factory pattern method: make a new Feature object. This object has the 
	 * game-specific method prepareInputVector(StateObservation so) which 
	 * returns a feature vector for the current game state. 
	 * @param 	featmode	different modi of features to generate
	 * @return	the Feature object
	 */
	abstract public Feature makeFeatureClass(int featmode);
}