package com.example.photoscreenguard;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import java.util.Calendar;

public class BrightnessService extends Service {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long phaseStart = 0L;
    private boolean lastActive = false;
    private int lastValue = -1;
    private final Runnable update = new Runnable() {
        @Override public void run() {
            if (!com.example.photoscreenguard.Settings.enabled(BrightnessService.this)) { stopSelf(); return; }
            if (!Settings.System.canWrite(BrightnessService.this)) { stopSelf(); return; }
            Calendar now=Calendar.getInstance();
            boolean active=com.example.photoscreenguard.Settings.activeNow(BrightnessService.this,now);
            if (phaseStart==0 || lastActive!=active) { phaseStart=SystemClock.elapsedRealtime(); lastActive=active; }
            int normal=com.example.photoscreenguard.Settings.getInt(BrightnessService.this,"normal",60);
            int dim=com.example.photoscreenguard.Settings.getInt(BrightnessService.this,"dim",5);
            int on=com.example.photoscreenguard.Settings.getInt(BrightnessService.this,"on_minutes",30);
            int off=com.example.photoscreenguard.Settings.getInt(BrightnessService.this,"off_minutes",20);
            long elapsed=(SystemClock.elapsedRealtime()-phaseStart)/60000L;
            boolean bright=active && (elapsed%(on+off)<on);
            int p=bright?normal:dim;
            int raw=com.example.photoscreenguard.Settings.toRaw(p);
            try {
                if(raw!=lastValue){
                    Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,raw);
                    lastValue=raw;
                }
            } catch (SecurityException e) { stopSelf(); return; }
            handler.postDelayed(this,15000L);
        }
    };
    @Override public void onCreate(){
        super.onCreate();
        NotificationManager mgr=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26) mgr.createNotificationChannel(new NotificationChannel("brightness","屏幕亮度管理",NotificationManager.IMPORTANCE_LOW));
        Intent intent=new Intent(this,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification=new Notification.Builder(this,"brightness")
            .setContentTitle("相册屏幕管家运行中")
            .setContentText("根据时间表自动调节亮度")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pi).setOngoing(true).build();
        startForeground(1,notification);
        // Save original system settings only once while starting a new session.
        android.content.SharedPreferences prefs=com.example.photoscreenguard.Settings.prefs(this);
        if(!prefs.contains("original_brightness")){
            try {
                prefs.edit().putInt("original_brightness",Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS))
                    .putInt("original_mode",Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE)).apply();
            }catch(Exception ignored){}
        }
        handler.post(update);
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){ return START_STICKY; }
    @Override public void onDestroy(){ handler.removeCallbacksAndMessages(null); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent){return null;}
    static void start(Context c){
        Intent intent=new Intent(c,BrightnessService.class);
        if (Build.VERSION.SDK_INT>=26) c.startForegroundService(intent); else c.startService(intent);
    }
    static void stop(Context c){
        c.stopService(new Intent(c,BrightnessService.class));
        android.content.SharedPreferences prefs=com.example.photoscreenguard.Settings.prefs(c);
        if(Settings.System.canWrite(c)){
            try {
                if (prefs.contains("original_brightness")) Settings.System.putInt(c.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,prefs.getInt("original_brightness",128));
                if (prefs.contains("original_mode")) Settings.System.putInt(c.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,prefs.getInt("original_mode",0));
            } catch (Exception ignored) {}
        }
        prefs.edit().remove("original_brightness").remove("original_mode").apply();
    }
}
