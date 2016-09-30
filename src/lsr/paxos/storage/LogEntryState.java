package lsr.paxos.storage;

/**
 * Represents possible states of consensus instance.
 */
public enum LogEntryState {
    /**
     * Represents the empty consensus state. There is no information about
     * current view nor value.
     */
    UNKNOWN,
    /**
     * The consensus in this state received the <code>PROPOSE</code> message
     * from the leader but hasn't received the majority of the
     * <code>ACCEPT</code> messages. In this state there is some view and
     * value specified, but they can be changed later.
     */
    KNOWN,
    /**
     * Represents state when {@link lsr.paxos.Learner} received majority of
     * <code>ACCEPT</code> message. In this state the view and value of
     * consensus instance cannot be changed.
     */
    DECIDED
}