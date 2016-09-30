package lsr.paxos.messages;

import static lsr.common.ProcessDescriptor.processDescriptor;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import scala.collection.mutable.ListBuffer;
import scala.runtime.BoxedUnit;

import lsr.paxos.core.MultiPaxosImpl;
import lsr.paxos.core.MPLib.*;
import lsr.paxos.core.MultiPaxosImpl.*;
import lsr.paxos.storage.ConsensusInstance;

public class Send1b extends Message {
   
	private static final long serialVersionUID = 1L;
	
    private final ConsensusInstance[] prepared;
    private final long[] epoch;
   
    /**
     * Creates new <code>PrepareOK</code> message without epoch vector.
     * 
     * @param view - sender view number
     * @param prepared - list of prepared consensus instances
     */
    public Send1b(int view, ConsensusInstance[] prepared) {
        this(view, prepared, new long[0]);
    }

    /**
     * Creates new <code>PrepareOK</code> message with epoch vector.
     * 
     * @param view - sender view number
     * @param prepared - list of prepared consensus instances
     * @param epoch - the epoch vector
     */
    public Send1b(int view, ConsensusInstance[] prepared, long[] epoch) {
        super(view);
        assert epoch != null;
        this.prepared = prepared;
        this.epoch = epoch;
    }
    
    
    /**
     * Creates new <cod>PrepareOK</code> message from serialized stream.
     * 
     * @param input - the input stream with serialized message
     * @throws IOException if I/O error occurs
     */
    public Send1b(DataInputStream input) throws IOException {
        super(input);
        prepared = new ConsensusInstance[input.readInt()];
        for (int i = 0; i < prepared.length; ++i) {
        	ConsensusInstance consensus = new ConsensusInstance(i);
        	consensus.read(input);
            prepared[i] = consensus;
        }

        int epochSize = input.readInt();
        epoch = new long[epochSize];
        for (int i = 0; i < epoch.length; ++i) {
            epoch[i] = input.readLong();
        }
    }
    
    public Send1b(Phase1b<byte[]> msg, long[] epoch) {
    	super(msg.b());
    	scala.collection.immutable.List<consensus_ext<byte[], BoxedUnit>> lastvs = msg.a();
    	int length = lastvs.length();
    	prepared = new ConsensusInstance[length];
    	for(int i = 0; i < length; i++)
    	{
    		ConsensusInstance consensus = new ConsensusInstance(i);
    		consensus.readConsensus(lastvs.apply(i));
            prepared[i] = consensus;
    	}
    	this.epoch = epoch;
    }
    
    public Phase1b<byte[]> getPhase1b() {
    	ListBuffer<consensus_ext<byte[], BoxedUnit>> lastvs = new ListBuffer<consensus_ext<byte[], BoxedUnit>>();
    	int length = prepared.length;
    	int numReplicas = processDescriptor.numReplicas;
    	for(int i = 0; i < length; i++)
    	{
    		lastvs.$plus$eq(prepared[i].getConsensus(numReplicas));
    	}
    	Phase1b<byte[]> msg = new Phase1b<byte[]>(lastvs.toList(), this.view);
    	return msg;
    }

    
    /**
     * Returns prepared list of consensus instances.
     * 
     * @return prepared list of consensus instances.
     */
    public ConsensusInstance[] getPrepared() {
        return prepared;
    }

    /**
     * Returns epoch vector. This value should never be equal to
     * <code>null</code>. If this message doesn't contain epoch vector, then it
     * will be represented as empty array.
     * 
     * @return epoch vector
     */
    public long[] getEpoch() {
        return epoch;
    }

    public MessageType getType() {
        return MessageType.Phase1b;
    }

    public int byteSize() {
        int size = super.byteSize() + 4;
        for (ConsensusInstance ci : prepared) {
            size += ci.byteSize();
        }

        size += epoch.length * 8 + 4;

        return size;
    }

    public String toString() {
        return "Send1b(" + super.toString() + ", values: " + Arrays.toString(getPrepared()) + ")";
    }

    protected void write(ByteBuffer bb) 
    {
    	bb.putInt(prepared.length);
        for (ConsensusInstance ci : prepared) {
            ci.write(bb);
        }
        
        bb.putInt(epoch.length);
        for (int i = 0; i < epoch.length; ++i) {
            bb.putLong(epoch[i]);
        }
    }

        
  }
