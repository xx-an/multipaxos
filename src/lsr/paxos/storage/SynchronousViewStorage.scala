package lsr.paxos.storage;

/**
 * Represents the storage where the view number is saved to stable storage every
 * time it changes. It is using <code>SingleNumberWriter</code> to read and
 * write the from stable storage.
 */
class SynchronousViewStorage extends StateReplica {
    private final var writer : SingleNumberWriter = _;

    /**
     * Creates new storage with synchronous writes per view change.
     * 
     * @param writer - used to read and write the view number to disc
     */
    def this(writer : SingleNumberWriter) {
        this();
        this.writer = writer;
        this.setView(writer.readNumber().toInt);
    }

    @throws(classOf[IllegalArgumentException])
    def setView(view : Integer) {
        writer.writeNumber(view.toLong);
        this.setView(view);
    }
}
