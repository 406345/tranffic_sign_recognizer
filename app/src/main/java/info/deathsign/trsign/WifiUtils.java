package info.deathsign.trsign;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;

/**
 * WiFi连接管理
 * 申请权限
 * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
 * <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
 * <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
 * <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
 * 动态权限
 * Manifest.permission.ACCESS_COARSE_LOCATION
 * Manifest.permission.ACCESS_FINE_LOCATION
 */
public class WifiUtils extends BroadcastReceiver {
    private static WifiUtils utils = null;
    private Semaphore sWaiter = new Semaphore(0);
    private Context context;

    public WifiUtils(Context context) {
        this.context = context;
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//        context.registerReceiver(this, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    public static WifiUtils getInstance(Context context) {
        if (utils == null) {
            synchronized (WifiUtils.class) {
                if (utils == null) {
                    utils = new WifiUtils(context);
                }
            }
        }
        return utils;
    }

    private WifiManager wifiManager;

    /**
     * wifi是否打开
     *
     * @return
     */
    public boolean isWifiEnable() {
        boolean isEnable = false;
        if (wifiManager != null) {
            if (wifiManager.isWifiEnabled()) {
                isEnable = true;
            }
        }
        return isEnable;
    }

    /**
     * 打开WiFi
     */
    public void openWifi() {
        if (wifiManager != null && !isWifiEnable()) {
            wifiManager.setWifiEnabled(true);
        }
    }

    /**
     * 关闭WiFi
     */
    public void closeWifi() {
        if (wifiManager != null && isWifiEnable()) {
            wifiManager.disconnect();

            if (wifiManager.getConnectionInfo() != null)
                wifiManager.disableNetwork(wifiManager.getConnectionInfo().getNetworkId());

            wifiManager.setWifiEnabled(false);
        }
    }

    public WifiInfo getConnection() {
        return this.wifiManager.getConnectionInfo();
    }


    public void startScan() {

        wifiManager.disconnect();

        if (wifiManager.getConnectionInfo() != null) {
            wifiManager.disableNetwork(wifiManager.getConnectionInfo().getNetworkId());
        }

        if (wifiManager != null && isWifiEnable()) {
            wifiManager.startScan();
        }
    }

    private List<ScanResult> resultList = new ArrayList<>();

    /**
     * 获取WiFi列表
     *
     * @return
     */
    public List<ScanResult> getWifiList() {
        return this.wifiManager.getScanResults();
    }

    /**
     * 有密码连接
     *
     * @param ssid
     * @param pws
     */
    public void connectWifiPws(String ssid, String pws) {
//        wifiManager.disableNetwork(wifiManager.getConnectionInfo().getNetworkId());
        WifiConfiguration wifiConfig = getWifiConfig(ssid, pws, true);

        int id = wifiConfig.networkId;
        if (wifiConfig.networkId < 0)
            id = wifiManager.addNetwork(wifiConfig);
        wifiManager.enableNetwork(id, true);
    }

    /**
     * 无密码连接
     *
     * @param ssid
     */
    public void connectWifiNoPws(String ssid) {
        wifiManager.disableNetwork(wifiManager.getConnectionInfo().getNetworkId());
        int netId = wifiManager.addNetwork(getWifiConfig(ssid, "", false));
        wifiManager.enableNetwork(netId, true);
    }

    public String getGatewayIP() {
        if (this.wifiManager.getConnectionInfo() != null) {
            return intToInetAddress(this.wifiManager.getDhcpInfo().gateway).getHostAddress();
        }

        return "127.0.0.1";
    }

    private InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = {(byte) (0xff & hostAddress),
                (byte) (0xff & (hostAddress >> 8)),
                (byte) (0xff & (hostAddress >> 16)),
                (byte) (0xff & (hostAddress >> 24))};

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * wifi设置
     *
     * @param ssid
     * @param pws
     * @param isHasPws
     */
    private WifiConfiguration getWifiConfig(String ssid, String pws, boolean isHasPws) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();

        config.SSID = "\"" + ssid + "\"";

        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        WifiConfiguration tempConfig = null;

        for (WifiConfiguration cfg : configuredNetworks) {
            if (cfg.SSID.equals(config.SSID)) {
                tempConfig = cfg;
                break;
            }
        }
//        Optional<WifiConfiguration> first = configuredNetworks.stream().filter(x -> x.SSID.equals(config.SSID)).findFirst();


        if (tempConfig != null) {
            return tempConfig;
//            wifiManager.removeNetwork(tempConfig.networkId);
        }
        if (isHasPws) {
            config.hiddenSSID = true;
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.preSharedKey = "\"".concat(pws).concat("\"");
        } else {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
        return config;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
//            this.resultList.clear();
//            this.resultList.addAll(this.wifiManager.getScanResults());
        }
    }
}