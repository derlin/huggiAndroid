package ch.eiafr.hugginess.bluetooth;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import ch.eiafr.hugginess.sql.Hug;
import ch.eiafr.hugginess.sql.HugsDataSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static ch.eiafr.hugginess.bluetooth.BluetoothState.*;

/**
 * @author: Lucy Linder
 * @date: 28.11.2014
 */
public class HuggiBluetoothService extends BluetoothService{

    private static final String DATA_PREFIX = "@";
    private static final String DATA_SEP = "!";

    private static final String ACK_PREFIX = "#";
    private static final String ACK_OK = ACK_PREFIX + "#";
    private static final String ACK_NOK = ACK_PREFIX + "?";

    private static final char CMD_ECHO = 'E';
    private static final char CMD_SET_ID = 'I';
    private static final char CMD_SET_DATA = 'D';
    private static final char CMD_SEND_HUGS = 'H';
    private static final char CMD_CALIBRATE = 'C';
    private static final char CMD_SLEEP = 'S';

    private List<Hug> hugBuffer = new ArrayList<>();
    private Timer timer = null;

    private static final long BT_TIMEOUT = 4000;


    @Override
    public IBinder onBind( Intent arg0 ){
        return super.onBind( arg0 );
    }


    @Override
    public int onStartCommand( Intent intent, int flags, int startId ){
        return super.onStartCommand( intent, flags, startId );
    }


    @Override
    protected void notifyDataReceived( String data ){

        if( data.startsWith( DATA_PREFIX + CMD_SEND_HUGS ) ){
            Hug hug = Hug.parseHug( data );

            if( hug != null ){
                send( ACK_PREFIX.getBytes() );
                hugBuffer.add( hug );

                if( timer == null ){
                    timer = new Timer();
                    timer.schedule( new TimerTask(){
                        @Override
                        public void run(){
                            insertHugs();
                        }
                    }, BT_TIMEOUT );
                }
            }

        }else if( data.matches( ACK_PREFIX + "[A-Z][#|?]" ) ){
            notifyAckReceived( data.charAt( 1 ),  data.charAt( 2 ) == '#' );
        }

        super.notifyDataReceived( data );

    }

    // ----------------------------------------------------

    private void insertHugs(){

        if( timer != null ){
            timer.cancel();
            timer = null;
        }

        HugsDataSource dbs = new HugsDataSource( getApplicationContext() );
        try{
            dbs.open();

            int dbCount = dbs.getHugsCount();
            int count = 0;

            for( Hug hug : hugBuffer ){
                Log.d( TAG, String.format( "Hug with %s, data = %s, dur = %d\n",//
                        hug.getHuggerID(), hug.getData(), hug.getDuration() ) );

                    if(dbs.addHug( hug )){
                        count++;

                    }else{
                        // error while inserting hug => key violation
                        Log.d( TAG, "Hug not unique !" );
                    }
            }//end for

            if(dbs.getHugsCount() - dbCount != count){
                Log.e( TAG, "OUPS: counts do not match: " + ( dbs.getHugsCount() - dbCount ) + ":" + count );
            }

            notifyHugsReceived( count );
        }catch( SQLException e ){
            e.printStackTrace();
        }finally{
            dbs.close();
        }

        hugBuffer.clear();

    }

    // ----------------------------------------------------

    private void notifyHugsReceived( int nbr ){
        Intent i = getIntent( EVT_HUGS_RECEIVED );
        i.putExtra( EVT_EXTRA_HUGS_CNT, nbr );
        mBroadcastManager.sendBroadcast( i );
    }


    private void notifyAckReceived( char cmd, boolean ok ){
        Intent i = getIntent( EVT_ACK_RECEIVED );
        i.putExtra( EVT_EXTRA_ACK_CMD, cmd );
        i.putExtra( EVT_EXTRA_ACK_STATUS, ok );
        mBroadcastManager.sendBroadcast( i );
    }


}//end class
