package lsr.paxos.recovery;

import static lsr.common.ProcessDescriptor.processDescriptor;

import java.io.IOException;

import lsr.paxos.SnapshotProvider;
import lsr.paxos.core.Paxos;
import lsr.paxos.storage.FullSSDiscWriter;
import lsr.paxos.storage.Storage;
import lsr.paxos.storage.SynchronousStorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullSSRecovery extends RecoveryAlgorithm {
    private final String logPath;
    private Paxos paxos;

    public FullSSRecovery(SnapshotProvider snapshotProvider,
                          String logPath) throws IOException {
        this.logPath = logPath;
        Storage storage = createStorage();
        paxos = new Paxos(snapshotProvider, storage);

    }

    public void start() throws IOException {
        fireRecoveryListener();
    }

    private Storage createStorage() throws IOException {
        logger.info("Reading log from: " + logPath);
        FullSSDiscWriter writer = new FullSSDiscWriter(logPath);
        Storage storage = new SynchronousStorage(writer);
        if (storage.getView() % processDescriptor.numReplicas == processDescriptor.localId) {
            storage.setView(storage.getView() + 1);
        }
        return storage;
    }

    private final static Logger logger = LoggerFactory.getLogger(FullSSRecovery.class);

    @Override
    public Paxos getPaxos() {
        return paxos;
    }
}
