package lsr.paxos.messages;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import lsr.common.ProcessDescriptor;
import lsr.paxos.storage.ConsensusInstance;

/**
 * Represents the catch-up mechanism response message
 */
public class CatchUpResponse extends Message {
    private static final long serialVersionUID = 1L;

    /**
     * List of all requested instances, which were decided by the sender
     */
    private List<ConsensusInstance> decided;

    /** Forwards the time of request, allowing dynamic timeouts for catch-up */
    private long requestTime;
    private boolean isLastPart = true;

    public CatchUpResponse(int view, long requestTime, List<ConsensusInstance> decided) {
        super(view);
        // Create a copy
        this.decided = new ArrayList<ConsensusInstance>(decided);
        this.requestTime = requestTime;
    }

    public CatchUpResponse(DataInputStream input) throws IOException {
        super(input);
        byte flags = input.readByte();
        isLastPart = (flags & 1) == 0 ? false : true;
        requestTime = input.readLong();

        decided = new Vector<ConsensusInstance>();
        for (int i = input.readInt(); i > 0; --i) {
        	ConsensusInstance newInstance = new ConsensusInstance(ProcessDescriptor.getInstance().localId);
        	newInstance.read(input);
            decided.add(newInstance);
        }
    }

    public void setDecided(List<ConsensusInstance> decided) {
        this.decided = decided;
    }

    public List<ConsensusInstance> getDecided() {
        return decided;
    }

    public void setRequestTime(long requestTime) {
        this.requestTime = requestTime;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public void setLastPart(boolean isLastPart) {
        this.isLastPart = isLastPart;
    }

    public boolean isLastPart() {
        return isLastPart;
    }

    public MessageType getType() {
        return MessageType.CatchUpResponse;
    }

    public int byteSize() {
        int sz = super.byteSize() + 1 + 8 + 4;
        for (ConsensusInstance ci : decided) {
            sz += ci.byteSize();
        }
        return sz;
    }

    public String toString() {
        return "CatchUpResponse" + " (" +
               super.toString() + ") for instances: " + decided.toString() +
               (isLastPart ? " END" : "");
    }

    protected void write(ByteBuffer bb) {
        bb.put((byte) (isLastPart ? 1 : 0));
        bb.putLong(requestTime);
        bb.putInt(decided.size());
        for (ConsensusInstance ci : decided) {
            ci.write(bb);
        }
    }
}
