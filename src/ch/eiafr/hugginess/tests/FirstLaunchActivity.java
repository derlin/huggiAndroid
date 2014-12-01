package ch.eiafr.hugginess.tests;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.bluetooth.DeviceList;
import ch.eiafr.hugginess.bluetooth.HuggiBluetoothService;

import static ch.eiafr.hugginess.bluetooth.BluetoothState.*;

/**
 * @author: Lucy Linder
 * @date: 01.12.2014
 */
public class FirstLaunchActivity extends Activity{

    //-------------------------------------------------------------

    private HuggiBluetoothService mSPP;
    private TextView mTextView;
    private ProgressBar mProgressBar;

    private String mShirtAddress;

    private ServiceConnection mServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected( ComponentName name, IBinder binder ){
            Log.d( "lala", "onServiceConnected" );
            mSPP = ( ( HuggiBluetoothService.BTBinder ) binder ).getService();
            step1();
        }


        @Override
        public void onServiceDisconnected( ComponentName name ){
            Log.d( "lala", "onServiceDisconnected" );
            mSPP = null;
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
        private int mFailedCount = 0;


        @Override
        public void onReceive( Context context, Intent intent ){

            switch( intent.getStringExtra( EXTRA_EVT_TYPE ) ){
                case EVT_CONNECTED:
                    step3a();
                    break;

                case EVT_DISCONNECTED:
                    Toast.makeText( FirstLaunchActivity.this, "Disconnected", Toast.LENGTH_SHORT ).show();
                    finish(); // TODO
                    break;

                case EVT_CONNECTION_FAILED:
                    mFailedCount++;
                    if( mFailedCount < 3 ){
                        mSPP.connect( mShirtAddress );
                        Toast.makeText( FirstLaunchActivity.this, "Failed to connect, retrying", Toast.LENGTH_SHORT )
                                .show();
                    }else{
                        // TODO: dialog
                        step1();
                    }
                    break;

                case EVT_DATA_RECEIVED:
                    String data = intent.getStringExtra( EVT_EXTRA_DATA );
                    if( data.startsWith( DATA_PREFIX + CMD_DUMP_ALL ) ){
                        String[] split = data.split( DATA_SEP );
                        if( split.length == 3 ){ // TODO
                            // split[0] is @A
                            step3b( split[ 1 ], split[ 2 ] );
                        }
                    }
                    break;
            }
        }
    };


    // ----------------------------------------------------


    @Override
    public void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        this.bindService( new Intent( this, HuggiBluetoothService.class ), mServiceConnection, 0 );
        LocalBroadcastManager.getInstance( this ).registerReceiver( mBroadcastReceiver, new IntentFilter(
                BTSERVICE_INTENT_FILTER ) );
        setContentView( R.layout.first_launch_working );

        mTextView = ( TextView ) findViewById( R.id.text );
        mProgressBar = ( ProgressBar ) findViewById( R.id.progressBar );

    }

    // ----------------------------------------------------


    @Override
    public void onDestroy(){
        LocalBroadcastManager.getInstance( this ).unregisterReceiver( mBroadcastReceiver );
        this.unbindService( mServiceConnection );
        super.onDestroy();
    }

    /* *****************************************************************
     * onActivityResult
     * ****************************************************************/


    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ){

        if( requestCode == REQUEST_CONNECT_DEVICE ){

            if( resultCode == Activity.RESULT_OK ){
                mShirtAddress = data.getExtras().getString( EXTRA_DEVICE_ADDRESS );
                mSPP.connect( mShirtAddress );

            }else{
                finish();
            }
        }else if( requestCode == REQUEST_ENABLE_BT ){

            if( resultCode == Activity.RESULT_OK ){
                step2();
            }else{
                finish();    // TODO
            }
        }
    }


    /* *****************************************************************
     * bluetooth enabled
     * ****************************************************************/


    private void step1(){
        if( !mSPP.isBluetoothEnabled() ){
            Intent btIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
            startActivityForResult( btIntent, REQUEST_ENABLE_BT );
        }else{
            step2();
        }
    }


    /* *****************************************************************
     * Choose device
     * ****************************************************************/


    private void step2(){

        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setCancelable( false );
        builder.setTitle( "Select your Huggi-Shirt" );
        builder.setMessage( "You will now choose your Huggi-Shirt." +  //
                "\nPlease, ensure that your Huggi-Shirt is powered and paired before proceeding." );
        builder.setCancelable( false );

        builder.setPositiveButton( "Select shirt", new DialogInterface.OnClickListener(){
            @Override
            public void onClick( DialogInterface dialog, int which ){
                Intent intent = new Intent( FirstLaunchActivity.this, DeviceList.class );
                startActivityForResult( intent, REQUEST_CONNECT_DEVICE );
            }
        } );

        builder.setNegativeButton( "Bluetooth settings", new DialogInterface.OnClickListener(){
            @Override
            public void onClick( DialogInterface dialog, int which ){
                Intent i = new Intent( Settings.ACTION_BLUETOOTH_SETTINGS );
                startActivity( i );
            }
        } );

        builder.create().show();

    }


    /* *****************************************************************
     * confirm ID
     * ****************************************************************/


    private void step3a(){
        mSPP.executeCommand( CMD_DUMP_ALL );
    }


    private void step3b( String id, String data ){
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setCancelable( true );
        builder.setTitle( "Current configuration" );
        builder.setMessage( String.format( "\nID: %s\nDATA: %s\nPhone: %s\nIs this correct ?", id, data,
                getMyPhoneNumber() ) );

        builder.setPositiveButton( "Yep", new DialogInterface.OnClickListener(){
            @Override
            public void onClick( DialogInterface dialogInterface, int i ){
                finalStep();
            }
        } );
        builder.create().show();
    }

    /* *****************************************************************
     * change id
     * ****************************************************************/

     /* *****************************************************************
     * final
     * ****************************************************************/


    private void finalStep(){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences( this ).edit();
        editor.putBoolean( getString( R.string.pref_is_configured ), true );
        editor.putString( getString( R.string.pref_paired_tshirt ), mShirtAddress );
        //editor.commit();

        Intent intent = new Intent( this, TabTestActivity.class );
        intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
        startActivity( intent );
    }


    private String getMyPhoneNumber(){
        TelephonyManager mTelephonyMgr;
        mTelephonyMgr = ( TelephonyManager ) getSystemService( Context.TELEPHONY_SERVICE );
        return mTelephonyMgr.getLine1Number();
    }


    /* *****************************************************************
     * *****************************************************************
     * ****************************************************************/

 }