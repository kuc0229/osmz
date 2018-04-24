package com.kru13.httpserver;

import android.util.Log;

import com.kru13.httpserver.service.HttpServerService;

import java.io.IOException;
import java.net.Socket;

public class RequestEvent extends Thread {

    private final Socket socket;
    private final ResponseProcessor responseProcessor;
    private boolean complete;

    public RequestEvent(Socket socket) {
        this.socket = socket;
        complete = false;
        responseProcessor = new ResponseProcessor();
    }

    @Override
    public void run() {
        Log.d("REQUEST EVENT", "Create request event with socket #" + socket.hashCode());
        Log.d("REQUEST EVENT", "Service by " + Thread.currentThread().getName());

        try {
            responseProcessor.processRequest(this.socket);
        } catch (IOException e) {
            Log.e("Socket", "error during process request " + e.getLocalizedMessage());
        } finally {
            try {
                this.socket.close();
                Log.d("REQUEST EVENT", "Socket #" + socket.hashCode() + " Closed");
            } catch (IOException e) {
                Log.d("REQUEST EVENT", "Cannot close socket #" + socket.hashCode() + " " + e.getLocalizedMessage());
            }
            complete = true;
        }
    }

    @Override
    public int hashCode() {
        return socket.hashCode();
    }

    public boolean isComplete() {
        return complete;
    }

    public Socket getSocket() {
        return socket;
    }
}
