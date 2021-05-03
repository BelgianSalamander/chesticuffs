package salamander.chesticuffs.queue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import salamander.chesticuffs.Chesticuffs;
import salamander.chesticuffs.inventory.ItemHandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class QueueHandler {
    static public List<ChesticuffsQueue> queues;

    static public void init(){
        queues = new LinkedList<ChesticuffsQueue>();

        JSONParser parser = new JSONParser();
        JSONObject JSONData = null;
        try{
            InputStream is = new FileInputStream(Chesticuffs.getQueuesFile());
            String data = ItemHandler.readFromInputStream(is);
            JSONData = (JSONObject) parser.parse(data);
        }catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
        //Gets chest data (Couldn't figure out how to just a file that is just an array)
        JSONArray JSONQueues = (JSONArray) JSONData.get("queues");

        for (Object jsonQueue : JSONQueues) {
            JSONArray queue = (JSONArray) jsonQueue;
            List<String> names = new LinkedList<>();
            for(Object name : (JSONArray) ((JSONArray) jsonQueue).get(0)) {
                names.add((String) name);
            }
            queues.add(new ChesticuffsQueue(names, (boolean) ((JSONArray) jsonQueue).get(1), (boolean) ((JSONArray) jsonQueue).get(2), (int) (long) ((JSONArray) jsonQueue).get(3)));
        }
    }
}
