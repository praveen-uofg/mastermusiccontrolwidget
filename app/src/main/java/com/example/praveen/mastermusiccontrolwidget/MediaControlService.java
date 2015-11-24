package com.example.praveen.mastermusiccontrolwidget;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.KeyEvent;

import com.example.praveen.mastermusiccontrolwidget.Utils.IntentActions;

import java.util.List;

/**
 * Created by praveen on 10/8/2015.
 */
public class MediaControlService extends NotificationListenerService implements MediaSessionManager.OnActiveSessionsChangedListener {
    public static final String T = MediaControlService.class.getName();

    String packageName;
    String title;
    RemoteView mViews;
    ComponentName componentName;
    MediaController mController;
    List<MediaController> mediaControllerList;
    MediaSessionManager mSessionManager;
    private static boolean sInstance = false;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        packageName = sbn.getPackageName();
        Bundle bundle = sbn.getNotification().extras;
        title = bundle.getString("android.title");
        Log.v(T, "onNotificationPosted title = " + title);

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        Log.v(T, "onNotificationRemoved");
        checkRemainingNotification();
    }

    private void checkRemainingNotification() {
        StatusBarNotification [] sbn = getActiveNotifications();
        for(StatusBarNotification s :sbn) {
            if (s.getPackageName().contains("music") ) {
                return;
            }
        }
        initializeMediaController();
        if (mController != null) {
           checkRemoteViewNeedsUpdate(mController);
        } else {
            mRemoteHandler.sendEmptyMessage(IntentActions.MSG_CLEAR_VIEW);
        }
    }

    @Override
    public StatusBarNotification[] getActiveNotifications() {
        return super.getActiveNotifications();

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(T, "onCreate");
        registerReceivers();
    }

    private void registerReceivers() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IntentActions.TOGGLE_ACTION);
        intentFilter.addAction(IntentActions.PLAY_NEXT);
        intentFilter.addAction(IntentActions.PLAY_PREV);
        intentFilter.addAction(IntentActions.SESSION_DESTROYED);
        registerReceiver(mIntentReceiver, intentFilter);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getAllMediaPlayers();
        startServiceController();
        return super.onStartCommand(intent, flags, startId);
    }

    private void startServiceController() {
        ComponentName mConponent = new ComponentName(this,MediaControlService.class);
        mSessionManager = (MediaSessionManager)getSystemService(Context.MEDIA_SESSION_SERVICE);
        mSessionManager.addOnActiveSessionsChangedListener(this, mConponent);
        initializeThread();
    }

    private void initializeThread() {
        HandlerThread h = new HandlerThread("playerThread");
        h.start();
        mRemoteHandler =   new RemoteHandler(h.getLooper());
        initializeMediaController();
    }

    RemoteHandler mRemoteHandler;

    public class RemoteHandler extends Handler {
        final Looper mLooper;
        RemoteHandler(Looper looper) {
            mLooper = looper;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(T,"handleMessage()");
            switch (msg.what) {
                case IntentActions.MSG_UPDATE_VIEW :
                    mViews = RemoteView.getInstance();
                    mViews.setContextAndController(getApplicationContext(),mController);
                    break;
                case IntentActions.MSG_CLEAR_VIEW :
                    mViews = RemoteView.getInstance();
                    mViews.setContextAndController(getApplicationContext(),null);
                    break;


            }
        }
    }
    private void initializeMediaController() {
        ComponentName mConponent = new ComponentName(this,MediaControlService.class);
        List<MediaController> controllers = mSessionManager.getActiveSessions(mConponent);
        Log.v(T, "initializeMediaController size = " + controllers.size());
        updateController(controllers);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sInstance = true;
        unregisterReceiver(mIntentReceiver);
        mController = null;
        RemoteView view = RemoteView.getInstance();
        view.setContextAndController(this, null);
    }

    @Override
    public void onActiveSessionsChanged(List<MediaController> controllers) {
        mediaControllerList = controllers;
        updateController(mediaControllerList);
    }

    private void updateController(List<MediaController> mediaControllerList) {
        if (mediaControllerList.size()>0) {
            if (checkValidPackage(mediaControllerList.get(0))) {
                if (canHandleMediaButton(mediaControllerList.get(0))) {
                    if (canHandleMediaButton(mediaControllerList.get(0))) {
                        checkRemoteViewNeedsUpdate(mediaControllerList.get(0));
                    }

                }
            }
        } else {
            mRemoteHandler.sendEmptyMessage(IntentActions.MSG_CLEAR_VIEW);
        }
    }

    private void checkRemoteViewNeedsUpdate(MediaController mediaController) {
        if (mController == null || (mController.getSessionToken() != mediaController.getSessionToken())){
            mController = mediaController;
            mRemoteHandler.sendEmptyMessage(IntentActions.MSG_UPDATE_VIEW);
        }
    }

    private boolean checkValidPackage(MediaController mediaController) {
        String packageName = mediaController.getPackageName();
        return !(packageName.contains("videoplayer") || packageName.contains("record"));
    }

    private boolean canHandleMediaButton(MediaController mediaController) {
        return (mediaController.getFlags() & MediaSession.FLAG_HANDLES_MEDIA_BUTTONS) != 0;
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(T, "onReceive() action = " + intent.getAction());
            String action = intent.getAction();
            switch (action) {
                case IntentActions.TOGGLE_ACTION:
                    mController.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                    mController.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                    break;
                case IntentActions.PLAY_NEXT:
                    mController.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
                    mController.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT));
                    break;
                case IntentActions.PLAY_PREV:
                    mController.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
                    mController.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
                    break;
                case IntentActions.SESSION_DESTROYED:
                    initializeMediaController();
                    if (mController == null) {
                        mRemoteHandler.sendEmptyMessage(IntentActions.MSG_CLEAR_VIEW);
                    } else {
                        mRemoteHandler.sendEmptyMessage(IntentActions.MSG_UPDATE_VIEW);
                    }
                    break;
            }
        }
    };

    void getAllMediaPlayers() {
        Log.i(T, "getAllMediaPlayers");
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        List<ResolveInfo> mPackages;
        PackageManager pm = getPackageManager();
        mPackages = pm.queryBroadcastReceivers(intent, 0);

        for(int i=0;i<mPackages.size();i++)
            Log.i(T,"package = "+mPackages.get(i).toString() );
    }

    public static  boolean getServiceInstance() {
        return sInstance;
    }
}
