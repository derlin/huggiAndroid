package ch.eiafr.hugginess.tests;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.myspp.BluetoothSPP;
import ch.eiafr.hugginess.myspp.BluetoothState;
import ch.eiafr.hugginess.myspp.DeviceList;

/**
 * @author: Lucy Linder
 * @date: 22.11.2014
 */
public class TabTestActivity extends FragmentActivity{
    ActionBar.Tab tab1, tab2, tab3;

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;

    Menu menu;
    BluetoothSPP bt;


    protected void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_tab );


        mViewPager = new ViewPager( this );
        mViewPager.setId( R.id.pager );
        setContentView( mViewPager );

        final ActionBar bar = getActionBar();
        bar.setNavigationMode( ActionBar.NAVIGATION_MODE_TABS );
        bar.setDisplayOptions( 0, ActionBar.DISPLAY_SHOW_TITLE );

        mTabsAdapter = new TabsAdapter( this, mViewPager );
        mTabsAdapter.addTab( bar.newTab().setText( "Simple" ), DummyFragment.class, null );
        mTabsAdapter.addTab( bar.newTab().setText( "Terminal" ), TerminalFragment.class, null );


        if( savedInstanceState != null ){
            bar.setSelectedNavigationItem( savedInstanceState.getInt( "tab", 0 ) );
        }

        btSetup();
    }

    // ----------------------------------------------------


    @Override
    protected void onSaveInstanceState( Bundle outState ){
        super.onSaveInstanceState( outState );
        outState.putInt( "tab", getActionBar().getSelectedNavigationIndex() );
    }

    // ----------------------------------------------------


    @Override
    public void onStart(){
        super.onStart();
        if( !bt.isBluetoothEnabled() ){
            Intent intent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
            startActivityForResult( intent, BluetoothState.REQUEST_ENABLE_BT );
        }else{
            if( !bt.isServiceAvailable() ){
                bt.setupService();
                bt.startService( BluetoothState.DEVICE_ANDROID );
                //setup();
            }
        }
    }

    // ----------------------------------------------------


    @Override
    public void onDestroy(){
        super.onDestroy();
        bt.stopService();
    }


    /* *****************************************************************
     * menu
     * ****************************************************************/


    @Override
    public boolean onCreateOptionsMenu( Menu menu ){
        this.menu = menu;
        getMenuInflater().inflate( R.menu.connection, menu );
        return true;
    }


    public boolean onOptionsItemSelected( MenuItem item ){
        int id = item.getItemId();
        if( id == R.id.menu_device_connect ){
            bt.setDeviceTarget( BluetoothState.DEVICE_OTHER );
			/*
			if(bt.getServiceState() == BluetoothState.STATE_CONNECTED)
    			bt.disconnect();*/
            Intent intent = new Intent( this, DeviceList.class );
            startActivityForResult( intent, BluetoothState.REQUEST_CONNECT_DEVICE );
        }else if( id == R.id.menu_disconnect ){
            if( bt.getServiceState() == BluetoothState.STATE_CONNECTED ) bt.disconnect();
        }
        return super.onOptionsItemSelected( item );
    }


    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ){
        if( requestCode == BluetoothState.REQUEST_CONNECT_DEVICE ){
            if( resultCode == Activity.RESULT_OK ) bt.connect( data );
        }else if( requestCode == BluetoothState.REQUEST_ENABLE_BT ){
            if( resultCode == Activity.RESULT_OK ){
                bt.setupService();
                bt.startService( BluetoothState.DEVICE_ANDROID );
                Toast.makeText( this, "Device connected", Toast.LENGTH_SHORT ).show();
            }else{
                Toast.makeText( getApplicationContext(), "Bluetooth was not enabled.", Toast.LENGTH_SHORT ).show();
                //finish();     TODO
            }
        }
    }

    /* *****************************************************************
     * bt
     * ****************************************************************/


    private void btSetup(){

        bt = new BluetoothSPP( this );
        if( !bt.isBluetoothAvailable() ){
            Toast.makeText( this, "Bluetooth is not available", Toast.LENGTH_SHORT ).show();
        }

        bt.setBluetoothConnectionListener( new BluetoothSPP.BluetoothConnectionListener(){
            public void onDeviceDisconnected(){
                menu.clear();
                getMenuInflater().inflate( R.menu.connection, menu );
            }


            public void onDeviceConnectionFailed(){
                Toast.makeText( TabTestActivity.this, "Status : Connection failed", Toast.LENGTH_SHORT ).show();
            }


            public void onDeviceConnected( String name, String address ){
                menu.clear();
                getMenuInflater().inflate( R.menu.disconnection, menu );
            }
        } );
    }//end btSetup

} // end class