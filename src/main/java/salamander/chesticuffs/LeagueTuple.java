package salamander.chesticuffs;

//Stores data for leagues and their threshold
public class LeagueTuple{
    public final String name;
    public final int threshold;
    public LeagueTuple(String name, int threshold) {
        this.name = name;
        this.threshold = threshold;
    }
}
