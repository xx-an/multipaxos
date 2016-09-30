package lsr.paxos.core;

import collection.mutable._

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import lsr.common.Dispatcher.Priority
import lsr.common.ProcessDescriptor
import lsr.common.Request
import lsr.paxos.messages.Message
import lsr.paxos.messages.Send1a
import lsr.paxos.messages.Send1b
import lsr.paxos.messages.Propose;
import lsr.paxos.network.Network;
import lsr.common.CrashModel;
import lsr.paxos.FailureDetector;
import lsr.paxos.storage.ConsensusInstance;
import lsr.paxos.storage.Log;
import lsr.paxos.storage.Storage;
import lsr.paxos.storage.LogEntryState;
import lsr.paxos.core.MPLib._;
import lsr.paxos.core.MultiPaxosImpl._;
import lsr.paxos.Retransmitter;
import lsr.paxos.RetransmittedMessage;
import lsr.paxos.Transmitter;
import lsr.paxos.client.ClientRequestManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Proposer (paxos : Paxos, network : Network ,failureDetector : FailureDetector, storage : Storage, crashModel : CrashModel) 
{
    val descriptor = ProcessDescriptor.getInstance();
    var propState : ProposerState = ProposerState.INACTIVE
    
    @volatile var startRead : Long = 0
    @volatile var endRead : Long = 0
    @volatile var instanceCnt : Long = 0
    @volatile var timesum : Long = 0;
    @volatile var lastInstance : Long = 0;

    /** Keeps track of the processes that have prepared for this view */
    private final var retransmitter : Retransmitter = new Retransmitter(network, "Retransmitter");
    
        /** retransmitted propose messages for instances */
    private final var proposeRetransmitters : Map[Integer, RetransmittedMessage]  =
        new HashMap[Integer, RetransmittedMessage]();
    
    /** retransmitted message for prepare request */
    private var transmitter : Transmitter = new Transmitter(retransmitter);
    
    def getState : ProposerState = propState
    
    def start() {
        retransmitter.init();
    }

    /**
         * If previous leader is suspected this procedure is executed. We're
         * changing the view (variable indicating order of the leaders in time)
         * accordingly, and we're sending the Send1a message.
         * 
         */
    def send1a : Unit = 
    {
        propState = ProposerState.PREPARING;
        
        val s : state_ext[Array[Byte], Unit] = storage.getCurrState
        
        val repId: Integer = descriptor.localId
        
        val (state, packlist) = def_send1a[Array[Byte]](s)
        
        if(packlist.equals(List()))
        {
            logger.info("Send 1a set is empty")
            return;
        }
          
        val pack:packet[Array[Byte]] = packlist.head
        val (source, dest, msg) = getFields[Array[Byte]](pack)
        
        msg match {
           case Phase1a(ballot, firstUncommitted) => {
             /* At present the list returned from receive1b is not being used for log maintainence, maybe later */
        
               /* Update the current state */
               storage.updateState(state)    
               //println("Send1a, state updated , Exiting");
              
              /*  Storage view is changed, it is time to trigger failureDetector.leaderChange */
              failureDetector.leaderChange(repId);
              
              logger.info("Send1a msg sent by replica " + repId + " Ballot = " + ballot);
               
              /* Send packets to network
               * Now, we are going to use the instance number from the Msg1a.
               */
              val prepare: Send1a = new Send1a(ballot, firstUncommitted);

              transmitter.startTransmitting(prepare, Network.OTHERS);

              // tell that local process is already prepared
              transmitter.update(repId);
           }
           case _ => {
               println("Message Type is incorrect."); 
               return;
           }
        }
    }
    
    def onReceive1b(message : Send1b, sender : Integer) : Unit =
    {
         /*Get current state */
         val s : state_ext[Array[Byte],Unit] = storage.getCurrState

         val msg : Phase1b[Array[Byte]] = message.getPhase1b()
         val (state, packetList) = processExternalEvent[Array[Byte]](sender, msg, s)
         
         /* Update the state */
         //storage.updateState(state)
        
         logger.info("Received Phase1b msg from sender " + sender + " msg = " + msg.toString) 
         if (propState == ProposerState.PREPARED) 
         {
              return;
         }

         updateLogFromPrepareOk(message);
         transmitter.update(sender);
         
          if(transmitter.isMajority())
          {
              logger.info("Majority acquired, next instance = " + storage.getLog().getNextId())
              stopPreparingStartProposing(packetList)
          }
    }
    
    def stopPreparingStartProposing (prepList : List[packet[Array[Byte]]]) : Unit =
    {
        transmitter.stop();

        //prepareRetransmitter.stop();
        propState = ProposerState.PREPARED;
        logger.info("stop Preparing Start Proposing")
        //logger.info("View prepared " + storage.getView());
        //ReplicaStats.getInstance().advanceView(storage.getView());
      
        // Send a proposal for all instances that were not decided.
        val log : Log = storage.getLog();
       
//        /* Not handling the NOP case here, but will do it once we are able to identify the condition */
//        for(pack <- prepList)
//        {
//            val (src, dest, msg) = getFields(pack)
//            msg match {
//              case Phase2a(inst, bal, cmd) => {
//                  val value = getCmdVal(cmd)
//                  val log = storage.getLog;
//                  val instance :  ConsensusInstance = log.append(storage.getView(), value, inst);
//                      
//                  val propmsg: Propose = new Propose(instance)
//                  propmsg.setBallot(bal)
//                  println("In stopprepstartpropose, going to send mesg " + propmsg.toString() + " Destination is " + dest)
//                  network.sendMessage(propmsg, dest)
//              }
//              case _ => {
//                  println("Message Type is incorrect."); 
//                  return;
//              }
//            }
//        }

        logger.info("firstUncommitted: {}; nextInstance: {}", storage.getFirstUncommitted(), log.getNextId);
        for (i <- storage.getFirstUncommitted() until log.getNextId()) {
            var instance : ConsensusInstance = log.getInstance(i);
            assert(instance != null);
            (instance.getState()) match {
                case LogEntryState.DECIDED => {}
                case LogEntryState.KNOWN => {
                    // No decision, but some value is known
                    logger.info("Proposing value from previous view: {}", instance);
                    instance.setView(storage.getView());
                    continueProposal(instance);}
                case LogEntryState.UNKNOWN => {
                    assert(instance.getValue() == null, "Unknow instance has value");
                    logger.warn("No value locked for instance {}: proposing no-op", i);
                    fillWithNoOperation(instance)}
            }
        }
        
        proposeNext();
    }
    
    private def updateLogFromPrepareOk(message : Send1b) {
        if (message.getPrepared() == null) {
            return;
        }

        // Update the local log with the data sent by this process
        for (ci <- message.getPrepared()) {
            val localLog = storage.getLog().getInstance(ci.getInst());
            // Happens if previous PrepareOK caused a snapshot execution
            if (localLog != null) {
                if (localLog.getState() != LogEntryState.DECIDED) {
                    ci.getState() match {
                        case LogEntryState.DECIDED => {
                                localLog.updateStateFromDecision(ci.getView(), ci.getValue());
                                paxos.decide(ci.getInst());
                            } 
                        case LogEntryState.KNOWN =>
                                localLog.updateStateFromKnown(ci.getView(), ci.getValue());
                        case LogEntryState.UNKNOWN =>
                            logger.debug("Ignoring: {}", ci);
                    }
                }
            }
        }

    }
    
    def proposeNext() : Unit = {
        while (!storage.isWindowFull()) {
            val proposal = paxos.requestBatch();
            if (proposal == null)
            {
                logger.info("The new proposal is null, stop proposing");
                return;
            }
            logger.info("The new proposal is {}", proposal);
            propose(proposal);
        }
    }
     
     /**
     * Asks the proposer to propose the given value. If there are currently too
     * many active propositions, this proposal will be enqueued until there are
     * available slots. If the proposer is <code>INACTIVE</code>, then message
     * is discarded. Otherwise value is added to list of active proposals.
     * 
     * @param value - the value to propose
     */
    def propose(value : Array[Byte]) : Unit = 
    {
        assert(paxos.getDispatcher().amIInDispatcher());
        if (propState != ProposerState.PREPARED) {
            /*
             * This can happen if there is a Propose event queued on the
             * Dispatcher when the view changes.
             */
            logger.warn("Cannot propose in INACTIVE or PREPARING state. Discarding batch");
            return;
        }

        logger.info(descriptor.logMark_OldBenchmark, "Proposing for instance: " +
            storage.getLog().getNextId() + ", view: " + storage.getView() + ", value: " + value);

        var instance : ConsensusInstance = storage.getLog().append(storage.getView(), value);

        // Mark the instance as accepted locally
        instance.getAccepts().set(descriptor.localId);
        if (instance.isMajority(descriptor.numReplicas)) {
            logger.warn("Either you use one replica only (what for?) or something is very wrong.");
            paxos.decide(instance.getInst());
        }

        val msg = retransmitter.startTransmitting(new Propose(instance));
        proposeRetransmitters.put(instance.getInst(), msg);
    }
     
     /**
     * After becoming the leader we need to take control over the consensus for
     * orphaned instances. This method activates retransmission of propose
     * messages for instances, which we already have in our logs (
     * {@link sendNextProposal} and {@link Propose} create a new instance)
     * 
     * @param instance instance we want to revoke
     */
    private def continueProposal(instance : ConsensusInstance) {
        assert(propState == ProposerState.PREPARED);
        assert(proposeRetransmitters.containsKey(instance.getInst()) == false, "Different proposal for the same instance");

        // TODO: current implementation causes temporary window size violation.
        val m : Message = new Propose(instance);

        // Mark the instance as accepted locally
        instance.getAccepts().set(descriptor.localId);

        val msg = retransmitter.startTransmitting(m);
        proposeRetransmitters.put(instance.getInst(), msg);
    }
    
    private def fillWithNoOperation(instance : ConsensusInstance ) {
        var bb = ByteBuffer.allocate(4 + 4 + 4);
        bb.putInt(1); // Size of batch
        bb.putInt(-1);
        bb.putInt(-1);// NOP request
        instance.updateStateFromKnown(storage.getView(), bb.array());
        continueProposal(instance);
    }

     
     /**
     * After reception of majority accepts, we suppress propose messages.
     * 
     * @param instanceId no. of instance, for which we want to stop
     *            retransmission
     */
    def stopPropose(instanceId : Integer) : Unit = 
    {
        val r = proposeRetransmitters.remove(instanceId);
        if (r != null) {
            r.stop();
        }
    }
    
    /**
     * If retransmission to some process for certain instance is no longer
     * needed, we should stop it
     * 
     * @param instanceId no. of instance, for which we want to stop
     *            retransmission
     * @param destination number of the process in processes PID list
     */
    def stopPropose(instanceId : Integer, destination : Integer) {
        assert(proposeRetransmitters.containsKey(instanceId));
        proposeRetransmitters.get(instanceId).stop(destination);
    }
    
     /**
     * As the process looses leadership, it must stop all message retransmission
     * - that is either prepare or propose messages.
     */
    def stopProposer : Unit = 
    {
        logger.info("Proposer stops proposing")

        propState = ProposerState.INACTIVE
        transmitter.stop();
        retransmitter.stopAll();
        proposeRetransmitters.clear();
    }
     
    def getPropose_Avge : Long = 
    {
        if(instanceCnt == lastInstance) -1
        else
        {
            val currInstance = instanceCnt - lastInstance
            val ret = timesum/currInstance
            lastInstance = instanceCnt
            timesum = 0
            ret
        }
    }
    
    def getPropose_Count : Long = 
    {
        instanceCnt - lastInstance
    }
    
    def notifyAboutNewBatch() : Unit =
    {
        // Called from batcher thread
        paxos.getDispatcher().dispatch(new Runnable() {
            def run() : Unit = {
                proposeNext();
            }
        });
    }
 
    private final var logger : Logger = LoggerFactory.getLogger(classOf[Proposer]);
}    