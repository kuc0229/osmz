package com.kru13.httpserver;

import android.os.Environment;
import android.util.Log;

import com.kru13.httpserver.entities.DataWrapper;
import com.kru13.httpserver.enums.HttpStatus;
import com.kru13.httpserver.service.HttpServerService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

// ADB je soucasti SDK, cestu naleznete v local.properties
// presmerovani portu na hostitelsky pocitac
// platform-tools/adb.exe forward tcp:12345 tcp:12345

// ladeni
// PuTTY  (connection type RAW,  IP 127.0.0.1 PORT 12345

// C:\Users\kucabpet\AppData\Local\Android\Sdk\platform-tools\adb.exe forward tcp:12345 tcp:12345


public class SocketServer {

    final int port = 12345;
    private boolean bRunning;
    private ServerSocket serverSocket;
    private List<RequestEvent> clients = new ArrayList<RequestEvent>();
    private HttpServerService systemService;

    public SocketServer(HttpServerService httpServerService) {
        this.systemService = httpServerService;
    }

    public void run() {
        try {
            Log.d("SERVER", "Creating Socket");
            systemService.createNotification("Create socket...");

            serverSocket = new ServerSocket(port);
            bRunning = true;

            while (bRunning) {
                Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept();
                Log.d("Socket SERVER", "Socket Accepted #" + s.hashCode());
                systemService.createNotification("Accepted connection from " + s.getInetAddress() + ":" + s.getPort());

                RequestEvent requestEvent = new RequestEvent(s);
                requestEvent.start();
                clients.add(requestEvent);
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
        }
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