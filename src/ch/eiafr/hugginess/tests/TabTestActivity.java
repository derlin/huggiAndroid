package ch.eiafr.hugginess.tests;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.SPPActivity;
import ch.eiafr.hugginess.myspp.BluetoothSPP;
import ch.eiafr.hugginess.myspp.BluetoothListener;
import ch.eiafr.hugginess.myspp.BluetoothState;
import ch.eiafr.hugginess.myspp.DeviceList;

/**
 * @author: Lucy Linder
 * @date: 22.11.2014
 */
public class TabTestActivity extends FragmentActivity implements SPPActivity{

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private TextView mTextStatus;
    private Menu menu;
    private BluetoothSPP mSPP;


    protected void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_tab );
        mTextStatus = ( TextView ) findViewById(R.id.textStatus);

        mViewPager = ( ViewPager ) findViewById( R.id.pager );
//        mViewPager.setId( R.id.pager );
//        setContentView( mViewPager );

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
        if( !mSPP.isBluetoothEnabled() ){
            Intent intent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
            startActivityForResult( intent, BluetoothState.REQUEST_ENABLE_BT );
        }else{
            if( !mSPP.isServiceAvailable() ){
                mSPP.setupService();
                mSPP.startService( BluetoothState.DEVICE_ANDROID );
                //setup();
            }
        }
    }

    // ----------------------------------------------------


    @Override
    public void onDestroy(){
        super.onDestroy();
        mSPP.stopService();
    }


    /* *****************************************************************
     * menu
     * ****************************************************************/


    @Override
    public boolean onCreateOptionsMenu( Menu menu ){
        this.menu = menu;
        getMenuInflater().inflate( R.menu.menu, menu );
        return true;
    }


    public boolean onOptionsItemSelected( MenuItem item ){
        int id = item.getItemId();
        if( id == R.id.menu_connect ){
            mSPP.setDeviceTarget( BluetoothState.DEVICE_OTHER );
			/*
			if(mSPP.getServiceState() == BluetoothState.STATE_CONNECTED)
    			mSPP.disconnect();*/
            Intent intent = new Intent( this, DeviceList.class );
            startActivityForResult( intent, BluetoothState.REQUEST_CONNECT_DEVICE );

        }else if( id == R.id.menu_disconnect ){
            if( mSPP.getServiceState() == BluetoothState.STATE_CONNECTED ) mSPP.disconnect();

        }else if(id == R.id.menu_bt_settings){
            Intent i = new Intent( Settings.ACTION_BLUETOOTH_SETTINGS );
            startActivity( i );
        }//end if

        return super.onOptionsItemSelected( item );
    }


    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ){
        if( requestCode == BluetoothState.REQUEST_CONNECT_DEVICE ){
            if( resultCode == Activity.RESULT_OK ) mSPP.connect( data );
        }else if( requestCode == BluetoothState.REQUEST_ENABLE_BT ){
            if( resultCode == Activity.RESULT_OK ){
                mSPP.setupService();
                mSPP.startService( BluetoothState.DEVICE_ANDROID );
                Toast.makeText( this, "Device connected", Toast.LENGTH_SHORT ).show();
            }else{
                Toast.makeText( getApplicationContext(), "Bluetooth was not enabled.", Toast.LENGTH_SHORT ).show();
                //finish();     TODO
            }
        }
    }

    /* *****************************************************************
     * mSPP
     * ****************************************************************/


    private void btSetup(){

        mSPP = new BluetoothSPP( this );
        if( !mSPP.isBluetoothAvailable() ){
            Toast.makeText( this, "Bluetooth is not available", Toast.LENGTH_SHORT ).show();
        }

        mSPP.setBluetoothConnectionListener( new BluetoothListener.ConnectionListener(){
            public void onDeviceDisconnected(){
                Toast.makeText( TabTestActivity.this, "Status : Disconnected", Toast.LENGTH_SHORT ).show();
                menu.findItem( R.id.menu_disconnect ).setVisible( false );
                menu.findItem( R.id.menu_connect ).setVisible( true );

                mTextStatus.setText( "Status : Not connected" );
                mTextStatus.setTextColor( Color.RED );
            }


            public void onDeviceConnectionFailed(){
                Toast.makeText( TabTestActivity.this, "Status : Connection failed", Toast.LENGTH_SHORT ).show();

                mTextStatus.setTextColor( Color.RED );
                mTextStatus.setText( "Status : Connection failed" );
            }


            public void onDeviceConnected( String name, String address ){
                Toast.makeText( TabTestActivity.this, "Status : Connected", Toast.LENGTH_SHORT ).show();
                menu.findItem( R.id.menu_connect ).setVisible( false );
                menu.findItem( R.id.menu_disconnect ).setVisible( true );

                mTextStatus.setTextColor( Color.WHITE );
                mTextStatus.setText( "Status : Connected to " + name );
            }
        } );
    }//end btSetup


    @Override
    public BluetoothSPP getSPP(){
        return mSPP;
    }
} // end class