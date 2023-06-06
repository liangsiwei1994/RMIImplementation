package field;

import centralserver.CentralServer;
import centralserver.ICentralServer;
import common.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FieldUnit implements IFieldUnit {
    private ICentralServer central_server;

    /* Note: Could you discuss in one line of comment what do you think can be
     * an appropriate size for buffsize?
     * (Which is used to init DatagramPacket?)
     */
    /* 1500 to stay within the usual max transmission unit. For this project,
    assume maximum msg count is 4000, size of 20 is minimum.
     */


    private static final int buffsize = 1500;
    private static int timeout = 500000;

    private DatagramSocket socket;

    private Integer counter = 0;

    private List<MessageInfo> receivedMessages;
    private List<MessageInfo> storedSMAs;

    private int expected = 0;

    private ILocationSensor locationSensor;

    public FieldUnit () {
        // Initialise array to store SMAs
        storedSMAs = new ArrayList<>();
        try {
            locationSensor = new LocationSensor();
        } catch (RemoteException e) {
            System.err.println("Remote exception caught: ");
            e.printStackTrace();
            throw new RuntimeException("Failed to instantiate location " +
                    "sensor. Exiting Program.\n");
        }
    }

    @Override
    public void addMessage (MessageInfo msg) {
        // Save received message in receivedMessages list
        receivedMessages.add(msg);
    }

    @Override
    public void sMovingAverage (int k) {
        System.out.printf("[Field Unit] Computing SMAs\n");

        // Compute SMA and store values in a class attribute
        Integer totalSMAs = receivedMessages.size();
        for(int i=0; i<totalSMAs; i++) {
            if (i<(k-1)) {
                float msgNum = receivedMessages.get(i).getMessage();
                storedSMAs.add(new MessageInfo(totalSMAs, i+1, msgNum));
            }
            else {
                float average = 0;
                for (int j=i; j >= i-k+1; j--) {
                    average = average + receivedMessages.get(j).getMessage();
                }
                storedSMAs.add(new MessageInfo(totalSMAs, i+1, average/k));
            }
        }
    }

    @Override
    public void receiveMeasures(int port, int timeout) throws SocketException {
        this.timeout = timeout;

        // Create UDP socket and bind to local port 'port
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException( "Socket could not be opened or "
                    + "failed to bind to port " + port + ". \n");
        }

        // Set time out for socket
        try {
            socket.setSoTimeout(timeout);
        } catch (SocketException e) {
            throw new RuntimeException("Failed to set time out on "
                    + "UDP controller socket. \n");
        }

        byte[] buf = new byte[buffsize];

        boolean listen = true;

        System.out.println("[Field Unit] Listening on port: " + port);

        // Repeatedly listen for incoming messages from sensor
        while (listen) {

            /* Receive until all messages in the transmission (msgTot) have
            been received or until there is nothing more to be received */
            DatagramPacket incomingMsg = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(incomingMsg);
            } catch (SocketTimeoutException ste) {
                System.out.printf("Socket Timed Out. Continue listening... \n");
                break;
            } catch (IOException e) {
                throw new RuntimeException("Issue receiving message. \n");
            }

            // Read messages
            String strIncomingMsg = new String(incomingMsg.getData(),
                    0 ,incomingMsg.getLength());
            strIncomingMsg = strIncomingMsg.trim();
            MessageInfo msgReceived;
            try {
                msgReceived = new MessageInfo(strIncomingMsg);
            } catch (Exception e) {
                System.out.printf("MessageInfo: Invalid string for message "
                        + "construction: " + strIncomingMsg
                        + ". Message Dropped. \n");
                continue;
            }
            Integer msgNum = msgReceived.getMessageNum();
            Integer msgTot = msgReceived.getTotalMessages();
            Float msgInfo = msgReceived.getMessage();

            // Initialise the receive data structure if this is first message
            if (counter.equals(0)) {
                receivedMessages = new ArrayList<>();
                expected = msgTot;
            }

            // Store the message
            this.addMessage(msgReceived);
            counter = counter + 1;
            System.out.printf(
                    "[Field Unit] Message " + msgNum + " out of " + msgTot
                            + " received. Value = " + msgInfo + "\n");

            // Exit controller after receiving last message
            if (msgNum.equals(msgTot)) {
                break;
            }
        }
        // Close socket
        socket.close();
    }

    public static void main (String[] args) throws SocketException {
        if (args.length < 2) {
            System.out.println("Usage: ./fieldunit.sh <UDP rcv port> " +
                    "<RMI server HostName/IPAddress>");
            return;
        }

        // Parse arguments
        int portNumber = Integer.parseInt(args[0]);
        String rmiAddress = args[1];

        // Construct Field Unit Object
        FieldUnit fieldUnit = new FieldUnit();

        // Call initRMI on the Field Unit Object
        fieldUnit.initRMI(rmiAddress);

        // Repeatedly wait for incoming transmission
        while(true) {
            // Wait for incoming transmission
            fieldUnit.receiveMeasures(portNumber, timeout);

            // Compute and print stats
            fieldUnit.printStats();

            // Compute Averages
            fieldUnit.sMovingAverage(7);

            // Send data to the Central Serve via RMI
            fieldUnit.sendAverages();
        }
    }


    @Override
    public void initRMI (String address) {
        // Bind to RMIServer */
        String name = "CentralServer";
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry(address);
        } catch (RemoteException e) {
            System.err.println("Remote exception caught: ");
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to registry." +
                    "Exiting Program. \n");
        }

        try {
            central_server = (ICentralServer) registry.lookup(name);
        } catch (RemoteException e) {
            System.err.println("Remote exception caught: ");
            e.printStackTrace();
            throw new RuntimeException("Unable to find " + name + ". Exiting " +
                    "Program.\n");
        } catch (NotBoundException e) {
            throw new RuntimeException(name + " is not in registry. " +
                    "Exiting Program. \n");
        }

        // Send pointer to LocationSensor to RMI Server
        try {
            central_server.setLocationSensor(locationSensor);
        } catch (RemoteException e) {
            System.err.println("Remote exception caught: ");
            e.printStackTrace();
            throw new RuntimeException("Failed to invoke Central Server's " +
                    "setLocationSensor(). Exiting Program.\n");
        }
    }

    @Override
    public void sendAverages () {
        System.out.printf("[Field Unit] Sending SMAs to RMI\n\n");
        // Attempt to send messages the specified number of times
        for (int i=0; i<storedSMAs.size(); i++) {
            try {
                central_server.receiveMsg(storedSMAs.get(i));
            } catch (RemoteException e) {
                System.err.println("Failed to invoke Central Server's " +
                        "receiveMsg(). Exiting Program. Exception: \n");
                e.printStackTrace();
                System.exit(1);
            }
        }

        // Re-initialise data structures for next round of messages
        receivedMessages = new ArrayList<>();
        storedSMAs = new ArrayList<>();
        counter = 0;
        expected = 0;
    }

    @Override
    public void printStats () {
        // Calculate the number of packets dropped
        int dropCount = expected - counter;

        System.out.printf("Total Missing Messages = " + dropCount + " out of "
                + expected + "\n");
        System.out.printf("===============================\n");

        // Check the sequence of message number to find the missing messages
        Integer prevMsgNum = -1;
        List<Integer> missingMsgNum = new ArrayList<>();
        boolean found = false;
        for (Integer i=1; i<=expected; i++) {
            found = false;
            for (Integer j=0; j<receivedMessages.size(); j++) {
                Integer currMsgNum = receivedMessages.get(j).getMessageNum();
                if (currMsgNum.equals(i)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missingMsgNum.add(i);
            }
        }

        // Print out the missing number sequence
        for (Integer i=0; i<missingMsgNum.size(); i++) {
            if (i.equals(0)) {
                System.out.printf("Missing Message Numbers: "
                        + missingMsgNum.get(i).toString() + " ");
            }
            else {
                System.out.printf(missingMsgNum.get(i).toString() + " ");
            }
        }
        if (missingMsgNum.size() > 0) {
            System.out.printf("\n");
        }
    }
}
