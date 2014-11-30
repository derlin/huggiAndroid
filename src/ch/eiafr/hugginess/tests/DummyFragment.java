package ch.eiafr.hugginess.tests;


import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import ch.eiafr.hugginess.HuggiBTActivity;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.bluetooth.HuggiBluetoothService;
import ch.eiafr.hugginess.sql.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import static ch.eiafr.hugginess.bluetooth.BluetoothState.*;

/**
 * @author: Lucy Linder
 * @date: 22.11.2014
 */
public class DummyFragment extends Fragment implements Button.OnClickListener{

    private static final long BT_TIMEOUT = 4000;
    private HuggiBluetoothService mSPP;

    TextView textview;


    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
        View view = inflater.inflate( R.layout.dummytab, container, false );
        textview = ( TextView ) view.findViewById( R.id.tabtextview );
        textview.setMovementMethod( new ScrollingMovementMethod() );
        view.findViewById( R.id.echo_button ).setOnClickListener( this );
        view.findViewById( R.id.sleep_button ).setOnClickListener( this );
        view.findViewById( R.id.getHugs_button ).setOnClickListener( this );
        mSPP = ( ( HuggiBTActivity ) getActivity() ).getHuggiService();

        debug();
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
        String phone = getPhoneNumber( "Lala Test", getActivity() );
        textview.append( phone );
    }

    private void listHugs(){

        textview.setText( "" );
        //        String[] h = new String[]{ "@-3!data 1 lucy!30", "@-2!data 3 lucy!10" };
        //        hugs = new ArrayList<>();
        //        for( String s : h ){
        //            Hug hug = Hug.parseHug( s );
        //            if( hug != null ) hugs.add( hug );
        //        }//end for

        try{
            HugsDataSource dbs = new HugsDataSource( getActivity() );
            dbs.open();

            List<Hug> hugs = dbs.getHugs();
            for( Hug hug : hugs ){
                textview.append( String.format( "Hug with %s, data = %s, dur = %d date = %s\n",//
                        hug.getHuggerID(), hug.getData(), hug.getDuration(), SqlHelper.DATE_FORMAT.format( hug
                                .getDate() ) ) );
            }//end for

            dbs.close();

        }catch( SQLException e ){
            e.printStackTrace();
        }

        textview.append( "\n" );

        try{
            HuggersDataSource dbs = new HuggersDataSource( getActivity() );
            dbs.open();

            List<Hugger> huggers = dbs.getHuggers();
            for( Hugger hugger : huggers ){
                textview.append( String.format( "Hugger %s\n", hugger.getId() ) );
            }//end for

            dbs.close();

        }catch( SQLException e ){
            e.printStackTrace();
        }
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

    public String getPhoneNumber(String name, Context context) {
        String ret = null;
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" like'%" + name +"%'";
        String[] projection = new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor c = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, null, null);
        if (c.moveToFirst()) {
            ret = c.getString(0);
        }
        c.close();
        if(ret==null)
            ret = "Unsaved";
        return ret;
    }

    public Bitmap test( String name, Context context ) {
        String id, dispName, number = null, address = null;
        Bitmap photo = null;
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" like'%" + name +"%'";
        String[] projection = new String[] {
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Photo.PHOTO
        };
        Cursor c = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, null, null);

        try{
            if( c.moveToFirst() ){
                id = c.getString( 0 );
                dispName = c.getString( 1 );
                number = c.getString( 2 );
                address = c.getString( 3 );
                byte[] data = c.getBlob( 4 );
                if( data != null ){
                    photo = BitmapFactory.decodeStream( new ByteArrayInputStream( data ));
                }
            }
        }finally{
            c.close();
        }


        return photo;
    }

    public InputStream openPhoto(long contactId) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = getActivity().getContentResolver().query( photoUri, new String[]{ ContactsContract.Contacts
                .Photo.PHOTO }, null, null, null );
        if (cursor == null) {
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);
                if (data != null) {
                    return new ByteArrayInputStream(data);
                }
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    private String getMyPhoneNumber(){
        TelephonyManager mTelephonyMgr;
        mTelephonyMgr = (TelephonyManager)
               getActivity().getSystemService( Context.TELEPHONY_SERVICE );
        return mTelephonyMgr.getLine1Number();
    }




}//end class
