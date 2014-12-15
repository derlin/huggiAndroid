package ch.eiafr.hugginess.services.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import static ch.eiafr.hugginess.services.bluetooth.BluetoothConstants.*;

/**
 * @author: Lucy Linder
 * @date: 14.12.2014
 */
public class HuggiBroadcastReceiver extends BroadcastReceiver{

    private static final IntentFilter INTENT_FILTER = new IntentFilter( BTSERVICE_INTENT_FILTER );

    // ----------------------------------------------------

    public void registerSelf( Context context ){
        LocalBroadcastManager.getInstance( context ).registerReceiver( this, INTENT_FILTER );
    }


    public void unregisterSelf( Context context ){
        LocalBroadcastManager.getInstance( context ).unregisterReceiver( this );
    }

    // ----------------------------------------------------


    public void onBtTurnedOn(){ }


    public void onBtTurnedOff(){ }


    public void onBtConnected(){ }


    public void onBtDisonnected(){ }

    public void onBtStateChanged(){}

    public void onBtConnectionFailed(){ }

    public void onBtDataReceived( String line ){ }

    public void onBtHugsReceived( int hugCOunt ){ }


    public void onBtAckReceived( char cmd, boolean ok ){ }

    /* *****************************************************************
     * broadcast management
     * ****************************************************************/


    @Override
    public void onReceive( Context context, Intent intent ){
        switch( intent.getStringExtra( EXTRA_EVT_TYPE ) ){

            case EVT_BT_TURNED_ON:
                onBtTurnedOn();
                onBtStateChanged();

            case EVT_BT_TURNED_OFF:
                onBtTurnedOff();
                onBtStateChanged();
                break;

            case EVT_CONNECTED:
                onBtConnected();
                onBtStateChanged();
                break;


            case EVT_DISCONNECTED:
                onBtDisonnected();
                onBtStateChanged();
                break;

            case EVT_CONNECTION_FAILED:
                onBtConnectionFailed();
                break;

            case EVT_DATA_RECEIVED:
                String line = intent.getStringExtra( EVT_EXTRA_DATA );
                onBtDataReceived(line);
                break;

            case EVT_HUGS_RECEIVED:
                int cnt = intent.getIntExtra( EVT_EXTRA_HUGS_CNT, 0 );
                onBtHugsReceived( cnt );
                break;

            case EVT_ACK_RECEIVED:
                char cmd = intent.getCharExtra( EVT_EXTRA_ACK_CMD, '-' );
                boolean ok = intent.getBooleanExtra( EVT_EXTRA_ACK_STATUS, false );
                onBtAckReceived( cmd, ok );
                break;
        }

    }



}//end class
