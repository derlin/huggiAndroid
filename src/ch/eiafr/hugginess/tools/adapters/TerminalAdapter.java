package ch.eiafr.hugginess.tools.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import ch.eiafr.hugginess.R;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is the Adapter used by the {@link ch.eiafr.hugginess.gui.main.frag.TerminalFragment}.
 * It mimics a TextView using a list and works as a FIFO: new lines will be added at the bottom and oldest items will
 * be discarded if the number of items  exceeds {@link #getMaxLines()}.
 *
 * @author Lucy Linder
 *
 * creation date    15.12.2014
 * context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
 */
public class TerminalAdapter extends BaseAdapter{

    private final Activity mActivity;
    private int mMaxLines = -1;
    private List<String> list = new LinkedList<>();


    public TerminalAdapter( Activity activity ){
        mActivity = activity;
    }


    /**
     * Create an adapter.
     *
     * @param activity the context
     * @param maxLines the maximum number of lines displayed
     */
    public TerminalAdapter( Activity activity, int maxLines ){
        mActivity = activity;
        mMaxLines = maxLines;
    }


    @Override
    public int getCount(){
        return list.size();
    }


    @Override
    public String getItem( int i ){
        return list.get( i );
    }


    @Override
    public long getItemId( int i ){
        return i;
    }

    /** Add one line to the list **/
    public void appendLine( String line ){

        line = line.trim();
        if( mMaxLines > 0 ){
            trimList();
        }

        list.add( line );
        notifyDataSetChanged();
    }

    /** Clear the adapter **/
    public void clear(){
        list.clear();
        notifyDataSetChanged();
    }


    /**
     * @return the current item's limit.
     */
    public int getMaxLines(){
        return mMaxLines;
    }


    /**
     * Set the maximal number of items to display.
     * @param maxLines the new item's limit.
     */
    public void setMaxLines( int maxLines ){
        this.mMaxLines = maxLines;
        trimList();
        notifyDataSetChanged();
    }


    @Override
    public View getView( int i, View convertView, ViewGroup viewGroup ){
        ViewHolder viewHolder;

        if( convertView == null ){

            LayoutInflater inflater = mActivity.getLayoutInflater();
            convertView = inflater.inflate( R.layout.adapter_terminal_item, null );
            viewHolder = new ViewHolder();
            viewHolder.mTextView = ( TextView ) convertView.findViewById( R.id.text );

            convertView.setTag( viewHolder );
        }else{
            viewHolder = ( ViewHolder ) convertView.getTag();
        }

        viewHolder.mTextView.setText( getItem( i ) );

        return convertView;
    }


    private void trimList(){
        // remove the oldest lines
        while( list.size() > mMaxLines ){
            list.remove( 0 );
        }//end while
    }

    /* *****************************************************************
     * View holder
     * ****************************************************************/

    private static class ViewHolder{
        // class used to implement the Holder Pattern
        // this is approximately 15 % faster than using the findViewById() method
        protected TextView mTextView;
    }
}//end class
