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
 * @author: Lucy Linder
 * @date: 15.12.2014
 */
public class TerminalAdapter extends BaseAdapter{

    private final Activity mActivity;
    private int mMaxLines = -1;
    private List<String> list = new LinkedList<>();


    public TerminalAdapter( Activity activity ){
        mActivity = activity;
    }


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


    public void appendLine( String line ){

        line = line.trim();
        if( mMaxLines > 0 ){
           trimList();
        }

        list.add( line );
        notifyDataSetChanged();
    }


    public void clear(){
        list.clear();
        notifyDataSetChanged();
    }


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
        while( list.size() > mMaxLines ){
            list.remove( 0 );
        }//end while
    }

    /* *****************************************************************
     * View holder
     * ****************************************************************/

     private static class ViewHolder{
        protected TextView mTextView;
    }
}//end class
