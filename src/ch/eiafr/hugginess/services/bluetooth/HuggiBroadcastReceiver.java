package ch.eiafr.hugginess.services.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import static ch.eiafr.hugginess.services.bluetooth.BluetoothConstants.*;

/**
 * This class is a helper to manage local broadcasts from the {@link ch.eiafr.hugginess.services.bluetooth
 * .HuggiBluetoothService}.
 * <p/>
 * It intercepts all the bluetooth broadcasts, extracts the parameters (if any) and defines one method par
 * event type, allowing you to handle and filter events easily.
 * <p/>
 * To use it, simply create a child, implement the method that you are interested in and call {@link
 * #registerSelf(android.content.Context)}.
 * Don't forget to unregister after use (you could for example register in the activity's onCreate method and
 * unregister in it onDestroy).
 *
 * @author Lucy Linder
 *         <p/>
 *         creation date    14.12.2014
 *         context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
 * @see ch.eiafr.hugginess.services.bluetooth.HuggiBluetoothService
 */
public abstract class HuggiBroadcastReceiver extends BroadcastReceiver{

    private static final IntentFilter INTENT_FILTER = new IntentFilter( BTSERVICE_INTENT_FILTER );

    // ----------------------------------------------------


    /**
     * Register this receiver to the local broadcast manager to start receiving events.
     *
     * @param context the context
     */
    public void registerSelf( Context context ){
        LocalBroadcastManager.getInstance( context ).registerReceiver( this, INTENT_FILTER );
    }


    /**
     * Unregister this receiver from the local broadcast manager to stop receiving events.
     *
     * @param context the context
     */
    public void unregisterSelf( Context context ){
        LocalBroadcastManager.getInstance( context ).unregisterReceiver( this );
    }

    // ----------------------------------------------------


    /** Called when the bluetooth adapter is turned on. * */
    public void onBtTurnedOn(){ }


    /** Called when the bluetooth adapter is turned off. * */
    public void onBtTurnedOff(){ }


    /** Called upon a successful connection. * */
    public void onBtConnected(){ }


    /** Called upon a disconnection. * */
    public void onBtDisonnected(){ }


    /** Called when the bluetooth state changed (turn on/off, connected/disconnected). * */
    public void onBtStateChanged(){}


    /** Called upon an unsuccessful connection. * */
    public void onBtConnectionFailed(){ }


    /**
     * Called when a new line has been received.
     *
     * @param line the new line, without linebreak.
     */
    public void onBtDataReceived( String line ){ }


    /**
     * Called when some hugs have been received (and saved to the database).
     *
     * @param hugCount the number of new hugs
     */
    public void onBtHugsReceived( int hugCount ){ }


    /**
     * Called when an ack/nak has been received.
     *
     * @param cmd the acknowledged command
     * @param ok  the ack status. True = ack, false = nak.
     */
    public void onBtAckReceived( char cmd, boolean ok ){ }

    /* *****************************************************************
     * broadcast management
     * ****************************************************************/


    @Override
    public void onReceive( Context context, Intent intent ){
        // handle the different kind of events
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
                onBtDataReceived( line );
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
