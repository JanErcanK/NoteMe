package de.noteme.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class RestoreBear extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ((Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))
                && BearService.enabled(context)) {
            context.startForegroundService(new Intent(context, BearService.class));
        }
    }
}

