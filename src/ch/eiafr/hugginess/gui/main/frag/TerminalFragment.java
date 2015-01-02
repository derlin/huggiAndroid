package ch.eiafr.hugginess.gui.main.frag;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.widget.*;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.services.bluetooth.HuggiBluetoothService;
import ch.eiafr.hugginess.services.bluetooth.HuggiBroadcastReceiver;
import ch.eiafr.hugginess.tools.adapters.TerminalAdapter;

import static ch.eiafr.hugginess.services.bluetooth.BluetoothConstants.CMD_SEND_HUGS;


/**
 * This class is the third fragment displayed in the main activity.
 * It acts as a terminal, displaying every line received from the HuggiShirt
 * and allowing the user to send a raw command/string through bluetooth.
 * <p/>
 * Instead of appending to a TextView, we use a list with one item per line.
 * It is not the most efficient, but it allows us to control how many lines
 * are kept in the buffer.
 * <p/>
 * By clicking on an item, the user can either clear the whole terminal or copy
 * the given line to the clipboard.
 * <p/>
 * creation date    22.11.2014
 * context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
 *
 * @author Lucy Linder
 */
public class TerminalFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener{

    private static final int TERMINAL_FRAG_GROUP_ID = 'T'; // uniquely identify events from this list
    private static final int TERMINAL_DEFAULT_MAX_LINES = 70; // default max number of lines displayed

    private HuggiBluetoothService mSPP;

    private Button mSendButton;
    private EditText mEditText;

    private TerminalAdapter mTerminalAdapter;

    // ----------------------------------------------------

    private HuggiBroadcastReceiver mBroadcastReceiver = new HuggiBroadcastReceiver(){


        @Override
        public void onBtConnected(){
            if( mSendButton != null ) mSendButton.setEnabled( true );
            mSPP.executeCommand( CMD_SEND_HUGS ); // auto fetch
        }


        @Override
        public void onBtDisonnected(){
            if( mSendButton != null ) mSendButton.setEnabled( false );
        }


        @Override
        public void onBtDataReceived( String newline ){
            mTerminalAdapter.appendLine( newline );
        }
    };

    // ----------------------------------------------------


    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
        View view = inflater.inflate( R.layout.activity_main_frag_terminal, container, false );

        mSPP = HuggiBluetoothService.getInstance();

        mEditText = ( EditText ) view.findViewById( R.id.etMessage );

        mSendButton = ( Button ) view.findViewById( R.id.btnSend );
        mSendButton.setEnabled( mSPP.isConnected() );
        mSendButton.setOnClickListener( new View.OnClickListener(){
            public void onClick( View v ){
                if( mEditText.getText().length() != 0 ){
                    // send the raw command
                    mSPP.send( mEditText.getText().toString(), true );
                }
            }
        } );


        if( mTerminalAdapter == null ){
            // get max number of lines displayed
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( getActivity() );
            int mMaxLines = sharedPreferences.getInt( getString( R.string.pref_terminal_max_lines ), 75 );
            sharedPreferences.registerOnSharedPreferenceChangeListener( this );

            mTerminalAdapter = new TerminalAdapter( getActivity(), mMaxLines );
        }

        ListView mListView = ( ListView ) view.findViewById( R.id.listview );
        mListView.setAdapter( mTerminalAdapter );

        setHasOptionsMenu( true );
        registerForContextMenu( mListView );

        return view;
    }


    @Override
    public void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        mBroadcastReceiver.registerSelf( getActivity() );
    }


    @Override
    public void onDestroy(){
        mBroadcastReceiver.unregisterSelf( getActivity() );
        super.onDestroy();
    }


    /* *****************************************************************
     * context menu
     * ****************************************************************/


    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo ){
        super.onCreateContextMenu( menu, v, menuInfo );
        menu.setHeaderTitle( "Options" );
        menu.add( TERMINAL_FRAG_GROUP_ID, v.getId(), 0, "Clear" );
        menu.add( TERMINAL_FRAG_GROUP_ID, v.getId(), 1, "Copy to clipboard" );
    }


    @Override
    public boolean onContextItemSelected( MenuItem item ){
        // check that the event comes from this list
        if( item.getGroupId() != TERMINAL_FRAG_GROUP_ID ) return false;

        if( item.getTitle() == "Clear" ){
            mTerminalAdapter.clear();
            return true;

        }else if( item.getTitle().equals( "Copy to clipboard" ) ){
            AdapterView.AdapterContextMenuInfo info = ( AdapterView.AdapterContextMenuInfo ) item.getMenuInfo();

            // get a handle to the clipboard service.
            ClipboardManager clipboard = ( ClipboardManager ) getActivity().getSystemService( Context
                    .CLIPBOARD_SERVICE );
            // get the text to copy
            String text = mTerminalAdapter.getItem( info.position );
            // copy to clipboard
            ClipData clip = ClipData.newPlainText( "huggi text", text );
            clipboard.setPrimaryClip( clip );

            Toast.makeText( getActivity(), "Copied line to clipbaord", Toast.LENGTH_SHORT ).show();
            Log.i( getActivity().getPackageName(), "Copied text '" + text + "' to clipboard" );
        }
        return true;
    }


    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String s ){
        // handle the "max lines" setting, in case it changes
        if( getString( R.string.pref_terminal_max_lines ).equals( s ) ){
            int maxLines = sharedPreferences.getInt( getString( R.string.pref_terminal_max_lines ),
                    TERMINAL_DEFAULT_MAX_LINES );
            mTerminalAdapter.setMaxLines( maxLines );
        }
    }
}//end class
