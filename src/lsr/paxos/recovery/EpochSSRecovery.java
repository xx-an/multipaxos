package lsr.paxos.recovery;

import static lsr.common.ProcessDescriptor.processDescriptor;

import java.io.IOException;
import java.util.BitSet;

import lsr.common.Dispatcher;
import lsr.paxos.RetransmittedMessage;
import lsr.paxos.Retransmitter;
import lsr.paxos.SnapshotProvider;
import lsr.paxos.core.Paxos;
import lsr.paxos.messages.Message;
import lsr.paxos.messages.MessageType;
import lsr.paxos.messages.Recovery;
import lsr.paxos.messages.RecoveryAnswer;
import lsr.paxos.network.MessageHandler;
import lsr.paxos.network.Network;
import lsr.paxos.storage.SingleNumberWriter;
import lsr.paxos.storage.StateReplica;
import lsr.paxos.storage.Storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EpochSSRecovery extends RecoveryAlgorithm implements Runnable {
    private static final String EPOCH_FILE_NAME = "sync.epoch";

    private Storage storage;
    private Paxos paxos;
    private RetransmittedMessage recoveryRetransmitter;
    private Retransmitter retransmitter;
    private Dispatcher dispatcher;
    private SingleNumberWriter epochFile;

    private long localEpochNumber;

    private int localId;
    private int numReplicas;

    public EpochSSRecovery(SnapshotProvider snapshotProvider, 
                           String logPath)
            throws IOException {
        epochFile = new SingleNumberWriter(logPath, EPOCH_FILE_NAME);
        localId = processDescriptor.localId;
        numReplicas = processDescriptor.numReplicas;
        storage = createStorage();
        paxos = new Paxos(snapshotProvider, storage);
        dispatcher = paxos.getDispatcher();
    }

    public void start() {
        dispatcher.dispatch(this);
    }

    public void run() {
        // do not execute recovery mechanism on first run
        localEpochNumber = storage.getEpoch()[localId];
        if (localEpochNumber == 1) {
            onRecoveryFinished();
            return;
        }

        retransmitter = new Retransmitter(paxos.getNetwork(), "EpochSSRecoveryRetransmitter");
        retransmitter.init();
        logger.info("Sending recovery message");
        Network.addMessageListener(MessageType.RecoveryAnswer, new RecoveryAnswerListener());
        recoveryRetransmitter = retransmitter.startTransmitting(new Recovery(-1, localEpochNumber));
    }

    private Storage createStorage() throws IOException {
        Storage storage = new StateReplica();
        if (storage.getView() % numReplicas == localId) {
            storage.setView(storage.getView() + 1);
        }

        long[] epoch = new long[numReplicas];
        epoch[localId] = epochFile.readNumber() + 1;
        epochFile.writeNumber(epoch[localId]);

        storage.setEpoch(epoch);

        return storage;
    }

    // Get all instances before <code>nextId</code>
    private void startCatchup(final int nextId) {
        /*new RecoveryCatchUp(paxos.getCatchup(), storage).recover(nextId, new Runnable() {
            public void run() {
                onRecoveryFinished();
            }
        });*/
    }

    private void onRecoveryFinished() {
        fireRecoveryListener();
        Network.addMessageListener(MessageType.Recovery, new EpochRecoveryRequestHandler(paxos));
    }

    private class RecoveryAnswerListener implements MessageHandler {
        private BitSet received;
        private RecoveryAnswer answerFromLeader = null;

        public RecoveryAnswerListener() {
            received = new BitSet(numReplicas);
        }

        public void onMessageReceived(Message msg, final int sender) {
            assert msg.getType() == MessageType.RecoveryAnswer;
            final RecoveryAnswer recoveryAnswer = (RecoveryAnswer) msg;
            assert recoveryAnswer.getEpoch().length == storage.getEpoch().length;

            // drop message if came from previous recovery
            if (recoveryAnswer.getEpoch()[localId] != localEpochNumber) {
                return;
            }

            logger.info("Got a recovery answer " + recoveryAnswer +
                        (recoveryAnswer.getView() % numReplicas == sender ? " from leader" : ""));

            dispatcher.dispatch(new Runnable() {
                public void run() {
                    // update epoch vector
                    storage.updateEpoch(recoveryAnswer.getEpoch());
                    recoveryRetransmitter.stop(sender);
                    received.set(sender);

                    // update view
                    if (storage.getView() < recoveryAnswer.getView()) {
                        storage.setView(recoveryAnswer.getView());
                        answerFromLeader = null;
                    }

                    if (storage.getView() % numReplicas == sender) {
                        answerFromLeader = recoveryAnswer;
                    }

                    if (received.cardinality() > numReplicas / 2) {
                        onCardinality();
                    }
                }
            });
        }

        private void onCardinality() {
            recoveryRetransmitter.stop();
            recoveryRetransmitter = null;

            if (answerFromLeader == null) {
                Recovery recovery = new Recovery(-1, localEpochNumber);
                recoveryRetransmitter = retransmitter.startTransmitting(recovery);
            } else {
                startCatchup((int) answerFromLeader.getNextId());
                Network.removeMessageListener(MessageType.RecoveryAnswer, this);
            }
        }

        public void onMessageSent(Message message, BitSet destinations) {
        }
    }

    public Paxos getPaxos() {
        return paxos;
    }

    private static final Logger logger = LoggerFactory.getLogger(EpochSSRecovery.class);
}
