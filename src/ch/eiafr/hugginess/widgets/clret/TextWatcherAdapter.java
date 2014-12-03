package ch.eiafr.hugginess.widgets.clret;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Simple adapter of the TextWatcher Interface
 *
 * User: lucy
 * Date: 20/06/13
 * Version: 0.1
 */
public class TextWatcherAdapter implements TextWatcher {

    //-------------------------------------------------------------
    // interface used by the classes using textwatcher
    // (avoids explicit extends)
    //-------------------------------------------------------------
    public interface TextWatcherListener {
        // the method to call on TextChange
        void onTextChanged( EditText view, String text );

    }

    //-------------------------------------------------------------
    private final EditText view;  // the view to listen to
    private final TextWatcherListener listener; // the class to call on textChange


    public TextWatcherAdapter( EditText editText, TextWatcherListener listener ) {
        this.view = editText;
        this.listener = listener;
    }


    @Override
    public void onTextChanged( CharSequence s, int start, int before, int count ) {
        // simply calls the method onTextChange
        listener.onTextChanged( view, s.toString() );
    }


    @Override
    public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
    }


    @Override
    public void afterTextChanged( Editable s ) {
    }

}// end class