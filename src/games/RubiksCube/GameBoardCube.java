package games.RubiksCube;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

import controllers.PlayAgent;
import controllers.TD.ntuple2.TDNTuple2Agt;
import games.GameBoard;
import games.StateObservation;
import games.Arena;
import games.Arena.Task;
import games.RubiksCube.CSArrayList.CSAListType;
import games.RubiksCube.CSArrayList.TupleInt;
import games.RubiksCube.CubeState.Twist;
import games.ArenaTrain;
import tools.Types;
import tools.Types.ACTIONS;

/**
 * Class GameBoardCube implements interface GameBoard for RubiksCube.
 * It has the board game GUI. 
 * It shows board game states, optionally the values of possible next actions,
 * and allows user interactions with the board to enter legal moves during 
 * game play or to enter board positions for which the agent reaction is 
 * inspected. 
 * 
 * @author Wolfgang Konen, TH K�ln, Feb'18
 */
public class GameBoardCube extends JFrame implements GameBoard {

	private int TICGAMEHEIGHT=280;
	private JPanel BoardPanel;
	private JPanel ButtonPanel;
	private JLabel leftInfo=new JLabel("");
	private JLabel rightInfo=new JLabel(""); 
	protected Arena  m_Arena;		// a reference to the Arena object, needed to 
									// infer the current taskState
	protected Random rand;
	protected Random rand2;
	/**
	 * The representation of the cube in the GUI. The 24 active panels in the 6*8 field
	 * represent the cubie faces of the flattened cube.
	 */
	protected JPanel[][] Board;
	protected JButton[][] Button;
	//
	// for guiUpdateBoard: which is the row index iarr and the column index jarr
	// for each of the cubie faces in CubeState.fcol[i], i=0,...,23
	private int[] iarr = {1,1,0,0, 2,2,3,3, 2,3,3,2, 5,4,4,5, 3,2,2,3, 3,3,2,2};
	private int[] jarr = {2,3,3,2, 1,0,0,1, 2,2,3,3, 3,3,2,2, 5,5,4,4, 6,7,7,6};

	/**
	 * The representation of the state corresponding to the current 
	 * {@link #Board} position.
	 */
	private StateObserverCube m_so;
	private double[][] VTable;
	/**
	 * the array of distance sets for training
	 */
	private CSArrayList[] D;		
	/**
	 * the array of distance sets for testing (= evaluation)
	 */
	private CSArrayList[] T;		
	private CSArrayList[] D2=null;
	private boolean arenaActReq=false;
	private int[][] realPMat;
	
	/**
	 * If true, select in {@link #chooseStartState(PlayAgent)} from distance set {@link #D}.
	 * If false, use {@link #selectByTwists2(int)}. 
	 */
	private boolean SELECT_FROM_D = false;  
	/**
	 * If true, increment the matrix realPMat, which measures the real p of each start state.  
	 * Make a debug printout of realPMat every 10000 training games.
	 * 
	 * @see #chooseStartState(PlayAgent) chooseStartState(PlayAgent) and its helpers selectByTwists1 or selectByTwists2
	 * @see #incrRealPMat(StateObserverCube, int)
	 * @see #printRealPMat()
	 */
	private boolean DBG_REALPMAT=false;

	
	// the colors of the TH K�ln logo (used for button coloring):
	private Color colTHK1 = new Color(183,29,13);
	private Color colTHK2 = new Color(255,137,0);
	private Color colTHK3 = new Color(162,0,162);
	
	public GameBoardCube(Arena ticGame) {
		initGameBoard(ticGame);
		this.initialize();
		this.T = generateDistanceSets(rand2);
		if (this.SELECT_FROM_D) {
			this.D = generateDistanceSets(rand);
			//this.checkIntersects();   // print out the intersection sizes of D and T 
		} 
	}
	
	public void initialize() {
//		long seed = 999;
//		rand 		= new Random(seed);
        rand 		= new Random(System.currentTimeMillis());	
        rand2 		= new Random(2*System.currentTimeMillis());	
		realPMat 	= new int[CubeConfig.pMax+1][CubeConfig.pMax+2];			
    	D2 		= new CSArrayList[12];
		D2[0] 	= new CSArrayList(CSAListType.GenerateD0);
		D2[1] 	= new CSArrayList(CSAListType.GenerateD1);	
	}
	
	private void initGameBoard(Arena ticGame) 
	{
		m_Arena		= ticGame;
		Board       = new JPanel[6][8];
		BoardPanel	= InitBoard();
		Button		= new JButton[3][3];
		ButtonPanel = InitButton();
		VTable		= new double[3][3];
		m_so		= new StateObserverCube();	// empty table

		JPanel titlePanel = new JPanel();
		titlePanel.setBackground(Types.GUI_BGCOLOR);
		JLabel Blank=new JLabel(" ");		// a little bit of space
		JLabel Title=new JLabel("   ",SwingConstants.CENTER);  // no title, it appears sometimes in the wrong place
		Title.setForeground(Color.black);	
		Font font=new Font("Arial",1,20);			
		Title.setFont(font);	
		titlePanel.add(Blank);
		titlePanel.add(Title);
		
		JPanel boardPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		boardPanel.add(BoardPanel);
		boardPanel.add(Blank); boardPanel.add(Blank);		// a little bit of space
		boardPanel.add(ButtonPanel);
		boardPanel.setBackground(Types.GUI_BGCOLOR);
		ButtonPanel.setVisible(false);
		
		JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		infoPanel.setBackground(Types.GUI_BGCOLOR);
		Font font2=new Font("Arial",1,10);			
		leftInfo.setFont(font);	
		rightInfo.setFont(font);	
		infoPanel.add(leftInfo);
		infoPanel.add(rightInfo);
		infoPanel.setSize(100,10);
		
		setLayout(new BorderLayout(10,00));
		add(titlePanel,BorderLayout.NORTH);				
		add(boardPanel,BorderLayout.CENTER);
		add(infoPanel,BorderLayout.SOUTH);
		pack();
		setVisible(false);
	}

	private JPanel InitBoard()
	{
		JPanel panel=new JPanel();
		panel.setLayout(new GridLayout(6,8,1,1));
		panel.setBackground(Types.GUI_BGCOLOR);
		Dimension minimumSize = new Dimension(25,25); //controls the cube face sizes
		for(int i=0;i<6;i++){
			for(int j=0;j<8;j++){
				Board[i][j] = new JPanel();
				Board[i][j].setBackground(colTHK2);
				Board[i][j].setForeground(Color.white);
				Font font=new Font("Arial",Font.BOLD,14);
		        Board[i][j].setFont(font);
				Board[i][j].setPreferredSize(minimumSize); 
				boolean v = (i==2 || i==3 || j==2 || j==3);
				Board[i][j].setVisible(v);
				panel.add(Board[i][j]);
			}
		}
		return panel;
	}
	
	private JPanel InitButton()
	{
		JPanel outerPanel=new JPanel(new BorderLayout(0,10));
		JLabel Blank=new JLabel("  ");		// a little bit of space
		Blank.setBackground(Types.GUI_BGCOLOR);  
		JPanel panel=new JPanel();
		Font bfont=new Font("Arial",Font.BOLD,12);
		Font lfont=new Font("Arial",Font.BOLD,14);
		String[] twiStr = {"U","L","F"};
		panel.setLayout(new GridLayout(4,4,4,4));
		panel.setBackground(Types.GUI_BGCOLOR);
		Dimension minimumSize = new Dimension(35,35); //controls the button sizes
		for(int i=0;i<3;i++){
			for(int j=0;j<3;j++){
				Button[i][j] = new JButton();
				Button[i][j].setBackground(Color.GRAY);  //(colTHK3);
				Button[i][j].setForeground(Color.white);
		        Button[i][j].setFont(bfont);
				Button[i][j].setPreferredSize(minimumSize); 
				Button[i][j].setVisible(true);
				Button[i][j].setEnabled(true);
				Button[i][j].setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
				//.createEtchedBorder(), .createLoweredSoftBevelBorder()
				Button[i][j].addActionListener(					
						new ActionHandler(i,j)  // constructor copies (i,j) to members (x,y)
						{
							public void actionPerformed(ActionEvent e)
							{
								Arena.Task aTaskState = m_Arena.taskState;
								if (aTaskState == Arena.Task.PLAY)
								{
									HGameMove(x,y);		// i.e. make human move (i,j), if Button[i][j] is clicked								
								}
								if (aTaskState == Arena.Task.INSPECTV)
								{
									InspectMove(x,y);	// i.e. update inspection, if Button[i][j] is clicked								
								}	
								int dummy=1;
							}
						}
				);
				panel.add(Button[i][j]);
			} 
			JLabel jlab = new JLabel();
			jlab.setFont(lfont);
			jlab.setText(twiStr[i]);
			panel.add(jlab);
		} // for (i)
		for (int j=0; j<3; j++) {
			JLabel jlab = new JLabel();
			jlab.setFont(lfont);
			jlab.setText(""+(j+1));
			jlab.setPreferredSize(new Dimension(20,20));
			jlab.setHorizontalAlignment(JLabel.CENTER);
			jlab.setVerticalAlignment(JLabel.TOP);
			panel.add(jlab);
		}
//		outerPanel.add(Blank,BorderLayout.NORTH);
		//outerPanel.add(Blank,BorderLayout.CENTER);
		outerPanel.add(panel,BorderLayout.CENTER);
		return outerPanel;
	}
	
	public CSArrayList[] generateDistanceSets(Random rand) {
		
		System.out.println("\nGenerating distance sets ..");
		boolean silent=false;
		boolean doAssert=true;
//		CSAListType csaType = CSAListType.GenerateNextColSymm;
		CSAListType csaType = CSAListType.GenerateNext;
		ArrayList<TupleInt>[] tintList = new ArrayList[12];
    	CSArrayList[] gD 	= new CSArrayList[12];
		gD[0] = new CSArrayList(CSAListType.GenerateD0);
		gD[1] = new CSArrayList(CSAListType.GenerateD1);
		//D[1].assertTwistSeqInArrayList();
		for (int p=2; p<=CubeConfig.pMax; p++) {			// a preliminary set up to pMax - later we need it up to p=11
			if (p>1) silent=true;
			if (p>3) doAssert=false;
			tintList[p] = new ArrayList();
			//System.out.print("Generating distance set for p="+p+" ..");
			long startTime = System.currentTimeMillis();
			
			gD[p] = new CSArrayList(csaType, gD[p-1], gD[p-2], CubeConfig.Narr[p],
									tintList[p], silent, doAssert, rand);
			
			double elapsedTime = (double)(System.currentTimeMillis() - startTime)/1000.0;
			//assert(CubeStateMap.countDifferentStates(D[p])==D[p].size()) : "D["+p+"]: size and # diff. states differ!";
			//D[p].assertTwistSeqInArrayList();
			System.out.println("\nCoverage D["+p+"] = "+gD[p].size()+" of "+ CubeConfig.theoCov[p]
					+"    Time="+elapsedTime+" sec");
			//CSArrayList.printTupleIntList(tintList[p]);
			//CSArrayList.printLastTupleInt(tintList[p]);
			int dummy=1;
		}
			
		return gD;
	}

	@Override
	public void clearBoard(boolean boardClear, boolean vClear) {
		if (boardClear) {
			m_so = new StateObserverCube();			// solved cube
			leftInfo.setText("  ");
		}
		if (vClear) {
			if (VTable==null) VTable		= new double[3][3];
			for(int i=0;i<3;i++){
				for(int j=0;j<3;j++){
					VTable[i][j] = Double.NaN;
				}
			}
		}
	}

	/**
	 * Update the play board and the associated VBoard.
	 * 
	 * @param so	the game state
	 * @param showValueOnGameboard	if true, show the game values for the available actions
	 * 				(only if they are stored in 'so')
	 * @param enableOccupiedCells  if true, allow user interaction on occupied 
	 * 				cells (may be needed for inspecting the value function)
	 */
	@Override
	public void updateBoard(StateObservation so, 
							boolean enableOccupiedCells, boolean showValueOnGameboard) {
		int i,j;
		
		// show ButtonPanel only if it is needed (for showing action values or for entering actions) 
		switch(m_Arena.taskState) {
		case INSPECTV: { ButtonPanel.setVisible(true); break; }
		case PLAY:     { ButtonPanel.setVisible(m_Arena.hasHumanAgent()||showValueOnGameboard); break; }
		default: 	   { ButtonPanel.setVisible(false); break; }
		}
//		this.pack();
		
		if (so!=null) {
	        assert (so instanceof StateObserverCube)
			: "StateObservation 'so' is not an instance of StateObserverCube";
			StateObserverCube soT = (StateObserverCube) so;
			m_so = soT.copy();
			if (so.isGameOver()) {
				leftInfo.setText("Solved in "+so.getMoveCounter() +" twists! (p=" 
						+m_so.getCubeState().minTwists+")"); 				
			} else {
				if (so.getMoveCounter() > m_Arena.m_xab.getEpisodeLength(0)) {
					leftInfo.setText("NOT solved in "+so.getMoveCounter() +" twists! (p=" 
							+m_so.getCubeState().minTwists+")"); 				
					
				}				
			}
			
			if (showValueOnGameboard && soT.storedValues!=null) {
				for(i=0;i<3;i++)
					for(j=0;j<3;j++) 
						VTable[i][j]=Double.NaN;	
				
				for (int k=0; k<soT.storedValues.length; k++) {
					Types.ACTIONS action = soT.storedActions[k];
					int iAction = action.toInt();
					j=iAction%3;
					i=(iAction-j)/3;
					VTable[i][j] = soT.storedValues[k];					
				}	
				rightInfo.setText("");					
			} 
		} // if(so!=null)
		
		guiUpdateBoard(true,showValueOnGameboard);
	}

	/**
	 * Update the play board and the associated action values (raw score*100) for the state 
	 * {@code this.m_so}.
	 * <p>
	 * TODO: color coding for the action buttons, if {@code showValueOnGameBoard==true}. 
	 * Color coding corresponding to action values (as in Hex).
	 * 
	 * @param enable as a side effect, all buttons Button[i][j] 
	 * 				 will be set into enabled state <code>enable</code>. 
	 * @param showValueOnGameboard if true, show the values on the action buttons. If false, 
	 * 				 clear any previous values.
	 */ 
	private void guiUpdateBoard(boolean enable, boolean showValueOnGameboard)
	{		
		int i,j;
		double value;
		String valueTxt;
		int imax=0,jmax=0;
		int[] fcol = m_so.getCubeState().fcol;
		Color[] colors = {Color.white, Color.blue, colTHK2, Color.yellow, Color.green, colTHK1};		//{w,b,o,y,g,r}
		for(i=0;i<fcol.length;i++){			
			Board[iarr[i]][jarr[i]].setEnabled(enable);
			Board[iarr[i]][jarr[i]].setBackground(colors[fcol[i]]);
			Board[iarr[i]][jarr[i]].setForeground(Color.white);
		}
		
		for(i=0;i<3;i++)
			for(j=0;j<3;j++) {
				value = VTable[i][j]; 				
				if (Double.isNaN(value)) {
					valueTxt = "   ";
				} else {
					valueTxt = " "+(int)(value*100);
					if (value<0) valueTxt = ""+(int)(value*100);
				}
				if (showValueOnGameboard) {
					Button[i][j].setText(valueTxt);
				} else {
					Button[i][j].setText("   ");					
				}
				Button[i][j].setEnabled(enable);
			}

		//paint(this.getGraphics());
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

	// --- currently not used ---
	@Override
	public void enableInteraction(boolean enable) {
//		for(int i=0;i<3;i++){
//			for(int j=0;j<3;j++){
//				Button[i][j].setEnabled(enable);
//			}
//		}
	}

	/**
	 * This class is needed for each ActionListener of {@code Board[i][j]} in 
	 * {@link #InitBoard()}
	 *
	 */
	class ActionHandler implements ActionListener
	{
		int x,y;
		
		ActionHandler(int num1,int num2)			
		{		
			x=num1;
			y=num2;
		}
		public void actionPerformed(ActionEvent e){}			
	}
	
	private void HGameMove(int x, int y)
	{
		String[] twiStr = {"U","L","F"};
		System.out.println(twiStr[x]+(y+1));
		int iAction = 3*x+y;
		Types.ACTIONS act = Types.ACTIONS.fromInt(iAction);
		assert m_so.isLegalAction(act) : "Desired action is not legal";
		m_so.advance(act);			// perform action (optionally add random elements)
		System.out.println(m_so.stringDescr());
		(m_Arena.getLogManager()).addLogEntry(act, m_so, m_Arena.getLogSessionID());
		updateBoard(null,false,false);
		arenaActReq = true;			// ask Arena for next action
	}
	
	private void InspectMove(int x, int y)
	{
		String[] twiStr = {"U","L","F"};
		System.out.println(twiStr[x]+(y+1));
		int iAction = 3*x+y;
		Types.ACTIONS act = Types.ACTIONS.fromInt(iAction);
		if (!m_so.isLegalAction(act)) {
			System.out.println("Desired action is not legal!");
			m_Arena.setStatusMessage("Desired action is not legal");
			return;
		} else {
			m_Arena.setStatusMessage("Inspecting the value function ...");
		}
		m_so.advance(act);			// perform action (optionally add random elements from game 
									// environment - not necessary in RubiksCube)
		m_so.getCubeState().clearLast();		// clear lastTwist and lastTimes of the CubeState,
		m_so.setAvailableActions();				// then set the available actions which causes all
												// 9 actions to be added to m_so.acts. We need this
												// to see the values for all 9 actions.
												// (If lastTwist were set, 3 actions would be excluded
												// which we do not want during INSPECTV.) 
		updateBoard(null,false,false);
		arenaActReq = true;		
	}
	
	@Override
	public void showGameBoard(Arena ticGame, boolean alignToMain) {
		this.setVisible(true);
		if (alignToMain) {
			// place window with game board below the main window
			int x = ticGame.m_xab.getX() + ticGame.m_xab.getWidth() + 8;
			int y = ticGame.m_xab.getLocation().y;
			if (ticGame.m_LaunchFrame!=null) {
				x = ticGame.m_LaunchFrame.getX();
				y = ticGame.m_LaunchFrame.getY() + ticGame.m_LaunchFrame.getHeight() +1;
				this.setSize(ticGame.m_LaunchFrame.getWidth(),TICGAMEHEIGHT);	
			}
			this.setLocation(x,y);	
		}		
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
	 * Choose a random start state when playing a game.
	 * 
	 * @return a random start state which is p twists away from the solved cube. 
	 *         p from {1,...,{@link CubeConfig#pMax}} is picked randomly.
	 *      
	 * @see Arena#PlayGame()
	 */
	@Override
	public StateObservation chooseStartState() {
		clearBoard(true, true);			// m_so is in default start state 
		int p = 1+rand.nextInt(CubeConfig.pMax);
		System.out.println("p = "+p);
		int index = rand.nextInt(T[p].size());
		CubeState cS = (CubeState)T[p].get(index);
		m_so = new StateObserverCube(cS);
//		m_so = clearCube(m_so,p);
		m_so = new StateObserverCubeCleared(m_so,p);
		return m_so;
	}


	/**
	 * Choose a random start state when training for an episode. Return a start state depending 
	 * on {@code pa}'s {@link PlayAgent#getGameNum()} and {@link PlayAgent#getMaxGameNum()} 
	 * by randomly selecting from the distance sets D[p]. 
	 * <p>
	 * In more detail: Set X={@link CubeConfig#Xper}[{@link CubeConfig#pMax}]. 
	 * If the proportion of training games is in the first X[1] percent, select from D[1], 
	 * if it is between X[1] and X[2] percent, select from D[2], and so on.  In this 
	 * way we realize <b>time-reverse learning</b> (from the cube's end-game to the more complex
	 * cube states) during the training process. The cumulative percentage X is currently 
	 * hard-coded in {@link CubeConfig#Xper}.
	 * <p>
	 * If {@link #SELECT_FROM_D}==true, then select from {@code this.D} (distance sets created at program startup).<br>
	 * If {@link #SELECT_FROM_D}==false, then use {@link #selectByTwists2(int)}.
	 * 
	 * @param 	pa the agent to be trained, we need it here only for its {@link PlayAgent#getGameNum()} 
	 * 			and {@link PlayAgent#getMaxGameNum()}
	 * @return 	the start state for the next training episode
	 *      
	 * @see PlayAgent#trainAgent(StateObservation)   
	 * @see TDNTuple2Agt#trainAgent(StateObservation) 
	 */
	@Override
	public StateObservation chooseStartState(PlayAgent pa) {
		int p;
		clearBoard(true, true);			// m_so is in default start state 
		double[] X = CubeConfig.Xper[CubeConfig.pMax];
		double x = ((double)pa.getGameNum())/pa.getMaxGameNum() + 1e-10;
		for (p=1; p<=CubeConfig.pMax; p++) {
			if (X[p-1]<x && x<X[p]) break;
		}
		if (SELECT_FROM_D) {
			int index = rand.nextInt(D[p].size());
//			D[p].remove(cS);	// remove elements already picked -- currently NOT used
			m_so = new StateObserverCube(D[p].get(index));
		} else {
//			m_so = selectByTwists1(p);
			m_so = selectByTwists2(p);
			
			// only debug:
			if (DBG_REALPMAT) {
				if (pa.getGameNum() % 10000 == 0 ) {
					this.printRealPMat(); 
					int dummy=1;
				}
				if (pa.getGameNum()==(pa.getMaxGameNum()-1)) {
					this.printRealPMat();				
					int dummy=1;
				}
			}
		}
		
		// StateObserverCubeCleared is VERY important, so that no actions are 'forgotten' when 
		// trying to solve m_so (!!)
		m_so = new StateObserverCubeCleared(m_so,p);
		return m_so;
	}
	
	/** 
	 * --- NOT the recommended choice! --> better use selectByTwists2 ---
	 * 
	 * Experimental method to select a start state by doing 1.2*p random twist on the default cube. 
	 * This may be the only way to select a start state being p=8,9,... twists away from the 
	 * solved cube (where the distance D[p] becomes to big). 
	 * But it has the caveat that p random twists do not guarantee to produce a state in D[p]. 
	 * Due to twins etc. the resulting state may be actually in D[p-1], D[p-2], ...
	 */
	private StateObserverCubeCleared selectByTwists1(int p) {
		StateObserverCubeCleared d_so;
		CubeState cS;
		int index;
		d_so = new StateObserverCubeCleared(); // default cube
		// the not-recommended choice: make 1.2*p twists and hope that we land in 
		// distance set D[p] (which is very often not true for p>5)
		int twists = (int)(1.2*p);
		for (int k=0; k<twists; k++)  {
			index = rand.nextInt(d_so.getAvailableActions().size());
			d_so.advance(d_so.getAction(index));  				
		}
		d_so = new StateObserverCubeCleared(d_so,p);
		
		if (DBG_REALPMAT) incrRealPMat(d_so, p);	// increment realPMat		
		
		return d_so; 
	}
	
	/** 
	 * Method to select a start state in distance set D[p] by random twists and maintaining 
	 * a list D2[k], k=0,...,p, of all already visited states. 
	 * A new state in D[p] is created by randomly selecting a state from D2[p-1], advancing it 
	 * and - if it happens to be in D2[p-1] or D2[p-2] - advancing again (and again) .
	 * <p>
	 * <b>Details</b>:
	 * This method is guaranteed to return a state in the true D[p] if and only if
	 * D2[p-1], D2[p-2], ... are complete. If they are not, <ul>
	 * <li> certain elements from D[p] may be missed
	 *      (since its predecessor in D2[p-1] is not there)    -- or -- 
	 * <li> an element may claim to be in D[p], but truly it belongs to D[p-1] or D[p-2]
	 *      (not detected, since this element is not present in D2[p-1] or D2[p-2]).
	 * </ul><p>
	 * But nevertheless, in the limit of a large number of calls for every p-1, p-2, ..., this
	 * method will produce with high probability every element from D[p] and only from D[p].
	 */
	private StateObserverCubeCleared selectByTwists2(int p) {
		StateObserverCube d_so;
		StateObserverCubeCleared d_soC;
		CubeState cS;
		int index;
//			d_so = new StateObserverCube(); // default cube
//			for (int k=1; k<=p; k++)  {
			index = rand.nextInt(D2[p-1].size());
			d_so =new StateObserverCube(D2[p-1].get(index)); // pick randomly cube from D2[p-1]
			for (int k=p; k<=p; k++)  {
				index = rand.nextInt(d_so.getAvailableActions().size());
				d_so.advance(d_so.getAction(index));  	
				if (k>=3) {
					if (D2[k-1].contains(d_so.getCubeState())) {
						k = k-1;
					} else if (D2[k-2].contains(d_so.getCubeState())) {
						k = k-2;
					}
				}
			}	
			// StateObserverCubeCleared is VERY important, so that no actions are 'forgotten' when 
			// trying to solve m_so (!!)
			d_soC = new StateObserverCubeCleared(d_so,p);
			if (D2[p]==null) D2[p]=new CSArrayList(); 
			if (!D2[p].contains(d_soC.getCubeState())) D2[p].add(d_soC.getCubeState());
		
			if (DBG_REALPMAT) incrRealPMat(d_soC, p);	// increment realPMat		
		
		return d_soC; 
	}

	// --- obsolete now, we have StateObserverCubeCleared ---
//	private StateObserverCube clearCube(StateObserverCube d_so, int p) {
////		CubeState cS;
////		cS = d_so.getCubeState();
////		cS.minTwists = p;
////		cS.clearLast();
////		d_so = new StateObserverCube(cS);
//		d_so.getCubeState().minTwists = p; 
//		d_so.getCubeState().clearLast(); 	// clear lastTwist and lastTimes (which we do not know 
//											// for the initial state in an episode)	
//		d_so.setAvailableActions();	// then set the available actions which causes all
//									// 9 actions to be added to m_so.acts. We need this
//									// to test all 9 actions when looking for the best
//									// next action.
//		// (If lastTwist were set, 3 actions would be excluded
//		// which we do not want for a start state.) 
//		return d_so;
//	}
	
    /**
     * @return the array of distance sets for training
     */
    public CSArrayList[] getD() {
 	   return D;
    }
   
    /**
     * @return the array of distance sets for testing (= evaluation)
     */
    public CSArrayList[] getT() {
	   return T;
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
	public void toFront() {
   	super.setState(Frame.NORMAL);	// if window is iconified, display it normally
		super.toFront();
	}
   
    /* ---- METHODS BELOW ARE ONLY FOR DEBUG --- */

    private void checkIntersects() {
    	for (int p=1; p<=CubeConfig.pMax; p++) {
    	    Iterator itD = D[p].iterator();
    	    int intersectCount = 0;
    	    while (itD.hasNext()) {
    		    CubeState twin = (CubeState)itD.next();
    		    if (T[p].contains(twin)) intersectCount++;
            } 
    		System.out.println("checkIntersects: p="+p+", intersect(D[p],T[p])="+intersectCount+", D[p].size="+D[p].size());
    	}   	
    }
    
    /**
     * Find the real pR for state d_so which claims to be in T[p] and increment realPMat 
     * accordingly.
     * The real pR is only guaranteed to be found, if T[p] is complete.
     */
    void incrRealPMat(StateObserverCube d_so, int p) {
		boolean found=false;
		for (int pR=0; pR<=CubeConfig.pMax; pR++) {
			if (T[pR].contains(d_so.getCubeState())) {
				// the real p is pR
				realPMat[p][pR]++;
				found = true;
				break;
			}
		}
		
		if (!found) realPMat[p][CubeConfig.pMax+1]++;
		// A count in realPMat[X][pMax+1] means: the real p is not known for p=X.
		// This can happen if T[pR] is not the complete set: Then d_so might be truly in 
		// the distance set of pR, but it is not found in T[pR].
	}

	public void printRealPMat() {
		DecimalFormat df = new DecimalFormat("  00000");
		for (int i=0; i<realPMat.length; i++) {
			for (int j=0; j<realPMat[i].length; j++) {
				System.out.print(df.format(realPMat[i][j]));
			}
			System.out.println("");
		}
		
	}
   
}
