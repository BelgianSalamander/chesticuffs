package salamander.chesticuffs.traits;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import salamander.chesticuffs.inventory.ItemHandler;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TraitsHolder {
    private boolean[] traits;
    private int[] values;

    public TraitsHolder(ItemMeta meta){
        traits = new boolean[Trait.length];

        if(meta == null) return;

        int[] itemTraits = meta.getPersistentDataContainer().get(ItemHandler.getTraitsKey(), PersistentDataType.INTEGER_ARRAY);
        if(itemTraits == null){
            return;
        }

        for(int trait : itemTraits){
            traits[trait] = true;
        }

        values = meta.getPersistentDataContainer().get(ItemHandler.getTraitValueKey(), PersistentDataType.INTEGER_ARRAY);
    }

    public TraitsHolder(ItemStack item){
        this(item.getItemMeta());
    }

    public TraitsHolder(){
        traits = new boolean[Trait.length];
        values = new int[Trait.amountOfValues];
    }

    public TraitsHolder(Iterable<String> traitsList){
        this();

        for(String trait : traitsList){
            int level = 0;
            int index = trait.length() - 1;
            while( Character.isDigit(trait.charAt(index))){
                level *= 10;
                level += (int) trait.charAt(index);
            }
            if(level == 0){
                index += 1;
            }
            int traitIndex = Trait.valueOf(trait.toLowerCase().substring(0, index).replace(" ", "_")).toInt();
            traits[traitIndex] = true;
            values[Trait.fromInt(traitIndex).getValueIndex()] = level;
        }
    }

    public TraitsHolder(String[] traitsList){
        this(Arrays.asList(traitsList));
    }

    public boolean hasTrait(Trait trait){
        return traits[trait.toInt()];
    }

    public boolean addTrait(Trait trait){
        int index = trait.toInt();
        boolean previous = traits[index];
        traits[index] = true;
        return !previous;
    }

    public boolean removeTrait(Trait trait){
        int index = trait.toInt();
        boolean previous = traits[index];
        traits[index] = false;
        return previous;
    }

    public void setTraitsOf(ItemStack item){
        if(item == null) return;
        ItemMeta meta = item.getItemMeta();
        if(meta == null) return;
        String type = meta.getPersistentDataContainer().get(ItemHandler.getTypeKey(), PersistentDataType.STRING);
        if(type == null) return;
        if(type.equals("item")){
            setTraitsOf(meta);
            item.setItemMeta(meta);
        }
    }

    public void setTraitsOf(ItemMeta meta){
        int amountOfTraits = 0;
        for(boolean hasTrait : traits){
            if(hasTrait) {
                amountOfTraits++;
            }
        }

        int[] traitsArray = new int[amountOfTraits];
        int[] valueArray = new int[Trait.amountOfValues];
        int index = 0;
        int traitsArrayIndex = 0;
        for(boolean hasTrait : traits){
            if(hasTrait){
                traitsArray[traitsArrayIndex] = index;
                traitsArrayIndex++;
                int valueIndex = Trait.fromInt(index).getValueIndex();
                if(valueIndex != -1){
                    valueArray[valueIndex] = values[valueIndex];
                }
            }
            index++;
        }

        meta.getPersistentDataContainer().set(ItemHandler.getTraitsKey(), PersistentDataType.INTEGER_ARRAY, traitsArray);
        meta.getPersistentDataContainer().set(ItemHandler.getTraitValueKey(), PersistentDataType.INTEGER_ARRAY, valueArray);
    }

    public List<String> getTraitsToDisplay(){
        List<String> traitNames = new LinkedList<>();

        int n = 0;
        for(boolean hasTrait : traits){
            if(hasTrait){
                traitNames.add(Trait.fromInt(n).getDisplayName());
            }
            n++;
        }

        return traitNames;
    }

    public int getLevelOf(Trait trait){
        return values[trait.toInt()];
    }

    @Override
    public String toString() {
        return "TraitsHolder{" +
                "traits=" + Arrays.toString(traits) +
                '}';
    }
}
