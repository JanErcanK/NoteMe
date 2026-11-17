package de.noteme.app;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.view.*;

public final class BearService extends Service {
    static final String DISABLE="de.noteme.DISABLE";
    private static BearService active;
    private static boolean capturing;
    private WindowManager wm;
    private BearArt bear;
    static boolean enabled(Context c) {
        return c.getSharedPreferences("bear",MODE_PRIVATE).getBoolean("enabled",false) && Settings.canDrawOverlays(c);
    }
    static void enable(Context c,boolean value) {
        c.getSharedPreferences("bear",MODE_PRIVATE).edit().putBoolean("enabled",value).apply();
        Intent i=new Intent(c,BearService.class);
        if(value) c.startForegroundService(i); else c.stopService(i);
    }
    static void capturing(boolean value) {
        capturing=value;
        if(active!=null && active.bear!=null) active.bear.setVisibility(value?View.GONE:View.VISIBLE);
    }
    public int onStartCommand(Intent intent,int flags,int startId) {
        if((intent!=null && DISABLE.equals(intent.getAction())) || !enabled(this)) {
            getSharedPreferences("bear",MODE_PRIVATE).edit().putBoolean("enabled",false).apply();
            stopSelf(); return START_NOT_STICKY;
        }
        NotificationManager nm=getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("bear","Randbär",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,BearService.class).setAction(DISABLE),PendingIntent.FLAG_IMMUTABLE);
        startForeground(8,new Notification.Builder(this,"bear").setSmallIcon(R.drawable.ic_bear)
            .setContentTitle("Dein Randbär ist aktiv").setContentText("Antippen, um das Notizbuch zu öffnen.")
            .setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Ausblenden",stop).build()).build());
        active=this;
        if(bear!=null) return START_STICKY;
        wm=getSystemService(WindowManager.class);
        bear=new BearArt(this,BearArt.Scene.EDGE); bear.setContentDescription("Schnellnotiz schreiben"); bear.setFocusable(true);
        bear.setOnClickListener(v->startActivity(new Intent(this,CaptureActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP)));
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(Ui.dp(this,54),Ui.dp(this,68),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.RIGHT|Gravity.CENTER_VERTICAL;
        try { wm.addView(bear,params); bear.setVisibility(capturing?View.GONE:View.VISIBLE); }
        catch (SecurityException | WindowManager.BadTokenException e) {
            bear=null; getSharedPreferences("bear",MODE_PRIVATE).edit().putBoolean("enabled",false).apply(); stopSelf(); return START_NOT_STICKY;
        }
        return START_STICKY;
    }
    public IBinder onBind(Intent intent) { return null; }
    public void onDestroy() {
        if(bear!=null) wm.removeView(bear);
        active=null; super.onDestroy();
    }
}

