package ch.eiafr.hugginess.sql.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * This class is in charge of the sqlite database, which holds two tables: hugs and huggers.
 * We also need an index table to ensure uniqueness of the tuple [hugger id, hug duration, hug date].
 * <p/>
 * hugs
 * cannot have the same hugger ID, date and duration (hence the index table).
 * <p/>
 * The hugger table contains only the hugger ID, in the form of a swiss phone number (0XXXXXXXXX).
 * <p/>
 * creation date    26.11.2014
 * context          Projet de semestre Hugginess, EIA-FR, I3 2014-2015
 *
 * @author Lucy Linder
 */
public class SqlHelper extends SQLiteOpenHelper{
    // database
    private static final String DB_NAME = "Hugginess.db";
    private static final int DB_VERSION = 1;

    // tables
    private static final String HUGS_INDEX = "hugs_index";
    public static final String HUGGERS_TABLE = "huggers";
    public static final String HUGS_TABLE = "hugs";

    // huggers table columns
    public static final String HR_COL_ID = "hr_id";
    public static final String HR_COL_ID_TYPE = "TEXT";

    // hugs table columns
    public static final String HG_COL_ID = "hg_id";
    public static final String HG_COL_DATE = "date";
    public static final String HG_COL_DUR = "duration";
    public static final String HG_COL_DATA = "data";
    public static final String HG_COL_ID_REF = "hugger_id";


    // create statements
    private static final String CREATE_HUGGERS_TABLE = String.format(//
            "CREATE TABLE %s (" + //
                    "%s %s PRIMARY KEY" + //
                    ");",  //
            HUGGERS_TABLE, HR_COL_ID, HR_COL_ID_TYPE );

    private static final String CREATE_HUGS_TABLE = String.format(//
            "CREATE TABLE %s (" + //
                    "%s INTEGER PRIMARY KEY AUTOINCREMENT," + //
                    "%s %s NOT NULL," + // hugger id
                    "%s INTEGER NOT NULL," + // date
                    "%s INTEGER NOT NULL," + // duration, in ms
                    "%s TEXT NOT NULL," + //  data
                    "FOREIGN KEY(%s) REFERENCES %s(%s)" + //
                    ");",  //
            HUGS_TABLE, HG_COL_ID, HG_COL_ID_REF, HR_COL_ID_TYPE, HG_COL_DATE, HG_COL_DUR, HG_COL_DATA, //
            HG_COL_ID_REF, HUGGERS_TABLE, HR_COL_ID );

    private static final String CREATE_UNIQUE_INDEX = String.format( //
            "CREATE UNIQUE INDEX %s ON %s (%s,%s,%s)", //
            HUGS_INDEX, HUGS_TABLE, HG_COL_ID_REF, HG_COL_DUR, HG_COL_DATE );


    public SqlHelper( Context context ){
        super( context, DB_NAME, null, DB_VERSION );
    }


    @Override
    public void onCreate( SQLiteDatabase db ){
        db.execSQL( CREATE_HUGGERS_TABLE );
        db.execSQL( CREATE_HUGS_TABLE );
        db.execSQL( CREATE_UNIQUE_INDEX );
    }


    @Override
    public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ){
        Log.w( SqlHelper.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion + ", " +
                "which will destroy all old data" );

        db.execSQL( "DROP TABLE IF EXISTS " + HUGS_TABLE );
        db.execSQL( "DROP TABLE IF EXISTS " + HUGGERS_TABLE );
        db.execSQL( "DROP INDEX IF EXISTS" + HUGS_INDEX );
        onCreate( db );
    }


}//end class
