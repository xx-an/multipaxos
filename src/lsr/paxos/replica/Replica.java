package lsr.paxos.replica;

import static lsr.common.ProcessDescriptor.processDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import lsr.common.Configuration;
import lsr.common.CrashModel;
import lsr.common.ProcessDescriptor;
import lsr.common.Reply;
import lsr.common.Request;
import lsr.common.RequestId;
import lsr.common.SingleThreadDispatcher;
import lsr.paxos.DecideCallback;
import lsr.paxos.Snapshot;
import lsr.paxos.SnapshotProvider;
import lsr.paxos.client.ClientRequestForwarder;
import lsr.paxos.client.ClientRequestManager;
import lsr.paxos.client.ClientManager;
import lsr.paxos.client.InternalClient;
import lsr.paxos.client.NioClientProxy;
import lsr.paxos.core.Paxos;
import lsr.paxos.events.AfterCatchupSnapshotEvent;
import lsr.paxos.recovery.CrashStopRecovery;
import lsr.paxos.recovery.EpochSSRecovery;
import lsr.paxos.recovery.FullSSRecovery;
import lsr.paxos.recovery.RecoveryAlgorithm;
import lsr.paxos.recovery.RecoveryListener;
import lsr.paxos.recovery.ViewSSRecovery;
import lsr.paxos.storage.ConsensusInstance;
import lsr.paxos.storage.LogEntryState;
import lsr.paxos.storage.SingleNumberWriter;
import lsr.paxos.storage.Storage;
import lsr.service.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages replication of a service. Receives requests from the client, orders
 * them using Paxos, executes the ordered requests and sends the reply back to
 * the client.
 * <p>
 * Example of usage:
 * <p>
 * <blockquote>
 * 
 * <pre>
 * public static void main(String[] args) throws IOException {
 *  int localId = Integer.parseInt(args[0]);
 *  Replica replica = new Replica(localId, new MapService());
 *  replica.start();
 * }
 * </pre>
 * 
 * </blockquote>
 * 
 * @author Utkarsh Pandey (VT)
 */
public class Replica {
    /**
     * Represents different crash models.
     */

    private String logPath;

    private Paxos paxos;
    private final ServiceProxy serviceProxy;
    
    private DecideCallback decideCallback;
    private InternalClient intCli = null;
    
    private ClientManager clientManager;
    private ClientRequestManager requestManager;

    /** Next request to be executed. */
    private int executeUB = 0;

    // TODO: JK check if this map is cleared where possible
    /** caches responses for clients */
    private final NavigableMap<Integer, List<Reply>> executedDifference =
            new TreeMap<Integer, List<Reply>>();

    /**
     * For each client, keeps the sequence id of the last request executed from
     * the client.
     * 
     * TODO: the executedRequests map grows and is NEVER cleared!
     * 
     * For theoretical correctness, it must stay so. In practical approach, give
     * me unbounded storage, limit the overall client count or simply let eat
     * some stale client requests.
     * 
     * Bad solution keeping correctness: record time stamp from client, his
     * request will only be valid for 5 minutes, after that time - go away. If
     * client resends it after 5 minutes, we ignore request. If client changes
     * the time stamp, it's already a new request, right? Client with broken
     * clocks will have bad luck.
     */
    private final ConcurrentHashMap<Long, Reply> executedRequests =
            new ConcurrentHashMap<Long, Reply>();

    /** Temporary storage for the instances that finished out of order. */
    private final NavigableMap<Integer, Deque<Request>> decidedWaitingExecution =
            new TreeMap<Integer, Deque<Request>>();

    private final HashMap<Long, Reply> previousSnapshotExecutedRequests = new HashMap<Long, Reply>();

    private final SingleThreadDispatcher dispatcher;
    private final ProcessDescriptor descriptor;

    private SnapshotListener2 innerSnapshotListener2;
    private SnapshotProvider innerSnapshotProvider;
    
    private ClientRequestForwarder requestForwarder;

    /**
     * Initializes new instance of <code>Replica</code> class.
     * <p>
     * This constructor doesn't start the replica and Paxos protocol. In order
     * to run it the {@link #start()} method should be called.
     * 
     * @param config - the configuration of the replica
     * @param localId - the id of replica to create
     * @param service - the state machine to execute request on
     * @throws IOException if an I/O error occurs
     */
    public Replica(Configuration config, int localId, Service service) throws IOException {
    	ProcessDescriptor.initialize(config, localId);
    	descriptor = ProcessDescriptor.getInstance();
    	
    	logPath = descriptor.logPath + '/' + localId;
    	
        innerSnapshotListener2 = new InnerSnapshotListener2();
        innerSnapshotProvider = new InnerSnapshotProvider();

        dispatcher = new SingleThreadDispatcher("Replica");
        
        serviceProxy = new ServiceProxy(service, executedDifference, dispatcher);
        serviceProxy.addSnapshotListener(innerSnapshotListener2);
        	
        executedDifference.put(executeUB, new ArrayList<Reply>(2048));
    }

    /**
     * Starts the replica.
     * <p>
     * First the recovery phase is started and after that the replica joins the
     * Paxos protocol and starts the client manager and the underlying service.
     * 
     * @throws IOException if some I/O error occurs
     */
    public void start() throws IOException {
        logger.info("Recovery phase started.");
        
        dispatcher.start();

        RecoveryAlgorithm recovery = createRecoveryAlgorithm(descriptor.crashModel);
        paxos = recovery.getPaxos();
        
        decideCallback = new DecideCallback(this, executeUB);
        paxos.setDecideCallback(decideCallback);
        
        requestForwarder = new ClientRequestForwarder(paxos);
        requestForwarder.start();

        recovery.addRecoveryListener(new InnerRecoveryListener());
        recovery.start();
    }

    private RecoveryAlgorithm createRecoveryAlgorithm(CrashModel crashModel) throws IOException {
        switch (crashModel) {
            case CrashStop:
                return new CrashStopRecovery(innerSnapshotProvider);
            case FullSS:
                return new FullSSRecovery(innerSnapshotProvider, logPath);
            case EpochSS:
                return new EpochSSRecovery(innerSnapshotProvider, logPath);
            case ViewSS:
                return new ViewSSRecovery(innerSnapshotProvider, new SingleNumberWriter(logPath, "sync.view"));
            default:
                throw new RuntimeException("Unknown crash model: " + crashModel);
        }
    }

    public void forceExit() {
        dispatcher.shutdownNow();
    }

    /**
     * Sets the path to directory where all logs will be saved.
     * 
     * @param path to directory where logs will be saved
     */
    public void setLogPath(String path) {
        logPath = path;
    }

    /**
     * Gets the path to directory where all logs will be saved.
     * 
     * @return path
     */
    public String getLogPath() {
        return logPath;
    }

    /**
     * Processes the requests that were decided but not yet executed.
     */
    private void executeDecided() {
        while (true) {
            Deque<Request> requestByteArray;
            synchronized (decidedWaitingExecution) {
                requestByteArray = decidedWaitingExecution.remove(executeUB);
            }

            if (requestByteArray == null) {
                return;
            }

            assert paxos.getStorage().getLog().getNextId() > executeUB;

            Vector<Reply> cache = new Vector<Reply>();
            executedDifference.put(executeUB, cache);

            for (Request request : requestByteArray) {
                if (request.isNop()) {
                    // TODO: handling a no-op request
                    logger.warn("Executing a nop request. Instance: " + executeUB);
                    serviceProxy.executeNop();

                } else {
                    Integer lastSequenceNumberFromClient = null;
                    Reply lastReply = executedRequests.get(request.getRequestId().getClientId());
                    if (lastReply != null) {
                        lastSequenceNumberFromClient = lastReply.getRequestId().getSeqNumber();
                    }
                    // prevents executing the same request few times.
                    // Do not execute the same request several times.
                    if (lastSequenceNumberFromClient != null &&
                        request.getRequestId().getSeqNumber() <= lastSequenceNumberFromClient) {
                        logger.warn("Request ordered multiple times. Not executing " +
                                       executeUB +
                                       ", " + request);
                        continue;
                    }

                    // Here the replica thread is given to Service.
                    byte[] result = serviceProxy.execute(request);

                    Reply reply = new Reply(request.getRequestId(), result);

                    // add request to executed history
                    cache.add(reply);

                    executedRequests.put(request.getRequestId().getClientId(), reply);
                }
            }

            // batching requests: inform the service that all requests assigned
            serviceProxy.instanceExecuted(executeUB);

            executeUB++;
        }
    }

    /**
     * Listener called after recovery algorithm is finished and paxos can be
     * started.
     */
    private class InnerRecoveryListener implements RecoveryListener {
        public void recoveryFinished() {
            
            if (CrashModel.FullSS.equals(processDescriptor.crashModel))
                paxos.getDispatcher().executeAndWait(new Runnable() {
                    public void run() {
                        recoverReplicaFromStorage();
                    }
                });

            requestManager = new ClientRequestManager(Replica.this, decideCallback,
                executedRequests, requestForwarder, paxos);
            requestForwarder.setClientRequestManager(requestManager);
            
            intCli = new InternalClient(dispatcher, requestManager);

            try {
                NioClientProxy.createIdGenerator(paxos.getStorage());
                clientManager = new ClientManager(requestManager);
                clientManager.start();
            } catch (IOException e) {
                throw new RuntimeException("Could not prepare the socket for clients! Aborting.");
            }

            logger.info(processDescriptor.logMark_Benchmark,
                    "Recovery phase finished. Starting paxos protocol.");
            paxos.startPaxos();

            dispatcher.execute(new Runnable() {
                public void run() {
                    serviceProxy.recoveryFinished();
                }
            });
        }

        private void recoverReplicaFromStorage() {
            Storage storage = paxos.getStorage();

            // we need a read-write copy of the map
            SortedMap<Integer, ConsensusInstance> instances =
                    new TreeMap<Integer, ConsensusInstance>();
            instances.putAll(storage.getLog().getInstanceMap());

            // We take the snapshot
            Snapshot snapshot = storage.getLastSnapshot();
            if (snapshot != null) {
                innerSnapshotProvider.handleSnapshot(snapshot);
                instances = instances.tailMap(snapshot.getNextInstanceId());
            }

            for (ConsensusInstance instance : instances.values()) {
                if (instance.getState() == LogEntryState.DECIDED) {
                	decideCallback.onRequestOrdered(instance.getInst(), instance);
                }
            }
            storage.updateFirstUncommitted();
        }

    }

    private class InnerSnapshotListener2 implements SnapshotListener2 {
        public void onSnapshotMade(final Snapshot snapshot) {
            dispatcher.checkInDispatcher();

            if (snapshot.getValue() == null) {
                throw new RuntimeException("Received a null snapshot!");
            }

            // add header to snapshot
            Map<Long, Reply> requestHistory = new HashMap<Long, Reply>(
                    previousSnapshotExecutedRequests);

            // Get previous snapshot next instance id
            int prevSnapshotNextInstId;
            Snapshot lastSnapshot = paxos.getStorage().getLastSnapshot();
            if (lastSnapshot != null) {
                prevSnapshotNextInstId = lastSnapshot.getNextInstanceId();
            } else {
                prevSnapshotNextInstId = 0;
            }

            // update map to state in moment of snapshot
            for (int i = prevSnapshotNextInstId; i < snapshot.getNextInstanceId(); ++i) {
                List<Reply> ides = executedDifference.remove(i);

                // this is null only when NoOp
                if (ides == null) {
                    continue;
                }

                for (Reply reply : ides) {
                    requestHistory.put(reply.getRequestId().getClientId(), reply);
                }
            }

            snapshot.setLastReplyForClient(requestHistory);

            previousSnapshotExecutedRequests.clear();
            previousSnapshotExecutedRequests.putAll(requestHistory);

            paxos.onSnapshotMade(snapshot);
        }
    }

    private class InnerSnapshotProvider implements SnapshotProvider {
        public void handleSnapshot(final Snapshot snapshot) {
            logger.info("New snapshot received");
            dispatcher.execute(new Runnable() {
                public void run() {
                    handleSnapshotInternal(snapshot);
                }
            });
        }

        public void askForSnapshot() {
            dispatcher.execute(new Runnable() {
                public void run() {
                    serviceProxy.askForSnapshot();
                }
            });
        }

        public void forceSnapshot() {
            dispatcher.execute(new Runnable() {
                public void run() {
                    serviceProxy.forceSnapshot();
                }
            });
        }

        /**
         * Restoring state from a snapshot
         * 
         * @param snapshot
         */
        private void handleSnapshotInternal(Snapshot snapshot) {
            assert dispatcher.amIInDispatcher();
            assert snapshot != null : "Snapshot is null";

            if (snapshot.getNextInstanceId() < executeUB) {
                logger.warn("Received snapshot is older than current state." +
                               snapshot.getNextInstanceId() + ", executeUB: " + executeUB);
                return;
            }

            logger.info("Updating machine state from snapshot." + snapshot);
            serviceProxy.updateToSnapshot(snapshot);
            
            decideCallback.atRestoringStateFromSnapshot(snapshot.getNextInstanceId());

            executedRequests.clear();
            executedDifference.clear();
            executedRequests.putAll(snapshot.getLastReplyForClient());
            previousSnapshotExecutedRequests.clear();
            previousSnapshotExecutedRequests.putAll(snapshot.getLastReplyForClient());
            executeUB = snapshot.getNextInstanceId();
            executedDifference.put(executeUB, new ArrayList<Reply>(2048));

            final Object snapshotLock = new Object();

            synchronized (snapshotLock) {
                AfterCatchupSnapshotEvent event = new AfterCatchupSnapshotEvent(snapshot,
                        paxos.getStorage(), snapshotLock);
                paxos.getDispatcher().dispatch(event);

                try {
                    while (!event.isFinished()) {
                        snapshotLock.wait();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            executeDecided();
        }
    }

	public Paxos getPaxos() {
		return this.paxos;
	}

   /** Called when an instance is NOP, in order to count properly the instances */
    void executeNopInstance(final int nextInstance) {
        logger.warn("Executing a nop request. Instance: {}", executeUB);
    }
    
    /**
     * Adds the request to the set of requests be executed. If called e(A) e(B),
     * the delivery will be either d(A) d(B) or d(B) d(A).
     * 
     * If the replica crashes before the request is delivered, the request may
     * get lost.
     * 
     * @param requestValue - the exact request that will be delivered to the
     *            Service execute method
     * @throws IllegalStateException if the method is called before the recovery
     *             has finished
     */
    public void executeNonFifo(byte[] requestValue) throws IllegalStateException {
        if (intCli == null)
            throw new IllegalStateException(
                    "Request cannot be executed before recovery has finished");
        intCli.executeNonFifo(requestValue);
    }
    
    private void innerInstanceExecuted(final int instance, final Request[] requests) {
        assert executeUB == instance : executeUB + " " + instance;
        // TODO (JK) get rid of unnecessary instance parameter
        logger.info("Instance finished: {}", instance);
        executeUB = instance + 1;
        executedDifference.put(executeUB, new ArrayList<Reply>(2048));
        serviceProxy.instanceExecuted(instance);
    }

    public void instanceExecuted(final int instance, final Request[] requests) {
        dispatcher.executeAndWait(new Runnable() {
            @Override
            public void run() {
                innerInstanceExecuted(instance, requests);
            }
        });
    }
   
   /**
     * Called by the RequestManager when it has the Request that should be
     * executed next.
     * 
     * @param instance
     * @param bInfo
     */
    private void innerExecuteClientBatch(int instance, Request[] requests) {
        assert dispatcher.amIInDispatcher() : "Wrong thread: " +
                                                     Thread.currentThread().getName();

        for (Request cRequest : requests) {
            RequestId rID = cRequest.getRequestId();
            Reply lastReply = executedRequests.get(rID.getClientId());
            if (lastReply != null) {
                int lastSequenceNumberFromClient = lastReply.getRequestId().getSeqNumber();

                // Do not execute the same request several times.
                if (rID.getSeqNumber() <= lastSequenceNumberFromClient) {
                    logger.warn(
                            "Request ordered multiple times. inst: {}, req: {}, lastSequenceNumberFromClient: ",
                            instance, cRequest, lastSequenceNumberFromClient);

                    // (JK) FIXME: investigate if the client could get the
                    // response multiple times here.

                    // Send the cached reply back to the client
                    if (rID.getSeqNumber() == lastSequenceNumberFromClient) {
                        // req manager can be null on fullss disk read
                        if (requestManager != null)
                            requestManager.onRequestExecuted(cRequest, lastReply);
                    }
                    continue;
                }
                // else there is a cached reply, but for a past request only.
            }

            // Executing the request (at last!)
            // Here the replica thread is given to Service.
            byte[] result = serviceProxy.execute(cRequest);

            Reply reply = new Reply(cRequest.getRequestId(), result);

            executedRequests.put(rID.getClientId(), reply);

            // req manager can be null on fullss disk read
            if (requestManager != null)
                requestManager.onRequestExecuted(cRequest, reply);
        }
    }

	public void executeClientBatchAndWait(final int instance, final Request[] requests) {
	   dispatcher.executeAndWait(new Runnable() {
			@Override
			public void run() {
			innerExecuteClientBatch(instance, requests);
			}
		});
	}

	public SingleThreadDispatcher getReplicaDispatcher() {
		return dispatcher;
	}
	
	private final static Logger logger = LoggerFactory.getLogger(Replica.class);
}
