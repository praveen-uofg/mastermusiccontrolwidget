package com.example.praveen.mastermusiccontrolwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;

import com.example.praveen.mastermusiccontrolwidget.Utils.ImageUtils;
import com.example.praveen.mastermusiccontrolwidget.Utils.IntentActions;

import java.util.List;

/**
 * Created by praveen on 10/8/2015.
 */
public class RemoteView {
    public static final String T = RemoteView.class.getName();
    Context mContext;
    RemoteViews views;
    String packageName;
    MediaMetadata mediaMetadata;
    public static  RemoteView mInstance;
    boolean isPlayiing = false;
    List <String> appIcons;

    int mMainLayout;
    int mDefaultLayout;
    int mControlLayout;
    int mPlayButton;
    int mNextButton;
    int mPrevButton;
    int mAppIcon;
    int mTitle;

    MediaController mController;


    public RemoteView() {
        initializeLayout();
    }

    public static RemoteView getInstance() {
        if (mInstance == null) {
            mInstance = new RemoteView();
            return mInstance;
        }
        return mInstance;
    }

    public void setContextAndController (Context context,MediaController mediaController) {
        Log.v(T, "setContextAndController mediacontroller = "+mediaController);
        mContext = context;

        if (mediaController == null) {
            setDefaultLayout();
        }else {
            mController = mediaController;
            mController.registerCallback(mCallback);
            mediaMetadata = mController.getMetadata();
            if (mController.getPlaybackState() != null) {
                isPlayiing = (mController.getPlaybackState().getState() == PlaybackState.STATE_PLAYING);

            }
            packageName = mController.getPackageName();
            performUpdate(isPlayiing);
        }
    }

    private void initializeLayout() {
        Log.v(T, "initializeLayout");
        mMainLayout = R.layout.new_app_widget;
        mDefaultLayout =R.id.defaultLayout;
        mControlLayout = R.id.controlLayout;
        mPlayButton = R.id.play;
        mNextButton = R.id.play_next;
        mPrevButton = R.id.play_prev;
        mAppIcon = R.id.app_icon;
        mTitle = R.id.title;

    }

    LayoutInflater inflater;
    RecyclerView mRecyclerView;
    LinearLayoutManager manager;

    private void setDefaultLayout() {
        Log.v(T, "setDefaultLayout");
        final RemoteViews views = new RemoteViews(mContext.getPackageName(), mMainLayout);
        views.setViewVisibility(mDefaultLayout, View.VISIBLE);
        views.setViewVisibility(mControlLayout, View.GONE);

        inflater = (LayoutInflater)mContext.getSystemService(mContext.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.default_layout,null,false);

        mRecyclerView = (RecyclerView)layout.findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);

        manager = new LinearLayoutManager(mContext);
        manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(manager);

        RecyclerViewAdapter adapter = new RecyclerViewAdapter(mContext,getAllMediaPlayers());
        mRecyclerView.setAdapter(adapter);

        linkButtons(views);
        pushUpdates(views);
    }

    private List getAllMediaPlayers() {
        Log.i(T, "getAllMediaPlayers");
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        List<ResolveInfo> mPackages;
        PackageManager pm = mContext.getPackageManager();
        mPackages = pm.queryBroadcastReceivers(intent, 0);
        return mPackages;
    }


    public void performUpdate(boolean isPlaying) {
        Log.v(T, "performUpdate isplaying = " + isPlaying);
        final RemoteViews views = new RemoteViews(mContext.getPackageName(), mMainLayout);
        views.setViewVisibility(mDefaultLayout, View.GONE);
        views.setViewVisibility(mControlLayout, View.VISIBLE);
        if (isPlaying) {
            views.setImageViewResource(mPlayButton,R.drawable.ic_new_icon_pause);
        } else {
            views.setImageViewResource(mPlayButton,R.drawable.ic_new_icon_play);
        }

        if (mediaMetadata != null) {
            CharSequence title = mediaMetadata.getText(MediaMetadata.METADATA_KEY_TITLE);
            views.setTextViewText(mTitle, title);
            views.setBoolean(mTitle,"setMarqueeAlwaysEnable",true);

            Bitmap bitmap = mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (bitmap == null) {
                bitmap = mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
            }
            if (bitmap != null) {
                views.setImageViewBitmap(mAppIcon, ImageUtils.getRoundedCornerBitmap(bitmap.createScaledBitmap(bitmap,64,64,true)));
                setLayoutColor(bitmap);
            } else {
                Bitmap icon = ImageUtils.getPackageIcon(mContext, packageName);
                views.setImageViewBitmap(mAppIcon, ImageUtils.getRoundedCornerBitmap(icon));
                setLayoutColor(icon);
            }

        }

        linkButtons(views);
        pushUpdates(views);
    }

    private void setLayoutColor( Bitmap bitmap) {
        final RemoteViews views = new RemoteViews(mContext.getPackageName(), mMainLayout);
        views.setInt(R.id.bgcolor, "setColorFilter", ImageUtils.getColor(mContext,bitmap,packageName));
        linkButtons(views);
        pushUpdates(views);
    }

    private void pushUpdates(RemoteViews views) {
        Log.v(T, "pushUpdates");
        AppWidgetManager manager = AppWidgetManager.getInstance(mContext);
        final ComponentName componentName = new ComponentName(mContext,UniversalMusicWidgetProvider.class);
        int [] ids = manager.getAppWidgetIds(componentName);
        manager.updateAppWidget(ids, views);
        //manager.updateAppWidget(componentName,views);
    }

    private void linkButtons(RemoteViews views) {
        Log.v(T, "linkButtons");
        Intent intent;
        PendingIntent pendingIntent;

        intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_MUSIC);
        pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        views.setOnClickPendingIntent(mDefaultLayout, pendingIntent);

        if (mController != null) {
            intent = mContext.getPackageManager().getLaunchIntentForPackage(mController.getPackageName());
            if (intent != null) {
                pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
                views.setOnClickPendingIntent(mAppIcon, pendingIntent);
            }

            intent = new Intent();
            intent.setAction(IntentActions.TOGGLE_ACTION);
            pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
            views.setOnClickPendingIntent(mPlayButton, pendingIntent);

            intent = new Intent(IntentActions.PLAY_NEXT);
            pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
            views.setOnClickPendingIntent(mNextButton, pendingIntent);

            intent = new Intent(IntentActions.PLAY_PREV);
            pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
            views.setOnClickPendingIntent(mPrevButton, pendingIntent);
        }
    }

    private MediaController.Callback mCallback;

    {
        mCallback = new MediaController.Callback() {
            @Override
            public void onMetadataChanged(MediaMetadata metadata) {
                Log.v(T, "onMetadataChanged title = " + metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
                super.onMetadataChanged(metadata);
                mediaMetadata = metadata;
                performUpdate(isPlayiing);

            }

            @Override
            public void onPlaybackStateChanged(PlaybackState state) {
                Log.v(T, "onPlaybackStateChanged state = " + state);
                super.onPlaybackStateChanged(state);
                if (state.getState() == PlaybackState.STATE_PLAYING) {
                    isPlayiing = true;
                } else {
                    isPlayiing = false;
                }
                performUpdate(isPlayiing);

            }

            @Override
            public void onSessionDestroyed() {
                Log.v(T, " onSessionDestroyed");
                unregisterCallback();
                Intent intent = new Intent("session_destroyed");
                mContext.sendBroadcast(intent);
                super.onSessionDestroyed();
            }
        };
    }

    private void unregisterCallback() {
        if (mController != null) {
            mController.unregisterCallback(mCallback);
        }
    }


}
