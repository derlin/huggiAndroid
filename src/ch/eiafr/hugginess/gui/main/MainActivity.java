package ch.eiafr.hugginess.gui.main;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import ch.eiafr.hugginess.HuggiBTActivity;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.gui.bt.DeviceListActivity;
import ch.eiafr.hugginess.services.bluetooth.HuggiBluetoothService;
import ch.eiafr.hugginess.gui.firstlaunch.FirstLaunchActivity;
import ch.eiafr.hugginess.gui.main.frag.HomeTabFragment;
import ch.eiafr.hugginess.gui.main.frag.HugsListFragment;
import ch.eiafr.hugginess.gui.main.frag.TerminalFragment;
import ch.eiafr.hugginess.gui.prefs.PrefsActivity;
import ch.eiafr.hugginess.tools.AnimatedSyncImageView;
import ch.eiafr.hugginess.tools.adapters.TabsAdapter;

import static ch.eiafr.hugginess.services.bluetooth.BluetoothConstants.*;


/**
 * @author: Lucy Linder
 * @date: 22.11.2014
 */
public class MainActivity extends FragmentActivity implements HuggiBTActivity{

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private TextView mTextStatus;
    private Menu menu;
    private AnimatedSyncImageView mAnim;
    private ActionBar mActionBar;

    private boolean isCreated, isBounded, isOk;
    HuggiBluetoothService mSPP;

    //-------------------------------------------------------------

    protected ServiceConnection mServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected( ComponentName name, IBinder binder ){
            Log.d( "lala", "onServiceConnected" );
            if( mSPP == null ){
                mSPP = ( ( HuggiBluetoothService.BTBinder ) binder ).getService();
                mSPP.executeCommand( CMD_SEND_HUGS );// TODO
                isBounded = true;
                onBTServiceBounded();
            }
        }


        @Override
        public void onServiceDisconnected( ComponentName name ){
            Log.d( "lala", "onServiceDisconnected" );
            //mSPP = null;     // TODO no, avoid errors when configuration change
            //isBounded = false;
        }
    };


    // ----------------------------------------------------


    protected void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        Intent i = new Intent( this, HuggiBluetoothService.class );
        this.bindService( i, mServiceConnection, 0 ); //Context.BIND_AUTO_CREATE );

        boolean isConfigured = PreferenceManager.getDefaultSharedPreferences( this ).getBoolean( getString( R.string
                .pref_is_configured ), false );

        if( !isConfigured ){

            Intent intent = new Intent( this, FirstLaunchActivity.class );
            intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
            startActivity( intent );
            finish();
            return;
        }


        setContentView( R.layout.activity_main );

        //checkFirstLaunch();



        mTextStatus = ( TextView ) findViewById( R.id.textStatus );
        mViewPager = ( ViewPager ) findViewById( R.id.pager );

        mActionBar = getActionBar();
        mActionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_TABS );
        //mActionBar.setDisplayOptions( 0, ActionBar.DISPLAY_SHOW_TITLE ); TODO

        mTextStatus.setOnClickListener( new View.OnClickListener(){
            @Override
            public void onClick( View view ){
                if( mSPP == null ) return;

                switch( mSPP.getState() ){
                    case STATE_TURNED_OFF:
                        mSPP.enable();
                        break;

                    case STATE_CONNECTED:
                        mSPP.disconnect();
                        break;

                    case STATE_NONE:
                        connect();
                        break;
                }
            }
        } );

//        checkFirstLaunch();
        isCreated = true;
        onBTServiceBounded();
    }


    @Override
    protected void onSaveInstanceState( Bundle outState ){
        PreferenceManager.getDefaultSharedPreferences( this ).edit().putInt( "tab", mActionBar
                .getSelectedNavigationIndex() ).commit();
        super.onSaveInstanceState( outState );
    }


    // ----------------------------------------------------


    @Override
    public void onDestroy(){
        //        if(mSPP != null) mSPP.disconnect(); // TODO errors when switch to landscape
        this.unbindService( mServiceConnection );
        super.onDestroy();
    }


    /* *****************************************************************
     * menu
     * ****************************************************************/


    @Override
    public boolean onCreateOptionsMenu( Menu menu ){
        this.menu = menu;
        getMenuInflater().inflate( R.menu.activity_main_menu, menu );
        mAnim = ( AnimatedSyncImageView ) menu.findItem( R.id.menu_spiner_anim ).getActionView();
        if( mSPP != null && mSPP.getState() == STATE_CONNECTING ) mAnim.start();
        return true;
    }

    // ----------------------------------------------------


    public boolean onOptionsItemSelected( MenuItem item ){
        int id = item.getItemId();
        switch( id ){
            case R.id.menu_connect:
                mSPP.setDeviceTargetType( DEVICE_OTHER );
                // TODO DEBUG mSPP.connect( "00:06:66:68:18:1A" );
                Intent intent = new Intent( this, DeviceListActivity.class );
                startActivityForResult( intent, REQUEST_CONNECT_DEVICE );
                break;


            case R.id.menu_disconnect:
                if( mSPP.isConnected() ) mSPP.disconnect();
                break;

            case R.id.menu_bt_settings:
                Intent i = new Intent( Settings.ACTION_BLUETOOTH_SETTINGS );
                startActivity( i );
                break;

            case R.id.menu_prefs:
                startActivity( new Intent( this, PrefsActivity.class ) );
                break;
        }

        return super.onOptionsItemSelected( item );
    }

    // ----------------------------------------------------


    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ){

        if( requestCode == REQUEST_CONNECT_DEVICE ){

            if( resultCode == Activity.RESULT_OK ){
                String address = data.getExtras().getString( EXTRA_DEVICE_ADDRESS );
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences( this ).edit();
                editor.putBoolean( getString( R.string.pref_is_configured ), true );
                editor.putString( getString( R.string.pref_paired_tshirt ), address );
                editor.commit();
                mSPP.connect( address );
            }else{
                finish();
            }
        }else if( requestCode == REQUEST_ENABLE_BT ){
            if( resultCode == Activity.RESULT_OK ){
                mSPP.setDeviceTargetType( DEVICE_ANDROID );
                Toast.makeText( this, "Bluetooth enabled", Toast.LENGTH_SHORT ).show();
            }else{
                Toast.makeText( getApplicationContext(), "Bluetooth was not enabled.", Toast.LENGTH_SHORT ).show();
                finish();    // TODO
            }
        }
    }

    /* *****************************************************************
     * mSPP
     * ****************************************************************/

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive( Context context, Intent intent ){

            switch( intent.getStringExtra( EXTRA_EVT_TYPE ) ){

                case EVT_BT_TURNED_ON:
                    menu.findItem( R.id.menu_disconnect ).setVisible( false );
                    menu.findItem( R.id.menu_connect ).setVisible( true );
                    updateStatus();

                case EVT_BT_TURNED_OFF:
                    menu.findItem( R.id.menu_disconnect ).setVisible( false );
                    menu.findItem( R.id.menu_connect ).setVisible( false );
                    updateStatus();
                    break;

                case EVT_CONNECTED:
                    Toast.makeText( MainActivity.this, "Connected", Toast.LENGTH_SHORT ).show();
                    menu.findItem( R.id.menu_connect ).setVisible( false );
                    menu.findItem( R.id.menu_disconnect ).setVisible( true );
                    updateStatus();
                    break;


                case EVT_DISCONNECTED:
                    Toast.makeText( MainActivity.this, "Disconnected", Toast.LENGTH_SHORT ).show();
                    menu.findItem( R.id.menu_disconnect ).setVisible( false );
                    menu.findItem( R.id.menu_connect ).setVisible( true );
                    updateStatus();
                    break;

                case EVT_CONNECTION_FAILED:
                    Toast.makeText( MainActivity.this, "Connection failed", Toast.LENGTH_SHORT ).show();
                    updateStatus();
                    break;

                case EVT_HUGS_RECEIVED:
                    int cnt = intent.getIntExtra( EVT_EXTRA_HUGS_CNT, 0 );
                    String msg = "Received " + cnt + " new hug";
                    if(cnt > 1) msg += "s";
                    Toast.makeText( MainActivity.this, msg, Toast.LENGTH_SHORT ).show();

                    break;

                case EVT_ACK_RECEIVED:
                    char cmd = intent.getCharExtra( EVT_EXTRA_ACK_CMD, '-' );
                    boolean ok = intent.getBooleanExtra( EVT_EXTRA_ACK_STATUS, false );
                    Toast.makeText( MainActivity.this, "Cmd " + cmd + " : " + ( ok ? "ack" : "nak" ), Toast
                            .LENGTH_SHORT ).show();
                    break;
            }


        }
    };


    private void onBTServiceBounded(){
        if( !isCreated || !isBounded || isOk ) return;
        // check that the bluetooth is on
        if( !mSPP.isBluetoothEnabled() ){
            Toast.makeText( this, "Bluetooth is not available", Toast.LENGTH_SHORT ).show();
            //mSPP.enable();
            //            if( !mSPP.isBluetoothEnabled() ){
            //                Intent intent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
            //                startActivityForResult( intent, BluetoothState.REQUEST_ENABLE_BT );
            //            }     // TODO
        }

        // register listeners
        LocalBroadcastManager.getInstance( this ).registerReceiver( mBroadcastReceiver, new IntentFilter(
                BTSERVICE_INTENT_FILTER ) );


        // create the fragments
        if( mTabsAdapter == null ){
            mTabsAdapter = new TabsAdapter( MainActivity.this, mViewPager );
            mTabsAdapter.addTab( mActionBar.newTab().setText( "Your stats" ), HomeTabFragment.class, null );
            //DummyFragment.class, null );
            mTabsAdapter.addTab( mActionBar.newTab().setText( "Hugs" ), HugsListFragment.class, null );
            mTabsAdapter.addTab( mActionBar.newTab().setText( "Terminal" ), TerminalFragment.class, null );

            int tabIndex = PreferenceManager.getDefaultSharedPreferences( this ).getInt( "tab", 0 );

            if( tabIndex >= 0 && tabIndex < mTabsAdapter.getCount() ){
                mActionBar.setSelectedNavigationItem( tabIndex );
            }
        }

        updateStatus();
        connect();

        isOk = true;
    }//end btSetup


    @Override
    public HuggiBluetoothService getHuggiService(){
        return mSPP;
    }


    /* *****************************************************************
     * first use
     * ****************************************************************/


    private void checkFirstLaunch(){
        boolean isConfigured = PreferenceManager.getDefaultSharedPreferences( this ).getBoolean( getString( R.string
                .pref_is_configured ), false );

        if( !isConfigured ){

            AlertDialog.Builder builder = new AlertDialog.Builder( this );
            builder.setTitle( "Huggi-Shirt configuration" );
            builder.setMessage( "It seems like there is no shirt configured yet. \n" + //
                    "Please, turn on bluetooth and choose a device." );
            builder.setCancelable( false );
            builder.setPositiveButton( "Choose device", new DialogInterface.OnClickListener(){
                @Override
                public void onClick( DialogInterface dialog, int which ){
                    if( !mSPP.isBluetoothEnabled() ) mSPP.enable();
                    Intent intent = new Intent( MainActivity.this, DeviceListActivity.class );
                    startActivityForResult( intent, REQUEST_CONNECT_DEVICE );
                }
            } );

            builder.setNegativeButton( "Cancel", new DialogInterface.OnClickListener(){
                @Override
                public void onClick( DialogInterface dialog, int which ){
                    finish();
                }
            } );

            builder.create().show();

        }
    }


    private void connect(){
        // try to autoconnect
        String addr = PreferenceManager.getDefaultSharedPreferences( this ).getString( getString( R.string
                .pref_paired_tshirt ), null );

        if( addr != null &&  //
                !( mSPP.isConnected() && addr.equals( mSPP.getDeviceAddress() ) ) ){
            mTextStatus.setEnabled( false );
            if( mAnim != null ) mAnim.start();
            mSPP.connect( addr );
        }

    }



    private void updateStatus(){
        // TODO
        mTextStatus.setEnabled( true );
        if( mAnim != null ) mAnim.stop();

        if( mSPP == null ) return;
        switch( mSPP.getState() ){

            case STATE_NONE:
                mTextStatus.setText( "Status : no connected" );
                mTextStatus.setTextColor( Color.RED );
                break;

            case STATE_TURNED_OFF:
                mTextStatus.setText( "Status : no connected" );
                mTextStatus.setTextColor( Color.RED );
                break;

            case STATE_CONNECTED:
                mTextStatus.setTextColor( Color.GREEN );
                mTextStatus.setText( "Status : Connected to " + mSPP.getDeviceName() );
                break;
        }
    }

} // end class