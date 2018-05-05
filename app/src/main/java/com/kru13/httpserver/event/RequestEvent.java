package com.kru13.httpserver.event;

import android.util.Log;

import com.kru13.httpserver.http.ResponseProcessor;
import com.kru13.httpserver.service.HttpServerService;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class RequestEvent extends Thread {

    private final Socket socket;
    private final ResponseProcessor responseProcessor;
    private final HttpServerService service;

    private boolean complete;
    private boolean issued;
    private boolean proccessing;


    public RequestEvent(Socket socket, HttpServerService systemService) {
        this.socket = socket;
        this.complete = false;
        this.issued = false;
        this.proccessing = true;
        this.service = systemService;
        this.responseProcessor = new ResponseProcessor(systemService);
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


    public boolean isIssued() {
        return issued;
    }

    public void setIssued(boolean issued) {
        this.issued = issued;
    }


    public List<String> getHttpHeaders() {
        return this.responseProcessor.getHttp_req();
    }

}

