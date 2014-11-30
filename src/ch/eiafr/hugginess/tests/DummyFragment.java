package ch.eiafr.hugginess.tests;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import ch.eiafr.hugginess.HuggiBTActivity;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.bluetooth.HuggiBluetoothService;
import ch.eiafr.hugginess.widgets.AnimatedSyncImageView;

import static ch.eiafr.hugginess.bluetooth.BluetoothState.*;

/**
 * @author: Lucy Linder
 * @date: 22.11.2014
 */
public class DummyFragment extends Fragment implements Button.OnClickListener{

    private static final long BT_TIMEOUT = 4000;
    private HuggiBluetoothService mSPP;
    private AnimatedSyncImageView mAnim;
    TextView textview;


    @Override
    public void onCreateOptionsMenu( Menu menu, MenuInflater inflater ){
        super.onCreateOptionsMenu( menu, inflater );
        mAnim = ( AnimatedSyncImageView ) menu.findItem( R.id.menu_spiner_anim ).getActionView();
    }


    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
        View view = inflater.inflate( R.layout.dummytab, container, false );

        textview = ( TextView ) view.findViewById( R.id.tabtextview );
        textview.setMovementMethod( new ScrollingMovementMethod() );

        view.findViewById( R.id.echo_button ).setOnClickListener( this );
        view.findViewById( R.id.sleep_button ).setOnClickListener( this );
        view.findViewById( R.id.getHugs_button ).setOnClickListener( this );

        mSPP = ( ( HuggiBTActivity ) getActivity() ).getHuggiService();

        debug();

        setHasOptionsMenu( true );

        return view;
    }


    @Override
    public void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        LocalBroadcastManager.getInstance( getActivity() ).registerReceiver( mBroadcastReceiver, new IntentFilter(
                BTSERVICE_INTENT_FILTER ) );
    }


    @Override
    public void onStop(){
        LocalBroadcastManager.getInstance( getActivity() ).unregisterReceiver( mBroadcastReceiver );
        super.onStop();
    }


    private void debug(){
        String phone = getMyPhoneNumber();
        if( phone != null ) textview.append( phone );
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

        mAnim.start();

    }


    private void getHugs(){
        mSPP.executeCommand( CMD_SEND_HUGS );
    }


    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive( Context context, Intent intent ){
            switch( intent.getStringExtra( EXTRA_EVT_TYPE ) ){

                case EVT_HUGS_RECEIVED:
                    Object test = intent.getSerializableExtra( EVT_EXTRA_HUGS_LIST );
                    debug();
                    break;
            }
        }
    };

    // ----------------------------------------------------


    private String getMyPhoneNumber(){
        TelephonyManager mTelephonyMgr;
        mTelephonyMgr = ( TelephonyManager ) getActivity().getSystemService( Context.TELEPHONY_SERVICE );
        return mTelephonyMgr.getLine1Number();
    }


}//end class
