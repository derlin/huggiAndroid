/*
 * Copyright 2014 Akexorcist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package ch.eiafr.hugginess.myspp;

public class BluetoothState {
    // Constants that indicate the current connection state
    public static final int STATE_UNAVAILABLE = -1; // bluetooth turned off
    public static final int STATE_NONE = 0;       	// we're doing nothing
    public static final int STATE_CONNECTING = 2; 	// now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  	// now connected to a remote device

    // Intent request codes
    public static final int REQUEST_CONNECT_DEVICE = 384;
    public static final int REQUEST_ENABLE_BT = 385;

    // type of the other device (to choose the correct UUID)
    public static final boolean DEVICE_ANDROID = true;
    public static final boolean DEVICE_OTHER = false;
    
    // Return Intent extra
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    
    // Broadcast actions - intent filter
    public static final String BTSERVICE_INTENT_FILTER = "BTService";

    public static final String EVT_STATE_CHANGED = "evt_state_changed";

    public static final String EXTRA_EVT_TYPE = "extra_evt_type";
    public static final String EVT_CONNECTED = "evt_connected";
    public static final String EVT_DISCONNECTED = "evt_disconnected";
    public static final String EVT_CONNECTION_FAILED = "evt_connection_failed";

    public static final String EVT_DATA_RECEIVED = "evt_data_received";

    // String extra some events
    public static final String EVT_EXTRA_DNAME = "extra_device_name";
    public static final String EVT_EXTRA_DADDR = "extra_device_address";
    public static final String EVT_EXTRA_DATA = "extra_data_received";
}
