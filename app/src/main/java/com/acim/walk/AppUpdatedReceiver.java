package com.acim.walk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AppUpdatedReceiver extends BroadcastReceiver {

    /**
     * When a broadcast is received the sensor listener will be started
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(final Context context, final Intent intent) {
        context.startForegroundService(new Intent(context, SensorListener.class));
    }

}
