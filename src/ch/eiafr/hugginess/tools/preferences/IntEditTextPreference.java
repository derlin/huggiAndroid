package ch.eiafr.hugginess.tools.preferences;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * This class implements an EditTextPreference which holds an int value. This value will thus be persisted as an int in
 * the shared preferences.
 *
 * @author Lucy Linder
 *
 * creation date    31.12.2014
 * context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
 */
public class IntEditTextPreference extends EditTextPreference{

    public IntEditTextPreference( Context context ){
        super( context );
    }


    public IntEditTextPreference( Context context, AttributeSet attrs ){
        super( context, attrs );
    }


    public IntEditTextPreference( Context context, AttributeSet attrs, int defStyle ){
        super( context, attrs, defStyle );
    }


    @Override
    protected String getPersistedString( String defaultReturnValue ){
        return String.valueOf( getPersistedInt( -1 ) );
    }


    @Override
    protected boolean persistString( String value ){
        return persistInt( Integer.valueOf( value ) );
    }

    public int getValue(){
        try{
            return Integer.valueOf( getText() );
        }catch( NullPointerException | NumberFormatException e ){
            return -1;
        }
    }
}
