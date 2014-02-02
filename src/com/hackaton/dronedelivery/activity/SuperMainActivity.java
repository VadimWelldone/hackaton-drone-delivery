package com.hackaton.dronedelivery.activity;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.hackaton.dronedelivery.flight.DroneConnection;
import com.hackaton.dronedelivery.flight.DroneProxy;
import com.hackaton.dronedelivery.flight.GLBGVideoSprite;
import com.hackaton.dronedelivery.flight.GLSprite;
import com.hackaton.dronedelivery.flight.Text;
import com.hackaton.dronedelivery.flight.VideoStageRenderer;

public class SuperMainActivity extends Activity {
	private GLSurfaceView mGlView;
    private GLBGVideoSprite mVideoSprite;
    private Text mBatteryLevel;
    private Text mAltitude;
    private Text mRecStatus;
    private Text mClickCount;
    private Text mAxisState;
    private GLSprite mControlModeSprite;
    private Text mTextLog;
    private static final String TAG = "DRONE";

    private final Runnable ACTION_SYNC_VIDEO_TIME = new Runnable() {
        @Override
        public void run() {
            mRecStatus.setText(String.format("Video rec. %.2f sec", (System.currentTimeMillis()-mTimeStarted)/1000.0f));
            mMainHandler.postDelayed(ACTION_SYNC_VIDEO_TIME, 300);
        }
    };
    private final Handler mMainHandler = new Handler();
    private long mTimeStarted = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Log.v(TAG, "Main activity started...");
        mGlView = new GLSurfaceView(this);
		mGlView.setEGLContextClientVersion(2);
        VideoStageRenderer renderer = new VideoStageRenderer();
        mVideoSprite = new GLBGVideoSprite();
        mRecStatus = new Text(this);
        mControlModeSprite = new GLSprite();
        mClickCount = new Text(this);

        mBatteryLevel = new Text(this);
        mBatteryLevel.setText("not set");
        mBatteryLevel.setTextColor(0xffffffff);
        mBatteryLevel.setTextSize(32);
        mBatteryLevel.setAlpha(1.0f);
        mBatteryLevel.setPosition(40.0f, 40.0f);

        mAxisState = new Text(this);
        mAxisState.setAlpha(1.0f);
        mAxisState.setTextColor(0xffffffff);
        mAxisState.setTextSize(32);
        mAxisState.setPosition(290.0f, 30.0f);

        mAltitude = new Text(this);
        mAltitude.setText("not set");
        mAltitude.setTextColor(0xffffffff);
        mAltitude.setTextSize(32);
        mAltitude.setAlpha(1.0f);
        mAltitude.setPosition(40.0f, 70.0f);

        mTextLog = new Text(this);
        mTextLog.setTextColor(0xffffffff);
        mTextLog.setTextSize(32);
        mTextLog.setAlpha(1.0f);
        mTextLog.setPosition(200.0f, 100.0f);

        mRecStatus.setTextColor(0xffffffff);
        mRecStatus.setTextSize(32);
        mRecStatus.setAlpha(1.0f);

        mClickCount.setTextSize(100);
        mClickCount.setCenter(true);
        mClickCount.setAlpha(0.7f);
        mClickCount.setBold(true);
        mClickCount.setTextColor(0xffff9900);

        mRecStatus.setPosition(40.0f, 10.0f);
//        Bitmap shot = BitmapFactory.decodeResource(getResources(), R.drawable.shot);
//        mControlModeSprite.updateTexture(shot);
//        mControlModeSprite.setCenter(true);
//        shot.recycle();
        renderer.addSprite(mVideoSprite);
        renderer.addSprite(mBatteryLevel);
        renderer.addSprite(mRecStatus);
//        renderer.addSprite(mControlModeSprite);
        renderer.addSprite(mTextLog);
        renderer.addSprite(mClickCount);
        renderer.addSprite(mAxisState);
        mControlModeSprite.hide();
		mGlView.setRenderer(renderer);
		setContentView(mGlView);
		DroneConnection.startup(this, new DroneConnection.DroneStateListener() {
            @Override
            public void onDroneEnable(DroneConnection.DroneControl droneControl) {
                final DroneProxy mDrone = droneControl.getDrone();
                DroneConnection.DroneControl mDroneControl = droneControl;
                mDrone.doConnect(SuperMainActivity.this, DroneProxy.EVideoRecorderCapability.VIDEO_360P, new DroneProxy.ConnectCallback() {
                    @Override
                    public void onConnect() {
                        Log.v(TAG, "connected!");
                        mDrone.resume();
                        mDrone.triggerConfigUpdateNative();
                        mDrone.flatTrimNative();
//                        isDroneOnLine = true;
                    }
                    @Override
                    public void onDisconnect() {
                        Log.v(TAG, "disconnected!");
//                        isDroneOnLine = false;
//                        Toast.makeText(this, "Disconnected", Toast.LENGTH_LONG).show();
//                        mDrone = null;
                        finish();
                    }
                    @Override
                    public void onBatteryLevelChange(int level) {
                        this.onBatteryLevelChange(level);
                    }
                    @Override
                    public void onNewVideoRecorded(String path) {
                    	this.onNewVideoRecorded(path);
                    }
                    @Override
                    public void onVideoRecStarted() {
                        this.onVideoRecStarted();
                    }
                    @Override
                    public void onVideoRecStopped() {
                        this.onVideoRecStopped();
                    }

                    @Override
                    public void onAltitudeChange(int altitude) {
                        this.onAltitudeChange(altitude);
                    }
                });
            }

			@Override
			public void onDroneOffline() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onDroneDisable() {
			}
		});
	}
//    @Override
//    protected void onCounterChanged(int counter) {
//        if (counter == 0) {
//            mClickCount.hide();
//        } else {
//            mClickCount.show();
//            mClickCount.setText(String.valueOf(counter));
//        }
//    }
//    @Override
//    protected void onNewVideoRecorded(String path) {
//        TimelineHelper helper = new TimelineHelper();
//        TimelineItem.Builder b = helper.createTimelineItemBuilder(this, new SettingsSecure(getContentResolver()));
//        b.setIsPinned(false);
//        b.setText("Recorded new video");
//        b.setNotification(NotificationConfig.newBuilder().setLevel(NotificationConfig.Level.DEFAULT));
//        b.setId(UUID.randomUUID().toString());
//        List<TimelineItem> items = new ArrayList<TimelineItem>();
//        items.add(b.build());
//        helper.bulkInsertTimelineItem(this, items);
//        finish();
//    }
//    @Override
//    protected void onVideoRecStarted() {
//        mRecStatus.show();
//        mTimeStarted = System.currentTimeMillis();
//        mMainHandler.post(ACTION_SYNC_VIDEO_TIME);
//    }
//    @Override
//    protected void onVideoRecStopped() {
//        mRecStatus.hide();
//    }
//    @Override
//    protected void onBatteryLevelChange(int level) {
//        mBatteryLevel.setText("Battery: " + level + "%");
//    }
//    @Override
//    protected void onAltitudeChange(int altitude) {
//        mAltitude.setText("Altitude: "+altitude);
//    }
	@Override
	public void onPause() {
		mGlView.onPause();
		super.onPause();
	}
//    @Override
//    protected void onControlModeChanged(boolean isControlByHead) {
//        if (isControlByHead) {
//            mControlModeSprite.show();
//        } else {
//            mControlModeSprite.hide();
//        }
//    }
    @Override
	protected void onResume() {
		mGlView.onResume();
		super.onResume();
	}
//    @Override
//    protected void showOrientation(float[] orientation) {
//        mTextLog.setText(String.format("%1.3f %1.3f %1.3f", orientation[0], orientation[1], orientation[2]));
//    }
//    @Override
//    protected void showLine(String someLine) {
//        mAxisState.setText(someLine);
//    }
}