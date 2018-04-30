package com.kru13.httpserver;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.kru13.httpserver.event.RequestEvent;
import com.kru13.httpserver.model.StatisticData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

public class StatisticManager extends Thread {

    private static final int SYNC_TIME = 1500;
    private static final File storage;

    static {
        try {
            storage = File.createTempFile("statistic-manager", null);
            Log.d("STATISTIC MANAGER", "create storage " + storage.getPath());
        } catch (IOException e) {
            throw new IllegalStateException("Could not create storage");
        }
    }

    private final List<RequestEvent> clients;
    private final Handler handler;
    private boolean running;

    public StatisticManager(List<RequestEvent> clients) {
        this.clients = clients;
        this.handler = null;
    }

    public StatisticManager(Handler handler) {
        this.handler = handler;
        this.clients = null;
    }

    @Override
    public void run() {
        running = true;
        Log.d("STATISTIC MANAGER", "start service");
        int activeClients;
        int transferredBytes;
        int requestCount;
        while (running) {

            if (clients != null) {
                activeClients = 0;
                transferredBytes = 0;
                requestCount = 0;

                synchronized (clients) {
                    for (RequestEvent r : clients) {
                        if (!r.isIssued()) {
                            transferredBytes += parseContentLength(r);
                            requestCount++;
                            r.setIssued(true);
                        }
                    }
                    activeClients = parseActiveClients(clients);
                }

                if (!(activeClients == 0 && transferredBytes == 0 && requestCount == 0)) {
                    updateData(activeClients, transferredBytes, requestCount);
                }
            }

            if (handler != null) {
                StatisticData data = readData();
                Message message = Message.obtain();
                message.obj = data;
                handler.sendMessage(message);
            }

            try {
                // Synchronized
                Thread.sleep(SYNC_TIME);
            } catch (InterruptedException e) {
                Log.d("STATISTIC MANAGER", "synchronization error");
            }
        }

        Log.d("STATISTIC MANAGER", "delete storage file " + (storage.delete() ? "success" : "false"));
    }

    private int parseActiveClients(List<RequestEvent> clients) {
        int counter = 0;
        for (RequestEvent r : clients) {
            if (!r.isComplete()) {
                counter++;
            }
        }
        return counter;
    }

    private StatisticData readData() {

        StatisticData statisticData = new StatisticData();

        String buffer;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(storage)));
            buffer = reader.readLine();

            if (buffer != null) {
                if (!buffer.isEmpty()) {
                    String[] data = buffer.split(" ");
                    statisticData.setCurrentActiveClients(Integer.parseInt(data[0]));
                    statisticData.setCurrentTransferredBytes(Integer.parseInt(data[1]));
                    statisticData.setCurrentRequestCount(Integer.parseInt(data[2]));
                }
            }
        } catch (FileNotFoundException e) {
            Log.d("STATISTIC", "could not read data " + e);
        } catch (IOException e) {
            Log.d("STATISTIC", "could not read data " + e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.d("STATISTIC", "" + e);
                }
            }
        }

        return statisticData;
    }

    private int parseContentLength(RequestEvent r) {
        int contentLength = 0;

        for (String h : r.getHttpHeaders()) {
            if (h.startsWith("Content-Length: ")) {
                try {
                    contentLength = Integer.parseInt(h.split("Content-Length: ")[1]);
                } catch (Exception e) {
                    Log.d("STATISTIC MANAGER", "can not parse " + h + " header");
                }
                break;
            }
        }

        return contentLength;
    }

    private synchronized void updateData(int activeClients, int transferredBytes, int requestCount) {

        StatisticData statisticData = readData();

        int currentTransferredBytes = statisticData.getCurrentTransferredBytes();
        int currentRequestCount = statisticData.getCurrentRequestCount();

        currentTransferredBytes += transferredBytes;
        currentRequestCount += requestCount;

        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(storage)));
            out.write(String.valueOf(activeClients) + " " + String.valueOf(currentTransferredBytes) + " " + String.valueOf(currentRequestCount) + "\n");
            out.newLine();
            out.flush();
        } catch (FileNotFoundException e) {
            Log.d("STATISTIC", "could not write data " + e);
        } catch (IOException e) {
            Log.d("STATISTIC", "could not write data" + e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.d("STATISTIC", "" + e);
                }
            }
        }
    }

    public void cancel() {
        Log.d("STATISTIC MANAGER", "stopping service");
        this.running = false;
    }
}