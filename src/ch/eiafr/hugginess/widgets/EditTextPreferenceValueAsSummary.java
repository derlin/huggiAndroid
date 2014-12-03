package ch.eiafr.hugginess.widgets;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * @author: Lucy Linder
 * @date: 30.11.2014
 */
public class EditTextPreferenceValueAsSummary extends EditTextPreference{

    private final static String TAG = EditTextPreferenceValueAsSummary.class.getName();

    public EditTextPreferenceValueAsSummary( Context context, AttributeSet attrs ) {
        super(context, attrs);
    }

    public EditTextPreferenceValueAsSummary( Context context ) {
        super(context);
    }

    @Override
    public void setText(String value) {
        super.setText(value);
        setSummary(value);
    }
//
//    private void init() {
//
//        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
//
//            @Override
//            public boolean onPreferenceChange(Preference arg0, Object arg1) {
//                arg0.setSummary(getSummary() + "\nCurrent: " + getText());
//                return true;
//            }
//        });
//    }
//
//    @Override
//    public CharSequence getSummary() {
//        return super.getSummary() + "\nCurrent: " + getText();
//    }
}