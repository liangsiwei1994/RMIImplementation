package centralserver;

import common.*;
import field.ILocationSensor;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class CentralServer extends UnicastRemoteObject implements ICentralServer {

    private ILocationSensor locationSensor;
    private Integer counter = 0;
    private Integer expected = 0;
    private List<MessageInfo> receivedMessages;

    protected CentralServer () throws RemoteException {
        super();
        // Initialise Array receivedMessages
        receivedMessages = new ArrayList<>();
    }

    public static void main (String[] args) throws RemoteException {
        ICentralServer cs = new CentralServer();

        // Create (or Locate) Registry
        Registry registry = LocateRegistry.getRegistry();

        // Bind to Registry
        String name = "CentralServer";
        registry.rebind(name, cs);

        System.out.println("Central Server is ready");

    }


    @Override
    public void receiveMsg (MessageInfo msg) {
        System.out.println("[Central Server] Received message "
                + (msg.getMessageNum()) + " out of "
                + msg.getTotalMessages() + ". Measure = " + msg.getMessage());

        Integer msgNum = msg.getMessageNum();
        Integer msgTot = msg.getTotalMessages();

        // Reset counter and initialise data structure if this is first msg
        if (msgNum.equals(1)) {
            counter = 0;
            receivedMessages = new ArrayList<>();
            expected = msgTot;
        }

        // Save current message
        receivedMessages.add(msg);
        counter = counter + 1;

        // Prints stats when final message is received
        if(msgTot.equals(msgNum)) {
            this.printStats();
        }
    }

    public void printStats() {
        // Calculated and display the number of missing and received messages
        Integer dropCount = expected - counter;

        System.out.printf("Total Missing Messages = " + dropCount + " out of "
                + expected + "\n");

        // Check the sequence of the message number to find the missing messages
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

        // Print the location of the Field Unit that sent the messages
        try {
            printLocation();
        } catch (RemoteException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to print location of sensor" +
                    "Exiting Program. \n");
        }

        // Now re-initialise data structures for next time
        receivedMessages = new ArrayList<>();
        counter = 0;
        expected = 0;

    }

    @Override
    public void setLocationSensor (ILocationSensor locationSensor) throws RemoteException {
        // Set location sensor
        this.locationSensor = locationSensor;
        System.out.println("Location Sensor Set");
    }

    public void printLocation() throws RemoteException {
        // Print location on screen from remote reference
        Location location = locationSensor.getCurrentLocation();
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        System.out.printf("[Field Unit] Current Location: lat = " + latitude + " long = " + longitude + "\n");
    }
}
