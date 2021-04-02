package games.BlackJack;

import java.util.ArrayList;
import static games.BlackJack.BlackJackConfig.START_CHIPS;

public class Player {
    ArrayList<Hand> hands = new ArrayList<Hand>();
    Hand activeHand = null;
    private double chips = START_CHIPS;
    private double betThisRound[] = new double[3];
    private boolean splitHand = false;
    private int activeHandIndex = 0;
    private double insurance = 0;
    private boolean surrender = false;
    private double roundPayoff = 0;

    String name;

    public void setChips(double chips) {
        this.chips = chips;
    }

    public Player(String name) {
        this.name = name;
    }

    public Player(Player other) {
        if (other.activeHand != null) {
            this.activeHand = new Hand(other.activeHand);
            int count = 0;
            for (Hand a : other.hands) {
                if (count++ == other.activeHandIndex) {
                    this.hands.add(this.activeHand);
                } else {
                    this.hands.add(new Hand(a));
                }
            }
        }
        this.surrender = other.surrender;
        this.insurance = other.insurance;
        this.name = other.name;
        this.chips = other.chips;
        this.splitHand = other.splitHand;
        this.activeHandIndex = other.activeHandIndex;
        this.betThisRound = other.betThisRound.clone();
        this.roundPayoff = other.roundPayoff;
    }

    public Hand getActiveHand() {
        return activeHand;
    }

    public ArrayList<Hand> getHands() {
        return hands;
    }

    public void addCardToActiveHand(Card c) {
        if (activeHand == null) {
            activeHand = new Hand(c);
            hands.add(activeHand);
        } else {
            activeHand.addCard(c);
        }
    }

    public Hand setNextHandActive() {
        if (hands.size() <= activeHandIndex + 1) {
            return null;
        }
        return activeHand = hands.get(++activeHandIndex);
    }

    public void bet(double amount) {
        chips -= amount;
        betThisRound[activeHandIndex] += amount;
    }

    public void clearHand() {
        activeHand = null;
        hands.clear();
        betThisRound = new double[3];
        activeHandIndex = 0;
        insurance = 0;
        splitHand = false;
        surrender = false;
        roundPayoff = 0;
    }



    public double betOnActiveHand() {
        return betThisRound[activeHandIndex];
    }

    public double getBetAmountForHand(Hand h) {
        int index = hands.indexOf(h);
        return betThisRound[index];
    }

    public boolean hasHand() {
        return !hands.isEmpty();
    }

    public double getChips() {
        return chips;
    }

    public boolean hasSplitHand() {
        return splitHand;
    }

    public double collect(double chips) {
        return this.chips += chips;
    }

    public void splitHand() {
        splitHand = true;
        hands.add(activeHand.split());
        betThisRound[hands.size() - 1] = betThisRound[activeHandIndex];
        chips -= betThisRound[activeHandIndex];
    }

    public void insurance() {
        chips -= betThisRound[0];
        insurance = betThisRound[0];
    }

    public double addPayOff(double payOff){
        return roundPayoff += payOff;
    }

    public double getRoundPayoff(){
        return roundPayoff;
    }

    public double insuranceAmount() {
        return insurance;
    }

    public void surrender() {
        surrender = true;
    }

    public boolean hasSurrender() {
        return surrender;
    }

    public boolean hasLost(){
        return chips < 10 && betOnActiveHand() == 0;
    }

    public double getSumAllBets(){
        return betThisRound[0] + betThisRound[1] + betThisRound[2] + insurance;

    }

    public String toString() {
        String result = name + " | chips: " + chips;
        for (Hand h : hands) {
            result += " | hand : " + h + " = "+ h.getHandValue();
        }
        return result;
    }
}