package ch.eiafr.hugginess.tests;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.sql.Hugger;
import ch.eiafr.hugginess.sql.HuggersDataSource;
import ch.eiafr.hugginess.sql.SqlHelper;

import static ch.eiafr.hugginess.bluetooth.BluetoothState.*;
import static ch.eiafr.hugginess.sql.SqlHelper.HG_COL_ID_REF;
import static ch.eiafr.hugginess.sql.SqlHelper.HUGS_TABLE;

/**
 * @author: Lucy Linder
 * @date: 22.11.2014
 */
public class MyStatsFragment extends Fragment{

    private TextView hugCount;

    private View[] mTop3Views;
    private TextView mNbrHugsTextView;


    // ----------------------------------------------------

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive( Context context, Intent intent ){
            switch( intent.getStringExtra( EXTRA_EVT_TYPE ) ){
                case EVT_HUGS_RECEIVED:
                   populateViews();
            }
        }
    };
            // ----------------------------------------------------

    @Override
    public void onCreateOptionsMenu( Menu menu, MenuInflater inflater ){
        super.onCreateOptionsMenu( menu, inflater );
    }


    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
        View view = inflater.inflate( R.layout.your_stats, container, false );

        setHasOptionsMenu( true );

        mTop3Views = new View[]{ view.findViewById( R.id.top_hugger_1 ), //
                view.findViewById( R.id.top_hugger_2 ),           //
                view.findViewById( R.id.top_hugger_3 )            //
        };

        mNbrHugsTextView = ( TextView ) view.findViewById( R.id.nbr_of_hugs );

        populateViews();
        LocalBroadcastManager.getInstance( getActivity() ).registerReceiver( mBroadcastReceiver, new IntentFilter(
                BTSERVICE_INTENT_FILTER ) );

        return view;
    }

    @Override
    public void onDestroy(){
        LocalBroadcastManager.getInstance( getActivity() ).unregisterReceiver( mBroadcastReceiver );
        super.onDestroy();
    }

    // ----------------------------------------------------


    private void populateViews(){
        SQLiteDatabase db = new SqlHelper( getActivity() ).getWritableDatabase();
        HuggersDataSource ds = new HuggersDataSource( getActivity() );
        Cursor cursor = null;
        int totalHugsCount = 0;

        try{
            ds.open();

            totalHugsCount = ( int ) DatabaseUtils.queryNumEntries( db, HUGS_TABLE );


            cursor = db.rawQuery( String.format( "select %s, count(*) from %s group by %s order by 2 DESC limit " + "3", //
                    HG_COL_ID_REF, HUGS_TABLE, HG_COL_ID_REF ), null );
            cursor.moveToFirst();

            int i = 0;

            while( !cursor.isAfterLast() ){
                Hugger hugger = ds.getHugger( cursor.getString( 0 ) );
                int hugCount = cursor.getInt( 1 );
                setHuggerView( mTop3Views[ i ], hugger, hugCount );
                cursor.moveToNext();
                i++;
            }

            for(; i < 3; i++ ){
                mTop3Views[ i ].setVisibility( View.INVISIBLE );
            }//end for


        }catch( Exception e ){
            e.printStackTrace();
        }finally{
            ds.close();
            if( cursor != null ) cursor.close();
        }

        mNbrHugsTextView.setText( "" + totalHugsCount );
    }

    // ----------------------------------------------------


    private void setHuggerView( View rowView, Hugger hugger, int hugCount ){
        rowView.setVisibility( View.VISIBLE );
        Hugger.LocalContactDetails details = hugger.getDetails();

        if( details != null ){
            ( ( TextView ) rowView.findViewById( R.id.hug_row_header ) ).setText( details.getName() );
            if( details.getPhotoUri() != null ){
                ( ( ImageView ) rowView.findViewById( R.id.hug_row_image ) ).setImageURI( details.getPhotoUri() );
            }else{
                ( ( ImageView ) rowView.findViewById( R.id.hug_row_image ) ).setImageResource( R.drawable.pixelheart );
            }
        }else{
            ( ( TextView ) rowView.findViewById( R.id.hug_row_header ) ).setText( hugger.getId() );
        }

        ( ( TextView ) rowView.findViewById( R.id.hug_row_subheader ) ).setText( hugCount + " hugs so far." );
        ( ( TextView ) rowView.findViewById( R.id.hug_row_text ) ).setText( "" );
    }


}//end class
