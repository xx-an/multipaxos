package lsr.paxos.test.map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

import lsr.paxos.client.Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapClient {
    private Client client;

    public void run() throws IOException, Exception {
        client = new Client();
        client.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String line;

            line = reader.readLine();

            if (line == null) {
                break;
            }

            String[] args = line.trim().split(" ");

            if (args[0].equals("bye")) {
                System.exit(0);
            }

            if (args.length != 2) {
                instructions();
                continue;
            }
            
            Long key = Long.parseLong(args[0]);
            Long value = Long.parseLong(args[1]);
            
            MapServiceCommand command = new MapServiceCommand(key, value);
            byte[] response = client.execute(command.toByteArray());
            ByteBuffer buffer = ByteBuffer.wrap(response);
            Long previousValue = buffer.getLong();
            logger.info(String.format("Previous value for %d was %d", key, previousValue));
        }
    }

    private static void instructions() {
        System.out.println("Provide key-value pair of integers to insert to hash map");
        System.out.println("<key> <value>");
    }

    public static void main(String[] args) throws IOException, Exception {
        instructions();
        MapClient client = new MapClient();
        client.run();
    }
    
//    public static void main(String[] args) throws IOException, Exception {
//    	if (args.length != 2) {
//    		logger.error("Usage: java MapClient <client_number>  <entry_number>");
//            System.exit(0);
//        }
//    	int clientNum = Integer.parseInt(args[0]);
//    	int keyvaluepair = Integer.parseInt(args[1]);
//    	for(int i = 0; i < clientNum; i++)
//    	{
//    		Thread newClient = new SimplifiedClient(keyvaluepair);
//    		newClient.start();
//    	}
//    }
    
    private final static Logger logger = LoggerFactory.getLogger(MapClient.class);
}
