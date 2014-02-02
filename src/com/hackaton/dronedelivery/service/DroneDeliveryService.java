package com.hackaton.dronedelivery.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.timeline.TimelineManager;
import com.hackaton.dronedelivery.R;
import com.hackaton.dronedelivery.activity.MenuActivity;

public class DroneDeliveryService extends Service {

	    private static final String TAG = "DroneDeliveryService";
	    private static final String LIVE_CARD_TAG = "drone_delivery";

//	    private ChronometerDrawer mCallback;

	    private TimelineManager mTimelineManager;
	    private LiveCard mLiveCard;

	    @Override
	    public void onCreate() {
	        super.onCreate();
	        mTimelineManager = TimelineManager.from(this);
	    }

	    @Override
	    public IBinder onBind(Intent intent) {
	        return null;
	    }

	    @Override
	    public int onStartCommand(Intent intent, int flags, int startId) {
	        if (mLiveCard == null) {
	            Log.d(TAG, "Publishing LiveCard");
	            mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_TAG);

//	            mLiveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(mCallback);
	            mLiveCard.setViews(new RemoteViews(getPackageName(), R.layout.main_live_card));

	            Intent menuIntent = new Intent(this, MenuActivity.class);
	            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
	            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

	            mLiveCard.publish(PublishMode.REVEAL);
	            Log.d(TAG, "Done publishing LiveCard");
	        } else {
	            // TODO(alainv): Jump to the LiveCard when API is available.
	        }

	        return START_STICKY;
	    }

	    @Override
	    public void onDestroy() {
	        if (mLiveCard != null && mLiveCard.isPublished()) {
	            Log.d(TAG, "Unpublishing LiveCard");
	            mLiveCard.unpublish();
	            mLiveCard = null;
	        }
	        super.onDestroy();
	    }
	}
