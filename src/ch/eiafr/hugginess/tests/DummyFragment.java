package ch.eiafr.hugginess.tests;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.SPPActivity;
import ch.eiafr.hugginess.myspp.BluetoothListener;
import ch.eiafr.hugginess.myspp.BluetoothSPP;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author: Lucy Linder
 * @date: 22.11.2014
 */
public class DummyFragment extends Fragment implements Button.OnClickListener {

    private static final long BT_TIMEOUT = 4000;
    private BluetoothSPP mSPP;


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
                mSPP.send( "$E@this is an echo test".getBytes(), true );
                break;
            case R.id.sleep_button:
                mSPP.send( "$S".getBytes(), true );
                break;

            case R.id.getHugs_button:
                getHugs();
                break;
        }

    }


    private void getHugs(){
        int count = 0;
        final BluetoothListener.OnDataReceivedListener listener = new BluetoothListener
                .OnDataReceivedListener() {
            @Override
            public void onDataReceived( String message ){
                if( message.startsWith( "@" ) ){
                    mSPP.send( "#".getBytes(), false );
                    Log.d( "lala", "Received hug ");
                }
            }
        };

        mSPP.send( "$H".getBytes(), false );
        Timer t = new Timer();
        t.schedule( new TimerTask() {
            @Override
            public void run(){
                Log.d( "lala", "bt timeout" );
                mSPP.removeOnDataReceivedListener( listener );
            }
        }, BT_TIMEOUT );

        mSPP.setOnDataReceivedListener( listener );
    }
}//end class
