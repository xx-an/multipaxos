package lsr.paxos.messages;

/**
 * Represents message type.
 */
public enum MessageType {
	Accept,
	Alive,
	Phase1a,
	Phase1b,
	Propose,
	CatchUpQuery,
	CatchUpResponse,
	
	CatchUpSnapshot,
	Recovery,
	RecoveryAnswer,
    
    ForwardedClientRequests,
        // Special markers used by the network implementation to raise callbacks
    // There are no classes with this messages types
    ANY, // any message
    SENT
    // sent messages

}
