package salamander.chesticuffs.inventory;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import salamander.chesticuffs.traits.Trait;

import java.nio.charset.StandardCharsets;

public class PersistentDataHolder implements PersistentDataType<byte[], DataHolder> {
    @Override
    public @NotNull Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Override
    public @NotNull Class<DataHolder> getComplexType() {
        return DataHolder.class;
    }

    @Override
    public byte @NotNull [] toPrimitive(@NotNull DataHolder complex, @NotNull PersistentDataAdapterContext context) {
        int totalLength = 8 /*Generic Data*/ + Trait.length + 4 * Trait.amountOfValues /*Trait Data*/ + 4;

        byte[] GenericData = {
                (byte) complex.getType().getID(), complex.getUseLimit(),
                (byte) ((complex.getATK() << 8) & 0xff), (byte) (complex.getATK() & 0xff),
                (byte) ((complex.getDEF() << 8) & 0xff), (byte) (complex.getDEF() & 0xff),
                (byte) ((complex.getHP() << 8) & 0xff), (byte) (complex.getHP() & 0xff)
        };

        byte[] flavor = toBytes(complex.getFlavor());
        byte[] buff = toBytes(complex.getBuff());
        byte[] debuff = toBytes(complex.getDebuff());
        byte[] desc = toBytes(complex.getDescription());

        byte[] traits = toBytes(complex.getTraits().getTraits());
        byte[] traitValues = toBytes(complex.getTraits().getValues());

        return new byte[0];
    }

    @Override
    public @NotNull DataHolder fromPrimitive(byte @NotNull [] primitive, @NotNull PersistentDataAdapterContext context) {
        return null;
    }

    //Most of this is from https://www.daniweb.com/programming/software-development/code/216874/primitive-types-as-byte-arrays

    static private byte[] toBytes(String str) {
        if (str == null) return "".getBytes(StandardCharsets.UTF_8);
        else {
            return str.getBytes(StandardCharsets.UTF_8);
        }
    }

    static private String fromBytes(byte[] data) {
        return new String(data);
    }

    private static byte[] toBytes(int data) {
        return new byte[]{
                (byte) ((data >> 24) & 0xff),
                (byte) ((data >> 16) & 0xff),
                (byte) ((data >> 8) & 0xff),
                (byte) ((data >> 0) & 0xff),
        };
    }

    private static int toInt(byte[] data) {
        if (data == null || data.length != 4) return 0x0;
        // ----------
        return (int) ( // NOTE: type cast not necessary for int
                (0xff & data[0]) << 24 |
                        (0xff & data[1]) << 16 |
                        (0xff & data[2]) << 8 |
                        (0xff & data[3]) << 0
        );
    }

    public static byte[] toBytes(boolean[] data) {
        // Advanced Technique: The byte array containts information
        // about how many boolean values are involved, so the exact
        // array is returned when later decoded.
        // ----------
        if (data == null) return null;
        // ----------
        int len = data.length;
        byte[] lena = toBytes(len); // int conversion; length array = lena
        byte[] byts = new byte[lena.length + (len / 8) + (len % 8 != 0 ? 1 : 0)];
        // (Above) length-array-length + sets-of-8-booleans +? byte-for-remainder
        System.arraycopy(lena, 0, byts, 0, lena.length);
        // ----------
        // (Below) algorithm by Matthew Cudmore: boolean[] -> bits -> byte[]
        for (int i = 0, j = lena.length, k = 7; i < data.length; i++) {
            byts[j] |= (data[i] ? 1 : 0) << k--;
            if (k < 0) {
                j++;
                k = 7;
            }
        }
        // ----------
        return byts;
    }

    private static boolean[] toBooleans(byte[] data) {
        // Advanced Technique: Extract the boolean array's length
        // from the first four bytes in the char array, and then
        // read the boolean array.
        // ----------
        if (data == null || data.length < 4) return null;
        // ----------
        int len = toInt(new byte[]{data[0], data[1], data[2], data[3]});
        boolean[] bools = new boolean[len];
        // ----- pack bools:
        for (int i = 0, j = 4, k = 7; i < bools.length; i++) {
            bools[i] = ((data[j] >> k--) & 0x01) == 1;
            if (k < 0) {
                j++;
                k = 7;
            }
        }
        // ----------
        return bools;
    }

    private static byte[] toBytes(int[] data) {
        if (data == null) return null;
        // ----------
        byte[] byts = new byte[data.length * 4];
        for (int i = 0; i < data.length; i++)
            System.arraycopy(toBytes(data[i]), 0, byts, i * 4, 4);
        return byts;
    }

    private static int[] toInts(byte[] data) {
        if (data == null || data.length % 4 != 0) return null;
        // ----------
        int[] ints = new int[data.length / 4];
        for (int i = 0; i < ints.length; i++)
            ints[i] = toInt( new byte[] {
                    data[(i*4)],
                    data[(i*4)+1],
                    data[(i*4)+2],
                    data[(i*4)+3],
            } );
        return ints;
    }
}
