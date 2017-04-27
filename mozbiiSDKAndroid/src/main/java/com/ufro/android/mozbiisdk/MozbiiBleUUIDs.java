package com.ufro.android.mozbiisdk;

import java.util.UUID;

/**
 * This file is the uuids for Mozbii. You can add uuids you needs, but don't
 * remove the uuids in this class. This class define bluetooth uuids for Mozbii.
 */
public class MozbiiBleUUIDs {

	public static class Service {
		final static public UUID COLOR_PEN = UUID
				.fromString("00001f1f-0000-1000-8000-00805f9b34fb");
		final static public UUID BATTERY_SERVICE_UUID = UUID
				.fromString("0000180F-0000-1000-8000-00805f9b34fb");
	};

	public static class Characteristic {
		final static public UUID COLOR_PEN = UUID
				.fromString("00001f1f-0000-1000-8000-00805f9b34fb");
		final static public UUID INDEX = UUID
				.fromString("00001f13-0000-1000-8000-00805f9b34fb");
		final static public UUID RGB_COLOR = UUID
				.fromString("00001f14-0000-1000-8000-00805f9b34fb");
		final static public UUID EVENT = UUID
				.fromString("00001f15-0000-1000-8000-00805f9b34fb");
		final static public UUID BATTERY_LEVEL = UUID
				.fromString("00002a19-0000-1000-8000-00805f9b34fb");
	}
}
