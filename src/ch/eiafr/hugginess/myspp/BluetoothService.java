/**
 * @author : Lucy Linder
 * @date: 24 nov. 2014
 */
package ch.eiafr.hugginess.myspp;

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

import static ch.eiafr.hugginess.myspp.BluetoothState.*;

@SuppressLint( "NewApi" )
public class BluetoothService extends Service {


    // TODO : listen to bluetooth turned off !!!!!!!
    // TODO: rethink the events to include state changed ?
    // http://stackoverflow.com/questions/3806536/how-to-enable-disable-bluetooth-programmatically-in-android


    //-------------------------------------------------------------
    private final IBinder myBinder = new BTBinder();

    public class BTBinder extends Binder {
        public BluetoothService getService(){
            return BluetoothService.this;
        }
    }//end class


    @Override
    public IBinder onBind( Intent arg0 ){
        return myBinder;
    }


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

    //-------------------------------------------------------------

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive( Context context, Intent intent ){
            final String action = intent.getAction();

            if( action.equals( BluetoothAdapter.ACTION_STATE_CHANGED ) ){
                final int state = intent.getIntExtra( BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR );

                switch( state ){
                    case BluetoothAdapter.STATE_OFF:
                        stopThreads();
                        setState( STATE_UNAVAILABLE );
                        break;

                    case BluetoothAdapter.STATE_ON:
                        setState( STATE_NONE );
                        if( mPendingConnection != null ) connect( mPendingConnection );
                        break;
                    //                    case BluetoothAdapter.STATE_TURNING_ON:
                    //                        setButtonText("Turning Bluetooth on...");
                    //                        break;
                }
            }
        }
    };
    //-------------------------------------------------------------

    // Debugging
    private static final String TAG = "Bluetooth Service 2";

    // Unique UUID for this application
    private static final UUID UUID_ANDROID_DEVICE = UUID.fromString( "fa87c0d0-afac-11de-8a39-0800200c9a66" );
    private static final UUID UUID_OTHER_DEVICE = UUID.fromString( "00001101-0000-1000-8000-00805F9B34FB" );

    // Name and Address of the connected device
    private String mDeviceName = null;
    private String mDeviceAddress = null;

    // Member fields
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    LocalBroadcastManager mBroadcastManager;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState = STATE_NONE;
    private boolean isAndroid = DEVICE_OTHER;

    private BluetoothDevice mPendingConnection = null;

    /* *****************************************************************
     * getters
     * ****************************************************************/


    public synchronized String getDeviceName(){
        return mDeviceName;
    }


    public synchronized String getDeviceAddress(){
        return mDeviceAddress;
    }


    public synchronized int getState(){
        return mState;
    }


    public synchronized boolean isConnected(){
        return mState == STATE_CONNECTED;
    }


    public synchronized boolean isBluetoothEnabled(){
        return mAdapter.isEnabled();
    }


    /* *****************************************************************
     * public functions
     * ****************************************************************/


    public boolean enable(){
        Intent btIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
        btIntent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        getApplicationContext().startActivity( btIntent );
        return true;
    }


    public synchronized void connect( String address ){
        this.connect( mAdapter.getRemoteDevice( address ) );
    }


    // Start the ConnectThread to initiate a connection to a remote device
    // device : The BluetoothDevice to connect
    // secure : Socket Security type - Secure (true) , Insecure (false)
    public synchronized void connect( BluetoothDevice device ){

        if( !isBluetoothEnabled() ){  // TODO
            mPendingConnection = device;
            enable();
            return;
        }
        mPendingConnection = null;

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


    public void disconnect(){
        stopThreads();
        setState( STATE_NONE );
    }


    public synchronized void send( String msg, boolean nl ){
        if( nl ) msg += '\n';
        this.send( msg.getBytes() );
    }//end send


    // Write to the ConnectedThread in an unsynchronized manner
    // out : The bytes to write
    public void send( byte[] out ){
        // Create temporary object
        ConnectedThread thread;

        // Synchronize a copy of the ConnectedThread
        synchronized( this ){
            if( mState != STATE_CONNECTED ) return;
            thread = mConnectedThread;
        }

        // Perform the write unsynchronized
        thread.write( out );
    }


    public synchronized void setDeviceTargetType( boolean isAndroid ){
        this.isAndroid = isAndroid;
    }



    /* *****************************************************************
     * private functions
     * ****************************************************************/


    // Set the current state of the chat connection
    // state : An integer defining the current connection state
    private synchronized void setState( int newState ){
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
                    Intent i = getIntent( EVT_CONNECTED );
                    i.putExtra( EVT_EXTRA_DNAME, mDeviceName );
                    i.putExtra( EVT_EXTRA_DADDR, mDeviceAddress );
                    mBroadcastManager.sendBroadcast( i );

                }else{
                    // connection failed
                    mBroadcastManager.sendBroadcast( getIntent( EVT_CONNECTION_FAILED ) );
                }
                break;
        }

        mState = newState;
        mBroadcastManager.sendBroadcast( getIntent( EVT_STATE_CHANGED ) );
    }


    private synchronized void stopThreads(){
        // Cancel any thread attempting to make a connection
        if( mConnectThread != null ){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel any thread currently running a connection
        if( mConnectedThread != null ){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }


    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void launchConnectedThread( BluetoothSocket socket, BluetoothDevice device ){
        // Cancel the thread that completed the connection
        stopThreads();

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread( socket );
        mConnectedThread.start();

        // Update variables and send the name of the connected device back to the UI Activity
        mDeviceName = device.getName();
        mDeviceAddress = device.getAddress();
    }

    /* *****************************************************************
     * Connection thread
     * ****************************************************************/

    // This thread runs while attempting to make an outgoing connection
    // with a device. It runs straight through
    // the connection either succeeds or fails
    private class ConnectThread extends Thread {
        private BluetoothDevice mmDevice;
        private BluetoothSocket mmSocket;


        public ConnectThread( BluetoothDevice device ){
            mmDevice = device;
        }


        public void run(){
            // notify the change
            setState( STATE_CONNECTING );

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try{
                // Get a BluetoothSocket
                mmSocket = mmDevice.createRfcommSocketToServiceRecord( BluetoothService.this.isAndroid ?
                        UUID_ANDROID_DEVICE : UUID_OTHER_DEVICE );

                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();

                // Reset the ConnectThread because we're done
                synchronized( BluetoothService.this ){
                    mConnectThread = null;
                }
                // Start the connected thread
                launchConnectedThread( mmSocket, mmDevice );

                // notify the change
                setState( STATE_CONNECTED );


            }catch( IOException e ){
                // CONNECTION FAILED
                try{
                    // Close the socket
                    mmSocket.close();
                }catch( IOException _e ){
                }
                // notify the failure
                setState( STATE_NONE );
            }

        }


        void cancel(){
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
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;


        public ConnectedThread( BluetoothSocket socket ){
            mmSocket = socket;
        }


        public void run(){
            StringBuilder builder = new StringBuilder();

            // Get the BluetoothSocket input and output streams
            try{
                mmInStream = mmSocket.getInputStream();
                mmOutStream = mmSocket.getOutputStream();
            }catch( IOException e ){
                Log.e( TAG, "error while opening connected streams" );
                setState( STATE_NONE );
                return;
            }

            // Keep listening to the InputStream while connected
            while( true ){
                try{
                    int data = mmInStream.read();
                    if( data == '\n' ){
                        // notify a new line has been received
                        Intent i = getIntent( EVT_DATA_RECEIVED );
                        i.putExtra( EVT_EXTRA_DATA, builder.toString() );
                        mBroadcastManager.sendBroadcast( i );
                        builder.delete( 0, builder.length() ); // clear buffer
                    }else{
                        builder.append( ( char ) data );
                    }
                }catch( IOException e ){
                    // reset to STATE_NONE. the method will notify the listeners
                    // (CONNECTION LOST)
                    setState( STATE_NONE );
                    break;
                }
            }
        }


        // Write to the connected OutStream.
        void write( byte[] buffer ){
            try{
                mmOutStream.write( buffer );
            }catch( IOException e ){
                Log.e( TAG, "Exception while writing to BT device " + mDeviceName );
            }
        }


        void cancel(){
            try{
                mmSocket.close();
            }catch( IOException e ){
                Log.d( TAG, "Connected thread canceled." );
            }
        }
    } // end connectedThread


    private Intent getIntent( String evtType ){
        Intent i = new Intent( BTSERVICE_INTENT_FILTER );
        i.putExtra( EXTRA_EVT_TYPE, evtType );
        return i;
    }

} // end class