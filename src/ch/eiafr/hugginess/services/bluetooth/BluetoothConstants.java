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

/**
 * This class contains all the constants used by the {@link ch.eiafr.hugginess.services.bluetooth.BluetoothService} and
 * {@link ch.eiafr.hugginess.services.bluetooth.HuggiBluetoothService}.
 * <p/>
 * We decided to put all the constants (used by the generic and the specific service) into one class, to ease the
 * imports.
 * <p/>
 * Constants related to notifications: each broadcast from a bluetooth service has an extra with key {@link
 * #EXTRA_EVT_TYPE}, which encodes the type of event. Some events also have specific extras (see the javadoc).
 * <p/>
 * Constants related to commands: the communication between the App and the HuggiShirt uses a special format, which
 * is normally handled directly by the {@link ch.eiafr.hugginess.services.bluetooth.HuggiBluetoothService} and the
 * {@link ch.eiafr.hugginess.services.bluetooth.HuggiBroadcastReceiver}. However, you will need the command constants
 * to specify which command to send (see {@link ch.eiafr.hugginess.services.bluetooth.HuggiBluetoothService#executeCommand(char)}.
 * <p/>
 * Other constants: the maximal size of some fields, the intent filter for broadcasts, etc. are also defined.
 * <p/>
 * creation date    01.12.2014
 * context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
 *
 * @author Lucy Linder
 * @see ch.eiafr.hugginess.services.bluetooth.BluetoothService
 * @see ch.eiafr.hugginess.services.bluetooth.HuggiBroadcastReceiver
 */
public class BluetoothConstants{

    // ---------------------------------------------------- BluetoothService
    // Constants that indicate the current connection state
    /** Bluetooth turned off **/
    public static final int STATE_TURNED_OFF = -1;
    /** Bluetooth available, but doing nothin **/
    public static final int STATE_NONE = 0;
    /** Trying to connect **/
    public static final int STATE_CONNECTING = 2;
    /** Connected to a remote device **/
    public static final int STATE_CONNECTED = 3;

    /** Intent request code that can be used when requesting the bluetooth service to turn the adapter on **/
    public static final int REQUEST_ENABLE_BT = 385;

    // ----------------------------------------------------

    /** Intent filter for bluetooth local broadcasts. **/
    public static final String BTSERVICE_INTENT_FILTER = "Hugginess-BTService";

    /** Extra which is present in all local broadcast: it identifies the kind of event. **/
    public static final String EXTRA_EVT_TYPE = "extra_evt_type";

    // bt adapter-related events
    /** Event type: bluetooth turned on. **/
    public static final String EVT_BT_TURNED_ON = "evt_turned_on";
    /** Event type: bluetooth turned off. **/
    public static final String EVT_BT_TURNED_OFF = "evt_turned_off";
    // connection-related events
    /** Event type: device connected. **/
    public static final String EVT_CONNECTED = "evt_connected";
    /** Event type: device disconnected. **/
    public static final String EVT_DISCONNECTED = "evt_disconnected";
    /** Event type: could not connect. **/
    public static final String EVT_CONNECTION_FAILED = "evt_connection_failed";
    // data-related events
    /** Event type: data received. Extras: {@link #EVT_EXTRA_DATA} **/
    public static final String EVT_DATA_RECEIVED = "evt_data_received";

    // String extra some events
    /** Extra: data received, String (see {@link #EVT_DATA_RECEIVED})**/
    public static final String EVT_EXTRA_DATA = "extra_data_received";


    // ---------------------------------------------------- HuggiService only

    // timeout when waiting for an ack
    /** Max delay between two char received. Upon timeout, the HuggiShirt is considered offline **/
    public static final long BT_TIMEOUT = 2400;

    // acks
    /** Event type: ack received. Extras: {@link #EVT_EXTRA_ACK_CMD} and {@link #EVT_EXTRA_ACK_STATUS} **/
    public static final String EVT_ACK_RECEIVED = "evt_ack";
    /** Extra: acknowledged command, char (see {@link #EVT_ACK_RECEIVED})**/
    public static final String EVT_EXTRA_ACK_CMD = "extra_ack_cmd";
    /** Extra: ack status, boolean: true = ack, false = nak (see {@link #EVT_ACK_RECEIVED})**/
    public static final String EVT_EXTRA_ACK_STATUS = "extra_ack_status";

    // hugs
    /** Event type: hugs received. Extras: {@link #EVT_EXTRA_HUGS_CNT} and {@link #EVT_EXTRA_HUGS_LIST} **/
    public static final String EVT_HUGS_RECEIVED = "evt_hugs_received";
    /** Extra: number of hugs received, int (see {@link #EVT_HUGS_RECEIVED})**/
    public static final String EVT_EXTRA_HUGS_CNT = "extra_hugs_cnt";
    /** Extra: list of new hugs, parcellable (see {@link #EVT_HUGS_RECEIVED})**/
    public static final String EVT_EXTRA_HUGS_LIST = "extra_new_hugs";

    // prefixes and special chars related to the HuggiShirt-App communication
    /** Prefix used to identify a command **/
    public static final String CMD_PREFIX = "$";
    /** Prefix used to identify a data and/or a parameter **/
    public static final String DATA_PREFIX = "@";
    /** Prefix used to delimit parameters/data **/
    public static final String DATA_SEP = "!";
    /** Prefix used to identify an ack **/
    public static final String ACK_PREFIX = "#";
    /** Special char to encode an ack **/
    public static final String ACK_OK = ACK_PREFIX + "#";
    /** Special char to encode an nak **/
    public static final String ACK_NOK = ACK_PREFIX + "?";

    // available commands
    /** Echo command. Format: $E@[data to echo] **/
    public static final char CMD_ECHO = 'E';
    /** Change the HuggiShirt's id command. Format: $I@[new_id] (see {@link #ID_SIZE}) **/
    public static final char CMD_SET_ID = 'I';
    /**
     * Change the HuggiShirt's data command, i.e. the data exchanged during a hug.
     * Format: $I@[new_data] (see {@link #DATA_MAX_SIZE}) *
     */
    public static final char CMD_SET_DATA = 'D';
    /** Fetch hugs command. Format: $H **/
    public static final char CMD_SEND_HUGS = 'H';
    /** Calibrate the pressure sensor. Format: $C **/
    public static final char CMD_CALIBRATE = 'C';
    /** Put the HuggiShirt to sleep. It will wake up on the next character sent. Format: @S **/
    public static final char CMD_SLEEP = 'S';
    /** Get the current configuration (id + data) of the HuggiShirt. Format: $A, Answer: @A![id]![data] **/
    public static final char CMD_DUMP_ALL = 'A';

    // ----------------------------------------------------

    // sizes etc.
    /** Maximal size of a data (recall that the LilyPad has a small memory) **/
    public static final int DATA_MAX_SIZE = 50;
    /** Size of the ID field (should match a swiss phone number) **/
    public static final int ID_SIZE = 10;
}
