package lsr.paxos.events;

import lsr.paxos.core.Proposer;

public class StartProposerEvent implements Runnable {
    private Proposer proposer;

    public StartProposerEvent(Proposer proposer) {
        this.proposer = proposer;
    }

    public void run() {
        // logger.fine("Proposer starting.");
        proposer.send1a();
    }
}
