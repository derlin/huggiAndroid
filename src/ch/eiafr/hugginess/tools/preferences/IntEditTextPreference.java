package ch.eiafr.hugginess.tools.preferences;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * @author: Lucy Linder
 * @date: 31.12.2014
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
