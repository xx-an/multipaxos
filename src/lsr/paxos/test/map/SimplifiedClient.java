package lsr.paxos.test.map;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import lsr.paxos.client.Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimplifiedClient extends Thread {
	private int kvNum;
	
	public SimplifiedClient(int kv)
	{
		kvNum = kv;
	}

    public void run() {
        try 
        {
        	Client client = new Client();
			
			client.connect();

	        int iter = 0;
	        Random random = new Random();
	        for(iter = 0; iter < kvNum; iter++) {

	            Long key = random.nextLong();
	            Long value = random.nextLong();
	            
	            MapServiceCommand command = new MapServiceCommand(key, value);
	            byte[] response = client.execute(command.toByteArray());
	            ByteBuffer buffer = ByteBuffer.wrap(response);
	            Long previousValue = buffer.getLong();
	            logger.info(String.format("Previous value for %d was %d", key, previousValue));
	        }
		} 
        catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private final static Logger logger = LoggerFactory.getLogger(SimplifiedClient.class);
}
