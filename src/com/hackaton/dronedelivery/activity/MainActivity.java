package com.hackaton.dronedelivery.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MotionEvent;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

public class MainActivity extends SuperMainActivity {
	private GestureDetector mGestureDetector;
//	private DroneProxy mDrone;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mGestureDetector = createGestureDetector(this);
//		mDrone = DroneProxy.getInstance();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    private GestureDetector createGestureDetector(Context context) {
    GestureDetector gestureDetector = new GestureDetector(context);
        //Create a base listener for generic gestures
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TWO_TAP) {
                    // move forward
                    return true;
                } else if (gesture == Gesture.TWO_SWIPE_RIGHT) {
                    // move right
                    return true;
                } else if (gesture == Gesture.TWO_SWIPE_LEFT) {
                    // move left
                    return true;
                } else if (gesture == Gesture.TWO_SWIPE_UP) {
                    // move up
//                	drone.doConnect(this, recordVideoResolution, callback);
                    return true;
                } else if (gesture == Gesture.TWO_SWIPE_DOWN) {
                    // move down
                    return true;
                } else if (gesture == Gesture.TWO_LONG_PRESS) {
                    // hold on
                    return true;
                } 
                return false;
            }
        });
        return gestureDetector;
    }
    
    /*
     * Send generic motion events to the gesture detector
     */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }
    
//    DroneConnection.startup(this, new DroneConnection.DroneStateListener() {
//        @Override
//        public void onDroneEnable(DroneConnection.DroneControl droneControl) {
//            mDrone = droneControl.getDrone();
//            mDroneControl = droneControl;
//            mDrone.doConnect(DronActivity.this, DroneProxy.EVideoRecorderCapability.VIDEO_360P, new DroneProxy.ConnectCallback() {
//                @Override
//                public void onConnect() {
//                    Log.v(TAG, "connected!");
//                    mDrone.resume();
//                    mDrone.triggerConfigUpdateNative();
//                    mDrone.flatTrimNative();
//                    isDroneOnLine = true;
//                }
//                @Override
//                public void onDisconnect() {
//                    Log.v(TAG, "disconnected!");
//                    isDroneOnLine = false;
//                    Toast.makeText(DronActivity.this, "Disconnected", Toast.LENGTH_LONG).show();
//                    mDrone = null;
//                    finish();
//                }
//                @Override
//                public void onBatteryLevelChange(int level) {
//                    DronActivity.this.onBatteryLevelChange(level);
//                }
//                @Override
//                public void onNewVideoRecorded(String path) {
//                    DronActivity.this.onNewVideoRecorded(path);
//                }
//                @Override
//                public void onVideoRecStarted() {
//                    DronActivity.this.onVideoRecStarted();
//                }
//                @Override
//                public void onVideoRecStopped() {
//                    DronActivity.this.onVideoRecStopped();
//                }
//
//                @Override
//                public void onAltitudeChange(int altitude) {
//                    DronActivity.this.onAltitudeChange(altitude);
//                }
//            });
//        }
//        @Override
//        public void onDroneOffline() {
//            Toast.makeText(DronActivity.this, "Can't connect to drone", Toast.LENGTH_LONG).show();
//        }
//        @Override
//        public void onDroneDisable() {
//            Toast.makeText(DronActivity.this, "Disconnect from drone", Toast.LENGTH_LONG).show();
//        }
//    });
//}
}
