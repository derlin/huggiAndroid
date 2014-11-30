package ch.eiafr.hugginess.sql;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import ch.eiafr.hugginess.app.App;

import java.io.InputStream;

/**
 * @author: Lucy Linder
 * @date: 26.11.2014
 */
public class Hugger{
    private String id;
    private LocalContactDetails details;
    private boolean isLocalContactLoaded = false;

    // if the hugger is a contact


    public boolean isLocalContact(){
        return getDetails() != null;
    }


    public LocalContactDetails getDetails(){
        if( !isLocalContactLoaded ){
            details = getContactDetails( id );
            isLocalContactLoaded = true;
        }
        return details;
    }


    // ----------------------------------------------------


    public String getId(){
        return id;
    }


    public void setId( String id ){
        this.id = id;
    }


    // ----------------------------------------------------


    @Override
    public boolean equals( Object o ){
        if( this == o ) return true;
        if( o == null || getClass() != o.getClass() ) return false;

        Hugger hugger = ( Hugger ) o;
        return id.equals( hugger.id );
    }


    @Override
    public int hashCode(){
        return id.hashCode();
    }

    // ----------------------------------------------------

    public static class LocalContactDetails{

        private long contactId;
        private String name;
        public Uri photoUri;


        public Uri getPhotoUri(){
            return photoUri;
        }


        public long getContactId(){
            return contactId;
        }


        public String getName(){
            return name;
        }
    }


    private static LocalContactDetails getContactDetails( String number ){


        Context context = App.getAppContext();
        InputStream photoInputStream;

        LocalContactDetails details = new LocalContactDetails();

        // define the columns I want the query to return
        String[] projection = new String[]{ ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup
                ._ID, ContactsContract.PhoneLookup.PHOTO_URI };

        // encode the phone number and build the filter URI
        Uri contactUri = Uri.withAppendedPath( ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode( number ) );

        // query time
        Cursor cursor = context.getContentResolver().query( contactUri, projection, null, null, null );

        if( cursor.moveToFirst() ){

            // Get values from contacts database:
            details.contactId = cursor.getLong( cursor.getColumnIndex( ContactsContract.PhoneLookup._ID ) );
            details.name = cursor.getString( cursor.getColumnIndex( ContactsContract.PhoneLookup.DISPLAY_NAME ) );
            String s = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI));
            if(s != null) details.photoUri = Uri.parse( s );
            // Get photo of contactId as input stream:
            Uri uri = ContentUris.withAppendedId( ContactsContract.Contacts.CONTENT_URI, details.contactId );
            //photoInputStream = ContactsContract.Contacts.openContactPhotoInputStream( context.getContentResolver(), uri); //, true );

        }else{
            Log.v( "ffnet", "Started uploadcontactphoto: Contact Not Found @ " + number );
            return null; // contact not found
        }

        // Only continue if we found a valid contact photo:
//        if( photoInputStream != null ){
//            Log.v( "ffnet", "Started uploadcontactphoto: Photo found, id = " + details.contactId + " name = " + details.name );
//            Bitmap picture = BitmapFactory.decodeStream( photoInputStream );
//        }

        return details;
    }


    //    public Bitmap loadContactPhoto(ContentResolver cr, long id) {
    //        Uri uri = ContentUris.withAppendedId( ContactsContract.Contacts.CONTENT_URI, id );
    //        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream( cr, uri );
    //        if (input == null) {
    //            return null;
    //        }
    //        return BitmapFactory.decodeStream( input );
    //    }


}//end class
