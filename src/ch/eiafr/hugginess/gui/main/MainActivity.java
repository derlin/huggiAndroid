package ch.eiafr.hugginess.gui.main;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.gui.firstlaunch.FirstLaunchActivity;
import ch.eiafr.hugginess.gui.main.frag.HomeTabFragment;
import ch.eiafr.hugginess.gui.main.frag.HugsListFragment;
import ch.eiafr.hugginess.gui.main.frag.TerminalFragment;
import ch.eiafr.hugginess.gui.prefs.PrefsActivity;
import ch.eiafr.hugginess.services.bluetooth.HuggiBluetoothService;
import ch.eiafr.hugginess.services.bluetooth.HuggiBroadcastReceiver;
import ch.eiafr.hugginess.tools.adapters.TabsAdapter;

import static ch.eiafr.hugginess.services.bluetooth.BluetoothConstants.*;


/**
 * @author: Lucy Linder
 * @date: 22.11.2014
 */
public class MainActivity extends FragmentActivity {

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private TextView mTextStatus;
    private Menu menu;
    private ActionBar mActionBar;

    HuggiBluetoothService mSPP;

    //-------------------------------------------------------------

    private HuggiBroadcastReceiver mBroadcastReceiver = new HuggiBroadcastReceiver(){
        @Override
        public void onBtStateChanged(){
            updateStatus();
        }


        @Override
        public void onBtHugsReceived( int cnt ){
            String msg = "Received " + cnt + " new hug";
            if(cnt > 1) msg += "s";
            Toast.makeText( MainActivity.this, msg, Toast.LENGTH_SHORT ).show();
        }


        @Override
        public void onBtAckReceived( char cmd, boolean ok ){
            Toast.makeText( MainActivity.this, "Cmd " + cmd + " : " + ( ok ? "ack" : "nak" ), Toast
                    .LENGTH_SHORT ).show();
        }


        @Override
        public void onBtConnectionFailed(){
            Toast.makeText( MainActivity.this, "Connection failed", Toast.LENGTH_SHORT ).show();
            updateStatus();
        }
    };

    // ----------------------------------------------------

    @Override
    protected void onNewIntent(Intent intent) {
        // overriding this method fixes the bugs related to
        // configuration change. The activity is no longer restarted !
        super.onNewIntent(intent);
        setIntent(intent);
    }

    protected void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );

        boolean isConfigured = PreferenceManager.getDefaultSharedPreferences( this ).getBoolean( getString( R.string
                .pref_is_configured ), false );

        if( !isConfigured ){

            Intent intent = new Intent( this, FirstLaunchActivity.class );
            intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
            startActivity( intent );
            finish();
            return;
        }

        // requestFeature() must be called before adding content
        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature( Window.FEATURE_PROGRESS);
        setProgressBarIndeterminate( true );
        setProgressBarIndeterminateVisibility( true );

        setContentView( R.layout.activity_main );

//        checkFirstLaunch();



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
        new InitAsyncTask().execute();
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
        super.onDestroy();
    }

    /* *****************************************************************
     * private utils
     * ****************************************************************/


    private void setupTabs(){
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
    }

    /* *****************************************************************
     * menu
     * ****************************************************************/


    @Override
    public boolean onCreateOptionsMenu( Menu menu ){
        this.menu = menu;
        getMenuInflater().inflate( R.menu.activity_main_menu, menu );
        return true;
    }

    // ----------------------------------------------------


    public boolean onOptionsItemSelected( MenuItem item ){
        int id = item.getItemId();
        switch( id ){

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

         if( requestCode == REQUEST_ENABLE_BT ){
            if( resultCode == Activity.RESULT_OK ){
                mSPP.setDeviceTargetType( DEVICE_ANDROID );
                Toast.makeText( this, "Bluetooth enabled", Toast.LENGTH_SHORT ).show();
            }else{
                Toast.makeText( getApplicationContext(), "Bluetooth was not enabled.", Toast.LENGTH_SHORT ).show();
                finish();    // TODO
            }
        }else{
            super.onActivityResult( requestCode, resultCode, data );
        }
    }


    /* *****************************************************************
     * first use
     * ****************************************************************/


    private void connect(){
        // try to autoconnect
        String addr = PreferenceManager.getDefaultSharedPreferences( this ).getString( getString( R.string
                .pref_paired_tshirt ), null );

        if( addr != null &&  //
                !( mSPP.isConnected() && addr.equals( mSPP.getDeviceAddress() ) ) ){
            mTextStatus.setEnabled( false );
            setProgressBarIndeterminateVisibility( true );
            mSPP.connect( addr );
        }

    }



    private void updateStatus(){
        // TODO
        mTextStatus.setEnabled( true );
        setProgressBarIndeterminateVisibility( false );

        int state = mSPP.getState();

        switch( state ){

            case STATE_NONE:
                mTextStatus.setText( "Status : not connected" );
                mTextStatus.setTextColor( Color.RED );
                break;

            case STATE_TURNED_OFF:
                mTextStatus.setText( "Status : unavailable" );
                mTextStatus.setTextColor( Color.RED );
                break;

            case STATE_CONNECTED:
                mTextStatus.setTextColor( Color.GREEN );
                mTextStatus.setText( "Status : Connected to " + mSPP.getDeviceName() );
                break;
        }
    }


    /* *****************************************************************
     * async
     * ****************************************************************/

    private class InitAsyncTask extends AsyncTask<Void, Void, Void>{

        Context context = MainActivity.this;

        @Override
        protected Void doInBackground( Void... voids ){

            while( HuggiBluetoothService.getInstance() == null || menu == null ){
                try{
                    Thread.sleep( 200 );
                }catch( InterruptedException e ){
                    e.printStackTrace();
                }
            }//end while

            mSPP = HuggiBluetoothService.getInstance();
            return null;
        }


        @Override
        protected void onPostExecute( Void aVoid ){
            super.onPostExecute( aVoid );

            if( !mSPP.isBluetoothEnabled() ){
                Toast.makeText( context, "Bluetooth is not available", Toast.LENGTH_SHORT ).show();
            }

            // register listeners
            mBroadcastReceiver.registerSelf( context );
            setupTabs();
            updateStatus();
            connect();
        }
    }

} // end class