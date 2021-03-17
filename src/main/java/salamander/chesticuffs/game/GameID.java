package salamander.chesticuffs.game;

public class GameID {
    /*This class generates unique ids as strings. These ids will be stored in the actual chest and will map
    to an actual ChesticuffsGame object (Not created yet) in memory which will store the actual info about
    the game (e.g how much of each item has been placed, players playing etc...)*/
    static int counter = 0;
    static char[] chars = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'};

    static public String next(){
        int amount;
        int n = counter++;
        if(n == 0){
            return "a";
        }
        String id = "";
        int exp = (int)( Math.log(n) / Math.log(chars.length));
        System.out.println(chars.length);
        System.out.println(n);
        System.out.println(exp);
        for(int i = exp; i >= 0; i--){
            //System.out.println(i);
            amount = (int) (n / Math.pow(chars.length, i));
            id = id + chars[amount];
            n -= amount * Math.pow(chars.length, i);
        }
        return id;
    }
}
