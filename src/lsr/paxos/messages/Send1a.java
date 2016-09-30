package lsr.paxos.messages;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import lsr.paxos.core.MultiPaxosImpl.Phase1a;

public class Send1a extends Message {
    private static final long serialVersionUID = 1L;
    private final int firstUncommitted;
    
    /**
     * Creates new <code>Propose</code> message to propose specified instance ID
     * and value.
     * 
     * @param view - sender view number (Will be zero now)
     * @param instanceId - the ID of instance to propose (Will be zero for now)
     * @param value - the value of the instance (Contains the source Replica number)
     */
    public Send1a(int view, int firstUncommitted) {
        super(view);
        //assert value != null;
        this.firstUncommitted = firstUncommitted;
    }
   /* public Send1a(ConsensusInstance instance) {
            super(instance.getView());
            instanceId = instance.getId();
            value = instance.getValue();
        }*/

        /**
         * Creates new <code>Propose</code> message from serialized input stream.
         * 
         * @param input - the input stream with serialized message
         * @throws IOException if I/O error occurs while deserializing
         */
        public Send1a(DataInputStream input) throws IOException {
        	super(input);
        	firstUncommitted = input.readInt();
        }

        /**
         * Returns the ID of proposed instance.
         * 
         * @return the ID of proposed instance
         */
        public int getFirstUncommitted() {
            return firstUncommitted;
        }

        public MessageType getType() {
            return MessageType.Phase1a;
        }
        
        public Phase1a<byte[]> getPhase1a()
        {
            Phase1a<byte[]> msg = new Phase1a<byte[]>(super.view, firstUncommitted);
            return msg;
        }

        public int byteSize() {
            return super.byteSize() + 4;
        }

        public String toString() {
            return "Send1a(" + super.toString() + ", instance : " + firstUncommitted + 
            		" ballot = " + super.view + ")" ;
        }

        protected void write(ByteBuffer bb) {
        	bb.putInt(firstUncommitted);
        }

    
    }
