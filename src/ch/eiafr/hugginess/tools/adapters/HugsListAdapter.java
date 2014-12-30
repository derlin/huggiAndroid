package ch.eiafr.hugginess.tools.adapters;

import android.app.Activity;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.sql.entities.Hug;
import ch.eiafr.hugginess.sql.entities.Hugger;

import java.util.List;
import java.util.Map;

/**
 * @author: Lucy Linder
 * @date: 29.11.2014
 */
public class HugsListAdapter extends ArrayAdapter<Hug>{
    private final Activity activity;
    private final List<Hug> hugs;
    private Map<String, Hugger> huggers;


    public HugsListAdapter( Activity activity, List<Hug> hugs, Map<String, Hugger> huggers ){
        super( activity, R.layout.adapter_hugslist_item, hugs );
        this.activity = activity;
        this.hugs = hugs;
        this.huggers = huggers;
    }


    @Override
    public View getView( int position, View convertView, ViewGroup parent ){

        ViewHolder viewHolder;

        if( convertView == null ){

            LayoutInflater inflater = activity.getLayoutInflater();
            convertView = inflater.inflate( R.layout.adapter_hugslist_item, null );

            viewHolder = new ViewHolder();
            viewHolder.header = ( TextView ) convertView.findViewById( R.id.hug_row_header );
            viewHolder.subheader = ( TextView ) convertView.findViewById( R.id.hug_row_subheader );
            viewHolder.text = ( TextView ) convertView.findViewById( R.id.hug_row_text );
            viewHolder.avatar = ( ImageView ) convertView.findViewById( R.id.hug_row_image );

            convertView.setTag( viewHolder );
        }else{
            viewHolder = ( ViewHolder ) convertView.getTag();
        }

        Hug hug = hugs.get( position );
        Hugger hugger = huggers.get( hug.getHuggerID() );

        if( hugger.isLocalContact() ){
            Hugger.LocalContactDetails details = hugger.getDetails();
            viewHolder.header.setText(  details.getName() );

            Uri uri = details.getPhotoUri();
            if( uri != null ) viewHolder.avatar.setImageURI( uri );
        }else{
            viewHolder.header.setText( hug.getHuggerID() );
            viewHolder.avatar.setImageResource( R.drawable.logo_android );
        }
            viewHolder.subheader.setText(String.format( "Data: %s",  hug.getData() ) );
            viewHolder.text.setText(String.format( "%s, %s",  hug.getStringDuration(),  hug.getStringDate() ) );
        return convertView;
    }


    protected static class ViewHolder{
        protected TextView header;
        protected TextView subheader;
        protected TextView text;
        protected ImageView avatar;
    }
}