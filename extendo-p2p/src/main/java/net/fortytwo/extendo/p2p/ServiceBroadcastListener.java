package net.fortytwo.extendo.p2p;

import net.fortytwo.extendo.Extendo;
import net.fortytwo.extendo.util.TypedProperties;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Logger;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class ServiceBroadcastListener {
    protected static final Logger logger = Logger.getLogger(ServiceBroadcastListener.class.getName());

    private static final int BROADCAST_MAX_LENGTH = 1000;

    private final EventHandler eventHandler;

    private boolean stopped;

    public ServiceBroadcastListener(final EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    public void start() {
        stopped = false;

        new Thread(new Runnable() {
            public void run() {
                logger.info("starting listener thread for facilitator broadcast messages");

                try {
                    listenForFacilitatorBroadcast();
                } catch (Throwable t) {
                    logger.severe("listener thread for facilitator broadcast messages failed with error: "
                            + t.getMessage());
                    t.printStackTrace(System.err);
                } finally {
                    logger.info("listener thread for facilitator broadcast messages stopped");
                }
            }
        }).start();
    }

    public void stop() {
        stopped = true;
    }

    private void listenForFacilitatorBroadcast() throws TypedProperties.PropertyException {
        int port = Extendo.getConfiguration().getInt(Extendo.P2P_BROADCAST_PORT);

        byte[] buffer = new byte[BROADCAST_MAX_LENGTH];
        try {
            DatagramPacket dataIn = new DatagramPacket(buffer, buffer.length);
            DatagramSocket socket = new DatagramSocket(port);
            try {
                // never time out; wait as long as it takes for a facilitator to become available
                socket.setSoTimeout(0);

                while (!stopped) {
                    socket.receive(dataIn);

                    String msg = new String(buffer).substring(0, dataIn.getLength());

                    // if the UDP packet arrives on the expected port and the data appears to be formatted as JSON,
                    // assume it is an Extendo message
                    if (msg.startsWith("{") && msg.endsWith("}")) {
                        logger.fine("UDP message received: " + msg);

                        try {
                            ServiceDescription d = new ServiceDescription(msg);

                            eventHandler.receivedServiceDescription(dataIn.getAddress(), d);
                        } catch (ServiceDescription.InvalidServiceDescriptionException e) {
                            logger.warning("invalid service description in datagram from "
                                    + dataIn.getAddress().getHostAddress() + ": " + msg);
                        }
                    }
                }
            } finally {
                socket.close();
            }
        } catch (IOException e) {
            logger.severe("IOException while waiting for broadcast datagram from facilitator: " + e.getMessage());
        }
    }

    public interface EventHandler {
        void receivedServiceDescription(InetAddress address,
                                        ServiceDescription description);
    }
}
