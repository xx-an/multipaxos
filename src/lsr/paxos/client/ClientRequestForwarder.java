package lsr.paxos.client;

import static lsr.common.ProcessDescriptor.processDescriptor;

import java.util.BitSet;

import lsr.common.Request;
import lsr.paxos.core.Paxos;
import lsr.paxos.messages.ForwardClientRequests;
import lsr.paxos.messages.Message;
import lsr.paxos.messages.MessageType;
import lsr.paxos.network.MessageHandler;
import lsr.paxos.network.Network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientRequestForwarder {

    private final Paxos paxos;
    private final Network network;
    private ClientRequestManager clientRequestManager = null;

    public ClientRequestForwarder(Paxos paxos) {
        this.paxos = paxos;
        this.network = paxos.getNetwork();
    }

    public void setClientRequestManager(ClientRequestManager clientRequestManager) {
        this.clientRequestManager = clientRequestManager;
    }

    void forward(Request[] requests) {
        // The object that will be sent.
        ForwardClientRequests fReqMsg = new ForwardClientRequests(requests);

        int leaderId = paxos.getLeaderId();
        logger.info("Forwarding requests {} to leader {}", fReqMsg, leaderId);
        if (processDescriptor.localId == leaderId) {
            if (clientRequestManager != null)
                clientRequestManager.dispatchOnClientRequest(requests, null);
        } 
        else
            network.sendMessage(fReqMsg, leaderId);
    }

    public void start() {
        Network.addMessageListener(MessageType.ForwardedClientRequests, new MessageHandler() {

            public void onMessageSent(Message message, BitSet destinations) {
                assert false;
            }

            public void onMessageReceived(Message msg, int sender) {
                assert msg instanceof ForwardClientRequests;
                ForwardClientRequests fcr = (ForwardClientRequests) msg;
                Request[] requests = fcr.getRequests();
                if (clientRequestManager != null)
                    clientRequestManager.dispatchOnClientRequest(requests, null);
            }
        });
    }

    static final Logger logger = LoggerFactory.getLogger(ClientRequestForwarder.class);
}
