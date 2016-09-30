package lsr.paxos.storage

import java.util.BitSet;

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

import lsr.paxos.core.MPLib._
import lsr.paxos.core.MultiPaxosImpl._
import lsr.common.ProcessDescriptor
import lsr.paxos.Snapshot
import lsr.common.CrashModel

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* This class contains the state and scala part of the replica code 
 * An field of this type is provided in the Replica.java class */

class StateReplica() extends Storage 
{
    // Must be volatile because it is read by other threads
    // other than the Protocol thread without locking.
    @volatile protected var view : Integer = 0;
    @volatile protected var firstUncommitted : Integer = 0;
    
    protected var log : Log = new Log();
    
    private var lastSnapshot : Snapshot = new Snapshot();
    
    private var viewChangeListeners = new ArrayBuffer[Storage.ViewChangeListener]();
    
    // must be non-null for proper serialization - NOPping otherwise
    private var epoch : Array[Long]  = new Array[Long](0);
    
    /* Initialize the state */ 
    //val acceptors = getAcceptors(0 until NumReplica, lsr.paxos.FSet.bot_fset)
    private val descriptor = ProcessDescriptor.getInstance();
    val winSize = descriptor.windowSize 
    
    def getCurrState() : state_ext[Array[Byte], scala.Unit] =
    {
        val instances = log.getInstances();
        val first = log.getLowestAvailableId();
        val nextID = log.getNextId();
        var insts = emptyInstances[Array[Byte]];
        val id = descriptor.localId;
        val numReplicas = descriptor.numReplicas;
        for (i <- first until nextID) {
            val instance = instances.get(i);
            val consensus = instance.getConsensus(numReplicas);
            if (instance != null) {
                insts = addInstance(i, consensus, insts);
            }
        }
        val obs = emptyOBS[Array[Byte]];
        val state = state_exta[Array[Byte], scala.Unit](id, descriptor.isLocalProcessLeader(view), accs(numReplicas), view, firstUncommitted, obs, nextID, insts, ());
        return state;
    }
    
    def updateState(state : state_ext[Array[Byte], Unit])
    {
        view = def_getBallot(state);
        firstUncommitted = def_getFirstUncommitted(state);
        log.nextId = def_getNextInstance(state);
        
        val insts = def_getInstances[Array[Byte],Unit](state);
        val ist = def_getInsts(insts);
        
        val instances = log.getInstances();
        val first = log.getLowestAvailableId();
        for (i <- ist) {
            val instance = instances.get(i);
            val newinstance = finfun_apply(insts, i);
            if (instance != null) {
                instance.readConsensus(newinstance);
            }
            else
            {
              var nInstance = new ConsensusInstance(i);
              nInstance.readConsensus(newinstance)
              instances.put(i, nInstance);
            }
        }
    }
    
    def getAcceptors (range : Range, list : List[Integer]): List[Integer] = {
      if(range.isEmpty)
      {
        list
      }
      else
      {
        val i = range.head
        getAcceptors(range.tail, i :: list)
      }
    }
    
    def get_next_instance : Integer = 
    {
      val natInstanceId = getLog().getNextId
      natInstanceId
    }
    
    /**
     * Initializes new instance of <code>InMemoryStorage</code> class with
     * specified log.
     * 
     * @param log - the initial content of the log
     */
    def this(log : Log) {
        this();
        this.log = log;
    }
    
    def getLog() : Log = {
        return log;
    }
    
    def setLog(log : Log) {
        this.log = log;
    }
    
    def getLastSnapshot() : Snapshot = {
        return lastSnapshot;
    }
    
    def setLastSnapshot(snapshot : Snapshot) =  {
        assert(lastSnapshot == null || lastSnapshot.compareTo(snapshot) <= 0);
        lastSnapshot = snapshot;
    }
    
    def getView() : Int = {
        return view;
    }
    
    @throws(classOf[IllegalArgumentException])
    def setView(view : Int) = {
        if (view <= this.view) {
            throw new IllegalArgumentException("Cannot set smaller or equal view.");
        }
        this.view = view;
    }
    
    def getFirstUncommitted() : Int = {
        return firstUncommitted;
    }
    
    def updateFirstUncommitted() = {
        if (lastSnapshot != null) {
            firstUncommitted = Math.max(firstUncommitted, lastSnapshot.getNextInstanceId());
        }

        val logs = log.getInstanceMap();
        while (firstUncommitted < log.getNextId() && logs.get(firstUncommitted).getState() == LogEntryState.DECIDED) {
            firstUncommitted += 1;
        }
    }
    
    def getAcceptors() : BitSet = {
      var acceptors : BitSet = new BitSet();
      acceptors.set(0, descriptor.numReplicas);
      return acceptors;
    }
    
    def getEpoch() : Array[Long] = {
        return epoch;
    }
    
    def setEpoch(epoch : Array[Long]) = {
        this.epoch = epoch;
    }
    
    def updateEpoch(epoch : Array[Long]) {
        if (epoch.length != this.epoch.length) {
            throw new IllegalArgumentException("Incorrect epoch length");
        }
        var i = 0;
      
        for (i <- 0 until epoch.length)
        {
            this.epoch(i) = Math.max(this.epoch(i), epoch(i));
        }
    }
    
    def updateEpoch(newEpoch : Long, id : Int) = {
        if (id >= epoch.length) {
            throw new IllegalArgumentException("Incorrect id");
        }
    
        epoch(id) = Math.max(epoch(id), newEpoch);
    }
    
    def isInWindow(instanceId : Int) : Boolean = {
        return instanceId < firstUncommitted + winSize;
    }
    
    def getWindowUsed() : Int = {
        return getLog().getNextId() - getFirstUncommitted();
    }
    
    def isWindowFull() : Boolean = {
        return getWindowUsed() == winSize;
    }
    
    def isIdle() : Boolean = {
        return getLog().nextId == firstUncommitted;
    }
    
    def addViewChangeListener(l : Storage.ViewChangeListener) : Unit = {
        if (viewChangeListeners.contains(l))
            return;
        viewChangeListeners += l;
    }
    
    def removeViewChangeListener(l : Storage.ViewChangeListener) : Unit = {
        viewChangeListeners -= l;
    }
    
    protected def fireViewChangeListeners() = {
        for (l <- viewChangeListeners)
            l.viewChanged(view, descriptor.getLeaderOfView(view));
    }
    
    def getRunUniqueId() : Long = {
        var base : Long = 0;
        base = descriptor.crashModel match{
            case CrashModel.FullSS => getEpoch()(0);
            case CrashModel.ViewSS => getView().toLong;
            case CrashModel.EpochSS => getEpoch()(descriptor.localId);
            case CrashModel.CrashStop => base;
            case _ => throw new RuntimeException();
        }
        return base;
    }
    
    private final var logger : Logger = LoggerFactory.getLogger(classOf[StateReplica]);
}