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
 * This class is the adapter used by the {@link ch.eiafr.hugginess.gui.main.frag.HugsListFragment}
 * to display the list of hugs. It uses a custom layout (see {@link R.layout#adapter_hugslist_item}).
 * <p/>
 * Since the Hug entities contain only the id of the hugger, this adapter also need a list of huggers
 * to properly function.
 * <p/>
 * If a hugger is a local contact, his name and picture will be displayed. If not, the hugger id and a
 * default picture ({@link R.drawable#huggi_logo}) will be used instead.
 * <p/>
 * The adapter uses the Holder Pattern to increase performances when dealing with views.
 * <p/>
 * creation date    29.11.2014
 * context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
 *
 * @author Lucy Linder
 */
public class HugsListAdapter extends ArrayAdapter<Hug>{

    private final Activity activity;
    private final List<Hug> hugs;
    private Map<String, Hugger> huggers;


    /**
     * Create the adapter
     *
     * @param activity the context
     * @param hugs     the list of hugs
     * @param huggers  a map of huggers, indexed by the hugger id
     */
    public HugsListAdapter( Activity activity, List<Hug> hugs, Map<String, Hugger> huggers ){
        super( activity, R.layout.adapter_hugslist_item, hugs );
        this.activity = activity;
        this.hugs = hugs;
        this.huggers = huggers;
    }


    @Override
    public View getView( int position, View convertView, ViewGroup parent ){

        ViewHolder viewHolder; // Holder Pattern

        if( convertView == null ){
            // inflate the view and create a holder
            LayoutInflater inflater = activity.getLayoutInflater();
            convertView = inflater.inflate( R.layout.adapter_hugslist_item, null );

            viewHolder = new ViewHolder();
            viewHolder.header = ( TextView ) convertView.findViewById( R.id.hug_row_header );
            viewHolder.subheader = ( TextView ) convertView.findViewById( R.id.hug_row_subheader );
            viewHolder.text = ( TextView ) convertView.findViewById( R.id.hug_row_text );
            viewHolder.avatar = ( ImageView ) convertView.findViewById( R.id.hug_row_image );

            convertView.setTag( viewHolder );
        }else{
            // retrieve the cached view
            viewHolder = ( ViewHolder ) convertView.getTag();
        }

        //-- update view

        Hug hug = hugs.get( position );
        Hugger hugger = huggers.get( hug.getHuggerID() );
        Uri imageUri = null;

        if( hugger.isLocalContact() ){
            // populate the view with info from the local contact database
            Hugger.LocalContactDetails details = hugger.getDetails();
            viewHolder.header.setText( details.getName() );
            imageUri = details.getPhotoUri();

        }else{
            // use default
            viewHolder.header.setText( hug.getHuggerID() );
        }

        viewHolder.subheader.setText( String.format( "Data: %s", hug.getData() ) );
        viewHolder.text.setText( String.format( "%s, %s", hug.getStringDuration(), hug.getStringDate() ) );
        setImage( viewHolder, imageUri );

        return convertView;
    }


    private void setImage( ViewHolder viewHolder, Uri uri ){
        if( uri == null ){  // set default image
            viewHolder.avatar.setImageResource( R.drawable.huggi_logo );
        }else{
            viewHolder.avatar.setImageURI( uri );
        }
    }

    // ----------------------------------------------------

    protected static class ViewHolder{
        // class used to implement the Holder Pattern
        // this is approximately 15 % faster than using the findViewById() method
        protected TextView header;
        protected TextView subheader;
        protected TextView text;
        protected ImageView avatar;
    }
}