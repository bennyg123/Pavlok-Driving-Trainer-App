package com.example.bennyg.pavlokdrivingtrainerv3;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

/**
 * Created by bennyg on 12/4/16.
 */

/*
    For ease of use we created a Notification wrapper class so we could create a notification from
    the BackgroundService
 */

public class NotificationWrapper {

    public static final int NOTIFICATION_ID = 1;
    public static NotificationCompat.Builder notificationBuilderR,notificationBuilderP;
    public static final String ACTION_P = "PAUSED",ACTION_R = "RESUME";
    public static Intent action1Intent,action2Intent;
    public static NotificationManagerCompat notificationManager;
    public static PendingIntent action1PendingIntent,action2PendingIntent;

    public static void displayNotification(Context context, boolean b) {
        action1Intent = new Intent(context, NotificationWrapper.NotificationActionService.class)
                    .setAction(ACTION_R);

        action2Intent = new Intent(context, NotificationWrapper.NotificationActionService.class)
                    .setAction(ACTION_P);

        action1PendingIntent = PendingIntent.getService(context, 0,
                action1Intent, PendingIntent.FLAG_UPDATE_CURRENT);

        action2PendingIntent = PendingIntent.getService(context, 0,
                action2Intent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilderR =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.mipmap.logo)
                        .setContentTitle("Pavlok driving trainer")
                        .setContentText("Service is running")
                        .setContentIntent(action2PendingIntent);

        notificationBuilderP =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.mipmap.logo)
                        .setContentTitle("Pavlok driving trainer")
                        .setContentText("Service is paused")
                        .setContentIntent(action1PendingIntent);


        notificationManager = NotificationManagerCompat.from(context);

        if (!b) {
            notificationManager.notify(NOTIFICATION_ID, notificationBuilderR.build());
        }else {
            notificationManager.notify(NOTIFICATION_ID, notificationBuilderP.build());
        }
    }

    public static class NotificationActionService extends IntentService {
        public NotificationActionService() {
            super(NotificationWrapper.NotificationActionService.class.getSimpleName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            String action = intent.getAction();

            if (ACTION_R.equals(action)) {
                BackgroundServices.setPaused(false);
                notificationManager.cancel(NOTIFICATION_ID);
                notificationManager.notify(
                        NOTIFICATION_ID,
                        notificationBuilderR.build()
                );
            }else if (ACTION_P.equals(action)){
                BackgroundServices.setPaused(true);
                notificationManager.cancel(NOTIFICATION_ID);
                notificationManager.notify(
                        NOTIFICATION_ID,
                        notificationBuilderP.build()
                );
            }
        }
    }

    public static void cancel() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

}
