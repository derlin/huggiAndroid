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


package ch.eiafr.hugginess.services.bluetooth;

public class BluetoothConstants{
    // Constants that indicate the current connection state
    public static final int STATE_TURNED_OFF = -1; // bluetooth turned off
    public static final int STATE_NONE = 0;       	// we're doing nothing
    public static final int STATE_CONNECTING = 2; 	// now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  	// now connected to a remote device

    // Intent request codes    TODO
    public static final int REQUEST_CONNECT_DEVICE = 384;
    public static final int REQUEST_ENABLE_BT = 385;

    // type of the other device (to choose the correct UUID)
    public static final boolean DEVICE_ANDROID = true;
    public static final boolean DEVICE_OTHER = false;
    
    // Return Intent extra from DeviceList
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";

    // ----------------------------------------------------
    // extra for all broadcasts sent from the service
    public static final String EXTRA_EVT_TYPE = "extra_evt_type";
    // intent filter to use for the broadcasts
    public static final String BTSERVICE_INTENT_FILTER = "Hugginess-BTService";


    // bt adapter-related events
    public static final String EVT_BT_TURNED_ON = "evt_turned_on";
    public static final String EVT_BT_TURNED_OFF = "evt_turned_off";
    // connection-related events
    public static final String EVT_CONNECTED = "evt_connected";
    public static final String EVT_DISCONNECTED = "evt_disconnected";
    public static final String EVT_CONNECTION_FAILED = "evt_connection_failed";
    // data-related events
    public static final String EVT_DATA_RECEIVED = "evt_data_received";

    // String extra some events
    public static final String EVT_EXTRA_DNAME = "extra_device_name";
    public static final String EVT_EXTRA_DADDR = "extra_device_address";
    public static final String EVT_EXTRA_DATA = "extra_data_received";


    // ---------------------------------------------------- HuggiService only

    // acks
    public static final String EVT_ACK_RECEIVED = "evt_ack";
    public static final String EVT_EXTRA_ACK_CMD = "extra_ack_cmd";
    public static final String EVT_EXTRA_ACK_STATUS = "extra_ack_status";

    // hugs
    public static final String EVT_HUGS_RECEIVED = "evt_hugs_received";
    public static final String EVT_EXTRA_HUGS_CNT = "extra_hugs_cnt";
    public static final String EVT_EXTRA_HUGS_LIST = "extra_new_hugs";


    // available commands
    public static final String DATA_PREFIX = "@";
    public static final String DATA_SEP = "!";

    public static final String ACK_PREFIX = "#";
    public static final String ACK_OK = ACK_PREFIX + "#";
    public static final String ACK_NOK = ACK_PREFIX + "?";

    public static final String CMD_PREFIX = "$";
    public static final char CMD_ECHO = 'E';
    public static final char CMD_SET_ID = 'I';
    public static final char CMD_SET_DATA = 'D';
    public static final char CMD_SEND_HUGS = 'H';
    public static final char CMD_CALIBRATE = 'C';
    public static final char CMD_SLEEP = 'S';
    public static final char CMD_DUMP_ALL = 'A';



    // sizes etc.
    public static final int DATA_MAX_SIZE = 50;
    public static final int ID_SIZE = 10;
}
