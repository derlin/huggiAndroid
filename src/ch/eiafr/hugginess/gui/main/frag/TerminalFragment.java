package ch.eiafr.hugginess.gui.main.frag;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
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
 * @author: Lucy Linder
 * @date: 22.11.2014
 */
public class TerminalFragment extends Fragment{

    private static final int TERMINAL_FRAG_GROUP_ID = 'T';
    private HuggiBluetoothService mSPP;
    private Button mSendButton;
    private EditText mEditText;

    private TerminalAdapter mTerminalAdapter;
    private int mMaxLines = 70; // TODO

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
                    mSPP.send( mEditText.getText().toString(), true );
                }
            }
        } );

        ListView mListView = ( ListView ) view.findViewById( R.id.listview );
        mTerminalAdapter = new TerminalAdapter( getActivity(), mMaxLines );
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
        menu.add( TERMINAL_FRAG_GROUP_ID, v.getId(), 0, "Copy to clipboard" );
    }


    @Override
    public boolean onContextItemSelected( MenuItem item ){
        // check that the event comes from this list
        if( item.getGroupId() != TERMINAL_FRAG_GROUP_ID ) return false;

        if( item.getTitle() == "Clear" ){
            mTerminalAdapter.clear();
            return true;
        }else if (item.getTitle().equals( "Copy to clipboard" )){
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo ) item.getMenuInfo();

            // get a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager )
                    getActivity().getSystemService( Context.CLIPBOARD_SERVICE );
            String text = mTerminalAdapter.getItem( info.position );
            ClipData clip = ClipData.newPlainText("huggi text", text);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(getActivity(), "Copied line to clipbaord", Toast.LENGTH_SHORT).show();
            Log.i( getActivity().getPackageName(), "Copied text '" + text + "' to clipboard" );
        }
        return true;
    }

}//end class
