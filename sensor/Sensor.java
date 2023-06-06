package sensor;
import common.MessageInfo;

import java.io.IOException;
import java.net.*;
import java.util.Random;

public class Sensor implements ISensor {
    private float measurement;

    private final static int max_measure = 50;
    private final static int min_measure = 10;

    private DatagramSocket s;
    private byte[] buffer;
    protected int totMsg;
    protected int port;
    protected String address;

    InetAddress inetAddress;

    /* Note: Could you discuss in one line of comment what do you think can be
     * an appropriate size for buffsize?
     * (Which is used to init DatagramPacket?)
     */
    /* 1500 to stay within the usual max transmission unit. For this project,
    assume maximum msg count is 4000, size of 20 is minimum.
     */

    private static final int buffsize = 1500;

    public Sensor(String address, int port, int totMsg) {
        // Build Sensor Object
        this.port = port;
        this.address = address;
        this.totMsg = totMsg;

        try {
            s = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(
                    "Socket could not be opened or "
                            + "failed to bind to port " + port + ". \n");
        }
    }

    @Override
    public void run (int N) throws InterruptedException {
        // Send N measurements
        totMsg = N;

        // Get measurement of messages
        float measurement = this.getMeasurement();

         // Send the msg to destination by calling sendMessage()
        MessageInfo msg;
        try {
            msg = new MessageInfo(totMsg, 0, measurement);
        } catch (Exception e) {
            // Exit as the message cannot be initialised
            throw new RuntimeException(
                    "Message initialisation failed, check input parameters.\n");
        }

        // Send N messages
        for (int i = 1; i <= N; i++) {
            measurement = getMeasurement();
            int nextMsgNum = msg.getMessageNum() + 1;
            msg.setMessageNum(nextMsgNum);
            msg.setMessage(measurement);
            sendMessage(address, port, msg);
        }
    }

    public static void main (String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: ./sensor.sh field_unit_address port number_of_measures");
            return;
        }

        // Parse input arguments
        String address = args[0];
        int port = Integer.parseInt(args[1]);
        int totMsg = Integer.parseInt(args[2]);

        // Call constructor of sensor to build Sensor object
        Sensor udpSensor = new Sensor(address, port, totMsg);

        // Use Run to send the messages
        try {
            udpSensor.run(totMsg);
        } catch (InterruptedException e) {
            throw new RuntimeException("UDP message sending was interrupted" +
                    "unexpectedly\n");
        }

        udpSensor.closeSocket();
    }

    @Override
    public void sendMessage (String address, int port, MessageInfo msg) {
        String toSend = msg.toString();

        // Build destination address object
        try {
            inetAddress = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            throw new RuntimeException("IP address could not be found. \n");
        }

        // Build datagram packet to send
        buffer = toSend.getBytes();

        DatagramPacket outgoingMsg
                = new DatagramPacket(buffer, buffer.length, inetAddress, port);

        Integer msgNum = msg.getMessageNum();
        Float msgInfo = msg.getMessage();
        System.out.printf(
                "[Sensor] Sending message " + msgNum + " out of " + totMsg
                        + ". Measure = " + msgInfo + "\n");

        // Send packet
        try {
            s.send(outgoingMsg);
        } catch (IOException e) {
            throw new RuntimeException("UDP sensor unable to send "
                    + "datagram packets, exiting. \n");
        }
    }

    @Override
    public float getMeasurement () {
        Random r = new Random();
        measurement = r.nextFloat() * (max_measure - min_measure) + min_measure;

        return measurement;
    }

    public void closeSocket() {
        s.close();
    }
}
