package ch.eiafr.hugginess.gui.main.frag;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.services.bluetooth.HuggiBroadcastReceiver;
import ch.eiafr.hugginess.sql.entities.Hugger;
import ch.eiafr.hugginess.sql.helpers.HuggiDataSource;
import ch.eiafr.hugginess.sql.helpers.SqlHelper;

import java.util.List;


/**
 * This class is the default fragment displayed in the main activity.
 * It advertises global statistics about the interpersonal touch interactions of the user,
 * like the total number of hugs and the top 3 partners.
 * <p/>
 * creation date    22.11.2014
 * context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
 *
 * @author Lucy Linder
 */
public class HomeTabFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener{

    private View[] mTop3Views;
    private TextView mNbrHugsTextView;
    private TextView mHugsPerDay, mHugsPerWeek, mHugsPerMonth;
    private TextView mHugsAvgDurations;


    // ----------------------------------------------------

    private HuggiBroadcastReceiver mBroadcastReceiver = new HuggiBroadcastReceiver(){
        @Override
        public void onBtHugsReceived( int hugCount ){
            populateViews();
        }
    };
    // ----------------------------------------------------


    @Override
    public void onCreateOptionsMenu( Menu menu, MenuInflater inflater ){
        super.onCreateOptionsMenu( menu, inflater );
    }


    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
        View view = inflater.inflate( R.layout.activity_main_frag_hometab, container, false );
        PreferenceManager.getDefaultSharedPreferences( getActivity() ).registerOnSharedPreferenceChangeListener( this );
        mBroadcastReceiver.registerSelf( getActivity() );
        setHasOptionsMenu( true );

        mTop3Views = new View[]{ view.findViewById( R.id.top_hugger_1 ), //
                view.findViewById( R.id.top_hugger_2 ),           //
                view.findViewById( R.id.top_hugger_3 )            //
        };

        mNbrHugsTextView = ( TextView ) view.findViewById( R.id.nbr_of_hugs );

        View v = view.findViewById( R.id.hugs_per_day );
        ( ( TextView ) v.findViewById( R.id.col1 ) ).setText( "Hugs per day" );
        mHugsPerDay = ( TextView ) v.findViewById( R.id.col2 );
        v = view.findViewById( R.id.hugs_per_week );
        ( ( TextView ) v.findViewById( R.id.col1 ) ).setText( "Hugs per week" );
        mHugsPerWeek = ( TextView ) v.findViewById( R.id.col2 );
        v = view.findViewById( R.id.hugs_per_month );
        ( ( TextView ) v.findViewById( R.id.col1 ) ).setText( "Hugs per month" );
        mHugsPerMonth = ( TextView ) v.findViewById( R.id.col2 );

        v = view.findViewById( R.id.hugs_durations );
        ( ( TextView ) v.findViewById( R.id.col1 ) ).setText( "Average hug's duration" );
        mHugsAvgDurations = ( TextView ) v.findViewById( R.id.col2 );

        populateViews();

        return view;
    }


    @Override
    public void onDestroyView(){
        mBroadcastReceiver.unregisterSelf( getActivity() );
        PreferenceManager.getDefaultSharedPreferences( getActivity() ) //
                .unregisterOnSharedPreferenceChangeListener( this );
        super.onDestroyView();
    }

    // ----------------------------------------------------


    private void populateViews(){

        int totalHugsCount = 0;

        try( HuggiDataSource dbs = new HuggiDataSource( getActivity(), true ) ){

            totalHugsCount = dbs.getHugsCount();

            // -- get the top 3
            List<Pair<Hugger, Integer>> top3 = dbs.getTopXHuggers( 3 );

            int i = 0;
            // populate views
            for( Pair<Hugger, Integer> pair : top3 ){
                setHuggerView( mTop3Views[ i++ ], pair.first, pair.second );
            }//end for

            // hide unused views in case the number of huggers is less than 3
            for(; i < 3; i++ ){
                mTop3Views[ i ].setVisibility( View.GONE );
            }//end for

            // -- get the avg hugs per day/week/month
            int[] stats = dbs.getAvgStats();
            mHugsPerDay.setText( "" + stats[ 0 ] );
            mHugsPerWeek.setText( "" + stats[ 1 ] );
            mHugsPerMonth.setText( "" + stats[ 2 ] );

            int avgDurations = dbs.getAvgHugsDuration();
            mHugsAvgDurations.setText( SqlHelper.formatDuration( avgDurations ) );

        }catch( Exception e ){
            e.printStackTrace();

        }

        mNbrHugsTextView.setText( "" + totalHugsCount );
    }

    // ----------------------------------------------------


    private void setHuggerView( View rowView, Hugger hugger, int hugCount ){

        rowView.setVisibility( View.VISIBLE );
        Hugger.LocalContactDetails details = hugger.getDetails();

        TextView headerView = ( TextView ) rowView.findViewById( R.id.hug_row_header );
        TextView subheaderView = ( TextView ) rowView.findViewById( R.id.hug_row_subheader );
        TextView textView = ( TextView ) rowView.findViewById( R.id.hug_row_text );
        ImageView imageView = ( ImageView ) rowView.findViewById( R.id.hug_row_image );

        if( details != null ){
            headerView.setText( details.getName() );
            if( details.getPhotoUri() != null ){
                imageView.setImageURI( details.getPhotoUri() );
            }else{
                imageView.setImageResource( R.drawable.pixelheart );
            }
        }else{
            headerView.setText( hugger.getId() );
        }

        subheaderView.setText( hugCount + " hugs so far." );
        textView.setText( "" );
    }


    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String s ){
        if( getString( R.string.flag_data_set_changed ).equals( s ) ){
            populateViews();
        }
    }
}//end class
