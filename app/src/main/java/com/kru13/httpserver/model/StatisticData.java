package com.kru13.httpserver.model;

public class StatisticData {
    private int currentActiveClients;
    private int currentTransferredBytes;
    private int currentRequestCount;

    public int getCurrentActiveClients() {
        return currentActiveClients;
    }

    public void setCurrentActiveClients(int currentActiveClients) {
        this.currentActiveClients = currentActiveClients;
    }

    public int getCurrentTransferredBytes() {
        return currentTransferredBytes;
    }

    public void setCurrentTransferredBytes(int currentTransferredBytes) {
        this.currentTransferredBytes = currentTransferredBytes;
    }

    public int getCurrentRequestCount() {
        return currentRequestCount;
    }

    public void setCurrentRequestCount(int currentRequestCount) {
        this.currentRequestCount = currentRequestCount;
    }

    @Override
    public String toString() {
        return "StatisticData{" +
                "currentActiveClients=" + currentActiveClients +
                ", currentTransferredBytes=" + currentTransferredBytes +
                ", currentRequestCount=" + currentRequestCount +
                '}';
    }
}
