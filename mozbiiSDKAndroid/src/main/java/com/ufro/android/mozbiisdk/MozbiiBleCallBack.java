package com.ufro.android.mozbiisdk;

/**
 * The MozbiiBleWrapper is the bluetooth connect wrapper for Mozbii. The android
 * device must above >= 4.3 for ble connect.
 * 
 * This SDK is reference form https://developer.bluetooth.org/Pages/default.aspx.
 * And you can modify this SDK if you need.
 * 
 * This class is the interface to receive callback of the Mozbii. You can receive
 * color array and color index after Mozbii connected.
 * 
 */

public interface MozbiiBleCallBack {

	/**
	 * Called when Mozbii connected. This method will be called after
	 * MozbiiBleWrapper.connect() has been called.
	 */
	public void onMozbiiConnected();

	/**
	 * Called when Mozbii disconnected. This method will be called after
	 * MozbiiBleWrapper.disconnect() has been called.
	 */
	public void onMozbiiDisconnected();

	/**
	 * Called when color array changed after captured a new color.
	 * 
	 * @param colors
	 *            : color array in mozbii
	 */
	public void onMozbiiColorArrayChanged(int[] colors);

	/**
	 * Called when color index changed. Like button up or down clicked, or
	 * captured a new color.
	 * 
	 * @param index : color index of Mozbii, should be 0-11.
	 */
	public void onMozbiiColorIndexChanged(int index);

	/**
	 * Called when battery reload. But you must call
	 * MozbiiBleWrapper.updateBattery() frist to trigger reload battery status.
	 * 
	 * @param battery : battery status, should be 0-100.
	 */
	public void onMozbiiBatteryStatusChanged(int battery);

	/* define Null Adapter class for that interface */
	public static class Null implements MozbiiBleCallBack {

		@Override
		public void onMozbiiConnected() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onMozbiiDisconnected() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onMozbiiColorArrayChanged(int[] colors) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onMozbiiColorIndexChanged(int index) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onMozbiiBatteryStatusChanged(int battery) {
			// TODO Auto-generated method stub

		}

	}
}
