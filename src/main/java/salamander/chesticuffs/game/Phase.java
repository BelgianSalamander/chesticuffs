package salamander.chesticuffs.game;

public enum Phase {
    CORE_PLACEMENT(0),
    REDSTONE(1),
    OPENING_PHASE(2),
    ATTACKER_SELECTION(3),
    DEFENDER_SELECTION(4),
    CLOSING_PHASE(5);

    public int getPhaseNumber() {
        return phaseNumber;
    }

    private int phaseNumber;

    private Phase(int phaseNumber){
        this.phaseNumber = phaseNumber;
    }
}
