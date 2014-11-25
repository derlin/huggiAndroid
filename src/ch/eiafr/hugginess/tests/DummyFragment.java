package ch.eiafr.hugginess.tests;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.SPPActivity;
import ch.eiafr.hugginess.myspp.BluetoothService;

import java.util.Timer;
import java.util.TimerTask;

import static ch.eiafr.hugginess.myspp.BluetoothState.*;

/**
 * @author: Lucy Linder
 * @date: 22.11.2014
 */
public class DummyFragment extends Fragment implements Button.OnClickListener{

    private static final long BT_TIMEOUT = 4000;
    private BluetoothService mSPP;


    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
        View view = inflater.inflate( R.layout.dummytab, container, false );
        TextView textview = ( TextView ) view.findViewById( R.id.tabtextview );
        textview.setText( "tab 1" );

        view.findViewById( R.id.echo_button ).setOnClickListener( this );
        view.findViewById( R.id.sleep_button ).setOnClickListener( this );
        view.findViewById( R.id.getHugs_button ).setOnClickListener( this );
        mSPP = ( ( SPPActivity ) getActivity() ).getSPP();

        return view;
    }


    @Override
    public void onClick( View v ){
        if( !mSPP.isConnected() ) return;

        switch( v.getId() ){
            case R.id.echo_button:
                mSPP.send( "$E@this is an echo test", true );
                break;
            case R.id.sleep_button:
                mSPP.send( "$S", true );
                break;

            case R.id.getHugs_button:
                getHugs();
                break;
        }

    }


    private void getHugs(){
        LocalBroadcastManager.getInstance( getActivity() ).registerReceiver( mBroadcastReceiver, new IntentFilter(
                BTSERVICE_INTENT_FILTER ) );


        mSPP.send( "$H", false );
        Timer t = new Timer();
        t.schedule( new TimerTask(){
            @Override
            public void run(){
                Log.d( "lala", "bt timeout" );
                LocalBroadcastManager.getInstance( getActivity() ).unregisterReceiver( mBroadcastReceiver );
            }
        }, BT_TIMEOUT );

    }


    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive( Context context, Intent intent ){
            switch( intent.getStringExtra( EXTRA_EVT_TYPE ) ){
                case EVT_DATA_RECEIVED:
                    String message = intent.getStringExtra( EVT_EXTRA_DATA );
                    if( message.startsWith( "@" ) ){
                        mSPP.send( "#", false );
                        Log.d( "lala", "Received hug " );
                    }
                    break;
            }
        }
    };

}//end class
