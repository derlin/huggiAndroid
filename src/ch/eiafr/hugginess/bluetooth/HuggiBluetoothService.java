package ch.eiafr.hugginess.bluetooth;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import ch.eiafr.hugginess.sql.Hug;
import ch.eiafr.hugginess.sql.HugsDataSource;

import java.sql.SQLException;
import java.util.*;

import static ch.eiafr.hugginess.bluetooth.BluetoothState.*;

/**
 * @author: Lucy Linder
 * @date: 28.11.2014
 */
public class HuggiBluetoothService extends BluetoothService {

    private static final int MAX_DEQUE_SIZE = 30;
    private ReceivedDataBuffer mReceivedDataBuffer = new ReceivedDataBuffer(MAX_DEQUE_SIZE);

    public List<Hug> hugBuffer = new ArrayList<>();
    private Timer timer = null;

    private IBinder myBinder = new BTBinder();

    private static final long BT_TIMEOUT = 2400;

    // ----------------------------------------------------

    public class BTBinder extends Binder {
        public HuggiBluetoothService getService(){
            return HuggiBluetoothService.this;
        }
    }//end class


    @Override
    public IBinder onBind( Intent arg0 ){
        return myBinder;
    }


    @Override
    public int onStartCommand( Intent intent, int flags, int startId ){
        return super.onStartCommand( intent, flags, startId );
    }


    @Override
    protected void notifyDataReceived( String data ){

        synchronized( this ){
            mReceivedDataBuffer.appendLine( data );
        }

        if( data.startsWith( DATA_PREFIX + CMD_SEND_HUGS ) ){
            Hug hug = Hug.parseHug( data );

            if( hug != null ){
                send( ACK_PREFIX.getBytes() );
                hugBuffer.add( hug );

                if( timer == null ){
                    timer = new Timer();
                    timer.schedule( new TimerTask() {
                        @Override
                        public void run(){
                            insertHugs();
                        }
                    }, BT_TIMEOUT );
                }
            }

        }else if( data.matches( ACK_PREFIX + "[A-Z][#|?]" ) ){
            notifyAckReceived( data.charAt( 1 ), data.charAt( 2 ) == '#' );
        }

        super.notifyDataReceived( data );

    }

    // ----------------------------------------------------

    public String getLastReceivedData(){
        synchronized( this ){
            return mReceivedDataBuffer.getAllLines();
        }
    }
    // ----------------------------------------------------


    public void executeCommand( char CMD, String data ){
        send( String.format( "%s%s%s%s", CMD_PREFIX, CMD, DATA_PREFIX, data ), true );
    }


    public void executeCommand( char CMD ){
        send( CMD_PREFIX + CMD, true );
    }

    /*
    public void getHugs(){
        send( CMD_PREFIX + CMD_SEND_HUGS, true );
    }


    public void doEcho( String s ){
        send( String.format( "%s%s%s%s", CMD_PREFIX, CMD_ECHO, DATA_PREFIX, s ) , true );
    }

    public void setId( String s ){
        send( String.format( "%s%s%s%s", CMD_PREFIX, CMD_SET_ID, DATA_PREFIX, s ) , true );
    }

    public void setName( String s ){
        send( String.format( "%s%s%s%s", CMD_PREFIX, CMD_SET_DATA, DATA_PREFIX, s ) , true );
    }

    public void doCalibrate( ){
        send( CMD_PREFIX + CMD_CALIBRATE, true );
    }
    */


    // ----------------------------------------------------


    private void insertHugs(){

        if( timer != null ){
            timer.cancel();
            timer = null;
        }

        HugsDataSource dbs = new HugsDataSource( getApplicationContext() );
        List<Hug> newHugs = new ArrayList<>();

        try{
            dbs.open();

            int dbCount = dbs.getHugsCount();
            int count = 0;

            for( Hug hug : hugBuffer ){
                Log.d( TAG, String.format( "Hug with %s, data = %s, dur = %d\n",//
                        hug.getHuggerID(), hug.getData(), hug.getDuration() ) );

                if( dbs.addHug( hug ) ){
                    count++;
                    newHugs.add( hug );
                }else{
                    // error while inserting hug => key violation
                    Log.d( TAG, "Hug not unique !" );
                }
            }//end for

            if( dbs.getHugsCount() - dbCount != count ){
                Log.e( TAG, "OUPS: counts do not match: " + ( dbs.getHugsCount() - dbCount ) + ":" + count );
            }

            notifyHugsReceived( newHugs.toArray( new Hug[ count ] ) );
        }catch( SQLException e ){
            e.printStackTrace();
        }finally{
            dbs.close();
        }

        hugBuffer.clear();

    }

    // ----------------------------------------------------


    private void notifyHugsReceived( Hug[] newHugs ){
        Intent i = getIntent( EVT_HUGS_RECEIVED );
        i.putExtra( EVT_EXTRA_HUGS_CNT, newHugs.length );
        i.putExtra( EVT_EXTRA_HUGS_LIST, newHugs );
        mBroadcastManager.sendBroadcast( i );
    }


    private void notifyAckReceived( char cmd, boolean ok ){
        Intent i = getIntent( EVT_ACK_RECEIVED );
        i.putExtra( EVT_EXTRA_ACK_CMD, cmd );
        i.putExtra( EVT_EXTRA_ACK_STATUS, ok );
        mBroadcastManager.sendBroadcast( i );
    }

    //-------------------------------------------------------------

    private static class ReceivedDataBuffer extends ArrayDeque<String> {
        private int limit = -1;


        public ReceivedDataBuffer( int limit ){
            super();
            this.limit = limit;
        }


        public void appendLine( String line ){
            if(!line.endsWith( "\n" )) line += "\n";
            if( limit > 0 ){
                while( size() > limit ){
                    removeFirst();
                }//end while
            }

            addLast( line );
        }


        public String getAllLines(){
            StringBuilder builder = new StringBuilder();
            for( Iterator<String> iter = iterator(); iter.hasNext(); ){
                builder.append( iter.next() );
            }

            return builder.toString();
        }


        public int getLimit(){
            return limit;
        }


        public void setLimit( int limit ){
            this.limit = limit;
        }
    }
}//end class
