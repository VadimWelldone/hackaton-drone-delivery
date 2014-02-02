/*
 * DroneProxy
 *
 *  Created on: May 5, 2011
 *      Author: Dmytro Baryskyy
 */

package com.hackaton.dronedelivery.flight;

import java.io.File;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.Toast;

public class DroneProxy {
    public static final int CONTROL_SET_YAW   = 0;
    public static final int CONTROL_SET_GAZ   = 1;
    public static final int CONTROL_SET_PITCH = 2;
    public static final int CONTROL_SET_ROLL  = 3;

    public enum DroneProgressiveCommandFlag
    {
        ARDRONE_PROGRESSIVE_CMD_ENABLE,              // 1: use progressive commands - 0: try hovering
        ARDRONE_PROGRESSIVE_CMD_COMBINED_YAW_ACTIVE, // 1: activate combined yaw - 0: Deactivate combined yaw
        ARDRONE_MAGNETO_CMD_ENABLE,	               // 1: activate the magneto piloting mode - 0: desactivate the mode
    };


    public enum EVideoRecorderCapability {
        NOT_SUPPORTED,
        VIDEO_360P,
        VIDEO_720P
    }
    public interface ConnectCallback {
        public void onConnect();
        public void onDisconnect();
        public void onBatteryLevelChange(int level);
        public void onNewVideoRecorded(String path);
        public void onVideoRecStarted();
        public void onVideoRecStopped();
        public void onAltitudeChange(int altitude);
    }
    private static final String TAG = "DroneProxy";
    private volatile static DroneProxy instance;
    private NavData navdata;
    private DroneConfig config;
    private ConnectCallback mConnectCallback;
    private final Handler mCallbackHandler = new Handler();
    private final Runnable ACTION_UPDATE = new Runnable() {
        private int batStatus = 0;
        private int altitude = -1;
        private boolean recordReady = false;
        @Override
        public void run() {
            NavData newData = takeNavDataSnapshot(navdata);
            if (batStatus != newData.batteryStatus) {
                batStatus = newData.batteryStatus;
                mCallbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mConnectCallback.onBatteryLevelChange(batStatus);
                    }
                });
            }
            Log.i(TAG, "Altitude: "+navdata.altitude);
            if (altitude != newData.altitude) {
                altitude = newData.altitude;
                mCallbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mConnectCallback.onAltitudeChange(altitude);
                    }
                });
            }
            if (recordReady != newData.recordReady) {
                recordReady = newData.recordReady;
                if (recordReady) {
                    mCallbackHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mConnectCallback.onVideoRecStopped();
                        }
                    });
                } else {
                    mCallbackHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mConnectCallback.onVideoRecStarted();
                        }
                    });

                }
            }
            mWorkHandler.postDelayed(ACTION_UPDATE, 300);
        }
    };
    private Handler mWorkHandler;
    private HandlerThread mWorkThread;

    public static DroneProxy getInstance() {
        if (instance == null) {
            synchronized (DroneProxy.class) {
                if (instance == null) {
                    instance = new DroneProxy();
                }
            }
        }
        return instance;
    }
    private DroneProxy() {
        navdata = new NavData();
        config = new DroneConfig();
        initNavdata();
    }
    public void doConnect(Context context, EVideoRecorderCapability recordVideoResolution, ConnectCallback callback) {
        mConnectCallback = callback;
        try {
            Log.d(TAG, "Connecting...");
            String uid = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
            String packageName = this.getClass().getPackage().getName();
            Log.d(TAG, "AppName: " + packageName + ", UserID: " + uid);
            File dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File cacheDir = context.getExternalCacheDir();
            connect(packageName.trim(), uid.trim(), cacheDir.getAbsolutePath(), dcimDir.getAbsolutePath(), 0, recordVideoResolution.ordinal());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void doDisconnect() {
        disconnect();
    }
    public void onConnected() {
        Log.i(TAG, "onConnectedFromNative");
        mCallbackHandler.post(new Runnable() {
            @Override
            public void run() {
                mWorkThread = new HandlerThread("WorkThread") {
                    @Override
                    protected void onLooperPrepared() {
                        mWorkHandler = new Handler(mWorkThread.getLooper());
                        mWorkHandler.post(ACTION_UPDATE);
                        mCallbackHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setMagnetoEnabled(false);
                                mConnectCallback.onConnect();
                            }
                        });
                    }
                };
                mWorkThread.start();
            }
        });
    }

    public void onDisconnected() {
        mWorkThread.quit();
        try {
            mWorkThread.join();
        } catch (InterruptedException e) { }
        mCallbackHandler.post(new Runnable() {
            @Override
            public void run() {
                mConnectCallback.onDisconnect();
            }
        });
    }
    private void onConfigChanged() {
        Log.i("DroneProxy", "onConfigChanged");
    }
    public void onAcademyNewMediaReady(final String path, boolean addToQueue) {
        Log.i("DroneProxy", "Path: "+path);
        if (! addToQueue && path != null) {
            mCallbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    mConnectCallback.onNewVideoRecorded(path);
                }
            });
            resume();
            triggerConfigUpdateNative();
        }
    }
    public void recordVideo() {
        record();
    }
    public native void initNavdata();
    public native void triggerTakeOff();
    public native void triggerEmergency();
    public native void setControlValue(int control, float value);
    public native void setMagnetoEnabled(boolean absoluteControlEnabled);
    public native void setCommandFlag(int flag, boolean enable);
    public native void setDeviceOrientation(int heading, int headingAccuracy);
    public native void switchCamera();
    public native void triggerConfigUpdateNative();
    public native void flatTrimNative();
    public native void setDefaultConfigurationNative();
    public native void resetConfigToDefaults();
    public native void takePhoto();
    private native void record();
    public native void calibrateMagneto();
    public native void doFlip();
    public native void setLocation(double latitude, double longitude, double altitude);
    private native void connect(String appName, String username, String rootDir, String flightDir, int flightSize, int recordingCapabilities);
    private native void disconnect();
    public native void pause();
    public native void resume();
    private native NavData takeNavDataSnapshot(NavData navdata);
    private native DroneConfig takeConfigSnapshot(DroneConfig settings);
}
