package lsr.paxos;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import lsr.common.Request;
import lsr.paxos.core.Proposer;
import lsr.paxos.core.Paxos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Batcher{

    private ConcurrentLinkedQueue<byte[]> fullBatches = new ConcurrentLinkedQueue<byte[]>();

    private final Proposer proposer;

    public Batcher(Paxos paxos) {
        this.proposer = paxos.getProposer();
    }
    
    public void start()
    {
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see lsr.paxos.Batcher#enqueueClientRequest(lsr.common.RequestType)
     */
    public void enqueueClientRequest(final Request request) {
    	logger.info("enqueue Client Request {}", request);
    	
        byte[] newBatch = new byte[request.byteSize() + 4];
        ByteBuffer bb = ByteBuffer.wrap(newBatch);
        bb.putInt(request.byteSize());
        request.writeTo(bb);
        fullBatches.add(newBatch);
        proposer.notifyAboutNewBatch();
    }

    public byte[] requestBatch()
    {
        byte[] batch = fullBatches.poll();
        return batch;
    }

    private final static Logger logger = LoggerFactory.getLogger(Batcher.class);
}
