package lsr.paxos;

import java.util.BitSet;

import lsr.common.ProcessDescriptor;
import lsr.paxos.messages.Message;

/**
 * Use the <code>Retransmitter</code> class and retransmits only to processes
 * that <code>PrepareOk</code> response has not been received.
 */
public class Transmitter {
    private final Retransmitter retransmitter;
    private RetransmittedMessage prepareRetransmitter;
    private BitSet prepared = new BitSet();

    public Transmitter(Retransmitter retransmitter) {
        this.retransmitter = retransmitter;
    }

    public void startTransmitting(Message message, BitSet acceptor) {
        prepared.clear();
        prepareRetransmitter = retransmitter.startTransmitting(message, acceptor);
    }

    public void stop() {
        prepareRetransmitter.stop();
    }

    public void update(int sender) {
        prepared.set(sender);
        prepareRetransmitter.stop(sender);
    }

    public boolean isMajority() {
        return prepared.cardinality() > ProcessDescriptor.getInstance().numReplicas / 2;
    }
}