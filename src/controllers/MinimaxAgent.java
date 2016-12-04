package controllers;

import controllers.PlayAgent;
import games.StateObservation;
import params.OtherParams;
import params.TDParams;
import tools.Types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * The Minimax {@link PlayAgent}. It traverses the game tree up to a prescribed 
 * depth (default: 10, see {@link OtherParams}). To speed up calculations, already 
 * visited states are stored in a HashMap.  
 * 
 * @author Wolfgang Konen, TH K�ln, Nov'16
 * 
 */
public class MinimaxAgent extends AgentBase implements PlayAgent, Serializable
{
	private Random rand;
	private int m_GameNum;
	private int	m_MaxGameNum;
	private double[] m_VTable;
	private int m_depth=10;
	private HashMap<String,Double> hm;
	private static final long serialVersionUID = 123L;
		
	public MinimaxAgent(String name)
	{
		super(name);
		super.setMaxGameNum(1000);		
		super.setGameNum(0);
        rand = new Random(System.currentTimeMillis());
		hm = new HashMap<String, Double>();
		setAgentState(AgentState.TRAINED);
	}
	
	public MinimaxAgent(String name, OtherParams opar)
	{
		this(name);
		m_depth = opar.getMinimaxDepth();
	}
	
	
	/**
	 * Get the best next action and return it
	 * @param sob			current game state (not changed on return)
	 * @param random		allow epsilon-greedy random action selection	
	 * @param VTable		must be an array of size n+1 on input, where 
	 * 						n=sob.getNumAvailableActions(). On output,
	 * 						elements 0,...,n-1 hold the score for each available 
	 * 						action (corresponding to sob.getAvailableActions())
	 * 						In addition, VTable[n] has the score for the 
	 * 						best action.
	 * @param silent
	 * @return actBest		the best action 
	 * 
	 */	
	@Override
	public Types.ACTIONS getNextAction(StateObservation so, boolean random, double[] VTable, boolean silent) {
		//VTable = new double[so.getNumAvailableActions()];
        // DON'T! The caller has to define VTable with the right length

		Types.ACTIONS actBest = getBestAction(so,  random,  VTable,  silent, 0);
		return actBest;
	}
	
	private Types.ACTIONS getBestAction(StateObservation so, boolean random, 
			double[] VTable, boolean silent, int depth) 
	{
		int i,j;
		double iMaxScore;
		double CurrentScore = 0; 	// NetScore*Player, the quantity to be
									// maximized
		StateObservation NewSO;
		int count = 1; // counts the moves with same iMaxScore
        Types.ACTIONS actBest = null;
        String stringRep;
        int iBest;
		//VTable = new double[so.getNumAvailableActions()];
        // DON'T! The caller has to define VTable with the right length
		
        stringRep = so.toString();
        if (stringRep.equals("XoXoXo-X-")) {
        	int dummy=1;
        }
		assert so.isLegalState() 
			: "Not a legal state"; // e.g. player to move does not fit to Table

		iMaxScore = -Double.MAX_VALUE;
        ArrayList<Types.ACTIONS> acts = so.getAvailableActions();
        Types.ACTIONS[] actions = new Types.ACTIONS[acts.size()];
        //VTable = new double[acts.size()];
        for(i = 0; i < acts.size(); ++i)
        {
        	actions[i] = acts.get(i);
        	NewSO = so.copy();
        	NewSO.advance(actions[i]);
        	
			// speed up MinimaxPlayer for repeated calls by storing the 
			// scores of visited states in a HashMap
			stringRep = NewSO.toString();
			//System.out.println(stringRep);
			Double sc = hm.get(stringRep); 
			if (depth<this.m_depth) {
				if (sc==null) {
					// here is the recursion: getScore calls getBestTable:
					CurrentScore = getScore(NewSO,depth+1);	
					switch(so.getNumPlayers()) {
					case (1): break;
		            case (2): 
		            	CurrentScore = - CurrentScore; 		// negamax variant for 2-player tree
		            	break;
		            default:		// i.e. n-player, n>2
		            	throw new RuntimeException("Minimax is not yet implemented for n-player games (n>2).");
		            }
					hm.put(stringRep, CurrentScore);
				} else {
					CurrentScore = sc;
				}
			} else {
				CurrentScore = estimateScore(NewSO);
				if (so.getNumPlayers()==2) 
					CurrentScore = - CurrentScore; 		
			}
			if (depth==0) {
				//System.out.println(stringRep + ", " + CurrentScore);
			}
        	VTable[i] = CurrentScore;
        	if (iMaxScore < CurrentScore) {
        		iMaxScore = CurrentScore;
        		actBest = actions[i];
        		iBest  = i; 
        		count = 1;
        	} else  {
        		if (iMaxScore == CurrentScore) count++;	        
        	}
        } // for
        if (count>actions.length) {
        	int dummy=1;
        }
        if (count>1) {  // more than one action with iMaxScore: 
        	// break ties by selecting one of them randomly
        	int selectJ = (int)(rand.nextDouble()*count);
        	for (i=0, j=0; i < actions.length; ++i) 
        	{
        		if (VTable[i]==iMaxScore) {
        			if (j==selectJ) actBest = actions[i];
        			j++;
        		}
        	}
        }

        assert actBest != null : "Oops, no best action actBest";
        // optional: print the best action
        if (!silent) {
        	NewSO = so.copy();
        	NewSO.advance(actBest);
        	System.out.print("---Best Move: "+NewSO.toString()+"   "+iMaxScore);
        }			

        VTable[actions.length] = iMaxScore;
       return actBest;         
	}


	/**
	 * 
	 * @return	returns true/false, whether the action suggested by last call 
	 * 			to getNextAction() was a random action. 
	 * 			Always true in the case of MinimaxAgent
	 */
	public boolean wasRandomAction() {
		return false;
	}

	/**
	 * Return the agent's score for that after state.
	 * @param sob			the current game state;
	 * @return				the probability that the player to move wins from that 
	 * 						state. If game is over: the score for the player who 
	 * 						*would* move (if the game were not over).
	 * Each player wants to maximize its score	 
	 * 				    	
	 */
	 // OLD return: V(), the prob. that X (Player +1) wins from that after state. 
	 // Player*V() is the quantity to be maximized by getBestTable.  
	@Override
	public double getScore(StateObservation sob) {
		return getScore(sob,0);
	}
	private double getScore(StateObservation sob, int depth) {
		String stringRep = sob.toString();
		if (stringRep.equals("XXoXXooo-")) {
			int dummy=1;
		}
		if (sob.isGameOver())
		{
			int res = sob.getGameWinner().toInt(); 
			return res; 	
			// +1/0/-1  for Player/tie/Opponent win	
			// e.g.: if board is a win for O, then X is to 'move' and res 
			// is thus a -1, since X has already lost.
		}
		
		int n=sob.getNumAvailableActions();
		double[] vtable	= new double[n+1];
		getBestAction(sob,  false,  vtable,  true, depth);  // sets vtable[n]=iMaxScore
		return vtable[n];		// return iMaxScore
	}

	/**
	 * When the recursion tree has reached its maximal depth m_depth, then return
	 * an estimate of the game score. This function may be overridden in a game-
	 * specific way by classes derived from {@link MinimaxAgent}. <p>
	 * This  stub method just returns {@link #getScore(StateObservation)}, which might 
	 * be too simplistic for not-yet finished games, because the score will be  
	 * mostly 0.
	 * @param sob	the state observation
	 * @return		the score.
	 */
	private double estimateScore(StateObservation sob) {
		return getScore(sob);
	}

	@Override
	public String stringDescr() {
		String cs = getClass().getName();
		cs = cs + ", depth:"+m_depth;
		return cs;
	}

	public int getDepth() {
		return m_depth;
	}

}