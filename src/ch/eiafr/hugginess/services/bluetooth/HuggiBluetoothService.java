package ch.eiafr.hugginess.services.bluetooth;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import ch.eiafr.hugginess.sql.entities.Hug;
import ch.eiafr.hugginess.sql.helpers.HuggiDataSource;

import java.sql.SQLException;
import java.util.*;

import static ch.eiafr.hugginess.services.bluetooth.BluetoothConstants.*;

/**
 * This class is a bluetooth service designed specifically for the Hugginess application.
 * It works as a singleton, startup service (i.e. start it once at the application start and
 * use {@link #getInstance()} to get a reference to it from activities).
 * <p/>
 * Main features:
 * <ul>
 * <li>Add new events, like hugs received or ack received</li>
 * <li>Ease the notification process by providing a {@link ch.eiafr.hugginess.services.bluetooth
 * .HuggiBroadcastReceiver}</li>
 * <li>Handle the incoming hugs by adding them to the sqlite database</li>
 * <li>Provide simple methods to send commands to the HuggiShirt</li>
 * </ul>
 *
 * @author Lucy Linder
 *         <p/>
 *         creation date    28.11.2014
 *         context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
 * @see ch.eiafr.hugginess.services.bluetooth.BluetoothConstants
 * @see ch.eiafr.hugginess.services.bluetooth.HuggiBroadcastReceiver
 */
public class HuggiBluetoothService extends BluetoothService{


    private static HuggiBluetoothService INSTANCE;
    private IBinder myBinder = new BTBinder();

    private Timer mTimer = null; // for ack and receive hugs
    private List<Hug> mHugBuffer = new ArrayList<>();


    // ----------------------------------- singleton

    public static HuggiBluetoothService getInstance(){
        return INSTANCE;
    }

    // ----------------------------------- overrides


    @Override
    public void onCreate(){
        super.onCreate();
        INSTANCE = this;
    }


    @Override
    public void onDestroy(){
        INSTANCE = null;
        super.onDestroy();
    }

    // ----------------------------------- binding

    public class BTBinder extends Binder{
        public HuggiBluetoothService getService(){
            return HuggiBluetoothService.this;
        }
    }//end class


    @Override
    public IBinder onBind( Intent intent ){
        return myBinder;
    }

    // ----------------------------------- notifications


    @Override
    protected void notifyDataReceived( String data ){

        if( data.startsWith( DATA_PREFIX + CMD_SEND_HUGS ) ){
            // one hug has been received
            Hug hug = Hug.parseHug( data );

            if( hug != null ){
                // send ack
                send( ACK_PREFIX.getBytes() );
                mHugBuffer.add( hug );

                // first, get all the hugs and then save them
                // since the save process might take some time...
                if( mTimer == null ){
                    mTimer = new Timer();
                    mTimer.schedule( new TimerTask(){
                        @Override
                        public void run(){
                            insertHugs();
                        }
                    }, BT_TIMEOUT );
                }
            }

        }else if( data.matches( ACK_PREFIX + "[A-Z][#|?]" ) ){
            // format: #<char cmd>[#|?], ack is #, nak is ?
            notifyAckReceived( data.charAt( 1 ), data.charAt( 2 ) == '#' );
        }

        super.notifyDataReceived( data );

    }



    private void notifyHugsReceived( Hug[] newHugs ){
        // add the number of hugs and the list as extras
        Intent i = getIntent( EVT_HUGS_RECEIVED );
        i.putExtra( EVT_EXTRA_HUGS_CNT, newHugs.length );
        i.putExtra( EVT_EXTRA_HUGS_LIST, newHugs );
        mBroadcastManager.sendBroadcast( i );
    }


    private void notifyAckReceived( char cmd, boolean ok ){
        // add the command and status as extra
        Intent i = getIntent( EVT_ACK_RECEIVED );
        i.putExtra( EVT_EXTRA_ACK_CMD, cmd );
        i.putExtra( EVT_EXTRA_ACK_STATUS, ok );
        mBroadcastManager.sendBroadcast( i );
    }


    // ----------------------------------- commands


    /**
     * Send a command with parameters to the HuggiShirt.
     * @param CMD  the command (see {@link ch.eiafr.hugginess.services.bluetooth.BluetoothConstants})
     * @param data the parameters
     */
    public void executeCommand( char CMD, String data ){
        send( String.format( "%s%s%s%s", CMD_PREFIX, CMD, DATA_PREFIX, data ), true );
    }


    /**
     * Send a command to the HuggiShirt.
     * @param CMD  the command (see {@link ch.eiafr.hugginess.services.bluetooth.BluetoothConstants})
     */
    public void executeCommand( char CMD ){
        send( CMD_PREFIX + CMD, true );
    }


    // ----------------------------------- hugs management


    private void insertHugs(){
        // cancel the mTimer task
        if( mTimer != null ){
            mTimer.cancel();
            mTimer = null;
        }

        List<Hug> newHugs = new ArrayList<>();

        try( HuggiDataSource dbs = new HuggiDataSource( getApplicationContext(), true ) ){

            int dbCount = dbs.getHugsCount();
            int count = 0; // keep track of the number of inserted hugs

            for( Hug hug : mHugBuffer ){
                Log.d( getPackageName(), TAG + String.format( "Hug with %s, data = %s, dur = %d\n",//
                        hug.getHuggerID(), hug.getData(), hug.getDuration() ) );

                if( dbs.addHug( hug ) ){
                    count++;
                    newHugs.add( hug );
                }else{
                    // error while inserting hug => key violation
                    Log.d( getPackageName(), TAG + " In insert new hug: '" + hug + "' not unique !" );
                }
            }//end for

            if( dbs.getHugsCount() - dbCount != count ){
                Log.e( getPackageName(), TAG + " Error: counts do not match: " + ( dbs.getHugsCount() - dbCount ) +
                        ":" + count );
            }

            // notifies the change
            notifyHugsReceived( newHugs.toArray( new Hug[ count ] ) );

        }catch( SQLException e ){
            Log.e( getPackageName(), "Error: sql exception while inserting new hugs " + e );
        }

        mHugBuffer.clear(); // cleanup

    }


}//end class
