package games.ZweiTausendAchtundVierzig;

import java.io.Serializable;

import games.StateObservation;
import games.XNTupleFuncs;

public class XNTupleFuncs2048 implements XNTupleFuncs, Serializable {

	//
	// The following five functions are only needed for the n-tuple interface:
	//
	/**
	 * @return the number of board cells
	 */
	@Override
	public int getNumCells() {
		return 16;
	}
	
	/**
	 * @return the number P of position values 0, 1, 2,..., P-1 that each board cell 
	 * can have (0: empty tile, 1: tile 2^1, ..., 14: tile 2^14, assuming that 2^14 is 
	 * the highest tile we encounter. Theoretically, 2^17 is the highest tile.) 
	 */
	@Override
	public int getNumPositionValues() {
		return 15; 
	}
	
	/**
	 * The board vector is an {@code int[]} vector where each entry corresponds to one 
	 * cell of the board. In the case of 2048 the mapping is
	 * <pre>
	 *    00 01 02 03
	 *    04 05 06 07
	 *    08 09 10 11
	 *    12 13 14 15
	 * </pre>
	 * @return a vector of length {@link #getNumCells()}, holding for each board cell its 
	 * position value 0:empty, 1: tile 2^1, 2: tile 2^2,..., P-1: tile 2^(P-1).
	 */
	@Override
	public int[] getBoardVector(StateObservation so) {
		int[] bvec = new int[getNumCells()]; 
		int b2,k;
		assert (so instanceof StateObserver2048);
		Tile[][] gameBoard = ((StateObserver2048) so).getGameBoard();
		for(int row = 0, n=0; row < Config.ROWS; row++) {
            for(int column = 0; column < Config.COLUMNS; column++,n++) {
            	b2 = gameBoard[row][column].getValue();
            	for (k=0; k<getNumPositionValues(); k++) {
            		// find the exponent k in 2^k by down-shifting:
                    b2 = b2>>1;
            		if (b2==0) break;
            	}
            	bvec[n]=k;                	
            }
        }
		return bvec;   
	}
	
	/**
	 * Given a board vector from {@link #getBoardVector(StateObservation)} and given that the 
	 * game has s symmetries, return an array which holds s symmetric board vectors: <ul>
	 * <li> the first row {@code boardArray[0]} is the board vector itself
	 * <li> the other rows are the board vectors when transforming {@code boardVector}
	 * 		according to the s-1 other symmetries (e. g. rotation, reflection, if applicable).
	 * </ul>
	 * @param boardVector
	 * @return boardArray
	 */
	@Override
	public int[][] symmetryVectors(int[] boardVector) {
		// TODO
		throw new RuntimeException("XNTupleFuncs2048.symmetryVectors() not yet implemented");
	}

	/** 
	 * Return a fixed set of {@code numTuples} n-tuples suitable for that game. 
	 * Different n-tuples may have different length. An n-tuple {0,1,4} means a 3-tuple 
	 * containing the cells 0, 1, and 4.
	 * 
	 * @return nTuples[numTuples][]
	 */
	@Override
	public int[][] fixedNTuples() {
		// TODO
		throw new RuntimeException("XNTupleFuncs2048.fixedNTuples() not yet implemented");
	}
	//
	// End n-tuple functions
	//
	

}