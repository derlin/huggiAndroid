/*
 * Copyright (C) 2014 Akexorcist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.eiafr.hugginess.myspp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SuppressLint( "NewApi" )

public class BluetoothSPP{
    // Listener for Bluetooth Status & Connection
    private List<BluetoothListener.OnStateChangedListener> mBluetoothStateListener = new ArrayList<>();
    private List<BluetoothListener.OnDataReceivedListener> mDataReceivedListener = new ArrayList<>();
    private List<BluetoothListener.ConnectionListener> mBluetoothConnectionListener = new ArrayList<>();
    private List<BluetoothListener.AutoConnectionListener> mAutoConnectionListener = new ArrayList<>();

    // Context from activity which call this class
    private Context mContext;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Member object for the chat services
    private BluetoothService mChatService = null;

    // Name and Address of the connected device
    private String mDeviceName = null;
    private String mDeviceAddress = null;

    private boolean isAutoConnecting = false;
    private boolean isAutoConnectionEnabled = false;
    private boolean isConnected = false;
    private boolean isConnecting = false;
    private boolean isServiceRunning = false;

    private String keyword = "";
    private boolean isAndroid = BluetoothState.DEVICE_ANDROID;

    private BluetoothListener.ConnectionListener bcl;
    private int c = 0;


    public BluetoothSPP( Context context ){
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }


    public boolean isBluetoothAvailable(){
        try{
            if( mBluetoothAdapter == null || mBluetoothAdapter.getAddress().equals( null ) ) return false;
        }catch( NullPointerException e ){
            return false;
        }
        return true;
    }


    public boolean isBluetoothEnabled(){
        return mBluetoothAdapter.isEnabled();
    }


    public boolean isServiceAvailable(){
        return mChatService != null;
    }


    public boolean isAutoConnecting(){
        return isAutoConnecting;
    }


    public boolean startDiscovery(){
        return mBluetoothAdapter.startDiscovery();
    }


    public boolean isDiscovery(){
        return mBluetoothAdapter.isDiscovering();
    }


    public boolean cancelDiscovery(){
        return mBluetoothAdapter.cancelDiscovery();
    }

    public boolean isConnected(){ return isConnected; }

    public void setupService(){
        mChatService = new BluetoothService( mContext, mHandler );
    }


    public BluetoothAdapter getBluetoothAdapter(){
        return mBluetoothAdapter;
    }


    public int getServiceState(){
        if( mChatService != null ){
            return mChatService.getState();
        }else{
            return -1;
        }
    }


    public void startService( boolean isAndroid ){
        if( mChatService != null ){
            if( mChatService.getState() == BluetoothState.STATE_NONE ){
                isServiceRunning = true;
                mChatService.start( isAndroid );
                BluetoothSPP.this.isAndroid = isAndroid;
            }
        }
    }


    public void stopService(){
        if( mChatService != null ){
            isServiceRunning = false;
            mChatService.stop();
        }
        new Handler().postDelayed( new Runnable(){
            public void run(){
                if( mChatService != null ){
                    isServiceRunning = false;
                    mChatService.stop();
                }
            }
        }, 500 );
    }


    public void setDeviceTarget( boolean isAndroid ){
        stopService();
        startService( isAndroid );
        BluetoothSPP.this.isAndroid = isAndroid;
    }


    @SuppressLint( "HandlerLeak" )
    private final Handler mHandler = new Handler(){
        public void handleMessage( Message msg ){
            switch( msg.what ){
                case BluetoothState.MESSAGE_WRITE:
                    break;
                case BluetoothState.MESSAGE_READ:
                    String readMessage = ( String ) msg.obj;
                    notifyDataReceived( readMessage );
                    break;

                case BluetoothState.MESSAGE_DEVICE_NAME:
                    mDeviceName = msg.getData().getString( BluetoothState.DEVICE_NAME );
                    mDeviceAddress = msg.getData().getString( BluetoothState.DEVICE_ADDRESS );
                    notifyDeviceConnected( mDeviceName, mDeviceAddress );
                    isConnected = true;
                    break;

                case BluetoothState.MESSAGE_TOAST:
                    Toast.makeText( mContext, msg.getData().getString( BluetoothState.TOAST ), Toast.LENGTH_SHORT ).show();
                    break;

                case BluetoothState.MESSAGE_STATE_CHANGE:
                    notifyServiceStateChanged( msg.arg1 );

                    if( isConnected && msg.arg1 != BluetoothState.STATE_CONNECTED ){
                        notifyDeviceDisconnected();
                        if( isAutoConnectionEnabled ){
                            isAutoConnectionEnabled = false;
                            autoConnect( keyword );
                        }
                        isConnected = false;
                        mDeviceName = null;
                        mDeviceAddress = null;
                    }

                    if( !isConnecting && msg.arg1 == BluetoothState.STATE_CONNECTING ){
                        isConnecting = true;
                    }else if( isConnecting ){
                        if( msg.arg1 != BluetoothState.STATE_CONNECTED ){
                            notifyDeviceConnectionFailed();
                        }
                        isConnecting = false;
                    }
                    break;
            }
        }
    };




    public void stopAutoConnect(){
        isAutoConnectionEnabled = false;
    }


    public void connect( Intent data ){
        String address = data.getExtras().getString( BluetoothState.EXTRA_DEVICE_ADDRESS );
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice( address );
        mChatService.connect( device );
    }


    public void connect( String address ){
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice( address );
        mChatService.connect( device );
    }


    public void disconnect(){
        if( mChatService != null ){
            isServiceRunning = false;
            mChatService.stop();
            if( mChatService.getState() == BluetoothState.STATE_NONE ){
                isServiceRunning = true;
                mChatService.start( BluetoothSPP.this.isAndroid );
            }
        }
    }


    public void enable(){
        mBluetoothAdapter.enable();
    }


    public void send( byte[] data, boolean nl ){
        if( mChatService.getState() == BluetoothState.STATE_CONNECTED ){
                mChatService.write( data );
                if(nl) mChatService.write( "\n".getBytes() );
        }
    }


    public void send( String data, boolean nl ){
        if( mChatService.getState() == BluetoothState.STATE_CONNECTED ){
            if( nl ) data += "\n";
            mChatService.write( data.getBytes() );
        }
    }


    public String getConnectedDeviceName(){
        return mDeviceName;
    }


    public String getConnectedDeviceAddress(){
        return mDeviceAddress;
    }


    public String[] getPairedDeviceName(){
        int c = 0;
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        String[] name_list = new String[ devices.size() ];
        for( BluetoothDevice device : devices ){
            name_list[ c ] = device.getName();
            c++;
        }
        return name_list;
    }


    public String[] getPairedDeviceAddress(){
        int c = 0;
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        String[] address_list = new String[ devices.size() ];
        for( BluetoothDevice device : devices ){
            address_list[ c ] = device.getAddress();
            c++;
        }
        return address_list;
    }


    public void autoConnect( String keywordName ){
        if( !isAutoConnectionEnabled ){
            keyword = keywordName;
            isAutoConnectionEnabled = true;
            isAutoConnecting = true;
            notifyAutoConnectionStarted();
            final ArrayList<String> arr_filter_address = new ArrayList<>();
            final ArrayList<String> arr_filter_name = new ArrayList<>();
            String[] arr_name = getPairedDeviceName();
            String[] arr_address = getPairedDeviceAddress();
            for( int i = 0; i < arr_name.length; i++ ){
                if( arr_name[ i ].contains( keywordName ) ){
                    arr_filter_address.add( arr_address[ i ] );
                    arr_filter_name.add( arr_name[ i ] );
                }
            }

            bcl = new BluetoothListener.ConnectionListener(){
                public void onDeviceConnected( String name, String address ){
                    bcl = null;
                    isAutoConnecting = false;
                }


                public void onDeviceDisconnected(){
                }


                public void onDeviceConnectionFailed(){
                    Log.e( "CHeck", "Failed" );
                    if( isServiceRunning ){
                        if( isAutoConnectionEnabled ){
                            c++;
                            if( c >= arr_filter_address.size() ) c = 0;
                            connect( arr_filter_address.get( c ) );
                            Log.e( "CHeck", "Connect" );
                            notifyNewConnection( arr_filter_name.get( c ), arr_filter_address.get( c ) );

                        }else{
                            bcl = null;
                            isAutoConnecting = false;
                        }
                    }
                }
            };

            setBluetoothConnectionListener( bcl );
            c = 0;
            notifyNewConnection( arr_name[ c ], arr_address[ c ] );

            if( arr_filter_address.size() > 0 ){
                connect( arr_filter_address.get( c ) );
            }else{
                Toast.makeText( mContext, "Device name mismatch", Toast.LENGTH_SHORT ).show();
            }
        }

    }


    /* *****************************************************************
     * listeners
     * ****************************************************************/

    // interfaces

    // ----------------------------------------------------

    // add
    public void setBluetoothStateListener( BluetoothListener.OnStateChangedListener listener ){
        if(!mBluetoothStateListener.contains( listener ))
            mBluetoothStateListener.add( listener );
    }


    public void setOnDataReceivedListener( BluetoothListener.OnDataReceivedListener listener ){
        if(!mDataReceivedListener.contains( listener ))
            mDataReceivedListener.add( listener );
    }


    public void setBluetoothConnectionListener( BluetoothListener.ConnectionListener listener ){
        if(!mBluetoothConnectionListener.contains( listener ))
            mBluetoothConnectionListener.add( listener );
    }


    public void setAutoConnectionListener( BluetoothListener.AutoConnectionListener listener ){
        if(!mAutoConnectionListener.contains( listener ))
            mAutoConnectionListener.add( listener );
    }


    // remove
    public void removeBluetoothStateListener( BluetoothListener.OnStateChangedListener listener ){
        if(mBluetoothStateListener.contains( listener ))
            mBluetoothStateListener.remove( listener );
    }


    public void removeOnDataReceivedListener( BluetoothListener.OnDataReceivedListener listener ){
        if(mDataReceivedListener.contains( listener ))
            mDataReceivedListener.remove( listener );
    }


    public void removeBluetoothConnectionListener( BluetoothListener.ConnectionListener listener ){
        if(mAutoConnectionListener.contains( listener ))
            mBluetoothConnectionListener.remove( listener );
    }


    public void removeAutoConnectionListener( BluetoothListener.AutoConnectionListener listener ){
        if(mAutoConnectionListener.contains( listener ))
            mAutoConnectionListener.remove( listener );
    }

    // notify
    private void notifyDeviceConnectionFailed(){
        for( BluetoothListener.ConnectionListener listener : mBluetoothConnectionListener ){
            listener.onDeviceConnectionFailed();
        }//end for
    }

    private void notifyDeviceDisconnected(){
        for( BluetoothListener.ConnectionListener listener : mBluetoothConnectionListener ){
            listener.onDeviceDisconnected();
        }//end for
    }

    private void notifyDeviceConnected(String name, String address){
        for( BluetoothListener.ConnectionListener listener : mBluetoothConnectionListener ){
            listener.onDeviceConnected( name, address );
        }//end for
    }


    private void notifyNewConnection(String name, String address){
        for( BluetoothListener.AutoConnectionListener listener : mAutoConnectionListener ){
            listener.onNewConnection( name, address );
        }//end for

    }

    private void notifyAutoConnectionStarted(){
        for( BluetoothListener.AutoConnectionListener listener : mAutoConnectionListener ){
            listener.onAutoConnectionStarted();
        }//end for
    }

    private void notifyDataReceived( String readMessage ){
        for( BluetoothListener.OnDataReceivedListener listener : mDataReceivedListener ){
            listener.onDataReceived( readMessage );
        }//end for
    }

    private void notifyServiceStateChanged( int state ){
        for( BluetoothListener.OnStateChangedListener listener : mBluetoothStateListener ){
            listener.onServiceStateChanged( state );
        }//end for
    }


}




