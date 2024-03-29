package lsr.paxos.client;

import static lsr.common.ProcessDescriptor.processDescriptor;

import java.io.IOException;
import java.nio.ByteBuffer;

import lsr.common.ClientCommand;
import lsr.common.ClientReply;
import lsr.common.IdGenerator;
import lsr.common.SimpleIdGenerator;
import lsr.paxos.client.ClientProxy;
import lsr.paxos.storage.Storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to handle one client connection. It uses
 * <code>ReaderAndWriter</code> for writing and reading packets from clients.
 * <p>
 * First it initializes client connection reading client id (or granting him a
 * new one). After successful initialization commands will be received, and
 * reply can be send to clients.
 * 
 * @see ReaderAndWriter
 */
public class NioClientProxy implements ClientProxy {
	
    private final ClientRequestManager requestManager;
    private long clientId;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(processDescriptor.clientRequestBufferSize);
    private final ReaderAndWriter readerAndWriter;

    /** Generator for client IDs */
    public static IdGenerator idGenerator;

    /**
     * Creates new client proxy.
     * 
     * @param readerAndWriter - used to send and receive data from clients
     * @param requestManager - callback for executing command from clients
     * @param idGenerator - generator used to generate id's for clients
     */
    public NioClientProxy(ReaderAndWriter readerAndWriter, ClientRequestManager requestManager) {
        this.readerAndWriter = readerAndWriter;
        this.requestManager = requestManager;

        if (logger.isInfoEnabled())
            logger.info("New client connection: {}", readerAndWriter.socketChannel.socket());

        this.readerAndWriter.setPacketHandler(new InitializePacketHandler());
    }

    /**
     * Sends the reply to client held by this proxy. This method has to be
     * called after client is initialized.
     * 
     * @param clientReply - reply send to underlying client
     */
    public void send(final ClientReply clientReply) {
        getSelectorThread().beginInvoke(new Runnable() {
            public void run() {
                try {
                    sendInternal(clientReply);
                } catch (IOException e) {
                    // cannot send message to the client; Client should send
                    // request again
                    logger.error(
                            "Could not send reply to client. Discarding reply {} (reason: {})",
                            clientReply, e);
                }
            }
        });
    }

    private void sendInternal(ClientReply clientReply) throws IOException {
        readerAndWriter.send(clientReply.toByteArray());
    }

    /**
     * executes command from byte buffer
     * 
     * @throws InterruptedException
     */
    private void execute(ByteBuffer buffer) throws InterruptedException {
        ClientCommand command = new ClientCommand(buffer);
        logger.info("execute command {}", command);
        requestManager.onClientRequest(command, this);
    }

    /**
     * Waits for first byte, 'T' or 'F' which specifies whether we should grant
     * new id for this client, or it has one already.
     */
    private class InitializePacketHandler implements PacketHandler {

        public InitializePacketHandler() {
            readBuffer.clear();
            readBuffer.limit(1);
        }

        public void finished() {
            readBuffer.rewind();
            byte b = readBuffer.get();

            if (b == lsr.paxos.client.Client.REQUEST_NEW_ID) {
                // grant new id for client
                clientId = idGenerator.next();
                byte[] bytesClientId = new byte[8];
                ByteBuffer.wrap(bytesClientId).putLong(clientId);
                readerAndWriter.send(bytesClientId);
                readerAndWriter.setPacketHandler(new ClientCommandPacketHandler());
            } else if (b == lsr.paxos.client.Client.HAVE_CLIENT_ID) {
                // wait for receiving id from client
                readerAndWriter.setPacketHandler(new ClientIdPacketHandler());
            } else {
                // command client is incorrect; close the underlying connection
                logger.error(
                        "Incorrect initialization header. Expected '{}' or '{}' but received {}",
                        lsr.paxos.client.Client.REQUEST_NEW_ID,
                        lsr.paxos.client.Client.HAVE_CLIENT_ID, b);
                readerAndWriter.scheduleClose();
            }
        }

        public ByteBuffer getByteBuffer() {
            return readBuffer;
        }
    }

    /**
     * Waits for the id from client. After receiving it starts receiving client
     * commands packets.
     */
    private class ClientIdPacketHandler implements PacketHandler {

        public ClientIdPacketHandler() {
            readBuffer.clear();
            readBuffer.limit(8);
        }

        public void finished() {
            readBuffer.rewind();
            clientId = readBuffer.getLong();
            readerAndWriter.setPacketHandler(new ClientCommandPacketHandler());
        }

        public ByteBuffer getByteBuffer() {
            return readBuffer;
        }
    }

    /**
     * Waits for the header and then for the message from the client.
     */
    private class ClientCommandPacketHandler implements PacketHandler {

        // TODO: (JK) consider keeping the once allocated bigger buffer

        /**
         * Client request may be bigger than default buffer size; in such case,
         * new buffer will be created and used for the request
         */
        private ByteBuffer currentBuffer;
        private boolean header = true;

        public ClientCommandPacketHandler() {
            currentBuffer = readBuffer;
            currentBuffer.clear();
            currentBuffer.limit(ClientCommand.HEADERS_SIZE);
        }

        public void finished() throws InterruptedException {

            if (header) {
                assert currentBuffer == readBuffer : "Default buffer should be used for reading header";

                readBuffer.position(ClientCommand.HEADER_VALUE_SIZE_OFFSET);
                int sizeOfValue = readBuffer.getInt();
                if (ClientCommand.HEADERS_SIZE + sizeOfValue <= readBuffer.capacity()) {
                    readBuffer.limit(ClientCommand.HEADERS_SIZE + sizeOfValue);
                } else {
                    currentBuffer = ByteBuffer.allocate(ClientCommand.HEADERS_SIZE + sizeOfValue);
                    currentBuffer.put(readBuffer.array(), 0, ClientCommand.HEADERS_SIZE);
                }
            } else {
                currentBuffer.flip();
                execute(currentBuffer);
                // for reading header we can use default buffer
                currentBuffer = readBuffer;
                readBuffer.clear();
                readBuffer.limit(ClientCommand.HEADERS_SIZE);
            }
            header = !header;
            readerAndWriter.setPacketHandler(this);
        }

        public ByteBuffer getByteBuffer() {
            return currentBuffer;
        }
    }

    public String toString() {
        return "client: " + clientId + " - " + readerAndWriter.socketChannel.socket().getPort();
    }

    public SelectorThread getSelectorThread() {
        return readerAndWriter.getSelectorThread();
    }

    public void closeConnection() {
        readerAndWriter.close();
    }

    public static void createIdGenerator(Storage storage) {
    	idGenerator = new SimpleIdGenerator();
    }

    private final static Logger logger = LoggerFactory.getLogger(NioClientProxy.class);
}
