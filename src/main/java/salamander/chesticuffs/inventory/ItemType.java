package salamander.chesticuffs.inventory;

public enum ItemType {
    ITEM(0), CORE(1), USABLE(2);

    private int ID;

    public int getID() {
        return ID;
    }

    private ItemType(int n){ID = n;}

    public static ItemType fromID(int n){
        return ItemType.values()[n];
    }
}
