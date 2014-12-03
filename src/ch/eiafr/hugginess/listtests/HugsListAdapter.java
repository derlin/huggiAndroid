package ch.eiafr.hugginess.listtests;

import android.app.Activity;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.sql.Hug;
import ch.eiafr.hugginess.sql.Hugger;

import java.util.List;
import java.util.Map;

/**
 * @author: Lucy Linder
 * @date: 29.11.2014
 */
public class HugsListAdapter extends ArrayAdapter{
    private final Activity activity;
    private final List<Hug> hugs;
    private Map<String, Hugger> huggers;


    public HugsListAdapter( Activity activity, List<Hug> hugs, Map<String, Hugger> huggers ){
        super( activity, R.layout.hugs_row_list, hugs );
        this.activity = activity;
        this.hugs = hugs;
        this.huggers = huggers;
    }


    @Override
    public View getView( int position, View rowView, ViewGroup parent ){

        RetainView view;

        if( rowView == null ){

            LayoutInflater inflater = activity.getLayoutInflater();
            rowView = inflater.inflate( R.layout.hugs_row_list, null );

            view = new RetainView();
            view.header = ( TextView ) rowView.findViewById( R.id.hug_row_header );
            view.subheader = ( TextView ) rowView.findViewById( R.id.hug_row_subheader );
            view.text = ( TextView ) rowView.findViewById( R.id.hug_row_text );
            view.avatar = ( ImageView ) rowView.findViewById( R.id.hug_row_image );

            rowView.setTag( view );
        }else{
            view = ( RetainView ) rowView.getTag();
        }

        Hug hug = hugs.get( position );
        Hugger hugger = huggers.get( hug.getHuggerID() );

        if( hugger.isLocalContact() ){
            Hugger.LocalContactDetails details = hugger.getDetails();
            view.header.setText(  details.getName() );

            Uri uri = details.getPhotoUri();
            if( uri != null ) view.avatar.setImageURI( uri );
        }else{
            view.header.setText( hug.getHuggerID() );
            view.avatar.setImageResource( R.drawable.logo_android );
        }
            view.subheader.setText(String.format( "Data: %s",  hug.getData() ) );
            view.text.setText(String.format( "%s, %s",  hug.getStringDuration(),  hug.getStringDate() ) );
        return rowView;
    }


    protected static class RetainView{
        protected TextView header;
        protected TextView subheader;
        protected TextView text;
        protected ImageView avatar;
    }
}