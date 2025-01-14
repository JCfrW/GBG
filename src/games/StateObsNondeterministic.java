package games;

import java.util.ArrayList;

import tools.Types;
import tools.Types.ACTIONS;

/**
 * Class {@link StateObsNondeterministic} is used to implement <b>nondeterministic</b> games 
 * (like 2048). It extends the normal {@link StateObservation} interface.<p>
 * 
 * @see StateObservation
 * 
 * @author Johannes Kutsch, Wolfgang Konen, TH Koeln, 2017-2020
 */
public interface StateObsNondeterministic extends StateObservation {
    /**
     * Advance the current state to a new afterstate (do the deterministic part of advance)
     *
     * @param action    the action
     */
	void advanceDeterministic(ACTIONS action);

    /**
     * Advance the current afterstate to a new state (do the nondeterministic part of advance)
	 *
	 * Choose the nondeterministic action according to its probability of occurence (!)
     */
	Types.ACTIONS advanceNondeterministic();

    /**
     * Advance the current afterstate to a new state (with a specific nondeterministic action)
     * @param randAction   the nondeterministic action
     */
	Types.ACTIONS advanceNondeterministic(ACTIONS randAction);
    
    boolean isNextActionDeterministic();

//    void setNextActionDeterministic(boolean b);
    
    /**
     * Get the next nondeterministic action
     *
     * @return the action, null if the next action is deterministic
     */
    ACTIONS getNextNondeterministicAction();

	ArrayList<ACTIONS> getAvailableRandoms();

	int getNumAvailableRandoms();
	
	/**
	 * 
	 * @param action  the nondeterministic action
	 * @return the probability that the random {@code action} is selected by a 
	 *         nondeterministic advance.
	 */
	double getProbability(ACTIONS action);

	/**
	 * Same semantic as {@link StateObservation#partialState()}. But in contrast to {@link StateObservation#partialState()},
	 * it returns an object implementing {@link StateObsNondeterministic}.
	 *
	 * @return a new {@link StateObsNondeterministic} object
	 *
	 * @see StateObservation#partialState()
	 */
	StateObsNondeterministic partialState();

	StateObsNondeterministic copy();
}
