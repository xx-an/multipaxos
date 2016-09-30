package lsr.paxos.core;

import static lsr.common.ProcessDescriptor.processDescriptor;

import java.io.IOException;
import java.util.BitSet;

import lsr.common.Dispatcher;
import lsr.common.Dispatcher.Priority;
import lsr.common.DispatcherImpl;
import lsr.common.Request;
import lsr.paxos.Batcher;
import lsr.paxos.DecideCallback;
import lsr.paxos.FailureDetector;
import lsr.paxos.core.Learner;
import lsr.paxos.core.Proposer;
import lsr.paxos.Snapshot;
import lsr.paxos.SnapshotMaintainer;
import lsr.paxos.SnapshotProvider;
import lsr.paxos.events.StartProposerEvent;
import lsr.paxos.messages.*;
import lsr.paxos.network.GenericNetwork;
import lsr.paxos.network.MessageHandler;
import lsr.paxos.network.Network;
import lsr.paxos.network.TcpNetwork;
import lsr.paxos.network.UdpNetwork;
import lsr.paxos.storage.ConsensusInstance;
import lsr.paxos.storage.LogEntryState;
import lsr.paxos.storage.Log;
import lsr.paxos.storage.Storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements state machine replication. It keeps a replicated log internally
 * and informs the listener of decisions using callbacks. This implementation is
 * monolithic, in the sense that leader election/view change are integrated on
 * the lsr.paxos protocol.
 * 
 * <p>
 * The first consensus instance is 0. Decisions might not be reached in sequence
 * number order.
 * </p>
 */
public class Paxos {
    
    
  	/**
     * Threading model - This class uses an event-driven threading model. It
     * starts a Dispatcher thread that is responsible for executing the
     * replication protocol and has exclusive access to the internal data
     * structures. The Dispatcher receives work using the pendingEvents queue.
     */
    /**
     * The Dispatcher thread executes the replication protocol. It receives and
     * executes events placed on the pendingEvents queue: messages from other
     * processes or proposals from the local process.
     * 
     * Only this thread is allowed to access the state of the replication
     * protocol. Therefore, there is no need for synchronization when accessing
     * this state. The synchronization is handled by the
     * <code>pendingEvents</code> queue.
     */

	private final CatchUp catchUp;
    private final FailureDetector failureDetector;
    
    private final SnapshotMaintainer snapshotMaintainer;

	private final Dispatcher dispatcher;
    private final Network network;
    private final UdpNetwork udpNetwork;
    
    private Proposer proposer;
    private Acceptor acceptor;
    private Learner learner;

    private DecideCallback decideCallback;
    private Storage storage;
    private final Batcher batcher;
    
    private volatile boolean viewChange = false;
    /**
     * Initializes new instance of {@link PaxosImpl}.
     * 
     * @param decideCallback - the class that should be notified about
     *            decisions.
     * @param snapshotProvider
     * @param storage - the state of the lsr.paxos protocol
     * 
     * @throws IOException if an I/O error occurs
     */
	    
    
    public Paxos(SnapshotProvider snapshotProvider, Storage storage) throws IOException {
    		
    	this.storage = storage;
	
       // Handles the replication protocol and writes messages to the network
       dispatcher = new DispatcherImpl("Dispatcher");

       if (snapshotProvider != null) {
    	   //logger.info("Starting snapshot maintainer");
    	   snapshotMaintainer = new SnapshotMaintainer(this.storage, dispatcher, snapshotProvider);
    	   storage.getLog().addLogListener(snapshotMaintainer);
       } 
       else 
       {
    	   snapshotMaintainer = null;
       }

       // UDPNetwork is always needed because of the failure detector
       this.udpNetwork = new UdpNetwork();
       if (processDescriptor.network.equals("TCP")) 
       {
    	   network = new TcpNetwork();
       } 
       else if (processDescriptor.network.equals("UDP")) 
       {
    	   network = udpNetwork;
       } else if (processDescriptor.network.equals("Generic")) 
       {
    	   TcpNetwork tcp = new TcpNetwork();
    	   network = new GenericNetwork(tcp, udpNetwork);
       } 
       else 
       {
    	   	throw new IllegalArgumentException("Unknown network type: " + processDescriptor.network +
                                      ". Check paxos.properties configuration.");
       }

       failureDetector = new FailureDetector(this, udpNetwork, this.storage);

       // create acceptors and learners
       proposer = new Proposer(this, network, failureDetector, this.storage, processDescriptor.crashModel);
       acceptor = new Acceptor(this, this.storage, network);
       learner = new Learner(this, proposer, this.storage);
       
       catchUp = new CatchUp(snapshotProvider, this, this.storage, network);
       batcher = new Batcher(this);
    }
    
    public void setDecideCallback(DecideCallback decideCallback) {
        this.decideCallback = decideCallback;
    }

    /**
     * Joins this process to the lsr.paxos protocol. The catch-up and failure
     * detector mechanisms are started and message handlers are registered.
     */
    public void startPaxos() {
        MessageHandler handler = new MessageHandlerImpl();
        Network.addMessageListener(MessageType.Alive, handler);
        Network.addMessageListener(MessageType.Phase1a, handler);
        Network.addMessageListener(MessageType.Phase1b, handler);
        Network.addMessageListener(MessageType.Propose, handler);
        Network.addMessageListener(MessageType.Accept, handler);
        Network.addMessageListener(MessageType.CatchUpQuery, handler);
        Network.addMessageListener(MessageType.CatchUpResponse, handler);

        udpNetwork.start();
        network.start();

        failureDetector.start();
        
        batcher.start();

        // Starts the threads on the child modules. Should be done after
        // all the dependencies are established, ie. listeners registered.
        dispatcher.start();
    }
    
    /**
     * Proposes new value to paxos protocol.
     * 
     * This process has to be a leader to call this method. If the process is
     * not a leader, exception is thrown.
     * 
     * @param request - the value to propose
     */
    public void enqueueRequest(Request request) {
        batcher.enqueueClientRequest(request);
    }

    public Batcher getBatcher() {
        return batcher;
    }

    public byte[] requestBatch() {
        return batcher.requestBatch();
    }

    /**
     * Adds {@link StartProposerEvent} to current dispatcher which starts the
     * proposer on current replica.
     */
    public void startProposer() {
        assert proposer.getState() == ProposerState.INACTIVE : "Already in proposer role.";

        logger.info("start proposer event");
        StartProposerEvent event = new StartProposerEvent(proposer);
        if (dispatcher.amIInDispatcher()) {
            event.run();
        } else {
            dispatcher.dispatch(event);
        }
    }

    /**
     * Is this process on the role of leader?
     * 
     * @return <code>true</code> if current process is the leader;
     *         <code>false</code> otherwise
     */
    public boolean isLeader() {
        
    	return getLeaderId() == processDescriptor.localId;

    }
    
    /**
     * Gets the id of the replica which is currently the leader.
     * 
     * @return id of replica which is leader
     */
    public int getLeaderId() {
        return storage.getView() % processDescriptor.numReplicas;
    }

    /**
     * Gets the dispatcher used by lsr.paxos to avoid concurrency in handling
     * events.
     * 
     * @return current dispatcher object
     */
    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public Learner getLearner() {
        return this.learner;
    }
//    /**
//     * Changes state of specified consensus instance to <code>DECIDED</code>.
//     * 
//     * @param instanceId - the id of instance that has been decided
//     */
//    public void decide(ConsensusInstance ci) 
//    {
//        int instanceId = ci.getInst();
//    	
//    	assert dispatcher.amIInDispatcher() : "Incorrect thread: " + Thread.currentThread();
//        //ConsensusInstance ci = storage.getLog().getInstance(instanceId);
//    	
//        assert ci != null : "Deciding on instance already removed from logs";
//        assert ci.getState() != LogEntryState.DECIDED : "Deciding on already decided instance";
//        
//        ci.setDecided();
//        
//        storage.updateFirstUncommitted();
//
//        if (isLeader()) {
//            proposer.stopPropose(instanceId);
//            proposer.proposeNext();
//        } else {
//            // not leader. Should we start the catchup?
//            if (ci.getInst() > storage.getFirstUncommitted() + processDescriptor.windowSize) {
//                	// The last uncommitted value was already decided, since
//                	// the decision just reached is outside the ordering window
//                	// So start catchup.
//            		logger.info("Catchup started in decide function");
//            		catchUp.forceCatchup();
//            	}
//        }
//  }

    /**
     * Changes state of specified consensus instance to <code>DECIDED</code>.
     * 
     * @param instanceId - the id of instance that has been decided
     */
    public void decide(int instanceId) {
        assert dispatcher.amIInDispatcher() : "Incorrect thread: " + Thread.currentThread();

        ConsensusInstance ci = storage.getLog().getInstance(instanceId);
        logger.info(processDescriptor.logMark_OldBenchmark, "Decided on the instance {}", ci.getInst());
        
        assert ci != null : "Deciding on instance already removed from logs";
        //assert ci.getState() != LogEntryState.DECIDED : "Deciding on already decided instance";

        ci.setDecided();

        storage.updateFirstUncommitted();

        if (isLeader()) 
        {
            proposer.stopPropose(instanceId);
            proposer.proposeNext();
        } 
        else 
        {
            // not leader. Should we start the catch-up?
            if (ci.getInst() > storage.getFirstUncommitted() + processDescriptor.windowSize) {
                // The last uncommitted value was already decided, since
                // the decision just reached is outside the ordering window
                // So start catch-up.
            	logger.info("Forcing the replica to catch up");
                catchUp.forceCatchup();
            }
        }

        logger.info("Using decideCallback to execute the command in the instance: " + 
        		ci.getInst() + ", " + ci.getView() + ", " + ci.getState());
        decideCallback.onRequestOrdered(instanceId, ci);
    }

    /**
     * Increases the view of this process to specified value. The new view has
     * to be greater than the current one.
     * 
     * @param newView - the new view number
     */
    public void advanceView(int newView) {
        assert dispatcher.amIInDispatcher();
        assert newView > storage.getView() : "Can't advance to the same or lower view";

        logger.info("Advancing to view " + newView + ", Leader = " +
                    (newView % processDescriptor.numReplicas) + ", original view is = " + storage.getView());

        if (isLeader()) {
        	logger.info("In advanceView, is leader, going to stop proposer");
            proposer.stopProposer();
        }

        storage.setView(newView);
        
        assert !isLeader() : "Cannot advance to a view where process is leader by receiving a message";
        failureDetector.leaderChange(getLeaderId());
    }

    public boolean getViewChange()
    {
    	return viewChange;
    }
    
    public void resetViewChange()
    {
    	viewChange = false;
    }
    // *****************
    // Auxiliary classes
    // *****************
    /**
     * Receives messages from other processes and stores them on the
     * pendingEvents queue for processing by the Dispatcher thread.
     */
    private class MessageHandlerImpl implements MessageHandler {
        public void onMessageReceived(Message msg, int sender) {
            MessageEvent event = new MessageEvent(msg, sender);
            	
            // prioritize Alive messages
            if (msg instanceof Alive) {
                dispatcher.dispatch(event, Priority.High);
            } else {
                dispatcher.dispatch(event);
            }
            //dispatcher.dispatch(event);
        }

        public void onMessageSent(Message message, BitSet destinations) {
            // Empty
        }
    }

    private class MessageEvent implements Runnable {
        private final Message msg;
        private final int sender;

        public MessageEvent(Message msg, int sender) {
            this.msg = msg;
            this.sender = sender;
        }

        public void run() {
            try {
                // The monolithic implementation of Paxos does not need Nack
                // messages because the Alive messages from the failure detector
                // are handled by the Paxos algorithm, so it can advance view
                // when it receives Alive messages. But in the modular
                // implementation, the Paxos algorithm does not use Alive
                // messages,
                // so if a process p is on a lower view and the system is idle,
                // p will remain in the lower view until there is another
                // request
                // to be ordered. The Nacks are required to force the process to
                // advance

                // Ignore any message with a lower view.
                if (msg.getView() < storage.getView()) {
                    return;
                }

                if (msg.getView() > storage.getView()) {
                    assert msg.getType() != MessageType.Phase1b : "Received PrepareOK for view " +
                                                                    msg.getView() +
                                                                    " without having sent a Prepare";
                    advanceView(msg.getView());
                }

                // Invariant for all message handlers: msg.view >= view
                switch (msg.getType()) {
                    case Phase1a:
                    	acceptor.onReceive1a((Send1a) msg, sender);
                    	//System.out.println("Acceptor onReceive 1a called");
                    	break;

                    case Phase1b:
                    	 //System.out.println("Phase 1b msg received, sender is = " + sender);
                    	 if (proposer.getState() == ProposerState.INACTIVE) {
                            //logger.fine("Not in proposer role. Ignoring message");
                    		 logger.info("Not in proposer role. Ignoring message");
                        } else {
                        	proposer.onReceive1b((Send1b)(msg), sender);
                        }
                        break;

                    case Propose:
                    	//System.out.println("Phase 2a msg received, sender is = " + sender);
                        acceptor.onPropose((Propose) msg, sender);
                        if (!storage.isInWindow(((Propose) msg).getInstanceId())) {
                            activateCatchup();
                        }
                        break;

                    case Accept:
                    	//System.out.println("Phase 2b msg received, sender is = " + sender);
                        learner.onAccept((Accept)msg, sender);
                    	///learner.onAccept((Accept) msg, sender);
                        break;
                    
                    case Alive:
                        // The function checkIfCatchUpNeeded also creates
                        // missing logs
                        if (!isLeader() && checkIfCatchUpNeeded(((Alive) msg).getLogSize())) {
                            activateCatchup();
                        }
                        break;
					
                    default:
                        System.out.println("Unknown message type: " + msg);
                }
            } catch (Throwable t) {
                
            	logger.error("Unexpected exception {}", t.toString());
                
            	System.out.println("Paxos received an incorrect paxos mesg");
                
            	java.io.StringWriter sw = new java.io.StringWriter();
            	java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            	t.printStackTrace(pw);
            	System.out.println("Stacktrace is " + sw.toString());
            	
                if(msg == null)
                	System.out.println("Msg is null");
                System.out.println("Mesg = " + msg.toString());
                System.out.println("Mesg type = " + msg.getType() + " Msg class = " + msg.getClass().getName());
                System.exit(1);
            }
        }
        
        
        private boolean checkIfCatchUpNeeded(int logSize) {
            Log log = storage.getLog();

            if (log.getNextId() < logSize) {

                // If we got information, that a newer instance exists, we can
                // create it
                log.getInstance(logSize - 1);
            }

            // We check if all ballots outside the window finished
            int i = storage.getFirstUncommitted();
            for (; i < log.getNextId() - processDescriptor.windowSize; i++) {
                if (log.getInstance(i).getState() != LogEntryState.DECIDED) {
                    return true;
                }
            }
            return false;
        }

        private void activateCatchup() {
        	catchUp.forceCatchup();
        }
    }     

    /**
     * After getting an alive message, we need to check whether we're up to
     * date.
     * 
     * @param logSize - the actual size of the log
     */
    

	public void onSnapshotMade(Snapshot snapshot) {
	    snapshotMaintainer.onSnapshotMade(snapshot);
	}

	public Storage getStorage() {
		return storage;
	}
    
    public Network getNetwork() {
        return network;
    }
   /**
    * Returns the catch-up mechanism used by paxos protocol.
    * 
    * @return the catch-up mechanism
    */
   public CatchUp getCatchup() {
       return catchUp;
   }

   public Proposer getProposer() {
       return proposer;
   }

   private final static Logger logger = LoggerFactory.getLogger(Paxos.class);

}
