package ch.eiafr.hugginess.services.bluetooth;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static ch.eiafr.hugginess.services.bluetooth.BluetoothConstants.*;

/**
 * This class is a generic bluetooth service which handles connections to
 * bluetooth devices using the SPP (Serial Port Profile) profile.
 * It works as a bound service and is thread-safe.
 * <p/>
 * Main features:
 * <ul>
 * <li>Enable/disable bluetooth adapter</li>
 * <li>Connect to another device, android or not</li>
 * <li>Local broadcasts on bluetooth events: adapter turned on/off, connection/disconnection, data received, ..
 * . (see {@link ch.eiafr.hugginess.services.bluetooth.BluetoothConstants} for more information)</li>
 * </ul>
 * <p/>
 * To use it:
 * <ol>
 * <li>Bind the service (see
 * {@link android.content.Context#bindService(android.content.Intent, android.content.ServiceConnection, int)}
 * and {@link android.content.ServiceConnection}).</li>
 * <li>Get a reference to the service using the {@link ch.eiafr.hugginess.services.bluetooth.BluetoothService
 * .BTBinder#getService} method of the binder returned by the {@link android.content
 * .ServiceConnection#onServiceConnected(android.content.ComponentName, android.os.IBinder)}</li>
 * <li>Use the methods directly to connect, disconnect and such</li>
 * </ol>
 * <p/>
 * All events are notified using a local broadcast. To register to such events, register a
 * received in your activity onStart. For example:
 * <p><blockquote><pre>
 * BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
 *         \@Override
 *         public void onReceive( Context context, Intent intent ){
 * <p/>
 *             mTextStatus.setEnabled( true );
 *             mAnim.stop();
 * <p/>
 *             switch( intent.getStringExtra( EXTRA_EVT_TYPE ) ){
 *                // ...
 *             }
 *         }
 * };
 * <p/>
 * LocalBroadcastManager.getInstance( this )
 *      .registerReceiver( mBroadcastReceiver,
 *              new IntentFilter(BluetoothConstants.BTSERVICE_INTENT_FILTER ) );
 * </pre></blockquote></p>
 * <p/>
 * creation date    24.11.2014
 * context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
 *
 * @author Lucy Linder
 * @see ch.eiafr.hugginess.services.bluetooth.BluetoothConstants
 */
@SuppressLint( "NewApi" )
public class BluetoothService extends Service{


    // TODO : listen to bluetooth turned off !!!!!!!
    // TODO: rethink the events to include state changed ?
    // http://stackoverflow.com/questions/3806536/how-to-enable-disable-bluetooth-programmatically-in-android

    //-------------------------------------------------------------

    // Debugging
    protected static final String TAG = "Bluetooth Service";

    // Unique UUID for this application
    // UUID to use when connecting to an android device
    protected static final UUID UUID_ANDROID_DEVICE = UUID.fromString( "fa87c0d0-afac-11de-8a39-0800200c9a66" );
    // UUID to use when connecting to a non-android device
    protected static final UUID UUID_OTHER_DEVICE = UUID.fromString( "00001101-0000-1000-8000-00805F9B34FB" );

    // The connecting/connected device
    protected BluetoothDevice mDevice = null;

    // Member fields
    protected final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    LocalBroadcastManager mBroadcastManager;
    protected ConnectThread mConnectThread;
    protected ConnectedThread mConnectedThread;
    protected int mState = STATE_NONE;
    protected boolean isAndroid = false;


    //-------------------------------------------------------------
    private final IBinder myBinder = new BTBinder();

    /** Binder for this service * */
    public class BTBinder extends Binder{
        /**
         * @return a reference to the bound service
         */
        public BluetoothService getService(){
            return BluetoothService.this;
        }
    }//end class


    @Override
    public IBinder onBind( Intent arg0 ){
        return myBinder;
    }


    //-------------------------------------------------------------
    // Receiver for bluetooth adapter state change
    private final BroadcastReceiver mReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive( Context context, Intent intent ){
            onBTBroadCastReceived( context, intent );
        }
    };
    // ----------------------------------------------------


    @Override
    public int onStartCommand( Intent intent, int flags, int startId ){
        mBroadcastManager = LocalBroadcastManager.getInstance( this );
        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter filter = new IntentFilter( BluetoothAdapter.ACTION_STATE_CHANGED );
        registerReceiver( mReceiver, filter );
        return super.onStartCommand( intent, flags, startId );
    }


    @Override
    public void onDestroy(){
        stopThreads();
        unregisterReceiver( mReceiver );
        super.onDestroy();
    }



    /* *****************************************************************
     * getters
     * ****************************************************************/


    /**
     * @return the name of the connecting/connected device, or null if not device is connected.
     */
    public synchronized String getDeviceName(){
        return mDevice != null ? mDevice.getName() : null;
    }


    /**
     * @return the mac address of the connecting/connected device, or null if not device is connected.
     */
    public synchronized String getDeviceAddress(){
        return mDevice != null ? mDevice.getAddress() : null;
    }


    /**
     * @return the current state of the service, i.e.
     * {@link ch.eiafr.hugginess.services.bluetooth.BluetoothConstants#STATE_TURNED_OFF},
     * {@link ch.eiafr.hugginess.services.bluetooth.BluetoothConstants#STATE_NONE},
     * {@link ch.eiafr.hugginess.services.bluetooth.BluetoothConstants#STATE_CONNECTING},
     * {@link ch.eiafr.hugginess.services.bluetooth.BluetoothConstants#STATE_CONNECTED}.
     */
    public synchronized int getState(){
        return mState;
    }


    /**
     * @return true if the service is currently connected to a device.
     */
    public synchronized boolean isConnected(){
        return mState == STATE_CONNECTED;
    }


    /**
     * @return true if the bluetooth is enabled.
     */
    public synchronized boolean isBluetoothEnabled(){
        return mAdapter.isEnabled();
    }


    /* *****************************************************************
     * public functions
     * ****************************************************************/


    /**
     * Enable the bluetooth adapter (without asking the user).
     * @return see {@link android.bluetooth.BluetoothAdapter#enable()}
     */
    public boolean enable(){
        if( !mAdapter.isEnabled() ){
            return mAdapter.enable();
        }
        return true;
    }


    /**
     * Connect to a remote device. Don't forget to call {@link #setDeviceTargetType(boolean)}
     * to android or other (default) before proceeding.
     * Note: this method will start the connection process and return immediately.
     * You need to listen to the broadcasts to know the outcome.
     * @param address the mac address of the remote device
     */
    public synchronized void connect( String address ){
        this.connect( mAdapter.getRemoteDevice( address ) );
    }

    /**
     * Connect to a remote device. Don't forget to call {@link #setDeviceTargetType(boolean)}
     * to android or other (default) before proceeding.
     * Note: this method will start the connection process and return immediately.
     * You need to listen to the broadcasts to know the outcome.
     * @param device the remote device
     */
    public synchronized void connect( BluetoothDevice device ){
        if( !isBluetoothEnabled() ){  // TODO
            enable();
            if( !isBluetoothEnabled() ) return;
        }

        if( device == null ){
            Log.e( TAG, "Connect: empty device parameter!" );
            return;
        }

        // Cancel any thread attempting to make a connection
        // or currently running a connection
        stopThreads();

        // Start a new thread to connect with the given device
        mConnectThread = new ConnectThread( device );
        mConnectThread.start();
    }

    /** Disconnect from the device **/
    public synchronized void disconnect(){
        stopThreads();
        notifyNewState( STATE_NONE );
    }


    /**
     * Send a message to the connected device
     * @param msg the data to send
     * @param nl  whether or not to add a line feed at the end of the data.
     */
    public synchronized void send( String msg, boolean nl ){
        if( nl ) msg += '\n';
        this.send( msg.getBytes() );
    }//end send


    // Write to the ConnectedThread in an unsynchronized manner
    // out : The bytes to write


    /**
     * Send a message to the connected device
     * @param msg the bytes to send
     */
    public void send( byte[] msg ){
        // Create temporary object
        ConnectedThread thread;

        // Synchronize a copy of the ConnectedThread
        synchronized( this ){
            if( mState != STATE_CONNECTED ) return;
            thread = mConnectedThread;
        }

        // Perform the write unsynchronized
        thread.write( msg );
    }


    /**
     * Set the kind of devices the service will connect to.
     * @param isAndroid true if the target device is another android, false otherwise.
     */
    public synchronized void setDeviceTargetType( boolean isAndroid ){
        this.isAndroid = isAndroid;
    }



    /* *****************************************************************
     * private functions
     * ****************************************************************/



    protected synchronized void notifyNewState( int newState ){
        if( newState == mState ) return; // nothing to do

        Log.d( TAG, "setState() " + mState + " -> " + newState );

        switch( mState ){
            case STATE_CONNECTED:
                if( newState != STATE_CONNECTED ){
                    // disconnect event
                    mBroadcastManager.sendBroadcast( getIntent( EVT_DISCONNECTED ) );
                }

                break;

            case STATE_CONNECTING:
                if( newState == STATE_CONNECTED ){
                    // new connection event
                    mBroadcastManager.sendBroadcast( getIntent( EVT_CONNECTED ) );

                }else{
                    // connection failed
                    mBroadcastManager.sendBroadcast( getIntent( EVT_CONNECTION_FAILED ) );
                }
                break;
        }

        mState = newState;
    }


    protected void notifyDataReceived( String data ){
        // add an extra to the broadcast
        Intent i = getIntent( EVT_DATA_RECEIVED );
        i.putExtra( EVT_EXTRA_DATA, data );
        mBroadcastManager.sendBroadcast( i );
    }


    protected synchronized void stopThreads(){
        // cancel any thread attempting to make a connection
        if( mConnectThread != null ){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // cancel any thread currently running a connection
        if( mConnectedThread != null ){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }


    /*
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    protected synchronized void launchConnectedThread( BluetoothSocket socket, BluetoothDevice device ){
        // Cancel the thread that completed the connection
        stopThreads();

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread( socket );
        mConnectedThread.start();

        // Update variables and send the name of the connected device back to the UI Activity
        mDevice = device;
    }

    /* *****************************************************************
     * Connection thread
     * ****************************************************************/

    // This thread runs while attempting to make an outgoing connection
    // with a device. It runs straight through
    // the connection either succeeds or fails
    private class ConnectThread extends Thread{
        private BluetoothDevice mmDevice;
        private BluetoothSocket mmSocket;


        public ConnectThread( BluetoothDevice device ){
            mmDevice = device;
        }


        public void run(){
            // notify the change
            notifyNewState( STATE_CONNECTING );

            // always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // make a connection to the BluetoothSocket
            try{
                // get a BluetoothSocket
                mmSocket = mmDevice.createRfcommSocketToServiceRecord( BluetoothService.this.isAndroid ?
                        UUID_ANDROID_DEVICE : UUID_OTHER_DEVICE );

                // this is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();

                // reset the ConnectThread because we're done
                synchronized( BluetoothService.this ){
                    mConnectThread = null;
                }
                // start the connected thread
                launchConnectedThread( mmSocket, mmDevice );

                // notify the change
                notifyNewState( STATE_CONNECTED );


            }catch( IOException e ){
                // CONNECTION FAILED
                try{
                    // Close the socket
                    mmSocket.close();
                }catch( IOException _e ){
                }
                // notify the failure
                notifyNewState( STATE_NONE );
            }

        }


        void cancel(){
            // close the socket, it will automatically disconnect the bt
            try{
                mmSocket.close();
            }catch( IOException e ){
            }
        }
    } // end connection thread

    /* *****************************************************************
     * Connected thread
     * ****************************************************************/

    // This thread runs during a connection with a remote device.
    // It handles all incoming and outgoing transmissions.
    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;


        public ConnectedThread( BluetoothSocket socket ){
            mmSocket = socket;
        }


        public void run(){
            StringBuilder builder = new StringBuilder();

            // get the BluetoothSocket input and output streams
            try{
                mmInStream = mmSocket.getInputStream();
                mmOutStream = mmSocket.getOutputStream();
            }catch( IOException e ){
                Log.e( TAG, "error while opening connected streams" );
                notifyNewState( STATE_NONE );
                return;
            }

            // keep listening to the InputStream while connected
            while( true ){
                try{
                    int data = mmInStream.read();
                    if( data == '\n' ){
                        // notify a new line has been received
                        String line = builder.toString();
                        builder.delete( 0, builder.length() ); // clear buffer

                        Log.v( getPackageName(), "BT service: received '" + line + "'" );
                        notifyDataReceived( line );
                    }else{
                        builder.append( ( char ) data );
                    }
                }catch( IOException e ){
                    // reset to STATE_NONE. the method will notify the listeners
                    // (CONNECTION LOST)
                    notifyNewState( STATE_NONE );
                    break;
                }
            }
        }


        void write( byte[] buffer ){
        // write to the connected OutStream.
            try{
                mmOutStream.write( buffer );
            }catch( Exception e ){
                Log.e( TAG, "Exception while writing to BT device " + mDevice.getName() );
            }
        }


        void cancel(){
            // close the socket, it will automatically disconnect the bt
            try{
                mmSocket.close();
            }catch( Exception e ){
                Log.d( TAG, "Connected thread canceled." );
            }
        }
    } // end connectedThread


    protected Intent getIntent( String evtType ){
        Intent i = new Intent( BTSERVICE_INTENT_FILTER );
        i.putExtra( EXTRA_EVT_TYPE, evtType );
        return i;
    }


    /* *****************************************************************
     * BroadcastReceiver for bluetooth events
     * ****************************************************************/


    protected void onBTBroadCastReceived( Context context, Intent intent ){
        // handle the the turn on/turn off events from the adapter
        final String action = intent.getAction();

        if( action.equals( BluetoothAdapter.ACTION_STATE_CHANGED ) ){
            final int state = intent.getIntExtra( BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR );

            switch( state ){
                case BluetoothAdapter.STATE_OFF:
                    stopThreads();
                    mState = STATE_TURNED_OFF;
                    mBroadcastManager.sendBroadcast( getIntent( EVT_BT_TURNED_OFF ) );
                    break;

                case BluetoothAdapter.STATE_ON:
                    mState = STATE_NONE;
                    mBroadcastManager.sendBroadcast( getIntent( EVT_BT_TURNED_ON ) );
                    break;
            }
        }
    }

} // end class