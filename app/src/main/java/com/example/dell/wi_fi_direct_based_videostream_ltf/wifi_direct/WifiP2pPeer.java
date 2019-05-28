package com.example.dell.wi_fi_direct_based_videostream_ltf.wifi_direct;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.preference.Preference;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.example.dell.wi_fi_direct_based_videostream_ltf.R;

public class WifiP2pPeer extends Preference {

//    private static final int[] STATE_SECURED = {R.attr.state_encrypted};
    public WifiP2pDevice device;

    private int mRssi;
    private ImageView mSignal;

    private static final int SIGNAL_LEVELS = 4;

    public WifiP2pPeer(Context context, WifiP2pDevice dev) {
        super(context);
        device = dev;
//        setWidgetLayoutResource(R.layout.preference_widget_wifi_signal);
        mRssi = 60; //TODO: fix
    }

    @Override
    protected void onBindView(View view) {
        if (TextUtils.isEmpty(device.deviceName)) {
            setTitle(device.deviceAddress);
        } else {
            setTitle(device.deviceName);
        }
//        mSignal = (ImageView) view.findViewById(R.id.signal);
        if (mRssi == Integer.MAX_VALUE) {
            mSignal.setImageDrawable(null);
        } else {
//            mSignal.setImageResource(R.drawable.wifi_signal);
//            mSignal.setImageState(STATE_SECURED,  true);
        }
        refresh();
        super.onBindView(view);
    }

    @Override
    public int compareTo(Preference preference) {
        if (!(preference instanceof WifiP2pPeer)) {
            return 1;
        }
        WifiP2pPeer other = (WifiP2pPeer) preference;

        // devices go in the order of the status
        if (device.status != other.device.status) {
            return device.status < other.device.status ? -1 : 1;
        }

        // Sort by name/address
        if (device.deviceName != null) {
            return device.deviceName.compareToIgnoreCase(other.device.deviceName);
        }

        return device.deviceAddress.compareToIgnoreCase(other.device.deviceAddress);
    }

    int getLevel() {
        if (mRssi == Integer.MAX_VALUE) {
            return -1;
        }
        return WifiManager.calculateSignalLevel(mRssi, SIGNAL_LEVELS);
    }

    private void refresh() {
        if (mSignal == null) {
            return;
        }
        Context context = getContext();
        mSignal.setImageLevel(getLevel());
//        String[] statusArray = context.getResources().getStringArray(R.array.wifi_p2p_status);
//        setSummary(statusArray[device.status]);
    }
}