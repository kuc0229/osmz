package com.kru13.httpserver;

import android.util.Log;

import com.kru13.httpserver.event.RequestEvent;
import com.kru13.httpserver.http.ResponseProcessor;
import com.kru13.httpserver.service.HttpServerService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;


public class SocketServer {

    private static final int MAX_CLIENTS = 2;
    private static final int PORT = 12345;

    private boolean running;
    private ServerSocket serverSocket;

    private final HttpServerService service;
    private final List<RequestEvent> clients;
    private final Semaphore available;

    public SocketServer(HttpServerService httpServerService) {
        this.service = httpServerService;
        this.available = new Semaphore(MAX_CLIENTS, true);
        this.clients = new ArrayList<>();
    }

    public void listen() {
        try {
            Log.d("SERVER", "Creating Socket");
            service.createNotification("HTTP server started");

            serverSocket = new ServerSocket(PORT);
            running = true;

            while (running) {
                getAcquire();
                Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept();
                Log.d("Socket SERVER", "Socket Accepted #" + s.hashCode());

                RequestEvent event = new RequestEvent(s);
                ResponseProcessor processor = new ResponseProcessor(service, event);
                new Thread(processor).start();

                synchronized (clients) {
                    clients.add(event);
                }

                release();
            }
        } catch (IOException e) {
            Log.d("SERVER", "Error " + e);
        } catch (InterruptedException e) {
            Log.d("SERVER", "Interrupted " + e);
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    Log.e("SERVER", "" + e);
                }
            }
            running = false;
            release();
        }
    }

    private void getAcquire() throws InterruptedException {
        available.acquire();
        Log.d("SEMAPHORE", "permit has been gotten");
    }

    private void release() {
        available.release();
        Log.d("SEMAPHORE", "permit has been released");
    }

    public void close() {
        try {
            service.createNotification("HTTP server stopped.");
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.d("SERVER", "Error, probably interrupted in accept(), see log");
        }
        running = false;
    }

    public List<RequestEvent> getClients() {
        return clients;
    }
}