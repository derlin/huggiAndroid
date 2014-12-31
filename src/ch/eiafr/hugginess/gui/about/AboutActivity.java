/*
 * Copyright (c) 2014. The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author	Lucy Linder
 */

package ch.eiafr.hugginess.gui.about;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebView;
import ch.eiafr.hugginess.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author: Lucy Linder
 * @date: 31.12.2014
 */
public class AboutActivity extends Activity{

    @Override
    protected void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_about );

        // allow to go back to main activity on home pressed
        getActionBar().setDisplayHomeAsUpEnabled( true );

        // set the content using a raw html file
        WebView webView = ( WebView ) findViewById( R.id.webview );
        String content = loadRawResource( R.raw.about );
        if( content != null ) webView.loadData( content, "text/html", "utf-8" );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ){
        switch( item.getItemId() ){
            case android.R.id.home:{
                onBackPressed();
                return true;
            }
            default:
                return super.onOptionsItemSelected( item );
        }
    }

    // ----------------------------------------------------

    private String loadRawResource( int resId ){
        InputStream inputStream = getResources().openRawResource( resId );
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i;

        try{
            i = inputStream.read();
            while( i != -1 ){
                byteArrayOutputStream.write( i );
                i = inputStream.read();
            }
            inputStream.close();

        }catch( IOException e ){
            Log.d( getPackageName(), "About Activity: error while reading raw input file " + e );
            return null;
        }
        return byteArrayOutputStream.toString();
    }

}//end class
