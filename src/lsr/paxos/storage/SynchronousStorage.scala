package lsr.paxos.storage;

import java.io.IOException;

import lsr.paxos.Snapshot;

/**
 * Implementation of <code>Storage</code> interface. This implementation is
 * using <code>DiscWriter</code> to save view number and last snapshot to disc.
 * It is also using synchronous version of log - <code>SynchronousLog</code>.
 * 
 * @see SynchronousLog
 * @see DiscWriter
 */
class SynchronousStorage extends StateReplica {
    private final var writer : DiscWriter = _;

    /**
     * Initializes new instance of <code>SynchronousStorage</code> class.
     * 
     * @param writer - the disc writer
     * @throws IOException if I/O error occurs
     */
    @throws(classOf[IOException])
    def this(writer : DiscWriter){
        this();
        this.setView(writer.loadViewNumber());
        this.writer = writer;

        // synchronous log reads the previous log files
        this.setLog(new SynchronousLog(writer));

        val snapshot : Snapshot = this.writer.getSnapshot();
        if (snapshot != null) {
            super.setLastSnapshot(snapshot);
        }
    }

    def setView(view : Integer) {
        writer.changeViewNumber(view);
        super.setView(view);
    }

    override def setLastSnapshot(snapshot : Snapshot) {
        writer.newSnapshot(snapshot);
        super.setLastSnapshot(snapshot);
    }
}
