/*
 *  Copyright (C) 2015 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.omnirom.omnigears.interfacesettings;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.omni.DeviceUtils;
import com.android.settings.Utils;
import com.android.settings.preference.SeekBarPreference;
import com.android.settings.preference.SystemCheckBoxPreference;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.List;
import java.util.ArrayList;

public class BarsSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "BarsSettings";

    private static final String NETWORK_TRAFFIC_ROOT = "category_network_traffic";
    private static final String CUSTOM_HEADER_IMAGE = "status_bar_custom_header";
    private static final String DAYLIGHT_HEADER_PACK = "daylight_header_pack";
    private static final String DEFAULT_HEADER_PACKAGE = "com.android.systemui";
    private static final String NAVIGATIONBAR_ROOT = "category_navigationbar";

    private ListPreference mDaylightHeaderPack;
    private CheckBoxPreference mCustomHeaderImage;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.OMNI_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.bars_settings);

        PreferenceScreen prefScreen = getPreferenceScreen();

        // Navigationbar catagory will not be displayed when the device is not a tablet
        // or the device has physical keys
        if ((!DeviceUtils.deviceSupportNavigationBar(getActivity())) || !DeviceUtils.isTablet(getActivity())) {
            prefScreen.removePreference(findPreference(NAVIGATIONBAR_ROOT));
        }

        // TrafficStats will return UNSUPPORTED if the device does not support it.
        if (TrafficStats.getTotalTxBytes() == TrafficStats.UNSUPPORTED ||
                TrafficStats.getTotalRxBytes() == TrafficStats.UNSUPPORTED) {
            prefScreen.removePreference(findPreference(NETWORK_TRAFFIC_ROOT));
        }

        final boolean customHeaderImage = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER, 0) == 1;
        mCustomHeaderImage = (CheckBoxPreference) findPreference(CUSTOM_HEADER_IMAGE);
        mCustomHeaderImage.setChecked(customHeaderImage);

        String settingHeaderPackage = Settings.System.getString(getContentResolver(),
                Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK);
        if (settingHeaderPackage == null) {
            settingHeaderPackage = DEFAULT_HEADER_PACKAGE;
        }
        mDaylightHeaderPack = (ListPreference) findPreference(DAYLIGHT_HEADER_PACK);
        mDaylightHeaderPack.setEntries(getAvailableHeaderPacksEntries());
        mDaylightHeaderPack.setEntryValues(getAvailableHeaderPacksValues());

        int valueIndex = mDaylightHeaderPack.findIndexOfValue(settingHeaderPackage);
        if (valueIndex == -1) {
            // no longer found
            settingHeaderPackage = DEFAULT_HEADER_PACKAGE;
            Settings.System.putString(getContentResolver(),
                    Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK, settingHeaderPackage);
            valueIndex = mDaylightHeaderPack.findIndexOfValue(settingHeaderPackage);
        }
        mDaylightHeaderPack.setValueIndex(valueIndex >= 0 ? valueIndex : 0);
        mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntry());
        mDaylightHeaderPack.setOnPreferenceChangeListener(this);
        mDaylightHeaderPack.setEnabled(customHeaderImage);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mCustomHeaderImage) {
            final boolean value = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_CUSTOM_HEADER, value ? 1 : 0);
            mDaylightHeaderPack.setEnabled(value);
            return true;
        }
        // If we didn't handle it, let preferences handle it.
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDaylightHeaderPack) {
            String value = (String) newValue;
            Settings.System.putString(getContentResolver(),
                    Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK, value);
            int valueIndex = mDaylightHeaderPack.findIndexOfValue(value);
            mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntries()[valueIndex]);
        }
        return true;
    }

    private String[] getAvailableHeaderPacksValues() {
        List<String> headerPacks = new ArrayList<String>();
        Intent i = new Intent();
        PackageManager packageManager = getPackageManager();
        i.setAction("org.omnirom.DaylightHeaderPack");
        for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
            String packageName = r.activityInfo.packageName;
            if (packageName.equals(DEFAULT_HEADER_PACKAGE)) {
                headerPacks.add(0, packageName);
            } else {
                headerPacks.add(packageName);
            }
        }
        return headerPacks.toArray(new String[headerPacks.size()]);
    }

    private String[] getAvailableHeaderPacksEntries() {
        List<String> headerPacks = new ArrayList<String>();
        Intent i = new Intent();
        PackageManager packageManager = getPackageManager();
        i.setAction("org.omnirom.DaylightHeaderPack");
        for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
            String packageName = r.activityInfo.packageName;
            String label = r.activityInfo.loadLabel(getPackageManager()).toString();
            if (label == null) {
                label = r.activityInfo.packageName;
            }
            if (packageName.equals(DEFAULT_HEADER_PACKAGE)) {
                headerPacks.add(0, label);
            } else {
                headerPacks.add(label);
            }
        }
        return headerPacks.toArray(new String[headerPacks.size()]);
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.bars_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    return result;
                }
            };
}
