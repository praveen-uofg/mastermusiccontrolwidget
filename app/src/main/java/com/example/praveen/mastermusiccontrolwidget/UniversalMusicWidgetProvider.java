package com.example.praveen.mastermusiccontrolwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * Implementation of App Widget functionality.
 */
public class UniversalMusicWidgetProvider extends AppWidgetProvider {

    public static final String T = UniversalMusicWidgetProvider.class.getName();
    private Context mContext;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.v(T, "onUpdate");
        mContext = context;
        //registerContentObserver();
        checkPackageHasAccess();

    }

    @Override
    public void onEnabled(Context context) {
        Log.v(T, "onEnabled");
        mContext = context;
        registerContentObserver();
        RemoteView view = RemoteView.getInstance();
        view.setContextAndController(context,null);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.v(T, "onReceive");
    }

    @Override
    public void onDisabled(Context context) {
        Log.v(T, "onDisabled");
        mContext = context;
        unregisterContentObserver();
        Intent i = new Intent(context,MediaControlService.class);
        context.stopService(i);
    }

    ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.v(T, "onChange");
            checkPackageHasAccess();
        }
    };

    void registerContentObserver() {
        ContentResolver resolver = mContext.getContentResolver();
        Log.v(T, "registerContentObserver uri = "+Settings.Secure.getUriFor("enabled_notification_listeners").toString());
        resolver.registerContentObserver(Settings.Secure.getUriFor("enabled_notification_listeners"),true,mObserver);
    }

    void unregisterContentObserver() {
        if (mObserver != null ){
            mContext.getContentResolver().unregisterContentObserver(mObserver);
        }
    }
    Handler handler = new Handler();
    void checkPackageHasAccess() {
        ContentResolver contentResolver = mContext.getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = mContext.getPackageName();

        if (enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName)) {
            Toast.makeText(mContext,"Cannot Start Application without Permission",Toast.LENGTH_LONG).show();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                }
            }, 1000L);
        } else {
            Intent i = new Intent(mContext,MediaControlService.class);
            mContext.startService(i);
        }

    }


}

