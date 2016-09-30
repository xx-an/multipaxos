package lsr.paxos.core;

import java.util.BitSet
import java.util.concurrent.TimeUnit

import lsr.paxos.core.MPLib._
import lsr.paxos.core.MultiPaxosImpl._
import lsr.paxos.network.TcpNetwork
import lsr.paxos.replica._
import lsr.paxos.storage._
import lsr.paxos.network._
import lsr.paxos.messages._
import lsr.paxos.storage.ConsensusInstance
import lsr.common.ProcessDescriptor
import lsr.common.Request
import lsr.paxos.storage.Storage

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Acceptor ( paxos : Paxos, storage: Storage, network : Network)
{
  var startRead : Long = 0
  var endRead : Long = 0
  
  @volatile var instanceCnt : Long = 0
  @volatile var timesum : Long = 0;
  @volatile var lastInstance : Long = 0;
  @volatile var descriptor = ProcessDescriptor.getInstance();
  
  def onReceive1a(message : Send1a, sender : Integer) : Unit =
  {
      try
      {
        val s : state_ext[Array[Byte], Unit] = storage.getCurrState();
  
        val log : Log = storage.getLog()
        
        val firstUncommitted = message.getFirstUncommitted;
        
        if (firstUncommitted < log.getLowestAvailableId()) {
            
            logger.info("Started proposer from acceptor: firstUncommitted < log.getLowestAvailableId()")
             // We're MUCH MORE up-to-date than the replica that sent Prepare
            paxos.startProposer()
            return;
        }
        
        val repId: Integer = ProcessDescriptor.getInstance().localId;
        
        logger.info("onReceive1a, sender = " + sender + ", message = " + message.getPhase1a.toString() + 
            ", firstUncommitted = " + firstUncommitted)

        val (state, packlist) = processExternalEvent[Array[Byte]](sender, message.getPhase1a, s)

        /* Update the current state */
        storage.updateState(state)
        
        logger.info("onReceive1a, packlist = " + packlist);
     
        if(!packlist.equals(List()))
        {
          val pack:packet[Array[Byte]] = packlist.head
          val (source, dest, msg) = getFields[Array[Byte]](pack)
          
          msg match {
             case Phase1b(last_vote, b) => {
               /* At present the list returned from receive1b is not being used for log maintainence, maybe later */
          
              val msg1b:Send1b = new Send1b(Phase1b(last_vote, b), storage.getEpoch())
                         
             //logger.info("Sending " + m);
              network.sendMessage(msg1b, sender);
              logger.info("Replica  " + repId + " sending 1b msg to " + sender + ", Msg = " + msg1b.toString)
             }
             case _ => {
                 logger.warn("Message Type is incorrect."); 
                 return;
                 }
          }
        }
     
      }
      catch
      {
        case e : Exception => logger.warn("Exception in Receive 1a")
      }
  }
  
  /**
 	* Accepts proposals higher or equal than the current view.
  * 
  * @param message - received propose message
  * @param sender - the id of replica that send the message
  */

   def onPropose(message : Propose, sender : Integer) : Unit =
  {
       assert(message.getView() == storage.getView(), "Msg.view: " + message.getView() +
                                                        ", view: " + storage.getView());

       /* Get the current state */ 
        val s : state_ext[Array[Byte],Unit] = storage.getCurrState
        
        val ballot = message.getBallot()
        val inst = message.getInstanceId()
  
        // leader will not send the accept message;
        if (!paxos.isLeader()) 
        {
            startRead = System.nanoTime
              
              /* Recreate the Scala state message */
            val scmsg = new Phase2a[Array[Byte]](inst, ballot, Comd(message.getValue))
            logger.info("The received 2a message: " + scmsg.toString());
              
            val repId = descriptor.localId
              
              /* Call the state change HOL generated function */
            val (state, packetList) = processExternalEvent[Array[Byte]](sender, scmsg, s)
              
              //val y = lsr.paxos.MultiPaxos4.last_decision(state)
              
            if(packetList.equals(List()))
            {
                /* This should not happen */
                logger.info("On Receive 2a returned an empty set");
                return;
            }
            endRead = System.nanoTime
            
            /* Update the current state */
            storage.updateState(state) 

            /* Check for catchup */
            val firstUncommitted = def_getFirstUncommitted(state)
            val wndSize = descriptor.windowSize;
            if (firstUncommitted + (wndSize * 3) < message.getInstanceId()) {
                // the instance is so new that we must be out of date.
                paxos.getCatchup().forceCatchup();
            }
            /* Create the Accept (Phase 2b) message */
            val sendMsg : Accept = new Accept(message.getView, message.getInstanceId, message.getBallot, message.getValue)
            
            network.sendToOthers(sendMsg);
        }

        // Check if we can decide (n<=3 or if some accepts overtook propose)
        if (storage.getLog.getInstance(inst).isMajority(descriptor.numReplicas)) {
            paxos.decide(inst);
        }
      
    
        timesum += (endRead - startRead)/1000
        instanceCnt = instanceCnt + 1
  }

  def getReceive2a_Avge : Long = 
  {
    if(instanceCnt == lastInstance)
    {
       -1
    }
    else
    {
      
      val currInstance = instanceCnt - lastInstance
      val ret = timesum/currInstance
      lastInstance = instanceCnt
      timesum = 0
      ret
    }
  }
  
  def getReceive2a_Count : Long = 
  {
    instanceCnt - lastInstance
  }
  
  private final var logger : Logger = LoggerFactory.getLogger(classOf[Acceptor]);
} 