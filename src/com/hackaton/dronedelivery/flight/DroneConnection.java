package com.hackaton.dronedelivery.flight;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;

/**
 * Created with IntelliJ IDEA.
 * User: valentine
 * Date: 7/26/13
 * Time: 10:01 AM
 */

public class DroneConnection {
    private static final String DRONE_SSID_PREFIX = "ardrone";

    public static void startup(Context context, final DroneStateListener droneStateListener) {
        final WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        final State state = new State(context);

        boolean isConnected = wifiManager.isWifiEnabled();
        if (isConnected) {
            checkCurrentConnection(wifiManager, droneStateListener, state);
        } else {
            state.isNeedToSwitchOffWifiOnClose = true;

            final BroadcastReceiver switchOnReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    state.context.unregisterReceiver(this);
                    checkCurrentConnection(wifiManager, droneStateListener, state);
                }
            };
            state.context.registerReceiver(switchOnReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
            wifiManager.setWifiEnabled(true);
        }
    }
    private static void checkCurrentConnection(WifiManager wifiManager, DroneStateListener droneStateListener, State state) {
        WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        String ssid = connectionInfo.getSSID();
        if (ssid == null) {
            switchToDronesNetwork(wifiManager, droneStateListener, state);
        } else if (!connectionInfo.getSSID().startsWith(DRONE_SSID_PREFIX)) {
            switchToDronesNetwork(wifiManager, droneStateListener, state);
        } else {
            droneStateListener.onDroneEnable(createControl(wifiManager, state));
        }
    }
    private static void switchToDronesNetwork(final WifiManager wifiManager, final DroneStateListener droneStateListener, final State state) {
        List<ScanResult> scanResults = wifiManager.getScanResults();
        ScanResult dronePoint = null;
        if (scanResults != null) {
            for (ScanResult sr : scanResults) {
                if (sr.SSID.startsWith(DRONE_SSID_PREFIX)) {
                    dronePoint = sr;
                    break;
                }
            }
        }
        if (dronePoint == null) {
            if (state.isScanStarted) {
                state.isScanStarted = false;
                droneStateListener.onDroneOffline();
            } else {
                final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        context.unregisterReceiver(this);
                        switchToDronesNetwork(wifiManager, droneStateListener, state);
                    }
                };
                state.context.registerReceiver(scanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                if (wifiManager.startScan()) {
                    state.isScanStarted = true;
                } else {
                    state.context.unregisterReceiver(scanReceiver);
                    throw new RuntimeException("not implemented");// TODO NEED TO StartScan
                }
            }
        } else {
            state.isScanStarted = false;
            connectToNetwork(wifiManager, dronePoint.SSID, droneStateListener, state);
        }
    }
    private static void connectToNetwork(final WifiManager wifiManager, String ssid, final DroneStateListener droneStateListener, final State state) {
        WifiConfiguration wifiConfiguration = null;
        List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        ssid = "\""+ssid+"\"";
        for (WifiConfiguration configuration : configurations) {
            if (configuration.SSID.equals(ssid)) {
                wifiConfiguration = configuration;
                break;
            }
        }
        state.lastWifiSsid = wifiManager.getConnectionInfo().getSSID();
        if (wifiConfiguration == null) {
            wifiConfiguration = new WifiConfiguration();
            wifiConfiguration.SSID = ssid;
            wifiConfiguration.preSharedKey  = "*";
            wifiConfiguration.hiddenSSID = false;
            wifiConfiguration.priority = 33;
            wifiConfiguration.status = WifiConfiguration.Status.DISABLED;
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wifiConfiguration.wepTxKeyIndex = 0;
            int res = wifiManager.addNetwork(wifiConfiguration);
            wifiManager.saveConfiguration();
            wifiConfiguration.networkId = res;
        }
        final BroadcastReceiver connectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                    NetworkInfo ni = (NetworkInfo)intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    NetworkInfo.State netState = ni.getState();
                    String ssid = wifiManager.getConnectionInfo().getSSID();
                    if (netState == NetworkInfo.State.CONNECTED && ssid != null && ssid.startsWith(DRONE_SSID_PREFIX)) {
                        state.context.unregisterReceiver(this);
                        state.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                droneStateListener.onDroneEnable(createControl(wifiManager, state));
                            }
                        });
                    }
                }

            }
        };
        IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        state.context.registerReceiver(connectReceiver, filter);
        wifiManager.enableNetwork(wifiConfiguration.networkId, true);
    }
    private static DroneControl createControl(final WifiManager wifiManager, final State state) {
        return new DroneControl() {
            @Override
            public DroneProxy getDrone() {
                return DroneProxy.getInstance();
            }
            @Override
            public void shutdown() {
                if (state.isNeedToSwitchOffWifiOnClose) {
                    wifiManager.setWifiEnabled(false);
                } else if (state.lastWifiSsid != null) {
                    List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
                    for (WifiConfiguration config : configs) {
                        if (config.SSID.equals(state.lastWifiSsid)) {
                            wifiManager.enableNetwork(config.networkId, true);
                            break;
                        }
                    }
                }
            }
        };
    }

    public interface DroneStateListener {
        public void onDroneEnable(DroneControl droneControl);
        public void onDroneOffline();
        public void onDroneDisable();
    }
    public interface DroneControl {
        public DroneProxy getDrone();
        public void shutdown();
    }
    private static class State {
        private boolean isNeedToSwitchOffWifiOnClose = false;
        private String lastWifiSsid;
        private final Context context;
        private boolean isScanStarted = false;
        private final Handler handler = new Handler();

        private State(Context context) {
            this.context = context;
        }
    }
}
