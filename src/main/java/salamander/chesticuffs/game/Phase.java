package salamander.chesticuffs.game;

public enum Phase {
    CORE_PLACEMENT(0),
    OPENING_PHASE(1),
    ATTACKER_SELECTION(2),
    DEFENDER_SELECTION(3),
    REDSTONE(4),
    CLOSING_PHASE(5);

    public int getPhaseNumber() {
        return phaseNumber;
    }

    private int phaseNumber;

    private Phase(int phaseNumber){
        this.phaseNumber = phaseNumber;
    }
}
