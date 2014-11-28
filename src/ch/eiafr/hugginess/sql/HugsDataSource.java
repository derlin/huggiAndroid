package ch.eiafr.hugginess.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static ch.eiafr.hugginess.sql.SqlHelper.*;

/**
 * @author: Lucy Linder
 * @date: 26.11.2014
 */
public class HugsDataSource{
    private SQLiteDatabase db;
    private SqlHelper helper;
    private static final String[] ALL_COLUMNS = new String[]{ HG_COL_ID, HG_COL_ID_REF, HG_COL_DATE, HG_COL_DUR, HG_COL_DATA };


    public HugsDataSource( Context context ){
        helper = new SqlHelper( context );
    }


    public void open() throws SQLException{
        db = helper.getWritableDatabase();
    }


    public void close(){
        helper.close();
    }


    public boolean deleteHug( String id ){
        return db.delete( HUGS_TABLE, HG_COL_ID + " = " + id, null ) > 0;
    }


    public boolean addHug( Hug hug ){
        if(!HuggersDataSource.huggerExists( db, hug.getHuggerID() )){
            Hugger hugger = new Hugger();
            hugger.setId( hug.getHuggerID() );
            HuggersDataSource.addHugger( db, hugger );
        }
        return db.insert( HUGS_TABLE, null, hugToContentValues( hug ) ) > 0;
    }


    public List<Hug> getHugs(){
        List<Hug> hugs = new ArrayList<>();

        Cursor cursor = db.query( HUGS_TABLE, null, null, null, null, null, null );
        cursor.moveToFirst();

        while( !cursor.isAfterLast() ){
            hugs.add( cursorToHug( cursor ) );
            cursor.moveToNext();
        }//end while
        cursor.close();

        return hugs;
    }


    public int getHugsCount(){
        return ( int ) DatabaseUtils.queryNumEntries( db, HUGS_TABLE );
    }

    private ContentValues hugToContentValues( Hug hug ){
        ContentValues values = new ContentValues();
        //values.put( HG_COL_ID, null );
        values.put( HG_COL_ID_REF, hug.getHuggerID() );
        values.put( HG_COL_DUR, hug.getDuration() );
        values.put( HG_COL_DATE, dateFormat.format( hug.getDate()) );
        values.put( HG_COL_DATA, hug.getData() );
        return values;
    }


    private Hug cursorToHug( Cursor cursor ){
        Hug hug = new Hug();
        int i = 0;

        hug.setId( cursor.getInt( i++ ) );
        hug.setHuggerID( cursor.getString( i++ ) );
        try{
            hug.setDate( dateFormat.parse( cursor.getString( i++ ) ) );
        }catch( ParseException e ){
            e.printStackTrace();
        }
        hug.setDuration( cursor.getInt( i++ ) );
        hug.setData( cursor.getString( i++ ) );
        return hug;
    }
}//end class
