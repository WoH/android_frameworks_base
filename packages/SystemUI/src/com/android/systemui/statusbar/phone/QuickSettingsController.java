/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.phone;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.quicksettings.AirplaneModeTile;
import com.android.systemui.statusbar.quicksettings.AlarmTile;
import com.android.systemui.statusbar.quicksettings.AutoRotateTile;
import com.android.systemui.statusbar.quicksettings.BatteryTile;
import com.android.systemui.statusbar.quicksettings.BluetoothTile;
import com.android.systemui.statusbar.quicksettings.BrightnessTile;
import com.android.systemui.statusbar.quicksettings.BugReportTile;
import com.android.systemui.statusbar.quicksettings.FlashLightTile;
import com.android.systemui.statusbar.quicksettings.GpsTile;
import com.android.systemui.statusbar.quicksettings.InputMethodTile;
import com.android.systemui.statusbar.quicksettings.MobileNetworkTile;
import com.android.systemui.statusbar.quicksettings.MobileNetworkModeTile;
import com.android.systemui.statusbar.quicksettings.NfcTile;
import com.android.systemui.statusbar.quicksettings.PreferencesTile;
import com.android.systemui.statusbar.quicksettings.QuickSettingsTile;
import com.android.systemui.statusbar.quicksettings.RingerModeTile;
import com.android.systemui.statusbar.quicksettings.RingerVibrationModeTile;
import com.android.systemui.statusbar.quicksettings.SyncTile;
import com.android.systemui.statusbar.quicksettings.SleepScreenTile;
import com.android.systemui.statusbar.quicksettings.UserTile;
import com.android.systemui.statusbar.quicksettings.VibrationModeTile;
import com.android.systemui.statusbar.quicksettings.WiFiDisplayTile;
import com.android.systemui.statusbar.quicksettings.WiFiTile;
import com.android.systemui.statusbar.quicksettings.WifiAPTile;

public class QuickSettingsController {
    private static String TAG = "QuickSettingsController";
    private static boolean DEBUG = BaseStatusBar.DEBUG;

    public static final String TILE_USER = "toggleUser";
    public static final String TILE_BATTERY = "toggleBattery";
    public static final String TILE_SETTINGS = "toggleSettings";
    public static final String TILE_WIFI = "toggleWifi";
    public static final String TILE_GPS = "toggleGPS";
    public static final String TILE_BLUETOOTH = "toggleBluetooth";
    public static final String TILE_BRIGHTNESS = "toggleBrightness";
    public static final String TILE_SOUND = "toggleSound";
    public static final String TILE_SYNC = "toggleSync";
    public static final String TILE_WIFIAP = "toggleWifiAp";
    public static final String TILE_SCREENTIMEOUT = "toggleScreenTimeout";
    public static final String TILE_MOBILEDATA = "toggleMobileData";
    public static final String TILE_NETWORKMODE = "toggleNetworkMode";
    public static final String TILE_AUTOROTATE = "toggleAutoRotate";
    public static final String TILE_AIRPLANE = "toggleAirplane";
    public static final String TILE_FLASHLIGHT = "toggleFlashlight";
    public static final String TILE_SLEEP = "toggleSleepMode";
    public static final String TILE_LTE = "toggleLte";
    public static final String TILE_WIMAX = "toggleWimax";
    public static final String TILE_NFC = "toggleNfc";

    private static final String TILE_DELIMITER = "|";
    private static final String TILES_DEFAULT = TILE_USER
            + TILE_DELIMITER + TILE_BRIGHTNESS
            + TILE_DELIMITER + TILE_SETTINGS
            + TILE_DELIMITER + TILE_WIFI
            + TILE_DELIMITER + TILE_MOBILEDATA
            + TILE_DELIMITER + TILE_BATTERY
            + TILE_DELIMITER + TILE_AIRPLANE
            + TILE_DELIMITER + TILE_BLUETOOTH;

    private final Context mContext;
    public PanelBar mBar;
    private final ViewGroup mContainerView;
    private final Handler mHandler;
    public BaseStatusBar mStatusBarService;
    private InputMethodTile IMETile;

    public QuickSettingsController(Context context, QuickSettingsContainerView container, BaseStatusBar statusBarService) {
        mContext = context;
        mContainerView = container;
        mHandler = new Handler();
        mStatusBarService = statusBarService;
    }


    void loadTiles(LayoutInflater inflater) {
        // Read the stored list of tiles
        ContentResolver resolver = mContext.getContentResolver();
        String tiles = Settings.System.getString(resolver, Settings.System.QUICK_SETTINGS_TILES);
        if (tiles == null) {
            if (DEBUG) Log.i(TAG, "Default tiles being loaded");
            tiles = TILES_DEFAULT;
        }

        if (DEBUG) Log.i(TAG, "Tiles list: " + tiles);

        QuickSettingsTile qs = null;

        // Add user selected tiles
        for (String tile : tiles.split("\\|")) {
            if (tile.equals(TILE_USER)) {
                qs = new UserTile(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this);
            } else if (tile.equals(TILE_BATTERY)) {
                qs = new BatteryTile(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this);
            } else if (tile.equals(TILE_SETTINGS)) {
                qs = new PreferencesTile(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this);
            } else if (tile.equals(TILE_WIFI)) {
                qs = new WiFiTile(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this);
            } else if (tile.equals(TILE_GPS)) {
                qs = new GpsTile(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this);
            } else if (tile.equals(TILE_BLUETOOTH)) {
                if(deviceSupportsBluetooth()) {
                    qs = new BluetoothTile(mContext, inflater,
                            (QuickSettingsContainerView) mContainerView, this);
                }
            } else if (tile.equals(TILE_BRIGHTNESS)) {
                qs = new BrightnessTile(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
            } else if (tile.equals(TILE_SOUND)) {
                qs = new RingerVibrationModeTile(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this);
            } else if (tile.equals(TILE_SYNC)) {
                qs = new SyncTile(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this);
            } else if (tile.equals(TILE_WIFIAP)) {
                if(deviceSupportsTelephony()) {
                    qs = new WifiAPTile(mContext, inflater,
                            (QuickSettingsContainerView) mContainerView, this);
                }
            } else if (tile.equals(TILE_SCREENTIMEOUT)) {
                // Not available yet
            } else if (tile.equals(TILE_MOBILEDATA)) {
                if(deviceSupportsTelephony()) {
                    qs = new MobileNetworkTile(mContext, inflater,
                            (QuickSettingsContainerView) mContainerView, this);
                }
            } else if (tile.equals(TILE_NETWORKMODE)) {
                if(deviceSupportsTelephony()) {
                    qs = new MobileNetworkModeTile(mContext, inflater,
                            (QuickSettingsContainerView) mContainerView, this);
                }
            } else if (tile.equals(TILE_AUTOROTATE)) {
                qs = new AutoRotateTile(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
            } else if (tile.equals(TILE_AIRPLANE)) {
                qs = new AirplaneModeTile(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this);
            } else if (tile.equals(TILE_FLASHLIGHT)) {
                qs = new FlashLightTile(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
            } else if (tile.equals(TILE_SLEEP)) {
                qs = new SleepScreenTile(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this);
            } else if (tile.equals(TILE_WIMAX)) {
                // Not available yet
            } else if (tile.equals(TILE_LTE)) {
                // Not available yet
            } else if(tile.equals(TILE_NFC)) {
                qs = new NfcTile(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this);
            }

            if (qs != null) {
                qs.setupQuickSettingsTile();
            }
        }

        // Load the dynamic tiles
        // These tiles must be the last ones added to the view, as they will show
        // only when they are needed
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_ALARM, 1) == 1) {
            qs = new AlarmTile(mContext, inflater,
                    (QuickSettingsContainerView) mContainerView, this, mHandler);
            qs.setupQuickSettingsTile();
        }
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_BUGREPORT, 1) == 1) {
            qs = new BugReportTile(mContext, inflater,
                    (QuickSettingsContainerView) mContainerView, this, mHandler);
            qs.setupQuickSettingsTile();
        }
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_WIFI, 1) == 1) {
            qs = new WiFiDisplayTile(mContext, inflater,
                    (QuickSettingsContainerView) mContainerView, this);
            qs.setupQuickSettingsTile();
        }
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_IME, 1) == 1) {
            IMETile = new InputMethodTile(mContext, inflater,
                    (QuickSettingsContainerView) mContainerView, this);
            IMETile.setupQuickSettingsTile();
        }
    }

    void setupQuickSettings() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        loadTiles(inflater);
    }

    boolean deviceSupportsTelephony() {
        PackageManager pm = mContext.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    boolean deviceSupportsBluetooth() {
        return (BluetoothAdapter.getDefaultAdapter() != null);
    }

    public void setBar(PanelBar bar) {
        mBar = bar;
    }

    public void setImeWindowStatus(boolean visible) {
        if (IMETile != null) {
            IMETile.toggleVisibility(visible);
        }
    }

    public void updateResources() {}

}
