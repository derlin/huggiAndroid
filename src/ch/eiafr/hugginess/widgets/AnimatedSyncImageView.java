package ch.eiafr.hugginess.widgets;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;
import ch.eiafr.hugginess.R;

/**
 * @author: Lucy Linder
 * @date: 30.11.2014
 */
public class AnimatedSyncImageView extends ImageView{

    private AnimationDrawable anim;

    public AnimatedSyncImageView( Context context, AttributeSet attrs, int defStyle ){
        super( context, attrs, defStyle );
    }


    public AnimatedSyncImageView( Context context ){
        super( context );
    }


    public AnimatedSyncImageView( Context context, AttributeSet attrs ){
        super( context, attrs );
    }

    // ----------------------------------------------------
    public void start(){
//       anim.setVisible( true, true );
        setVisibility( VISIBLE );
       anim.start();
    }


    public void stop(){
        anim.stop();
        setVisibility( INVISIBLE );
//        anim.setVisible( false, true );
    }


    public boolean isRunning(){
        return anim.isRunning();
    }
    // ----------------------------------------------------

    public void initializeAnimation(){
        setImageDrawable(null);
        setBackgroundAnimation();
        anim = ( AnimationDrawable ) getBackground();
        stop();
    }

    public void setBackgroundAnimation() {
        setBackgroundResource( R.drawable.stat_notify_sync_anim); // this is an animation-list
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Handler handler = new Handler();
        final AnimatedSyncImageView me = this;
        handler.post(new Runnable(){
            public void run() {
                me.initializeAnimation();
            }
        });
    }

}//end class
