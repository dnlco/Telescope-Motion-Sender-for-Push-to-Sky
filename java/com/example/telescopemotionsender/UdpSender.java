package com.example.telescopemotionsender;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UdpSender {
    private static final String TAG = "UdpSender";

    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private boolean isSending = false;

    public UdpSender(String address, int port) {
        try {
            serverAddress = InetAddress.getByName(address);
            serverPort = port;
            socket = new DatagramSocket();
            isSending = true;
            Log.d(TAG, "UDP Sender initialized for " + address + ":" + port);
        } catch (SocketException e) {
            Log.e(TAG, "Socket creation failed: " + e.getMessage());
        } catch (UnknownHostException e) {
            Log.e(TAG, "Unknown host: " + address + " - " + e.getMessage());
        }
    }

    public void sendData(final String data) {
        if (!isSending || socket == null) {
            Log.w(TAG, "UDP Sender is not initialized or closed. Cannot send data.");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buffer = data.getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
                    socket.send(packet);
                    // Log.d(TAG, "Sent: " + data); // Uncomment for verbose logging
                } catch (IOException e) {
                    Log.e(TAG, "Error sending UDP packet: " + e.getMessage());
                }
            }
        }).start();
    }

    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            isSending = false;
            Log.d(TAG, "UDP Sender closed.");
        }
    }
}

