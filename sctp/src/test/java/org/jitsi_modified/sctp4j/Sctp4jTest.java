package org.jitsi_modified.sctp4j;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class Sctp4jTest {
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes, int off, int len) {
        char[] hexChars = new char[len * 2];
        int hexCharsIndex = 0;
        for ( int j = off; j < off + len; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[hexCharsIndex] = hexArray[v >>> 4];
            hexChars[hexCharsIndex + 1] = hexArray[v & 0x0F];
            hexCharsIndex += 2;
        }
        return new String(hexChars);
    }

    /**
     * Reads 32 bit unsigned int from the buffer at specified offset
     *
     * @param buffer
     * @param offset
     * @return 32 bit unsigned value
     */
    private static long bytes_to_long(byte[] buffer, int offset)
    {
        int fByte = (0x000000FF & ((int) buffer[offset]));
        int sByte = (0x000000FF & ((int) buffer[offset + 1]));
        int tByte = (0x000000FF & ((int) buffer[offset + 2]));
        int foByte = (0x000000FF & ((int) buffer[offset + 3]));
        return ((long) (fByte << 24
                | sByte << 16
                | tByte << 8
                | foByte))
                & 0xFFFFFFFFL;
    }

    /**
     * Reads 16 bit unsigned int from the buffer at specified offset
     *
     * @param buffer
     * @param offset
     * @return 16 bit unsigned int
     */
    private static int bytes_to_short(byte[] buffer, int offset)
    {
        int fByte = (0x000000FF & ((int) buffer[offset]));
        int sByte = (0x000000FF & ((int) buffer[offset + 1]));
        return ((fByte << 8) | sByte) & 0xFFFF;
    }

    private static void debugChunks(byte[] packet)
    {
        int offset = 12;// After common header
        while((packet.length - offset) >= 4)
        {
            System.out.println("Packet data length: " + (packet.length - 12) + ", current offset = " + offset);
            int chunkType = packet[offset++] & 0xFF;

            int chunkFlags = packet[offset++] & 0xFF;

            int chunkLength = bytes_to_short(packet, offset);
            System.out.println("Chunk length: " + chunkLength);
            if (chunkLength < 4) {
                System.out.println("Empty chunk");
                return;
            }
            offset+=2;

            System.out.println("Chunk type: " + chunkType
                             + " Chunk flags: " + chunkFlags
                             + " Chunk length: "+ chunkLength );
            if(chunkType == 1)
            {
                System.out.println("Chunk type init");
                //Init chunk info

                long initTag = bytes_to_long(packet, offset);
                offset += 4;

                long a_rwnd = bytes_to_long(packet, offset);
                offset += 4;

                int nOutStream = bytes_to_short(packet, offset);
                offset += 2;

                int nInStream = bytes_to_short(packet, offset);
                offset += 2;

                long initTSN = bytes_to_long(packet, offset);
                offset += 4;

                System.out.println(
                    "ITAG: 0x" + Long.toHexString(initTag)
                    + " a_rwnd: " + a_rwnd
                    + " nOutStream: " + nOutStream
                    + " nInStream: " + nInStream
                    + " initTSN: 0x" + Long.toHexString(initTSN));

                // Parse Type-Length-Value chunks
                while(offset < chunkLength)
                {
                    //System.out.println(packet[offset++]&0xFF);
                    int type = bytes_to_short(packet, offset);
                    offset += 2;

                    int length = bytes_to_short(packet, offset);
                    offset += 2;

                    // value
                    offset += (length-4);
                    System.out.println(
                        "T: "+type+" L: "+length+" left: "+(chunkLength-offset));
                }

                offset += (chunkLength-4-16);
            }
            else if(chunkType == 0)
            {
                System.out.println("Chunk type payload data");
                // Payload
                boolean U = (chunkFlags & 0x4) > 0;
                boolean B = (chunkFlags & 0x2) > 0;
                boolean E = (chunkFlags & 0x1) > 0;

                long TSN = bytes_to_long(packet, offset); offset += 4;

                int streamIdS = bytes_to_short(packet, offset); offset += 2;

                int streamSeq = bytes_to_short(packet, offset); offset += 2;

                long PPID = bytes_to_long(packet, offset); offset += 4;

                System.out.println(
                    "U: " + U + " B: " +B + " E: " + E
                    + " TSN: 0x" + Long.toHexString(TSN)
                    + " SID: 0x" + Integer.toHexString(streamIdS)
                    + " SSEQ: 0x" + Integer.toHexString(streamSeq)
                    + " PPID: 0x" + Long.toHexString(PPID)
                );
                offset += (chunkLength-4-12);
            }
            else if(chunkType == 6)
            {
                // Abort
                System.out.println("We have abort!!!");

                if(offset >= chunkLength) {
                    System.out.println("No abort CAUSE!!!");
                }

                while(offset < chunkLength)
                {
                    int causeCode = bytes_to_short(packet, offset);
                    offset += 2;

                    int causeLength = bytes_to_short(packet, offset);
                    offset += 2;

                    System.out.println("Cause: " + causeCode + " L: " + causeLength);
                }
            }
            else
            {
                offset += (chunkLength-4);
            }
        }
    }

    public static void debugSctpPacket(byte[] packet, String id)
    {
        System.out.println(id);
        if(packet.length >= 12)
        {
            //Common header
            int srcPort = bytes_to_short(packet, 0);
            int dstPort = bytes_to_short(packet, 2);

            long verificationTag = bytes_to_long(packet, 4);
            long checksum = bytes_to_long(packet, 8);

            System.out.println(
                  "SRC P: " + srcPort + " DST P: " + dstPort + " VTAG: 0x"
                      + Long.toHexString(verificationTag) + " CHK: 0x"
                      + Long.toHexString(checksum));

            debugChunks(packet);
        }
    }

    @Test
    public void testInit() {
        // Very basic test to verify that everything has been linked up correctly
        Sctp4j.init();
    }

    /**
     * Spin up a client and server and connect
     */
    @Test
    public void basicLoop() throws InterruptedException, TimeoutException, ExecutionException {
//        Thread.sleep(30000);
        Sctp4j.init();

        final SctpSocket2 server = Sctp4j.createSocket();
        final SctpSocket2 client = Sctp4j.createSocket();

        server.outgoingDataSender = (data, offset, length) -> {
            new Thread(() -> {
                System.out.println("Server sending type " + (data[12] & 0xFF) + " to client-->");
                System.out.println(bytesToHex(data, offset, length));
                debugSctpPacket(data, "client outgoing");
                System.out.println();
                client.onConnIn(data, offset, length);
            }).start();
            return 0;
        };
        client.outgoingDataSender = (data, offset, length) -> {
            new Thread(() -> {
                System.out.println("\t\t\t\t<---Client sending type " + (data[12] & 0xFF) + " to server");
                System.out.println(bytesToHex(data, offset, length));
                debugSctpPacket(data, "client outgoing");
                System.out.println();
                server.onConnIn(data, offset, length);
            }).start();
            return 0;
        };

        CompletableFuture<String> serverReceivedData = new CompletableFuture<>();
        server.dataCallback = (data, sid, ssn, tsn, ppid, context, flags) -> {
            System.out.println("Server received app data for sid " + sid + "<---");
            System.out.println(bytesToHex(data, 0, data.length));
            debugSctpPacket(data, "server");
            System.out.println();
            String message = new String(data);
            serverReceivedData.complete(message);
        };
        client.dataCallback = (data, sid, ssn, tsn, ppid, context, flags) -> {
            System.out.println("\t\t\t\t--->Client received app data for sid " + sid);
            System.out.println(bytesToHex(data, 0, data.length));
            debugSctpPacket(data, "server");
            System.out.println();
        };

        client.eventHandler = new SctpSocket2.SctpSocketEventHandler() {
            @Override
            public void onConnected() {
                System.out.println("Client connected, sending data");
                String message = "Hello, world";
                client.send(ByteBuffer.wrap(message.getBytes()), true, 0, 1);
            }

            @Override
            public void onDisconnected() {
                System.out.println("Client disconnected");
            }
        };

        server.listen();

        new Thread(() -> {
            System.out.println("Client connecting");
            if (client.connect()) {
                System.out.println("Client connected");
            } else {
                System.out.println("Client failed to connect");
            }
        }, "Client thread").start();

        String serverMessage = serverReceivedData.get(5, TimeUnit.SECONDS);
        System.out.println(serverMessage + " should equal Hello, world");
    }
}