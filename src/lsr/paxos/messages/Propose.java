package lsr.paxos.messages;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import lsr.paxos.core.MultiPaxosImpl.Comd;
import lsr.paxos.core.MultiPaxosImpl.Phase2a;
import lsr.paxos.core.MultiPaxosImpl.cmd;
import lsr.paxos.storage.ConsensusInstance;

/**
 * Represents the <code>Propose</code> message sent by leader to vote on next
 * consensus instance. As every message it contains the view number of sender
 * process and additionally the id of new consensus instance and its value as
 * byte array.
 */
public class Propose extends Message {
    private static final long serialVersionUID = 1L;
    private final byte[] value;
    private final int instanceId;
    private int ballot; 							/* This field is only added for compatibility with HOL generated code */
    /**
     * Creates new <code>Propose</code> message to propose specified instance ID
     * and value.
     * 
     * @param view - sender view number
     * @param instanceId - the ID of instance to propose
     * @param value - the value of the instance
     */
    public Propose(int view, int instanceId, byte[] value) {
        super(view);
        assert value != null;
        this.instanceId = instanceId;
        this.value = value;
        this.ballot = view;
    }

    /**
     * Creates new <code>Propose</code> message from consensus instance. The ID
     * and the value is taken from this object.
     * 
     * @param instance - the consensus instance to propose
     */
    public Propose(ConsensusInstance instance) {
        super(instance.getView());
        instanceId = instance.getInst();
        value = instance.getValue();
        ballot = instance.getView();
    }
    
    public Phase2a<byte[]> getPhase2a()
    {
    	cmd<byte[]> comd = new Comd<byte[]>(value);
    	Phase2a<byte[]> msg = new Phase2a<byte[]>(instanceId, ballot, comd);
        return msg;
    }

    /**
     * Creates new <code>Propose</code> message from serialized input stream.
     * 
     * @param input - the input stream with serialized message
     * @throws IOException if I/O error occurs while deserializing
     */
    public Propose(DataInputStream input) throws IOException {
        super(input);

        instanceId = input.readInt();
        ballot = input.readInt();
        value = new byte[input.readInt()];
        input.readFully(value);
    }

    public void setBallot(int b)
    {
    	ballot = b;
    }
    
    public int getBallot()
    {
    	return ballot;
    }
    /**
     * Returns the ID of proposed instance.
     * 
     * @return the ID of proposed instance
     */
    public int getInstanceId() {
        return instanceId;
    }

    /**
     * Returns value of proposed instance.
     * 
     * @return value of proposed instance
     */
    public byte[] getValue() {
        return value;
    }

    public MessageType getType() {
        return MessageType.Propose;
    }

    public int byteSize() {
        return super.byteSize() + 4 + 4 + 4 + value.length;
    }

    public String toString() {
        return "Propose(" + super.toString() + ", inst: " + getInstanceId() +  ", Ballot: " + ballot + ")" ;
    }

    protected void write(ByteBuffer bb) {
        bb.putInt(instanceId);
        bb.putInt(ballot);
        bb.putInt(value.length);
        bb.put(value);
    }
}
