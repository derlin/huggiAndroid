package ch.eiafr.hugginess.listtests;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import ch.eiafr.hugginess.R;
import ch.eiafr.hugginess.sql.Hug;
import ch.eiafr.hugginess.sql.Hugger;
import ch.eiafr.hugginess.sql.HuggersDataSource;
import ch.eiafr.hugginess.sql.HugsDataSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author: Lucy Linder
 * @date: 29.11.2014
 */
public class ListFragment extends Fragment{
    private ListView mList;
    private List<Hug> hugs;
    private Map<String, Hugger> huggers;


    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
        super.onCreate(savedInstanceState);
        View view = inflater.inflate( R.layout.hugs_list, container, false );
        mList = (ListView ) view.findViewById(R.id.tab_contact_list);

        try{
            HugsDataSource db = new HugsDataSource( getActivity() );
            db.open();
            hugs = db.getHugs();
            db.close();
        }catch( SQLException e ){
            e.printStackTrace();
            hugs = new ArrayList<>(  );
        }


        try{
            HuggersDataSource db = new HuggersDataSource( getActivity() );
            db.open();
            huggers = db.getHuggersMap();
            db.close();
        }catch( SQLException e ){
            e.printStackTrace();
            huggers = new TreeMap<>(  );
        }


        HugsListAdapter hugsListAdapter = new HugsListAdapter( getActivity(), hugs, huggers );
        mList.setAdapter( hugsListAdapter );


        return view;
    }



}