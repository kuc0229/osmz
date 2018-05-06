package com.kru13.httpserver.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import com.kru13.httpserver.ScreenshotActivity;
import com.kru13.httpserver.SocketServer;
import com.kru13.httpserver.StatisticManager;
import com.kru13.httpserver.util.NotificationUtil;

import java.util.Date;

public class HttpServerService extends IntentService {

    public static final String HTTP_SERVER_SERVICE_NAME = "HttpServerService";
    private SocketServer socketServer;
    private NotificationManager notificationManager;
    private StatisticManager statisticManager;
    private Context context;

    public HttpServerService() {
        super(HTTP_SERVER_SERVICE_NAME);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        this.socketServer = new SocketServer(this);
        this.statisticManager = new StatisticManager(this.socketServer.getClients());
        this.statisticManager.start();
        this.socketServer.listen();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.context = getApplicationContext();
        this.notificationManager =
                (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeSocket();
        if (statisticManager != null) {
            this.statisticManager.cancel();
        }
    }

    private void closeSocket() {
        if (socketServer != null) {
            this.socketServer.close();
        }
    }

    public void createNotification(String content) {
        Notification n = NotificationUtil.makeNotification(this.context, HTTP_SERVER_SERVICE_NAME, content);
        notificationManager.notify(NotificationUtil.nextId(), n);
    }

    public void createScreenshot() {
        createNotification("Create Screenshot at " + new Date());
        Intent i = new Intent(this.context, ScreenshotActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }
}
