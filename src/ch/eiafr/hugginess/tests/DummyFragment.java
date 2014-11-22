package ch.eiafr.hugginess.tests;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ch.eiafr.hugginess.R;

/**
 * @author: Lucy Linder
 * @date: 22.11.2014
 */
public class DummyFragment extends Fragment{

    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
        View view = inflater.inflate( R.layout.dummytab, container, false );
        TextView textview = ( TextView ) view.findViewById( R.id.tabtextview );
        textview.setText( "tab 1" );
        return view;
    }

}//end class
