package com.kru13.httpserver.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.kru13.httpserver.SocketServer;
import com.kru13.httpserver.util.NotificationUtil;

public class HttpServerService extends IntentService {

    public static final String HTTP_SERVER_SERVICE_NAME = "HttpServerService";
    private SocketServer s;
    private NotificationManager notificationManager;
    private Context context;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public HttpServerService() {
        super(HTTP_SERVER_SERVICE_NAME);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        this.s = new SocketServer(this);
        try {
            this.s.run();
        } catch (InterruptedException e) {
            Log.d(HTTP_SERVER_SERVICE_NAME, "interrupted exception");
            this.stopSelf();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.context = getApplicationContext();
        this.notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service HTTP Server has been started", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        closeSocket();
        Toast.makeText(this, "Service HTTP Server has been stopped and will be destroyed", Toast.LENGTH_LONG).show();
        super.onDestroy();
    }

    private void closeSocket() {
        this.s.close();
    }

    public void createNotification(String content) {
        notificationManager.notify(NotificationUtil.nextId(), NotificationUtil.makeNotification(context, HTTP_SERVER_SERVICE_NAME, content));
    }
}
