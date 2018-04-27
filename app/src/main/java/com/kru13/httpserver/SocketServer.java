package com.kru13.httpserver;

import android.util.Log;

import com.kru13.httpserver.service.HttpServerService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

// ADB je soucasti SDK, cestu naleznete v local.properties
// presmerovani portu na hostitelsky pocitac
// platform-tools/adb.exe forward tcp:12345 tcp:12345

// ladeni
// PuTTY  (connection type RAW,  IP 127.0.0.1 PORT 12345

// C:\Users\kucabpet\AppData\Local\Android\Sdk\platform-tools\adb.exe forward tcp:12345 tcp:12345


public class SocketServer {

    private static final int MAX_CLIENTS = 2;
    private static final int PORT = 12345;

    private boolean bRunning;
    private ServerSocket serverSocket;
    private HttpServerService systemService;
    private List<RequestEvent> clients = new ArrayList<RequestEvent>();
    private final Semaphore available = new Semaphore(MAX_CLIENTS, true);

    public SocketServer(HttpServerService httpServerService) {
        this.systemService = httpServerService;
    }

    public void run() throws InterruptedException {
        try {
            Log.d("SERVER", "Creating Socket");
            systemService.createNotification("Create socket...");

            serverSocket = new ServerSocket(PORT);
            bRunning = true;

            while (bRunning) {
                getAcquire();

                Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept();
                Log.d("Socket SERVER", "Socket Accepted #" + s.hashCode());
                systemService.createNotification("Accepted connection from " + s.getInetAddress() + ":" + s.getPort());

                RequestEvent requestEvent = new RequestEvent(s);
                requestEvent.start();
                clients.add(requestEvent);

                release();
            }
        } catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed())
                Log.d("SERVER", "Normal exit");
            else {
                Log.d("SERVER", "Error");
                e.printStackTrace();
            }
        } finally {
            serverSocket = null;
            bRunning = false;
            release();
        }
    }

    protected void getAcquire() throws InterruptedException {
        available.acquire();
        Log.d("SEMAPHORE", "permit has been gotten");
    }

    private void release() {
        available.release();
        Log.d("SEMAPHORE", "permit has been released");
    }

    public void close() {
        try {
            systemService.createNotification("Close socket.");
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.d("SERVER", "Error, probably interrupted in accept(), see log");
            e.printStackTrace();
        }
        bRunning = false;
    }

}