package salamander.chesticuffs.inventory;

import salamander.chesticuffs.traits.TraitsHolder;

public class DataHolder {
    private ItemType type;
    private short ATK, DEF, HP;
    private TraitsHolder traits;
    String buff, debuff, description, flavor;
    byte useLimit;

    public short getATK() {
        return ATK;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public short getDEF() {
        return DEF;
    }

    public void setDEF(short DEF) {
        this.DEF = DEF;
    }

    public short getHP() {
        return HP;
    }

    public void setHP(short HP) {
        this.HP = HP;
    }

    public TraitsHolder getTraits() {
        return traits;
    }

    public void setTraits(TraitsHolder traits) {
        this.traits = traits;
    }

    public String getBuff() {
        return buff;
    }

    public void setBuff(String buff) {
        this.buff = buff;
    }

    public String getDebuff() {
        return debuff;
    }

    public void setDebuff(String debuff) {
        this.debuff = debuff;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFlavor() {
        return flavor;
    }

    public void setFlavor(String flavor) {
        this.flavor = flavor;
    }

    public byte getUseLimit() {
        return useLimit;
    }

    public void setUseLimit(byte useLimit) {
        this.useLimit = useLimit;
    }

    public void setATK(short ATK) {
        this.ATK = ATK;
    }
}
