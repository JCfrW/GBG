package TournamentSystem;

import TournamentSystem.Scoring.Elo.EloPlayer;
import TournamentSystem.Scoring.Elo.EloPlayerFIDE;
import TournamentSystem.Scoring.Elo.EloPlayerUSCF;
import TournamentSystem.Scoring.Glicko2.Glicko2Rating;
import TournamentSystem.Scoring.Glicko2.Glicko2RatingCalculator;
import controllers.PlayAgent;

import javax.swing.*;

import java.io.Serializable;

import static TournamentSystem.TSAgentManager.faktorLos;
import static TournamentSystem.TSAgentManager.faktorTie;
import static TournamentSystem.TSAgentManager.faktorWin;

/**
 * This class holds the individual {@link PlayAgent}, its information and game statistics.
 * All Agents are stored in {@link TSResultStorage} and manages in {@link TSAgentManager}.
 * <p>
 * This class implements the Serializable interface to be savable to storage, except for the PlayAgent itself.
 *
 * @author Felix Barsnick, University of Applied Sciences Cologne, 2018
 */
public class TSAgent implements Serializable {
    /**
     * change the version ID for serialization only if a newer version is no longer
     * compatible with an older one (older .tsr.zip will become unreadable or you have
     * to provide a special version transformation)
     */
    private static final long serialVersionUID = 1L;

    private String name;
    private String agent;
    private boolean isHddAgent;
    private transient PlayAgent mPlayAgent;
    private int won;
    private int lost;
    private int tie;
    public JCheckBox guiCheckBox;
    public EloPlayerFIDE mEloPlayerFIDE;
    public EloPlayerUSCF mEloPlayerUSCF;
    public Glicko2Rating mGlicko2Rating;

    /**
     * Create a new {@link TSAgent} instance with its properties and data.
     * @param name (file)name of the agent
     * @param agent agent type
     * @param checkbox JCheckBox used in {@link TSSettingsGUI2}
     * @param hddAgent boolean if this agent was loaded from disk
     * @param playAgent the agent itself ({@code null} if its a standard agent)
     * @param glicko2RatingSystem
     */
    public TSAgent(String name, String agent, JCheckBox checkbox, boolean hddAgent, PlayAgent playAgent, Glicko2RatingCalculator glicko2RatingSystem) {
        this.name = name;
        this.agent = agent;
        isHddAgent = hddAgent;
        mPlayAgent = playAgent;
        guiCheckBox = checkbox;
        won = 0;
        tie = 0;
        lost = 0;
        mEloPlayerFIDE = new EloPlayerFIDE(name);
        mEloPlayerUSCF = new EloPlayerUSCF(name);
        mGlicko2Rating = new Glicko2Rating(name, glicko2RatingSystem);
    }

    public void addWonGame(){
        won++;
    }

    public void addLostGame(){
        lost++;
    }

    public void addTieGame(){
        tie++;
    }

    public String getName(){
        return name;
    }

    public String getAgentType(){
        return agent;
    }

    public int getCountWonGames(){
        return won;
    }

    public int getCountLostGames(){
        return lost;
    }

    public int getCountTieGames(){
        return tie;
    }

    public int getCountAllGames() {
        return won + tie + lost;
    }

    /**
     * get the agent score based based on win/tie/loss multiplied by factors set in {@link TSAgentManager}
     * @return agents WTL score
     */
    public float getAgentScore() {
        float agentScore = getCountWonGames()*faktorWin+getCountTieGames()*faktorTie+getCountLostGames()*faktorLos;
        return agentScore;
    }

    /**
     * was the agent loaded from disk
     * @return boolean if agent was loaded from disk
     */
    public boolean isHddAgent() {
        return isHddAgent;
    }

    public PlayAgent getPlayAgent() {
        return mPlayAgent;
    }

    public String toString() {
        return "Agent Name:"+getName()+" Typ:"+getAgentType()+" from HDD:"+isHddAgent;
    }

}