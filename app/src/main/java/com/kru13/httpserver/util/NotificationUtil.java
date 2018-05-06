package com.kru13.httpserver.util;

import android.app.Notification;
import android.content.Context;

import com.kru13.httpserver.R;

public class NotificationUtil {

    private static int notificationId = Integer.MIN_VALUE;

    public static Notification makeNotification(Context context, String title, String content) {
        return new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher)
                .build();
    }

    public static int nextId() {
        if (notificationId == Integer.MAX_VALUE) {
            notificationId = Integer.MIN_VALUE;
        }
        return notificationId++;
    }
}
