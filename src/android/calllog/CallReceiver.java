package cz.raynet.raynetcrm.calllog;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.TelephonyManager;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Date;

import static android.app.Notification.DEFAULT_VIBRATE;
import static android.app.Notification.EXTRA_NOTIFICATION_ID;

public class CallReceiver extends BroadcastReceiver {

    private static final String NEW_OUTGOING_CALL = "android.intent.action.NEW_OUTGOING_CALL";
    private static final String PHONE_NUMBER = "android.intent.extra.PHONE_NUMBER";

    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static Date callStartTime;
    private static boolean isIncoming;
    private static String savedNumber;


    @Override
    public void onReceive(Context context, Intent intent) {
        if (!CallLogPlugin.logEnabled()) {
            return;
        }

        if (NEW_OUTGOING_CALL.equals(intent.getAction())) {
            savedNumber = intent.getExtras().getString(PHONE_NUMBER);
        } else {
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            int state = 0;
            if (TelephonyManager.EXTRA_STATE_IDLE.equals(stateStr)) {
                state = TelephonyManager.CALL_STATE_IDLE;
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(stateStr)) {
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            } else if (TelephonyManager.EXTRA_STATE_RINGING.equals(stateStr)) {
                state = TelephonyManager.CALL_STATE_RINGING;
            }

            onCallStateChanged(context, state, number);
        }
    }

    public void onCallStateChanged(Context context, int state, String number) {
        if (lastState == state) {
            //No change, debounce extras
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                callStartTime = new Date();
                savedNumber = number;

                Toast.makeText(context, "Incoming Call Ringing", Toast.LENGTH_LONG).show();
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false;
                    callStartTime = new Date();

                    Toast.makeText(context, "Outgoing Call Started", Toast.LENGTH_LONG).show();
                }

                break;
            case TelephonyManager.CALL_STATE_IDLE:
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    //Ring but no pickup-  a miss
                    Toast.makeText(context, "Ringing but no pickup" + savedNumber + " Call time " + callStartTime + " Date " + new Date(), Toast.LENGTH_LONG).show();
                } else {
//                    CallLogDialog.create(context, savedNumber, isIncoming).show();
                    sendNotification(context);

                    //                    Toast.makeText(context, "outgoing " + savedNumber + " Call time " + callStartTime + " Date " + new Date(), Toast.LENGTH_LONG).show();
                }

                break;
        }

        lastState = state;
    }

    private void sendNotification(Context context) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(-1, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("raynetCrmChannelId", "RAYNEt CRM", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent actionIntent = new Intent(context, CallDialogActionReceiver.class);
        actionIntent.setAction("ActionSaveCall");
        actionIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context, 0, actionIntent, 0);

        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.bigText("Big text");
        bigText.setBigContentTitle("Small text text");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "raynetCrmChannelId");
        builder.setSmallIcon(R.mipmap.icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.icon))
                .setColor(context.getResources().getColor(R.color.primary))
                .setContentTitle("Big text")
                .setContentIntent(notificationPendingIntent)
                .setContentText("Small text text")
                .setDefaults(Notification.DEFAULT_ALL)
                .setStyle(bigText)
                .setDefaults(DEFAULT_VIBRATE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(0, "Zapsat do CRM", actionPendingIntent)
                .addAction(0, "Zrušit", actionPendingIntent);

        builder.setAutoCancel(true);

        notificationManager.notify(0, builder.build());
    }
}