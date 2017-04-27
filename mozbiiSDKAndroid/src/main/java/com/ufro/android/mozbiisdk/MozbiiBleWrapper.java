package com.ufro.android.mozbiisdk;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;

/**
 * The MozbiiBleWrapper is the bluetooth connect wrapper for Mozbii. The android
 * device must above >= 4.3 for ble connect.
 * 
 * This SDK is reference form
 * https://developer.bluetooth.org/Pages/default.aspx. And you can modify this
 * SDK if you need.
 * 
 * This class is to handle bluetooth connect and disconnect. You can also get
 * bluetooth status by this method.
 * 
 * 
 */
public class MozbiiBleWrapper {
    /* defines (in milliseconds) how often RSSI should be updated */
    private static final int RSSI_UPDATE_TIME_INTERVAL = 1500; // 1.5 seconds

    private boolean mIsNotificationINDEX;
    private boolean mIsNotificationRGB;

    private BluetoothGattCharacteristic mINDEXCharGatt;
    private BluetoothGattCharacteristic mRGBCharGatt;

    private boolean mIsConnecting = false;
    public static final String MOZBII = "Mozbii_";
    private Hashtable<String, Integer> mBLERSSI = null;
    /* The max color array size of Mozbii */
    private static final int COLOR_MAX_SIZE = 12;
    private static int[] mColors = new int[COLOR_MAX_SIZE];

    /* callback object through which we are returning results to the caller */
    private MozbiiBleCallBack mMozbiiCallback = null;
    /* define NULL object for UI callbacks */
    private static final MozbiiBleCallBack NULL_CALLBACK = new MozbiiBleCallBack.Null();
    private String mMaxAddress = "";

    /**
     * creates MozbiiBleWrapper object, set its parent activity and callback
     * object
     */
    public MozbiiBleWrapper(Activity parent, MozbiiBleCallBack mozbiiCallback) {
        mParent = parent;
        mMozbiiCallback = mozbiiCallback;
        if (mMozbiiCallback == null) {
            mMozbiiCallback = NULL_CALLBACK;
        }
    }

    /**
     * Check is Mozbii has connected or not.
     * 
     * @return boolean : true, connected;false not connected.
     */
    public boolean isConnected() {
        return mConnected;
    }

    /** run test and check if this device has BT and BLE hardware available */
    public boolean checkBleHardwareAvailable() {
        // First check general Bluetooth Hardware:
        // get BluetoothManager...
        final BluetoothManager manager = (BluetoothManager) mParent
                .getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null)
            return false;
        // .. and then get adapter from manager
        final BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null) {
            return false;
        }

        // and then check if BT LE is also available
        boolean hasBle = mParent.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE);
        return hasBle;
    }

    /*
     * before any action check if BT is turned ON and enabled for us call this
     * in onResume to be always sure that BT is ON when Your application is put
     * into the foreground
     */
    public boolean isBtEnabled() {
        final BluetoothManager manager = (BluetoothManager) mParent
                .getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null) {
            return false;
        }

        final BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null) {
            return false;
        }
        if (!adapter.isEnabled()) {
			return adapter.enable();
		}
        return true;
    }

    /* start scanning for BT LE devices around */
    public void startScanning() {
        mBluetoothAdapter.startLeScan(mDeviceFoundCallback);
    }

    class AsyncScanning extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... arg0) {
            // TODO Auto-generated method stub
            return isBtEnabled();
        }

        @Override
        protected void onPostExecute(Boolean enable) {
            // TODO Auto-generated method stub
            super.onPostExecute(enable);
            if (enable) {
                mBluetoothAdapter.startLeScan(mDeviceFoundCallback);
            }
        }
    }

    /* stops current scanning */
    public void stopScanning() {
        if (mBluetoothAdapter != null) {
            try {
                mBluetoothAdapter.stopLeScan(mDeviceFoundCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * initialize BLE and get BT Manager & Adapter. It must be called after
     * MozbiiBleWrapper has been instantiated.
     * 
     * @return boolean : true, if enable success ; false, the otherwise
     */
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mParent
                    .getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }

        if (mBluetoothAdapter == null)
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }

    /**
     * connect to the device with specified address
     * 
     * @param deviceAddress
     *            : device address of android devices
     * @return boolean : true, if enable success ; false, the otherwise
     */
    public boolean connect(final String deviceAddress) {
        if (mBluetoothAdapter == null || deviceAddress == null)
            return false;
        mDeviceAddress = deviceAddress;

        // check if we need to connect from scratch or just reconnect to
        // previous device
        if (mBluetoothGatt != null
                && mBluetoothGatt.getDevice().getAddress()
                        .equals(deviceAddress)) {
            // just reconnect
            return mBluetoothGatt.connect();
        } else {
            // connect from scratch
            // get BluetoothDevice object for specified address
            mBluetoothDevice = mBluetoothAdapter
                    .getRemoteDevice(mDeviceAddress);
            if (mBluetoothDevice == null) {
                // we got wrong address - that device is not available!
                return false;
            }
            // connect with remote device
            mBluetoothGatt = mBluetoothDevice.connectGatt(mParent, false,
                    mBleCallback);
        }
        return true;
    }

    /**
     * disconnect the device. It is still possible to reconnect to it later with
     * this Gatt client
     */
    public void disconnect() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
    }

    /**
     * Reset configs of ble.
     */
    private void resetConfigs() {
        mIsNotificationINDEX = false;
        mIsNotificationRGB = false;

        mINDEXCharGatt = null;
        mRGBCharGatt = null;

        mIsConnecting = false;
        mBLERSSI = null;
        mColors = new int[COLOR_MAX_SIZE];
        mBluetoothManager = null;
        mBluetoothAdapter = null;
        mBluetoothDevice = null;
        mBluetoothGatt = null;
        this.initialize();
    }

    /* request new RSSi value for the connection */
    public void readPeriodicalyRssiValue(final boolean repeat) {
        mTimerEnabled = repeat;
        // check if we should stop checking RSSI value
        if (mConnected == false || mBluetoothGatt == null
                || mTimerEnabled == false) {
            mTimerEnabled = false;
            return;
        }

        mTimerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mBluetoothGatt == null || mBluetoothAdapter == null
                        || mConnected == false) {
                    mTimerEnabled = false;
                    return;
                }

                // request RSSI value
                mBluetoothGatt.readRemoteRssi();
                // add call it once more in the future
                readPeriodicalyRssiValue(mTimerEnabled);
            }
        }, RSSI_UPDATE_TIME_INTERVAL);
    }

    /* starts monitoring RSSI value */
    public void startMonitoringRssiValue() {
        readPeriodicalyRssiValue(true);
    }

    /* stops monitoring of RSSI value */
    public void stopMonitoringRssiValue() {
        readPeriodicalyRssiValue(false);
    }

    /*
     * request to discover all services available on the remote devices results
     * are delivered through callback object
     */
    public void startServicesDiscovery() {
        if (mBluetoothGatt != null)
            mBluetoothGatt.discoverServices();
    }

    /*
     * gets services and calls UI callback to handle them before calling
     * getServices() make sure service discovery is finished!
     */
    public void getSupportedServices() {
        if (mBluetoothGattServices != null && mBluetoothGattServices.size() > 0) {
            mBluetoothGattServices.clear();
        }
        if (mBluetoothGatt != null) {
            mBluetoothGattServices = mBluetoothGatt.getServices();
        }
        for (BluetoothGattService service : mBluetoothGattServices) {
            if (service.getUuid().equals(MozbiiBleUUIDs.Service.COLOR_PEN)) {
                getCharacteristicsForService(service);
                mIsNotificationRGB = false;
                mIsNotificationINDEX = false;
            }
        }
    }

    /*
     * get all characteristic for particular service and pass them to the UI
     * callback
     */
    public void getCharacteristicsForService(final BluetoothGattService service) {
        if (service == null) {
            return;
        }
        List<BluetoothGattCharacteristic> chars = null;

        chars = service.getCharacteristics();
        // keep reference to the last selected service
        for (BluetoothGattCharacteristic gattchar : chars) {
            if (gattchar.getUuid().equals(MozbiiBleUUIDs.Characteristic.INDEX)) {
                mINDEXCharGatt = gattchar;
                setNotificationForCharacteristic(mINDEXCharGatt, true);
            } else if (gattchar.getUuid().equals(
                    MozbiiBleUUIDs.Characteristic.RGB_COLOR)) {
                mRGBCharGatt = gattchar;
                setNotificationForCharacteristic(mRGBCharGatt, true);
            }
        }
    }

    /*
     * request to fetch newest value stored on the remote device for particular
     * characteristic
     */
    public void requestCharacteristicValue(BluetoothGattCharacteristic ch) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.readCharacteristic(ch);
        if (ch != null) {
            if (MozbiiBleUUIDs.Characteristic.BATTERY_LEVEL
                    .equals(ch.getUuid())) {
                byte[] percent = ch.getValue();
                if (percent != null) {
                    mMozbiiCallback
                            .onMozbiiBatteryStatusChanged((int) percent[0]);
                }
            }
        }
        // new value available will be notified in Callback Object
    }

    /*
     * get characteristic's value (and parse it for some types of
     * characteristics) before calling this You should always update the value
     * by calling requestCharacteristicValue()
     */
    public void getCharacteristicValue(BluetoothGattCharacteristic ch) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null) {
            return;
        }
        byte[] rawValue = ch.getValue();
        final int index = rawValue[0] - 1;
        if (ch.getUuid().equals(MozbiiBleUUIDs.Characteristic.INDEX)) {
            if (!mIsNotificationINDEX) {
                mIsNotificationINDEX = true;
            }
            mMozbiiCallback.onMozbiiColorIndexChanged(index);
        }
    }

    /*
     * reads and return what what FORMAT is indicated by characteristic's
     * properties seems that value makes no sense in most cases
     */
    public int getValueFormat(BluetoothGattCharacteristic ch) {
        int properties = ch.getProperties();

        if ((BluetoothGattCharacteristic.FORMAT_FLOAT & properties) != 0) {
            return BluetoothGattCharacteristic.FORMAT_FLOAT;
        }
        if ((BluetoothGattCharacteristic.FORMAT_SFLOAT & properties) != 0) {
            return BluetoothGattCharacteristic.FORMAT_SFLOAT;
        }
        if ((BluetoothGattCharacteristic.FORMAT_SINT16 & properties) != 0) {
            return BluetoothGattCharacteristic.FORMAT_SINT16;
        }
        if ((BluetoothGattCharacteristic.FORMAT_SINT32 & properties) != 0) {
            return BluetoothGattCharacteristic.FORMAT_SINT32;
        }
        if ((BluetoothGattCharacteristic.FORMAT_SINT8 & properties) != 0) {
            return BluetoothGattCharacteristic.FORMAT_SINT8;
        }
        if ((BluetoothGattCharacteristic.FORMAT_UINT16 & properties) != 0) {
            return BluetoothGattCharacteristic.FORMAT_UINT16;
        }
        if ((BluetoothGattCharacteristic.FORMAT_UINT32 & properties) != 0) {
            return BluetoothGattCharacteristic.FORMAT_UINT32;
        }
        if ((BluetoothGattCharacteristic.FORMAT_UINT8 & properties) != 0) {
            return BluetoothGattCharacteristic.FORMAT_UINT8;
        }
        return 0;
    }

    /* enables/disables notification for characteristic */
    public void setNotificationForCharacteristic(
            BluetoothGattCharacteristic ch, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(ch, enabled);
        // This is also sometimes required (e.g. for heart rate monitors) to
        // enable notifications/indications
        // see:
        // https://developer.bluetooth.org/gatt/descriptors/Pages/DescriptorViewer.aspx?u=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
        BluetoothGattDescriptor descriptor = ch.getDescriptor(UUID
                .fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if (descriptor != null) {
            byte[] val = enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            descriptor.setValue(val);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /* defines callback for scanning results */
    private BluetoothAdapter.LeScanCallback mDeviceFoundCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                final byte[] scanRecord) {
            // mUiCallback.uiDeviceFound(device, rssi, scanRecord);
            String btName = device.getName();
            if (btName != null && !mIsConnecting) {
                if (btName.contains(MOZBII) && this != null) {
                    if (mBLERSSI == null) {
                        mBLERSSI = new Hashtable<String, Integer>();
                    }
                    if (!mBLERSSI.containsKey(device.getAddress())) {
                        mBLERSSI.put(device.getAddress(), rssi);
                        // startConnect();
                        startConnect();
                    }

                }
            }
        }
    };

    /**
     * Start to connect ble device.
     */
    private void startConnect() {
        if (!mIsConnecting) {
            if (this != null) {
                stopScanning();
            }
            int max_rssi = -100000000;
            if (mBLERSSI != null) {
                Iterator<String> iterator = mBLERSSI.keySet().iterator();
                while (iterator.hasNext()) {
                    String address = (String) iterator.next();
                    int rssi = (int) mBLERSSI.get(address);
                    if (rssi > max_rssi) {
                        max_rssi = rssi;
                        mMaxAddress = address;
                    }
                }
            }
            mBLERSSI = null;
            mParent.runOnUiThread(new Runnable() {
    			@Override
    			public void run() {
    				connect(mMaxAddress);
    			}
    		});
            mIsConnecting = true;

        }
    }

    /* called for any action on particular Ble Device */
    private final BluetoothGattCallback mBleCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnected = true;
                mMozbiiCallback.onMozbiiConnected();
                updateBattery();

                // now we can start talking with the device, e.g.
                mBluetoothGatt.readRemoteRssi();
                // response will be delivered to callback object!

                // in our case we would also like automatically to call for
                // services discovery
                startServicesDiscovery();

                // and we also want to get RSSI value to be updated periodically
                startMonitoringRssiValue();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnected = false;
                resetConfigs();
                mMozbiiCallback.onMozbiiDisconnected();
            }
            mBLERSSI = null;
            if (this != null) {
                stopScanning();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // now, when services discovery is finished, we can call
                // getServices() for Gatt
                getSupportedServices();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            // we got response regarding our request to fetch characteristic
            // value
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // and it success, so we can get the value
                getCharacteristicValue(characteristic);
            }
            // mUiCallback.uiCharacteristicsRead(characteristic);
            if (characteristic != null) {
                if (MozbiiBleUUIDs.Characteristic.BATTERY_LEVEL
                        .equals(characteristic.getUuid())) {
                    byte[] percent = characteristic.getValue();
                    mMozbiiCallback
                            .onMozbiiBatteryStatusChanged((int) percent[0]);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic) {
            getCharacteristicValue(characteristic);
            byte[] rawValue = characteristic.getValue();
            final int index = rawValue[0] - 1;
            if (characteristic.getUuid().equals(
                    MozbiiBleUUIDs.Characteristic.INDEX)) {
                // Handle color index
                if (!mIsNotificationINDEX) {
                    mIsNotificationINDEX = true;
                }
                if (mRGBCharGatt != null && !mIsNotificationRGB) {
                    setNotificationForCharacteristic(mRGBCharGatt, true);
                }
                mMozbiiCallback.onMozbiiColorIndexChanged(index);

            } else if (characteristic.getUuid().equals(
                    MozbiiBleUUIDs.Characteristic.RGB_COLOR)) {
                if (!mIsNotificationRGB) {
                    mIsNotificationRGB = true;
                }
                int red = 0;
                int green = 0;
                int blue = 0;
                if (mINDEXCharGatt != null && !mIsNotificationINDEX) {
                    setNotificationForCharacteristic(mINDEXCharGatt, true);
                }

                // Parse color array
                for (int i = 0; i < (rawValue.length) / 4; i++) {
                    if (rawValue[i * 4] > 0) {
                        red = characteristic.getIntValue(
                                BluetoothGattCharacteristic.FORMAT_UINT8,
                                i * 4 + 1);
                        green = characteristic.getIntValue(
                                BluetoothGattCharacteristic.FORMAT_UINT8,
                                i * 4 + 2);
                        blue = characteristic.getIntValue(
                                BluetoothGattCharacteristic.FORMAT_UINT8,
                                i * 4 + 3);
                        try {
                            int color = Color.rgb(red, green, blue);
                            mColors[rawValue[i * 4] - 1] = color;
                        } catch (Exception e) {
                        }
                    }
                }
                mMozbiiCallback.onMozbiiColorArrayChanged(mColors);
            }
        }
    };

    /**
     * To update battery status.
     */
    public void updateBattery() {
        if (mBluetoothGatt != null) {
            BluetoothGattService batteryService = mBluetoothGatt
                    .getService(MozbiiBleUUIDs.Service.BATTERY_SERVICE_UUID);
            if (batteryService == null) {
                return;
            }

            BluetoothGattCharacteristic batteryLevel = batteryService
                    .getCharacteristic(MozbiiBleUUIDs.Characteristic.BATTERY_LEVEL);
            if (batteryLevel == null) {
                return;
            }
            requestCharacteristicValue(batteryLevel);
            mBluetoothGatt.readCharacteristic(batteryLevel);
        }
    }

    private Activity mParent = null;
    private boolean mConnected = false;
    private String mDeviceAddress = "";

    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice mBluetoothDevice = null;
    private BluetoothGatt mBluetoothGatt = null;
    private List<BluetoothGattService> mBluetoothGattServices = null;

    private Handler mTimerHandler = new Handler();
    private boolean mTimerEnabled = false;
}
