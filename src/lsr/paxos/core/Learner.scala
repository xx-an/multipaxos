package lsr.paxos.core

import lsr.common.ProcessDescriptor
import lsr.paxos.messages.Accept
import lsr.paxos.storage.Storage

import lsr.paxos.core.MPLib._
import lsr.paxos.core.MultiPaxosImpl._
import lsr.paxos.replica._
import lsr.paxos.storage.StateReplica

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Learner (paxos : Paxos, proposer : Proposer, storage : Storage) 
{
    var startRead : Long = 0
    var endRead : Long = 0
    @volatile var instanceCnt : Long = 0
    @volatile var timesum : Long = 0;
    @volatile var lastInstance : Long = 0;
 
    def onAccept(message : Accept, sender : Integer) : Unit = 
    {
        assert(message.getView() == storage.getView(), "Msg.view: " + message.getView() +
                                                        ", view: " + storage.getView());
        assert(paxos.getDispatcher().amIInDispatcher(), "Thread should not be here: " +
                                                         Thread.currentThread());
        /* Get the current state */
        val s : state_ext[Array[Byte], Unit] = storage.getCurrState

        val curView = def_getBallot(s)
        val curInst = message.getInstanceId()
        val value = message.getValue()
        
        if (message.getView() < curView) 
        {
            return
        }
        
        if (storage.getLog.getInstance(curInst).isDecided()) {
            logger.info("Ignoring Accept. Instance already decided: {}", message.getInstanceId());
            return;
        }
       
       /* Create the Phase 2b scala state message */  
       val msg : Phase2b[Array[Byte]] = new Phase2b(curInst, message.getBallot(), Comd(value))

       /* Update the state by calling the scala function */
       startRead = System.nanoTime
       //val x = lsr.paxos.MultiPaxos4.last_decision(s)
       val (state, packetList) = processExternalEvent[Array[Byte]](sender, msg, s)
       //val y = lsr.paxos.MultiPaxos4.last_decision(state)
       endRead = System.nanoTime  
       timesum += (endRead - startRead)/1000
       instanceCnt = instanceCnt + 1;  
         
       /* Update the state */
       storage.updateState(state)
       
       if (storage.getLog.getInstance(curInst).isMajority(ProcessDescriptor.getInstance().numReplicas)) {
            if (value != null) {
                paxos.decide(curInst);
            }
        }
    }

    def getAccept_Avge : Long = 
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
    
    def getAccept_Count : Long = 
    {
      instanceCnt - lastInstance
    }
    
    private final var logger : Logger = LoggerFactory.getLogger(classOf[Learner]);
}

    
