package ch.eiafr.hugginess.tests;

import android.app.ActionBar;
import android.app.Activity;
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
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.HuggiBTActivity;
import ch.eiafr.hugginess.bluetooth.DeviceList;
import ch.eiafr.hugginess.bluetooth.HuggiBluetoothService;
import ch.eiafr.hugginess.listtests.ListFragment;
import ch.eiafr.hugginess.widgets.AnimatedSyncImageView;
import ch.eiafr.hugginess.widgets.TabsAdapter;

import static ch.eiafr.hugginess.bluetooth.BluetoothState.*;


/**
 * @author: Lucy Linder
 * @date: 22.11.2014
 */
public class TabTestActivity extends FragmentActivity implements HuggiBTActivity{

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private TextView mTextStatus;
    private Menu menu;
    private AnimatedSyncImageView mAnim;
    private ActionBar mActionBar;

    HuggiBluetoothService mSPP;

    //-------------------------------------------------------------

    protected ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected( ComponentName name, IBinder binder ){
            Log.d( "lala", "onServiceConnected" );
            if( mSPP == null ){
                mSPP = ( ( HuggiBluetoothService.BTBinder ) binder ).getService();
                onBTServiceBonded();
            }
        }


        @Override
        public void onServiceDisconnected( ComponentName name ){
            Log.d( "lala", "onServiceDisconnected" );
            mSPP = null;
        }
    };


    // ----------------------------------------------------


    protected void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_tab );
        mTextStatus = ( TextView ) findViewById( R.id.textStatus );

        mViewPager = ( ViewPager ) findViewById( R.id.pager );

        mActionBar = getActionBar();
        mActionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_TABS );
        mActionBar.setDisplayOptions( 0, ActionBar.DISPLAY_SHOW_TITLE );

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
                        mAnim.start();
                        mSPP.connect( "00:06:66:68:18:1A" );
                        mTextStatus.setEnabled( false );
                        break;
                }
            }
        } );

    }


    @Override
    protected void onSaveInstanceState( Bundle outState ){
        PreferenceManager.getDefaultSharedPreferences( this ).edit().putInt( "tab", mActionBar
                .getSelectedNavigationIndex() ).commit();
        super.onSaveInstanceState( outState );
    }

    // ----------------------------------------------------


    @Override
    public void onStart(){
        // mContext is defined upper in code, I think it is not necessary to explain what is it
        Intent i = new Intent( this, HuggiBluetoothService.class );
        this.bindService( i, mServiceConnection, 0); //Context.BIND_AUTO_CREATE );

        super.onStart();

        //        if( !mSPP.isBluetoothEnabled() ){
        //            Intent intent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
        //            startActivityForResult( intent, BluetoothState.REQUEST_ENABLE_BT );
        //        }else{
        //            // TODO
        //        }
    }

    // ----------------------------------------------------


    @Override
    protected void onStop(){
        this.unbindService( mServiceConnection );
        super.onStop();
    }

    // ----------------------------------------------------


    @Override
    public void onDestroy(){
        super.onDestroy();
    }


    /* *****************************************************************
     * menu
     * ****************************************************************/


    @Override
    public boolean onCreateOptionsMenu( Menu menu ){
        this.menu = menu;
        getMenuInflater().inflate( R.menu.menu, menu );
        mAnim = ( AnimatedSyncImageView ) menu.findItem( R.id.menu_spiner_anim ).getActionView();
        return true;
    }

    // ----------------------------------------------------


    public boolean onOptionsItemSelected( MenuItem item ){
        int id = item.getItemId();
        if( id == R.id.menu_connect ){
            //            if(!mSPP.isBluetoothEnabled()){
            //                mSPP.enable();
            //
            //            }else{
            mSPP.setDeviceTargetType( DEVICE_OTHER );

            // DEBUG mSPP.connect( "00:06:66:68:18:1A" );

            Intent intent = new Intent( this, DeviceList.class );
            startActivityForResult( intent, REQUEST_CONNECT_DEVICE );
//                        }

        }else if( id == R.id.menu_disconnect ){
            if( mSPP.isConnected() ) mSPP.disconnect();

        }else if( id == R.id.menu_bt_settings ){
            Intent i = new Intent( Settings.ACTION_BLUETOOTH_SETTINGS );
            startActivity( i );
        }//end if

        return super.onOptionsItemSelected( item );
    }

    // ----------------------------------------------------


    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ){

        if( requestCode == REQUEST_CONNECT_DEVICE ){

            if( resultCode == Activity.RESULT_OK ){
                String address = data.getExtras().getString( EXTRA_DEVICE_ADDRESS );
                mSPP.connect( address );
            }
        }else if( requestCode == REQUEST_ENABLE_BT ){
            if( resultCode == Activity.RESULT_OK ){
                mSPP.setDeviceTargetType( DEVICE_ANDROID );
                Toast.makeText( this, "Bluetooth enabled", Toast.LENGTH_SHORT ).show();
            }else{
                Toast.makeText( getApplicationContext(), "Bluetooth was not enabled.", Toast.LENGTH_SHORT )
                        .show();
                finish();    // TODO
            }
        }
    }

    /* *****************************************************************
     * mSPP
     * ****************************************************************/

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive( Context context, Intent intent ){

            mTextStatus.setEnabled( true );
            mAnim.stop();

            switch( intent.getStringExtra( EXTRA_EVT_TYPE ) ){

                case EVT_BT_TURNED_ON:
                    menu.findItem( R.id.menu_disconnect ).setVisible( false );
                    menu.findItem( R.id.menu_connect ).setVisible( true );

                    mTextStatus.setText( "Status : no connected" );
                    mTextStatus.setTextColor( Color.RED );
                    break;

                case EVT_BT_TURNED_OFF:
                    menu.findItem( R.id.menu_disconnect ).setVisible( false );
                    menu.findItem( R.id.menu_connect ).setVisible( false );

                    mTextStatus.setText( "Status : offline" );
                    mTextStatus.setTextColor( Color.RED );
                    break;

                case EVT_CONNECTED:
                    Toast.makeText( TabTestActivity.this, "Status : Connected", Toast.LENGTH_SHORT ).show();
                    menu.findItem( R.id.menu_connect ).setVisible( false );
                    menu.findItem( R.id.menu_disconnect ).setVisible( true );

                    mTextStatus.setTextColor( Color.WHITE );
                    mTextStatus.setText( "Status : Connected to " + intent.getStringExtra( EVT_EXTRA_DNAME
                    ) );

                    break;


                case EVT_DISCONNECTED:
                    Toast.makeText( TabTestActivity.this, "Status : Disconnected", Toast.LENGTH_SHORT )
                            .show();
                    menu.findItem( R.id.menu_disconnect ).setVisible( false );
                    menu.findItem( R.id.menu_connect ).setVisible( true );

                    mTextStatus.setText( "Status : Not connected" );
                    mTextStatus.setTextColor( Color.RED );
                    break;

                case EVT_CONNECTION_FAILED:
                    Toast.makeText( TabTestActivity.this, "Status : Connection failed", Toast.LENGTH_SHORT
                    ).show();

                    mTextStatus.setTextColor( Color.RED );
                    mTextStatus.setText( "Status : Connection failed" );

                    break;

                case EVT_HUGS_RECEIVED:
                    int cnt = intent.getIntExtra( EVT_EXTRA_HUGS_CNT, 0 );
                    Toast.makeText( TabTestActivity.this, "Received " + cnt + " new hugs", Toast.LENGTH_SHORT ).show();

                    break;

                case EVT_ACK_RECEIVED:
                    char cmd = intent.getCharExtra( EVT_EXTRA_ACK_CMD, '-' );
                    boolean ok = intent.getBooleanExtra( EVT_EXTRA_ACK_STATUS, false );
                    Toast.makeText( TabTestActivity.this, "Cmd " + cmd + " : " + (ok ? "ack" : "nak"), Toast.LENGTH_SHORT ).show();
                    break;
            }


        }
    };


    private void onBTServiceBonded(){

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
            mTabsAdapter = new TabsAdapter( TabTestActivity.this, mViewPager );
            mTabsAdapter.addTab( mActionBar.newTab().setText( "Simple" ), DummyFragment.class, null );
            mTabsAdapter.addTab( mActionBar.newTab().setText( "List" ), ListFragment.class, null );
            mTabsAdapter.addTab( mActionBar.newTab().setText( "Terminal" ), TerminalFragment.class, null );

            int tabIndex = PreferenceManager.getDefaultSharedPreferences( this ).getInt( "tab", 0 );

            if( tabIndex >= 0 && tabIndex < mTabsAdapter.getCount() ){
                mActionBar.setSelectedNavigationItem( tabIndex );
            }
        }

    }//end btSetup



    @Override
    public HuggiBluetoothService getHuggiService(){
        return mSPP;
    }
} // end class