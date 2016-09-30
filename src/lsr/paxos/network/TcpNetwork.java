package lsr.paxos.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.BitSet;

import lsr.common.KillOnExceptionHandler;
import lsr.common.ProcessDescriptor;
import lsr.paxos.messages.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpNetwork extends Network implements Runnable {
    private final TcpConnection[] connections;
    private final ProcessDescriptor p;
    private final ServerSocket server;
    private final Thread acceptorThread;
    private boolean started = false;

    /**
     * Creates new network for handling connections with other replicas.
     * 
     * @throws IOException if opening server socket fails
     */
    public TcpNetwork() throws IOException {
        this.p = ProcessDescriptor.getInstance();
        this.connections = new TcpConnection[p.numReplicas];
        logger.info("Opening port: " + p.getLocalProcess().getReplicaPort());
        this.server = new ServerSocket();
        server.setReceiveBufferSize(256 * 1024);
        server.bind(new InetSocketAddress((InetAddress) null, p.getLocalProcess().getReplicaPort()));

        this.acceptorThread = new Thread(this, "TcpNetwork");
        acceptorThread.setUncaughtExceptionHandler(new KillOnExceptionHandler());
    }

    @Override
    public void start() {
        if (!started) {
            for (int i = 0; i < connections.length; i++) {
                if (i < p.localId) {
                    connections[i] = new TcpConnection(this, p.config.getProcess(i), false);
                    connections[i].start();
                }
                if (i > p.localId) {
                    connections[i] = new TcpConnection(this, p.config.getProcess(i), true);
                    connections[i].start();
                }
            }
            // Start the thread that listens and accepts new connections.
            // Must be started after the connections are initialized (code
            // above)
            acceptorThread.start();
            started = true;
        }
    }

    /**
     * Sends binary data to specified destination.
     * 
     * @param message - binary data to send
     * @param destination - id of replica to send data to
     * @return true if message was sent; false if some error occurred
     */
    boolean send(byte[] message, int destination) {
        assert destination != p.localId;
        
        if(connections[destination] == null)
        	System.out.println("Connection for dest " + destination + " is null");
        return connections[destination].send(message);
    }

    /**
     * Main loop which accepts incoming connections.
     */
    public void run() {
        logger.info(Thread.currentThread().getName() + " thread started");
        while (true) {
            try {
                Socket socket = server.accept();
                initializeConnection(socket);
            } catch (IOException e) {
                // TODO: probably too many open files exception occurred;
                // should we open server socket again or just wait and ignore
                // this exception?
                throw new RuntimeException(e);
            }
        }
    }

    private void initializeConnection(Socket socket) {
        try {
            logger.info("Received connection from " + socket.getRemoteSocketAddress());
            socket.setSendBufferSize(128 * 1024);
            socket.setTcpNoDelay(true);
            DataInputStream input = new DataInputStream(
                    new BufferedInputStream(socket.getInputStream()));
            DataOutputStream output = new DataOutputStream(
                    new BufferedOutputStream(socket.getOutputStream()));
            int replicaId = input.readInt();

            if (replicaId < 0 || replicaId >= p.numReplicas) {
                logger.warn("Remoce host id is out of range: " + replicaId);
                socket.close();
                return;
            }
            if (replicaId == p.localId) {
                logger.warn("Remote replica has same id as local: " + replicaId);
                socket.close();
                return;
            }

            connections[replicaId].setConnection(socket, input, output);
        } catch (IOException e) {
            logger.warn("Initialization of accepted connection failed.", e);
            try {
                socket.close();
            } catch (IOException e1) {
            }
        }
    }

   
    public void sendMessage(Message message, BitSet destinations) {
        assert !destinations.isEmpty() : "Sending a message to no one";

        //System.out.println("Sendmessages called , type is = " + message.getType());
        byte[] bytes = message.toByteArray();
        //System.out.println("Msg converted to bytearray");	
        // do not send message to us (just fire event)
        if (destinations.get(p.localId)) {
        	//System.out.println("Fired msg to host, type is = " + message.getType());
        	fireReceiveMessage(message, p.localId);
        }

        for (int i = destinations.nextSetBit(0); i >= 0; i = destinations.nextSetBit(i + 1)) {
            if (i != p.localId) {
                send(bytes, i);
            }
        }

        // Not really sent, only queued for sending,
        // but it's good enough for the notification
        fireSentMessage(message, destinations);
    }

    public void sendMessage(Message message, int destination) {
        BitSet target = new BitSet();
        target.set(destination);
        sendMessage(message, target);
    }

    public void sendToAll(Message message) {
    	//System.out.println("Send to all called");
    	BitSet all = new BitSet(p.numReplicas);
        all.set(0, p.numReplicas);
        sendMessage(message, all);
    }
    
    public void sendToOthers(Message message) {
    	//System.out.println("Send to all called");
    	BitSet dests = new BitSet(p.numReplicas);
    	dests.set(0, p.numReplicas);
        // Do not send ACCEPT to self
    	dests.clear(p.localId);
        sendMessage(message, dests);
    }

    private final static Logger logger = LoggerFactory.getLogger(TcpNetwork.class);
}
