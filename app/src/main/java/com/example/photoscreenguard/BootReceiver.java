package com.example.photoscreenguard;
import android.content.*;
import android.provider.Settings;
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent){
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && com.example.photoscreenguard.Settings.enabled(context) && Settings.System.canWrite(context)) {
            BrightnessService.start(context);
        }
    }
}
