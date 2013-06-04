/*
 * Copyright (C) 2013 ParanoidAndroid.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.Notification;
import android.app.INotificationManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.animation.Keyframe;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Configuration;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.RemoteException;
import android.os.Vibrator;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation;
import android.animation.TimeInterpolator;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.SoundEffectConstants;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar.NotificationClicker;
import com.android.internal.statusbar.StatusBarNotification;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.phone.Ticker;

public class Halo extends FrameLayout implements Ticker.TickerCallback {

	private int id;
    private String appName;

    private Context mContext;
	private LayoutInflater mLayoutInflater;
    private ImageView mIcon;
    private ImageView mOverlay;
    private TextView mNumber;
    private PackageManager mPm ;
    private Handler mHandler;
    private BaseStatusBar mBar;    
    private WindowManager mWindowManager;
    private View mRoot;
    private int mIconSize, mIconHalfSize;
    private WindowManager.LayoutParams mTickerPos;
    private boolean isBeingDragged = false;
    private boolean mHapticFeedback;
    private Vibrator mVibrator;
    private LayoutInflater mInflater;
    private HaloEffect mHaloEffect;
    private Display mDisplay;
    private View mContent, mHaloContent;
    private NotificationData.Entry mLastNotificationEntry = null;
    private NotificationData.Entry mCurrentNotficationEntry = null;
    private NotificationClicker mContentIntent, mTaskIntent;
    private NotificationData mNotificationData;
    private String mNotificationText = "";
    private GestureDetector mGestureDetector;

    private Paint mPaintHoloBlue = new Paint();
    private Paint mPaintWhite = new Paint();
    private Paint mPaintHoloRed = new Paint();

    private Drawable mHaloDismiss;
    private Drawable mHaloBackL;
    private Drawable mHaloBackR;
    private Drawable mHaloBlackX;

    public static final String TAG = "HaloLauncher";
    private static final boolean DEBUG = true;
    private static final int TICKER_HIDE_TIME = 2500;
    private static final int SLEEP_DELAY_DAYDREAMING = 3800;
    private static final int SLEEP_DELAY_SWIPE = 1000;
    private static final int SLEEP_DELAY_REM = 7000;

	public boolean mExpanded = false;
    public boolean mSnapped = true;
    public boolean mHidden = false;
    public boolean mFirstStart = true;
    private boolean mInitialized = false;
    private boolean mTickerLeft = true;
    private boolean mNumberLeft = true;
    private boolean mIsNotificationNew = true;
    private boolean mOverX = false;

    private boolean mInteractionReversed = true;
    private boolean mMarkerTop = false;
    private boolean mMarkerBottom = false;

    private int mScreenMin, mScreenMax;
    private int mScreenWidth, mScreenHeight;
    private Rect mPopUpRect;

    private int mKillX, mKillY;
    private int mMarkerIndex;

    private ValueAnimator mSleepREM = ValueAnimator.ofInt(0, 1);
    private AlphaAnimation mSleepNap = new AlphaAnimation(1, 0.65f);
    private int mAnimationFromX;
    private int mAnimationToX;

    private float mRawX, mRawY;

    private SettingsObserver mSettingsObserver;

    private final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.HALO_REVERSED), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.HAPTIC_FEEDBACK_ENABLED), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            mInteractionReversed =
                    Settings.System.getInt(mContext.getContentResolver(), Settings.System.HALO_REVERSED, 1) == 1;
            mHapticFeedback = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0;

            if (!selfChange) {
                wakeUp(true);
                snapToSide(true,0);
            }
        }
    }

    public Halo(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Halo(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mPm = mContext.getPackageManager();
        mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);         
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();
        mGestureDetector = new GestureDetector(mContext, new GestureListener());
        mHandler = new Handler();
        mRoot = this;

        mSettingsObserver = new SettingsObserver(new Handler());
        mSettingsObserver.observe();
        mSettingsObserver.onChange(true);

        // Init variables
        BitmapDrawable bd = (BitmapDrawable) mContext.getResources().getDrawable(R.drawable.halo_bg);
        mIconSize = bd.getBitmap().getWidth();
        mIconHalfSize = mIconSize / 2;
        mTickerPos = getWMParams();

        mHaloDismiss = mContext.getResources().getDrawable(R.drawable.halo_dismiss);
        mHaloBackL = mContext.getResources().getDrawable(R.drawable.halo_back_left);
        mHaloBackR = mContext.getResources().getDrawable(R.drawable.halo_back_right);
        mHaloBlackX = mContext.getResources().getDrawable(R.drawable.halo_black_x);

        // Init colors
        mPaintHoloBlue.setAntiAlias(true);
        mPaintHoloBlue.setColor(0xff33b5e5);
        mPaintWhite.setAntiAlias(true);
        mPaintWhite.setColor(0xfff0f0f0);
        mPaintHoloRed.setAntiAlias(true);
        mPaintHoloRed.setColor(0xffcc0000);

        // Animations
        mSleepNap.setInterpolator(new DecelerateInterpolator());
        mSleepNap.setFillAfter(true);
        mSleepNap.setStartOffset(2000);
        mSleepNap.setDuration(1000);

        mSleepREM.setDuration(1000);
        mSleepREM.setInterpolator(new AccelerateInterpolator());
        mSleepREM.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mTickerPos.x = (int)(mAnimationFromX + mAnimationToX * animation.getAnimatedFraction());
                updatePosition();
            }});
        mSleepREM.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationEnd(Animator animation)
            {
                mHidden = true;
            }
            @Override public void onAnimationRepeat(Animator animation) {}
            @Override public void onAnimationStart(Animator animation) 
            {
                mAnimationFromX = mTickerPos.x;
                mAnimationToX = (int)((mAnimationFromX < mScreenWidth / 2) ? -mIconSize * 0.4f: mIconSize * 0.4f);
            }});

        // Create effect layer
        mHaloEffect = new HaloEffect(mContext);
        mHaloEffect.setLayerType (View.LAYER_TYPE_HARDWARE, null);
        mHaloEffect.pingMinRadius = mIconHalfSize;
        mHaloEffect.pingMaxRadius = (int)(mIconSize * 1.1f);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                      WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                      | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                      | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                      | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                      | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
              PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.LEFT|Gravity.TOP;
        mWindowManager.addView(mHaloEffect, lp);
    }

    private void initControl() {
        if (mInitialized) return;

        mInitialized = true;

        // Get actual screen size
        mScreenWidth = mHaloEffect.getWidth();
        mScreenHeight = mHaloEffect.getHeight();

        mScreenMin = Math.min(mHaloEffect.getWidth(), mHaloEffect.getHeight());
        mScreenMax = Math.max(mHaloEffect.getWidth(), mHaloEffect.getHeight());

        if (mFirstStart) {
            // let's put halo in sight
            mTickerPos.x = mScreenWidth / 2 - mIconHalfSize;
            mTickerPos.y = mScreenHeight / 2 - mIconHalfSize;

            mHandler.postDelayed(new Runnable() {
                public void run() {
                    wakeUp(false);
                    snapToSide(true, 500);
                }}, 1500);
        }

        // Update dimensions
        unscheduleSleep();
        updateConstraints();

        if (!mFirstStart) {
            snapToSide(true, 500);
        } else {
            // Do the startup animations only once
            mFirstStart = false;
        }
    }

    ObjectAnimator mCurrentAnimator;
    private synchronized void wakeUp(boolean pop) {
        wakeUp(pop, 250);
    }

    private synchronized void wakeUp(boolean pop, int duration) {
        unscheduleSleep();
        mContent.setVisibility(View.VISIBLE);

        if (mCurrentAnimator != null) mCurrentAnimator.cancel();
        mCurrentAnimator = ObjectAnimator.ofFloat(mContent, "x", 0).setDuration(duration);
        mCurrentAnimator.start();

        // First things first, make the bubble visible
        AlphaAnimation alphaUp = new AlphaAnimation(mContent.getAlpha(), 1);
        alphaUp.setFillAfter(true);
        alphaUp.setDuration(duration);
        mContent.startAnimation(alphaUp);

        // Then pop
        if (pop) {
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    mHaloEffect.causePing(mPaintHoloBlue);
                }}, duration);
            
        }
    }

    private void unscheduleSleep() {
        mHandler.removeCallbacks(Sleep);
        mSleepREM.cancel();
        mSleepNap.cancel();
        mHidden = false;
    }

    Runnable Sleep = new Runnable() {
        public synchronized void run() {
            if (mCurrentAnimator != null) mCurrentAnimator.cancel();
            mCurrentAnimator = ObjectAnimator
                    .ofFloat(mContent, "x", mTickerLeft ? -mIconHalfSize : mIconHalfSize)
                    .setDuration(2000);
            mCurrentAnimator.start();
            mContent.startAnimation(mSleepNap);
        }};

    private void scheduleSleep(int daydreaming, int rem) {
        unscheduleSleep();

        if (isBeingDragged) return;
        mHandler.postDelayed(Sleep, daydreaming);

        if (rem > 0) {
            mSleepREM.setStartDelay(rem);
            mSleepREM.start();
        }
    }

    private void snapToSide(boolean sleep, final int daydreaming) {
        snapToSide(sleep, daydreaming, 0);
    }

    private void snapToSide(boolean sleep, final int daydreaming, final int rem) {
        final int fromX = mTickerPos.x;
        final boolean left = fromX < mScreenWidth / 2;
        final int toX = left ? -fromX : mScreenWidth-fromX-mIconSize;

        ValueAnimator topAnimation = ValueAnimator.ofInt(0, 1);
        topAnimation.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mTickerPos.x = (int)(fromX + toX * animation.getAnimatedFraction());
                updatePosition();
            }
        });
        if (sleep) {
            // Make it fall asleep
            topAnimation.addListener(new Animator.AnimatorListener() {
                @Override public void onAnimationCancel(Animator animation) {}
                @Override public void onAnimationEnd(Animator animation)
                {
                    scheduleSleep(daydreaming, rem);
                }
                @Override public void onAnimationRepeat(Animator animation) {}
                @Override public void onAnimationStart(Animator animation) {}
            });
        }
        topAnimation.setDuration(250);
        topAnimation.setInterpolator(new AccelerateInterpolator());
        topAnimation.start();
    }

    private void updatePosition() {
        try {
            mTickerLeft = (mTickerPos.x + mIconHalfSize < mScreenWidth / 2);
            if (mNumberLeft != mTickerLeft) {
                mNumberLeft = mTickerLeft;
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams
                        (RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT );
                params.addRule(mNumberLeft ? RelativeLayout.ALIGN_RIGHT : RelativeLayout.ALIGN_LEFT, mHaloContent.getId());
                mNumber.setLayoutParams(params);
                mHaloEffect.createBubble();
            }

            mWindowManager.updateViewLayout(mRoot, mTickerPos);
            mHaloEffect.invalidate();
        } catch(Exception e) {
            // Probably some animation still looking to move stuff around
        }
    }

    private void updateConstraints() {
        mKillX = mScreenWidth / 2;
        mKillY = mIconHalfSize;

        final int popupWidth;
        final int popupHeight;

        DisplayMetrics metrics = new DisplayMetrics();
        mDisplay.getMetrics(metrics);

        if (metrics.heightPixels > metrics.widthPixels) {
                popupWidth = (int)(metrics.widthPixels * 0.9f);
                popupHeight = (int)(metrics.heightPixels * 0.7f);
            } else {
                popupWidth = (int)(metrics.widthPixels * 0.7f);
                popupHeight = (int)(metrics.heightPixels * 0.8f);
        }

        mPopUpRect = new Rect(
            (mScreenWidth - popupWidth) / 2,
            (mScreenHeight - popupHeight) / 2,
            mScreenWidth - (mScreenWidth - popupWidth) / 2,
            mScreenHeight - (mScreenHeight - popupHeight) / 2);

        if (!mFirstStart) {
            if (mTickerPos.y < 0) mTickerPos.y = 0;
            if (mTickerPos.y > mScreenHeight-mIconSize) mTickerPos.y = mScreenHeight-mIconSize;
            mTickerPos.x = mTickerLeft ? 0 : mScreenWidth-mIconSize;
        }
        mWindowManager.updateViewLayout(mRoot, mTickerPos);
    }

    public void setStatusBar(BaseStatusBar bar) {
        mBar = bar;
        if (mBar.getTicker() != null) mBar.getTicker().setUpdateEvent(this);
        mNotificationData = mBar.getNotificationData();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Content
        mContent = (View) findViewById(R.id.content);
        mContent.setLayoutParams(new FrameLayout.LayoutParams(mIconSize, mIconSize));
        mContent.setOnClickListener(mIconClicker);
        mContent.setOnTouchListener(mIconTouchListener);

        mHaloContent = (View) findViewById(R.id.halo_content);
        
        // Icon
		mIcon = (ImageView) findViewById(R.id.app_icon);

        // Overlay
		mOverlay = (ImageView) findViewById(R.id.halo_overlay);

        // Number
        mNumber = (TextView) findViewById(R.id.number);
        mNumber.setVisibility(View.GONE);
    }

    OnClickListener mIconClicker = new OnClickListener() {
        @Override
		public void onClick(View v) {
            
        }
    };

    void launchTask(NotificationClicker intent) {
        try {
            ActivityManagerNative.getDefault().resumeAppSwitches();
            ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
        } catch (RemoteException e) {
            // ...
        }

        if (intent!= null) {
            intent.onClick(mRoot);
        }
    }

    private boolean mDoubleTap = false;
    class GestureListener extends GestureDetector.SimpleOnGestureListener {
        
        @Override
        public boolean onSingleTapUp (MotionEvent event) {
            playSoundEffect(SoundEffectConstants.CLICK);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, 
                float velocityX, float velocityY) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            wakeUp(false);
            snapToSide(true, 0);
            if (!isBeingDragged && !mHidden) {
                launchTask(mContentIntent);
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            wakeUp(false);
            if (!mInteractionReversed) {
                mDoubleTap = true;
                mBar.mHaloTaskerActive = true;
                mBar.updateNotificationIcons();
            } else {
                // Move
                isBeingDragged = true;
                mHaloEffect.intro();
            }
            return true;
        }
    }

    void resetIcons() {
        final float originalAlpha = mContext.getResources().getFraction(R.dimen.status_bar_icon_drawing_alpha, 1, 1);
        for (int i = 0; i < mNotificationData.size(); i++) {
            NotificationData.Entry entry = mNotificationData.get(i);            
            entry.icon.setAlpha(originalAlpha);
        }
    }

    void setIcon(int index) {
        float originalAlpha = mContext.getResources().getFraction(R.dimen.status_bar_icon_drawing_alpha, 1, 1);
        for (int i = 0; i < mNotificationData.size(); i++) {
            NotificationData.Entry entry = mNotificationData.get(i);
            entry.icon.setAlpha(index == i ? 1f : originalAlpha);
        }
    }

    OnTouchListener mIconTouchListener = new OnTouchListener() {
        private float initialX = 0;
        private float initialY = 0;        
        private int oldIconIndex = -1;
        private boolean hiddenState = false;
        private Drawable overlay, oldOverlay;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
            mGestureDetector.onTouchEvent(event);

            final int action = event.getAction();
            switch(action) {
                case MotionEvent.ACTION_DOWN:
                   
                    hiddenState = mHidden;
                    mOverlay.setImageDrawable(null);
                    mOverlay.setAlpha(0f);
                    overlay = null;
                    oldOverlay = null;
                    mMarkerTop = false;
                    mMarkerBottom = false;

                    if (mHidden) {
                        wakeUp(false);
                        snapToSide(false, 0);
                        hiddenState = true;
                        return false;
                    }

                    // Watch out here, in reversed mode we can not overwrite the double-tap action down.
                    if (!(mInteractionReversed && isBeingDragged)) {
                        mTaskIntent = null;
                        oldIconIndex = -1;
                        isBeingDragged = false;
                        mOverX = false;
                        initialX = event.getRawX();
                        initialY = event.getRawY();                        
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:

                    mOverlay.setImageDrawable(null);
                    mOverlay.setAlpha(0f);

                    // Snap HALO when it has been dragged or tasked or hidden
                    if (isBeingDragged || mDoubleTap || hiddenState && !(mMarkerTop || mMarkerBottom)) {
                        snapToSide(true, hiddenState ? SLEEP_DELAY_SWIPE : 0);
                    }

                    if (mDoubleTap) {                               
                        if (mTaskIntent != null) {
                            playSoundEffect(SoundEffectConstants.CLICK);
                            launchTask(mTaskIntent);                          
                        }

                        // Hide ticker from sight completely if that's what the user wants
                        if (mMarkerBottom) {
                            playSoundEffect(SoundEffectConstants.CLICK);
                            snapToSide(true, 0, 2500);
                        }

                        // Dismiss notification
                        if (mMarkerTop) {
                            playSoundEffect(SoundEffectConstants.CLICK);

                            if (mContentIntent != null) {
                                try {
                                    mBar.getService().onNotificationClick(mContentIntent.mPkg, mContentIntent.mTag, mContentIntent.mId);
                                } catch (RemoteException ex) {
                                    // system process is dead if we're here.
                                }

                                NotificationData.Entry entry = null;
                                if (mNotificationData.size() > 0) {
                                    for (int i = mNotificationData.size() - 1; i >= 0; i--) {
                                        NotificationData.Entry item = mNotificationData.get(i);
                                        if (item.notification != null && mCurrentNotficationEntry != null &&
                                                mCurrentNotficationEntry.notification != null &&
                                                mCurrentNotficationEntry.notification == item.notification) {
                                            continue;
                                        }

                                        boolean cancel = (item.notification.notification.flags &
                                                    Notification.FLAG_AUTO_CANCEL) == Notification.FLAG_AUTO_CANCEL;
                                        if (cancel) {
                                            entry = item;
                                            break;
                                        }
                                    }
                                }
                                tick(entry, "", 0, false);
                                mNotificationText = "";
                                mLastNotificationEntry = null;
                            }
                        }

                        resetIcons();
                        mBar.mHaloTaskerActive = false;
                        mBar.updateNotificationIcons();
                        mDoubleTap = false;
                    }

                    if (isBeingDragged) mHaloEffect.outro();

                    mHaloEffect.killTicker();
                    boolean oldState = isBeingDragged;
                    isBeingDragged = false;

                    mHaloEffect.invalidate();

                    // Do we erase ourselves?
                    if (mOverX) {
                        Settings.System.putInt(mContext.getContentResolver(),
                                Settings.System.HALO_ACTIVE, 0);
                        return true;
                    }
                    return oldState;
                case MotionEvent.ACTION_MOVE:
                    if (hiddenState) break;

                    mRawX = event.getRawX();
                    mRawY = event.getRawY();
                    float distanceX = mKillX-mRawX;
                    float distanceY = mKillY-mRawY;
                    float distanceToKill = (float)Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
                    distanceX = initialX-mRawX;
                    distanceY = initialY-mRawY;
                    float initialDistance = (float)Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));

                    if (!mDoubleTap) {
                        // Check kill radius
                        if (distanceToKill < mIconSize) {

                            // Magnetize X
                            mTickerPos.x = (int)mKillX - mIconHalfSize;
                            mTickerPos.y = (int)(mKillY - mIconHalfSize);
                            updatePosition();
                            
                            if (!mOverX) {
                                if (mHapticFeedback) mVibrator.vibrate(25);
                                mHaloEffect.causePing(mPaintHoloRed);
                                mOverlay.setImageDrawable(mHaloBlackX);
                                mOverlay.setAlpha(1f);
                                mOverX = true;
                            }

                            return false;
                        } else {
                            mOverlay.setImageDrawable(null);
                            mOverlay.setAlpha(0f);
                            mOverX = false;
                        }

                        // Drag
                        if (!isBeingDragged) {
                            if (initialDistance > mIconSize * 0.7f) {            
                                if (mInteractionReversed) {                                
                                    mDoubleTap = true;                  
                                    wakeUp(false);
                                    snapToSide(false, 0);
                                    // Show all available icons for easier browsing while the tasker is in motion
                                    mBar.mHaloTaskerActive = true;
                                    mBar.updateNotificationIcons();
                                } else {
                                    wakeUp(false, 0);
                                    isBeingDragged = true;
                                    mHaloEffect.intro();
                                    if (mHapticFeedback) mVibrator.vibrate(25);
                                }
                                return false;
                            }
                        } else {
                            mTickerPos.x = (int)mRawX - mIconHalfSize;
                            mTickerPos.y = (int)mRawY - mIconHalfSize;
                            if (mTickerPos.x < 0) mTickerPos.x = 0;
                            if (mTickerPos.y < 0) mTickerPos.y = 0;
                            if (mTickerPos.x > mScreenWidth-mIconSize) mTickerPos.x = mScreenWidth-mIconSize;
                            if (mTickerPos.y > mScreenHeight-mIconSize) mTickerPos.y = mScreenHeight-mIconSize;
                            updatePosition();
                            return false;
                        }
                    } else {
                        // We have three basic gestures, one horizontal for switching through tasks and
                        // two vertical for dismissing tasks or making HALO fall asleep

                        int deltaX = (int)(mTickerLeft ? mRawX : mScreenWidth - mRawX);
                        int deltaY = (int)(mTickerPos.y - mRawY + mIconSize);
                        int horizontalThreshold = (int)(mIconSize * 1.5f);
                        int verticalThreshold = mIconSize;

                        // Switch icons
                        if (deltaX > horizontalThreshold) {
                            overlay = null;
                            mOverlay.setAlpha(0f);

                            deltaX -= horizontalThreshold;

                            if (mNotificationData != null && mNotificationData.size() > 0) {
                                int items = mNotificationData.size();

                                // This will be the lenght we are going to use
                                int indexLength = (mScreenWidth - mIconSize * 2) / items;

                                // Calculate index                                
                                mMarkerIndex = mTickerLeft ? (items - deltaX / indexLength) - 1 : (deltaX / indexLength);

                                // Watch out for margins!
                                if (mMarkerIndex >= items) mMarkerIndex = items - 1;
                                if (mMarkerIndex < 0) mMarkerIndex = 0;
                            }
                        } else if (Math.abs(deltaY) > verticalThreshold) {
                            mMarkerIndex = -1;
                            if (overlay == null) mHaloEffect.killTicker();

                            // Up & down gestures
                            int newDeltaY = Math.abs(deltaY) - verticalThreshold;
                            int percent = Math.min(newDeltaY * 100 / mIconSize, mIconSize);
                            boolean trigger = percent >= 100;
                            mOverlay.setAlpha((float)percent / 100);

                            if (deltaY > 0) {
                                overlay = mHaloDismiss;
                                if (!mMarkerTop && trigger && mHapticFeedback) mVibrator.vibrate(10);
                                mMarkerTop = trigger;
                                mMarkerBottom = false;
                            } else {
                                overlay = mTickerLeft ? mHaloBackL : mHaloBackR;
                                if (!mMarkerBottom && trigger && mHapticFeedback) mVibrator.vibrate(10);
                                mMarkerTop = false;
                                mMarkerBottom = trigger;
                            }
                        } else {
                            mMarkerIndex = -1;
                            mMarkerTop = false;
                            mMarkerBottom = false;
                        }

                        if (overlay != oldOverlay) {
                            mOverlay.setImageDrawable(overlay);
                            oldOverlay = overlay;
                        }

                        // If the marker index changed, tick
                        if (mMarkerIndex != oldIconIndex) {
                            oldIconIndex = mMarkerIndex;

                            // Make a tiny pop if not so many icons are present
                            if (mHapticFeedback && mNotificationData.size() < 10) mVibrator.vibrate(1);

                            try {
                                if (mMarkerIndex == -1) {
                                    mTaskIntent = null;
                                    resetIcons();
                                    tick(mLastNotificationEntry, mNotificationText, 250, true);

                                    // Ping to notify the user we're back where we started
                                    mHaloEffect.causePing(mPaintHoloBlue);
                                } else {
                                    setIcon(mMarkerIndex);

                                    NotificationData.Entry entry = mNotificationData.get(mMarkerIndex);
                                    String text = "";
                                    if (entry.notification.notification.tickerText != null) {
                                        text = entry.notification.notification.tickerText.toString();
                                    }
                                    tick(entry, text, 250, true);
                                    mTaskIntent = entry.floatingIntent;
                                }
                            } catch (Exception e) {
                                // IndexOutOfBoundsException
                            }
                        }

                        mHaloEffect.invalidate();
                    }
                    break;
            }
            return false;
        }
    };

    public void cleanUp() {
        // Remove pending tasks, if we can
        unscheduleSleep();
        mHandler.removeCallbacksAndMessages(null);
        // Kill callback
        mBar.getTicker().setUpdateEvent(null);
        // Flag tasker
        mBar.mHaloTaskerActive = false;
        // Kill the effect layer
        if (mHaloEffect != null) mWindowManager.removeView(mHaloEffect);
        // Remove resolver
        mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    class HaloEffect extends FrameLayout {
        private static final int PING_TIME = 1500;
        private static final int PULSE_TIME = 1500;

        private Context mContext;
        private Paint mPingPaint;
        private int pingRadius = 0;
        private int mPingX, mPingY;
        protected int pingMinRadius = 0;
        protected int pingMaxRadius = 0;
        private View mContentView;
        private RelativeLayout mTickerContent;
        private TextView mTextViewR, mTextViewL;
        private boolean mPingAllowed = true;
        private boolean mSkipThrough = false;

        private Bitmap mMarkerL, mMarkerT, mMarkerR, mMarkerB;
        private Bitmap mBigRed;
        private Bitmap mX;
        private Bitmap mPulse;
        private Paint mPulsePaint = new Paint();
        private Paint mMarkerPaint = new Paint();
        private Paint xPaint = new Paint();

        CustomObjectAnimator xAnimator = new CustomObjectAnimator();
        CustomObjectAnimator tickerAnimator = new CustomObjectAnimator();

        class CustomObjectAnimator {
            public ObjectAnimator animator;
            public void animate(ObjectAnimator newInstance, TimeInterpolator interpolator, AnimatorUpdateListener update) {
                // Terminate old instance, if present
                if (animator != null) animator.cancel();
                animator = newInstance;

                // Invalidate
                if (update == null) {
                    animator.addUpdateListener(new AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            invalidate();
                        }
                    });
                } else {
                    animator.addUpdateListener(update);
                }
                animator.setInterpolator(interpolator);
                animator.start();
            }
        }

        public HaloEffect(Context context) {
            super(context);

            mContext = context;
            setWillNotDraw(false);
            setDrawingCacheEnabled(false);

            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
            mContentView = inflater.inflate(R.layout.halo_content, null);
            mTickerContent = (RelativeLayout) mContentView.findViewById(R.id.ticker);
            mTextViewR = (TextView) mTickerContent.findViewById(R.id.bubble_r);
            mTextViewL = (TextView) mTickerContent.findViewById(R.id.bubble_l);
            mTextViewL.setAlpha(0);
            mTextViewR.setAlpha(0);

            mBigRed = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.halo_bigred);
            mX = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.halo_x);
            mPulse = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.halo_pulse1);
            mMarkerL = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.halo_marker_l);
            mMarkerT = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.halo_marker_t);
            mMarkerR = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.halo_marker_r);
            mMarkerB = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.halo_marker_b);

            mPulsePaint.setAntiAlias(true);
            mPulsePaint.setAlpha(0);   
            mMarkerPaint.setAntiAlias(true);
            mMarkerPaint.setAlpha(0);
            xPaint.setAntiAlias(true);
            xPaint.setAlpha(0);
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            onConfigurationChanged(null);
        }

        @Override
        public void onConfigurationChanged(Configuration newConfiguration) {
            // This will reset the initialization flag
            mInitialized = false;
            // Generate a new content bubble
            createBubble();
        }

        @Override
        protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
            super.onLayout (changed, left, top, right, bottom);
            // We have our effect-layer, now let's kickstart HALO
            initControl();
        }

        public void createBubble() {
            mTextViewL.setVisibility(mTickerLeft ? View.VISIBLE : View.GONE);
            mTextViewR.setVisibility(mTickerLeft ? View.GONE : View.VISIBLE);

            mContentView.measure(MeasureSpec.getSize(mContentView.getMeasuredWidth()), MeasureSpec.getSize(mContentView.getMeasuredHeight()));
            mContentView.layout(400, 400, 400, 400);
        }

        public void killTicker() {
            tickerAnimator.animate(ObjectAnimator.ofFloat(mTextViewL, "alpha", 0f).setDuration(250),
                    new DecelerateInterpolator(), null);
        }

        public void ticker(String tickerText, int startDuration, boolean skipThrough) {
            if (tickerText == null || tickerText.isEmpty()) {
                killTicker();
                return;
            }
            mSkipThrough = skipThrough;

            mTextViewR.setText(tickerText);
            mTextViewL.setText(tickerText);
            createBubble();

            float total = TICKER_HIDE_TIME + startDuration + 1000;
            PropertyValuesHolder tickerUpFrames = PropertyValuesHolder.ofKeyframe("alpha",
                    Keyframe.ofFloat(0f, mTextViewL.getAlpha()),
                    Keyframe.ofFloat(startDuration / total, 1),
                    Keyframe.ofFloat((TICKER_HIDE_TIME + startDuration) / total, 1),
                    Keyframe.ofFloat(1, 0));
            tickerAnimator.animate(ObjectAnimator.ofPropertyValuesHolder(mTextViewL, tickerUpFrames).setDuration((int)total),
                    new DecelerateInterpolator(), null);
        }

        public void causePing(Paint paint) {
            if ((!mPingAllowed && paint != mPaintHoloRed) && !mDoubleTap) return;

            mPingX = mTickerPos.x + mIconHalfSize;
            mPingY = mTickerPos.y + mIconHalfSize;

            mPingAllowed = false;
            mPingPaint = paint;

            int c = Color.argb(0xff, Color.red(paint.getColor()), Color.green(paint.getColor()), Color.blue(paint.getColor()));
            mPulsePaint.setColorFilter(new PorterDuffColorFilter(c, PorterDuff.Mode.SRC_IN));            

            CustomObjectAnimator pingAnimator = new CustomObjectAnimator();
            pingAnimator.animate(ObjectAnimator.ofInt(mPingPaint, "alpha", 200, 0).setDuration(PING_TIME),
                    new DecelerateInterpolator(), new AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            pingRadius = (int)((pingMaxRadius - pingMinRadius) *
                                    animation.getAnimatedFraction()) + pingMinRadius;
                            invalidate();
                        }});

            CustomObjectAnimator pulseAnimator = new CustomObjectAnimator();
            pulseAnimator.animate(ObjectAnimator.ofInt(mPulsePaint, "alpha", 100, 0).setDuration(PULSE_TIME),
                    new AccelerateInterpolator(), null);

            // prevent ping spam            
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    mPingAllowed = true;
                }}, 3000);
        }

        public void intro() {
            xAnimator.animate(ObjectAnimator.ofInt(xPaint, "alpha", 255).setDuration(PING_TIME / 3),
                    new DecelerateInterpolator(), null);
        }

        public void outro() {
            xAnimator.animate(ObjectAnimator.ofInt(xPaint, "alpha", 0).setDuration(PING_TIME / 3),
                    new AccelerateInterpolator(), null);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int state;

            // Ping
            if (mPingPaint != null) {
                canvas.drawCircle(mPingX, mPingY, pingRadius, mPingPaint);
                int w = mPulse.getWidth() + (int)(mIconSize * 1.2f * mPulsePaint.getAlpha() / 100);
                Rect r = new Rect(mPingX - w / 2, mPingY - w / 2, mPingX + w / 2, mPingY + w / 2);
                canvas.drawBitmap(mPulse, null, r, mPulsePaint);
            }

            // Content
            state = canvas.save();

            int y = mTickerPos.y - mTickerContent.getMeasuredHeight() + (int)(mIconSize * 0.2);
            if (!mSkipThrough && mNumber.getVisibility() != View.VISIBLE) y += (int)(mIconSize * 0.15);
            if (y < 0) y = 0;

            int x = mTickerPos.x + (int)(mIconSize * 0.92f);
            int c = mTickerContent.getMeasuredWidth();
            if (x > mScreenWidth - c) {
                x = mScreenWidth - c;
                if (mTickerPos.x > mScreenWidth - (int)(mIconSize * 1.5f) ) {
                    x = mTickerPos.x - c + (int)(mIconSize * 0.08f);
                }
            }

            state = canvas.save();
            canvas.translate(x, y);
            mTextViewR.setAlpha(mTextViewL.getAlpha());
            mContentView.draw(canvas);
            canvas.restoreToCount(state);

            // X
            float fraction = 1 - ((float)xPaint.getAlpha()) / 255;
            int killyPos = (int)(mKillY - mBigRed.getWidth() / 2 - mIconSize * fraction);
            canvas.drawBitmap(mBigRed, mKillX - mBigRed.getWidth() / 2, killyPos, xPaint);

            // Marker
            if (mDoubleTap) {
                if (y > 0 && mNotificationData != null && mNotificationData.size() > 0) {
                    int pulseY = mTickerPos.y + mIconHalfSize - mMarkerR.getHeight() / 2;
                    int items = mNotificationData.size();
                    int indexLength = (mScreenWidth - mIconSize * 2) / items;

                    for (int i = 0; i < items; i++) {
                        float pulseX = mTickerLeft ? (mIconSize * 1.5f + indexLength * i)
                                : (mScreenWidth - mIconSize * 1.5f - indexLength * i - mMarkerR.getWidth());
                        boolean markerState = mTickerLeft ? mMarkerIndex >= 0 && i < items-mMarkerIndex : i <= mMarkerIndex;
                        mMarkerPaint.setAlpha(markerState ? 255 : 100);
                        canvas.drawBitmap(mTickerLeft ? mMarkerR : mMarkerL, pulseX, pulseY, mMarkerPaint);
                    }
                }

                int xPos = mTickerPos.x + mIconHalfSize - mMarkerT.getWidth() / 2;
                int yTop = (int)(mTickerPos.y + mIconHalfSize - mIconSize * 1.2f - mMarkerT.getHeight() / 2);
                int yButtom = (int)(mTickerPos.y + mIconHalfSize + mIconSize * 1.2f - mMarkerT.getHeight() / 2);
                mMarkerPaint.setAlpha(mMarkerTop ? 255 : 100);
                canvas.drawBitmap(mMarkerT, xPos, yTop, mMarkerPaint);
                mMarkerPaint.setAlpha(mMarkerBottom ? 255 : 100);
                canvas.drawBitmap(mMarkerB, xPos, yButtom, mMarkerPaint);
            }
        }
    }

    public WindowManager.LayoutParams getWMParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.LEFT|Gravity.TOP;
        return lp;
    }

    void tick(NotificationData.Entry entry, String text, int duration, boolean skipThrough) {
        if (entry == null) {
            mIcon.setImageDrawable(null);
            mNumber.setVisibility(View.GONE);
            mContentIntent = null;
            mCurrentNotficationEntry = null;
            return;
        }

        StatusBarNotification notification = entry.notification;
        Notification n = notification.notification;

        // Deal with the intent
        mContentIntent = entry.floatingIntent;
        mCurrentNotficationEntry = entry;

        // set the avatar
        mIcon.setImageDrawable(new BitmapDrawable(mContext.getResources(), entry.roundIcon));

        // Set Number
        if (n.number > 0) {
            mNumber.setVisibility(View.VISIBLE);
            mNumber.setText((n.number < 100) ? String.valueOf(n.number) : "99+");
        } else {
            mNumber.setVisibility(View.GONE);
        }

        // Set text
        mHaloEffect.ticker(text, duration, skipThrough);
    }

    // This is the android ticker callback
    public void updateTicker(StatusBarNotification notification, String text) {

        INotificationManager nm = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        boolean blacklisted = false; // default off
        try {
            blacklisted = nm.isPackageHaloBlacklisted(notification.pkg);
        } catch (android.os.RemoteException ex) {
            // this does not bode well
        }

        for (int i = 0; i < mNotificationData.size(); i++) {
            NotificationData.Entry entry = mNotificationData.get(i);

            if (entry.notification == notification) {

                // No intent, no tick ...
                if (entry.notification.notification.contentIntent == null) return;

                mIsNotificationNew = true;
                if (mLastNotificationEntry != null && notification == mLastNotificationEntry.notification) {
                    // Ok, this is the same notification
                    // Let's give it a chance though, if the text has changed we allow it
                    mIsNotificationNew = !mNotificationText.equals(text);
                }

                if (mIsNotificationNew) {
                    mNotificationText = text;
                    mLastNotificationEntry = entry;

                    if (!blacklisted) {
                        tick(entry, text, mHidden ? 1500 : 1000, false);

                        // Wake up and snap               
                        wakeUp(!mDoubleTap && mIsNotificationNew);
                        if (!isBeingDragged && !mDoubleTap) snapToSide(true, SLEEP_DELAY_DAYDREAMING);
                    }
                }
                break;
            }
        }
    }
}
