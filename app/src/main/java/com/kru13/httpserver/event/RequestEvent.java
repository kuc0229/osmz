package com.kru13.httpserver.event;

import java.net.Socket;

public class RequestEvent {

    private final Socket socket;

    private boolean complete;
    private boolean issued;
    private boolean proccessing;
    private int transferredBytes;


    public RequestEvent(Socket socket) {
        this.socket = socket;
        this.complete = false;
        this.issued = false;
        this.proccessing = true;
        this.transferredBytes = 0;
    }

    @Override
    public int hashCode() {
        return socket.hashCode();
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public boolean isIssued() {
        return issued;
    }

    public void setIssued(boolean issued) {
        this.issued = issued;
    }

    public boolean isProccessing() {
        return proccessing;
    }

    public void setProccessing(boolean proccessing) {
        this.proccessing = proccessing;
    }

    public int getTransferredBytes() {
        return transferredBytes;
    }

    public void setTransferredBytes(int transferredBytes) {
        this.transferredBytes = transferredBytes;
    }
}

