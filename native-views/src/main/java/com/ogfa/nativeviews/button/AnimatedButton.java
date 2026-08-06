package com.ogfa.nativeviews.button;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.ogfa.nativeviews.audio.NativeViewsSoundPlayer;
import com.ogfa.nativeviews.button.internal.PressAnimation;
import com.ogfa.nativeviews.button.internal.Region;

import java.util.ArrayList;
import java.util.Iterator;

public class AnimatedButton {
    private static final float SHRINK_SCALE_DEFAULT = 0.96f;
    public final ArrayList<ViewLayer> viewLayers;
    private final int left;
    private final int top;
    private float SHRINK_SCALE;

    Paint paint = new Paint();
    public RectF rectF ;
    RectF rectF_press ;
    private int mWidth;
    private int mHeight;
    private boolean mIsPressed;
    private OnClickListener mClickListener;
    private OnLongClickListener mLongClickListener;
    private Context context;
    private String id;

    private boolean isAnimationOn = false;
    private PressAnimation pressAnimation = null;
    private boolean isClickable;
    private boolean isLongClickable;

    private Region buttonRegion;
    private RectF dynamicRectF;

    private long lastDownTime = 0;
    private Runnable proxySoundPlay;

    public int getLeft() {
        return left;
    }

    public int getTop() {
        return top;
    }

    public String getId() {
        return id;
    }

    // Primary constructor
    private AnimatedButton(Context context, OnClickListener clickListener, OnLongClickListener longClickListener, String id, int width, int height, int left, int top, boolean isClickable, boolean isLongClickable, float shrink, ArrayList<ViewLayer> viewLayers, Runnable proxySoundPlay) {
        this.context = context;
        this.id = id;
        this.isClickable = isClickable;
        this.isLongClickable = isLongClickable;
        this.SHRINK_SCALE = shrink;
        this.proxySoundPlay = proxySoundPlay;
        if (proxySoundPlay == null) {
            NativeViewsSoundPlayer.preload(context);
        }
        this.left = left;
        this.top = top;
        mClickListener = clickListener;
        mLongClickListener = longClickListener;
        mWidth = width;
        mHeight = height;
        paint.setAntiAlias(true);
        paint.setDither(true);
        rectF = new RectF(left, top, mWidth + left, mHeight + top);
        rectF_press = getPressRect(left, top);
        buttonRegion = new Region(left, left + mWidth, top, top + mHeight, id);
        this.viewLayers = viewLayers;
        this.dynamicRectF = null;
    }

    public static void releaseLottieResources(ArrayList<AnimatedButton> buttonViewAnimatorList) {
        for (int i=0; i < buttonViewAnimatorList.size(); i++){
            for (ViewLayer viewLayer:buttonViewAnimatorList.get(i).viewLayers) {
                viewLayer.clear();
            }
        }
    }

    /**
     * Builder class for ButtonViewAnimator.
     */
    public static class Builder {
        private RectF dynamicRectF;
        private Context context;
        private OnClickListener clickListener;
        private OnLongClickListener longClickListener;
        private String id;
        private int width;
        private int height;
        private float left;
        private float top;
        private boolean isClickable;
        private boolean isLongClickable;
        private float shrink = SHRINK_SCALE_DEFAULT; // default value

        private Runnable proxySoundPlay;
        private ArrayList<ViewLayer> viewLayers;

        public Builder(Context context, String id, ArrayList<ViewLayer> viewLayers, RectF rectF) {
            this.context = context;
            this.id = id;
            this.viewLayers = viewLayers;
            this.width = (int) (rectF.right - rectF.left);
            this.height = (int) (rectF.bottom - rectF.top);
            this.left = rectF.left;
            this.top = rectF.top;
        }

        /**
         * Creates a simple single-bitmap button with explicit runtime bounds.
         */
        public Builder(Context context, String id, Bitmap bitmap, RectF rectF) {
            this(context, id, createBitmapLayers(bitmap, rectF), rectF);
        }

        /**
         * Builds the touch bounds from the largest BitmapView and a host-bound Position.
         *
         * <p>The bitmap dimensions are treated as Figma-space dimensions, matching
         * {@link BitmapView#get(android.graphics.Bitmap, Position)}.</p>
         */
        public Builder(
                Context context,
                String id,
                ArrayList<ViewLayer> viewLayers,
                Position position
        ) {
            this(context, id, viewLayers, resolveBitmapBounds(viewLayers, position));
        }

        /**
         * Creates a simple single-bitmap button whose bounds are evaluated from Position.
         */
        public Builder(Context context, String id, Bitmap bitmap, Position position) {
            this(context, id, createBitmapLayers(bitmap, position), position);
        }

        public Builder(Context context, String id, ArrayList<ViewLayer> viewLayers, RectF rectF, RectF dynamicRectF) {
            this.context = context;
            this.id = id;
            this.viewLayers = viewLayers;
            this.width = (int) (rectF.right - rectF.left);
            this.height = (int) (rectF.bottom - rectF.top);
            this.left = rectF.left;
            this.top = rectF.top;
            this.dynamicRectF = dynamicRectF;
        }

        private static ArrayList<ViewLayer> createBitmapLayers(Bitmap bitmap, RectF rectF) {
            if (bitmap == null) {
                throw new IllegalArgumentException("Button bitmap cannot be null.");
            }
            if (rectF == null) {
                throw new IllegalArgumentException("Button bounds cannot be null.");
            }

            ArrayList<ViewLayer> layers = new ArrayList<>();
            layers.add(BitmapView.get(bitmap, rectF));
            return layers;
        }

        private static ArrayList<ViewLayer> createBitmapLayers(
                Bitmap bitmap,
                Position position
        ) {
            if (bitmap == null) {
                throw new IllegalArgumentException("Button bitmap cannot be null.");
            }
            if (position == null) {
                throw new IllegalArgumentException("Position cannot be null.");
            }

            ArrayList<ViewLayer> layers = new ArrayList<>();
            layers.add(BitmapView.get(bitmap, position));
            return layers;
        }

        private static RectF resolveBitmapBounds(
                ArrayList<ViewLayer> viewLayers,
                Position position
        ) {
            if (position == null) {
                throw new IllegalArgumentException("Position cannot be null.");
            }
            if (viewLayers == null || viewLayers.isEmpty()) {
                throw new IllegalArgumentException(
                        "At least one ViewLayer is required to calculate button bounds."
                );
            }

            RectF largestBounds = null;
            float largestArea = -1f;

            for (ViewLayer viewLayer : viewLayers) {
                if (viewLayer instanceof BitmapView) {
                    BitmapView bitmapView = (BitmapView) viewLayer;
                    if (bitmapView.bitmap != null) {
                        RectF candidateBounds = position.toRectF(bitmapView.bitmap);
                        float candidateArea =
                                candidateBounds.width() * candidateBounds.height();
                        if (candidateArea > largestArea) {
                            largestArea = candidateArea;
                            largestBounds = candidateBounds;
                        }
                    }
                }
            }

            if (largestBounds != null) {
                return largestBounds;
            }

            throw new IllegalArgumentException(
                    "Position-based Builder requires at least one BitmapView. "
                            + "Use the RectF constructor for buttons without a bitmap layer."
            );
        }

        public Builder setClickListener(OnClickListener clickListener) {
            this.clickListener = clickListener;
            this.isClickable = true;
            return this;
        }
        public Builder setClickListener(OnClickListener clickListener,boolean value){
            this.clickListener = clickListener;
            this.isClickable = value;
            return this;
        }

        public Builder setOnLongClickListener(OnLongClickListener onClickLongListener, boolean value){
            this.longClickListener = onClickLongListener;
            this.isLongClickable = value;
            return this;
        }
        public Builder setShrink(float shrink) {
            this.shrink = shrink;
            return this;
        }


        public Builder setProxySoundPlay(Runnable proxySoundPlay) {
            this.proxySoundPlay = proxySoundPlay;
            return this;
        }


        public AnimatedButton build() {
            AnimatedButton animator = new AnimatedButton(
                    context, clickListener, longClickListener, id,
                    width, height, (int) left, (int) top,
                    isClickable, isLongClickable, shrink, viewLayers, proxySoundPlay
            );

            if (dynamicRectF!= null){
                animator.dynamicRectF = this.dynamicRectF; // ✅ assign here
            }

            return animator;
        }
    }



    private RectF getPressRect(float left, float top) {

        left = left+(((1-SHRINK_SCALE)/2)*mWidth);
        top = top+(((1-SHRINK_SCALE)/2)*mHeight);

        return new RectF(left, top, left + (SHRINK_SCALE*mWidth), top + (SHRINK_SCALE*mHeight));
    }

    public void onDraw(Canvas canvas) {

        if (!isAnimationOn) {
            if (mIsPressed || (System.currentTimeMillis() - lastDownTime) < 250) {
                canvas.save();
                float midPointX = rectF.left + mWidth/2f;
                float midPointY = rectF.top + mHeight/2f;
                canvas.scale(SHRINK_SCALE, SHRINK_SCALE, midPointX, midPointY);
                drawAllView(canvas);
                canvas.restore();
            } else {
                drawAllView(canvas);
            }
        }else {
            if (pressAnimation != null){

                pressAnimation.applyAnimationPressed(canvas);
                drawAllView(canvas);
                pressAnimation.restoreAnimationPressed(canvas);

                if (pressAnimation.isAnimationFinished()){
                    pressAnimation = null;
                    isAnimationOn = false;
                }

            }else {
                drawAllView(canvas);
            }
        }

    }

    private void drawAllView(Canvas canvas){
        for (ViewLayer viewLayer: viewLayers) {
            viewLayer.onDraw(canvas);
        }
    }



////////////////////////////////////////////////// Util method //////////////////////////////////////////////////////////////////////////////////////////////
    public static void Draw(Canvas canvas, ArrayList<AnimatedButton> buttonViewAnimators) {
        try {
            Iterator<AnimatedButton> iterator = buttonViewAnimators.iterator();

            while (iterator.hasNext()) {
                AnimatedButton buttonViewAnimator = iterator.next();
                buttonViewAnimator.onDraw(canvas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void visibleDraw(Canvas canvas, ArrayList<AnimatedButton> buttonViewAnimators, View scrollView) {
        try {

            ArrayList<AnimatedButton> buttonViewAnimatorsShow = new ArrayList<>();
            getVisible(buttonViewAnimators,buttonViewAnimatorsShow,scrollView);
            Draw(canvas,buttonViewAnimatorsShow);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void getVisible(ArrayList<AnimatedButton> buttonViewAnimators,  ArrayList<AnimatedButton> buttonViewAnimatorsShow, View view) {
        try {
            buttonViewAnimatorsShow.clear();
            // Get the visible rectangle of the ScrollView
            Rect scrollViewVisibleRect = new Rect();
            Rect buttonRect = new Rect();

            view.getLocalVisibleRect(scrollViewVisibleRect);

            Iterator<AnimatedButton> iterator = buttonViewAnimators.iterator();

            while (iterator.hasNext()) {
                AnimatedButton buttonViewAnimator = iterator.next();

                // Check if the buttonViewAnimator's rect intersects with the ScrollView's visible rect
                buttonViewAnimator.rectF.round(buttonRect);
                if (Rect.intersects(scrollViewVisibleRect, buttonRect)) {
                    buttonViewAnimatorsShow.add(buttonViewAnimator);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addButton(ArrayList<AnimatedButton> buttonViewAnimators, AnimatedButton buttonViewAnimator) {
        buttonViewAnimators.add(buttonViewAnimator);
    }


    public static void removeButton(String id, ArrayList<AnimatedButton> buttonViewAnimatorArrayList) {
        for (int i=0; i<buttonViewAnimatorArrayList.size(); i++) {
            if (buttonViewAnimatorArrayList.get(i).id.equals(id)){
                for (ViewLayer viewLayer:buttonViewAnimatorArrayList.get(i).viewLayers) {
                    viewLayer.clear();
                }
                buttonViewAnimatorArrayList.remove(i);
                return;
            }
        }
    }

    public static AnimatedButton findButtonById(String id, ArrayList<AnimatedButton> buttonViewAnimatorArrayList) {
        for (AnimatedButton buttonViewAnimator : buttonViewAnimatorArrayList) {
            if (buttonViewAnimator.id.equals(id)) {
                return buttonViewAnimator;
            }
        }
        return null; // Return null if no button with the specified id is found
    }
////////////////////////////////////////////////// Util method //////////////////////////////////////////////////////////////////////////////////////////////






////////////////////////////////////////////////// Touch methods //////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean HandleTouch(MotionEvent event, ArrayList<AnimatedButton> buttonViewAnimatorList) {

        try {

            for (int i=buttonViewAnimatorList.size()-1; i >= 0; i--){
                boolean isHandled = buttonViewAnimatorList.get(i).onTouchEvent(event);
                if (isHandled){
                    return true;
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return false;

    }

    public static void HandleTouchScrollChanged( ArrayList<AnimatedButton> buttonViewAnimatorList) {
        try {
            for (AnimatedButton buttonViewAnimator: buttonViewAnimatorList) {
                buttonViewAnimator.mIsPressed = false;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public boolean onTouchEvent(MotionEvent event) {
        if (!isClickable && !isLongClickable){
            return false;
        }
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                return checkIsAnyRegionClicked(x,y , false,true);
            case MotionEvent.ACTION_MOVE:
                return checkIsAnyRegionClicked(x,y , false,false);
            case  MotionEvent.ACTION_UP:
                return checkIsAnyRegionClicked(x,y,true,false);

        }
        return false;
    }

    private boolean checkIsAnyRegionClicked(float x, float y , boolean isUp,boolean isDown) {

        if (isUp){
            if (buttonRegion.isRegionClicked(x,y)){
                if (proxySoundPlay != null){
                    proxySoundPlay.run();
                }else {
                    NativeViewsSoundPlayer.playButtonSound(context);
                }

                if (isLongClickable && (System.currentTimeMillis()-lastDownTime) > 500){
                    mLongClickListener.onLongClick(buttonRegion.id);
                }else {
                    mClickListener.onClick(buttonRegion.id);
                }

                mIsPressed = false;
                //do up animation
                playUpAnim();

                return true;
            }
        }

        if (isDown && !mIsPressed && (System.currentTimeMillis()-lastDownTime) > 300){
            if (buttonRegion.regionClickedDown(x,y)){
                mIsPressed = true;
                //do down animation
                playDownAnim();

                return true;
            }

        }else if (isDown && mIsPressed ){
            if (buttonRegion.regionClickedDown(x,y)){
                return true;
            }
        }

        if (!isUp && !isDown && mIsPressed){ //move
            if (buttonRegion.regionClickedMove(x,y)){
                return true;
            }else {
                mIsPressed = false;   //click slide away from region
                //do up animation
                playUpAnim();

                return false;
            }
        }

        return false;
    }

    private void playDownAnim() {
        if (isAnimationOn){
            new Thread(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(this::playUpAnim);

            }).start();
            return;
        }

        lastDownTime = System.currentTimeMillis();
        isAnimationOn = true;
        float midPointX = rectF.left + mWidth/2f;
        float midPointY = rectF.top + mHeight/2f;
        pressAnimation = new PressAnimation(true, System.currentTimeMillis(), 130, midPointX, midPointY, SHRINK_SCALE);

    }

    public void animateToPositionWithValueAnimator(float targetLeft, float targetTop, long duration, View parentView, Runnable onComplete) {
        float startX = rectF.left;
        float startY = rectF.top;

        // Also track dynamicRectF start position if it exists
        float dynStartX = (dynamicRectF != null) ? dynamicRectF.left : startX;
        float dynStartY = (dynamicRectF != null) ? dynamicRectF.top : startY;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(duration);
        animator.setInterpolator(new LinearInterpolator());

        animator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            float currentX = startX + (targetLeft - startX) * fraction;
            float currentY = startY + (targetTop - startY) * fraction;

            rectF.offsetTo(currentX, currentY);
            rectF_press = getPressRect(currentX, currentY);
            buttonRegion.updateRegion((int) currentX, (int)(currentX + mWidth), (int) currentY, (int)(currentY + mHeight));

            // 💡 Animate dynamicRectF alongside rectF
            if (dynamicRectF != null) {
                float dynX = dynStartX + (targetLeft - startX) * fraction;
                float dynY = dynStartY + (targetTop - startY) * fraction;
                dynamicRectF.offsetTo(dynX, dynY);
            }

            updateViewLayerPositions();

            if (parentView != null) {
                parentView.invalidate();
            }
        });

        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (onComplete != null) onComplete.run();
            }
        });

        animator.start();
    }


    private void updateViewLayerPositions() {
        RectF currentRect = new RectF(rectF);

        for (ViewLayer viewLayer : viewLayers) {
            if (viewLayer instanceof DynamicView) {
                // ✅ Pass dynamicRectF if it exists, else fallback to rectF
                viewLayer.setRect(dynamicRectF != null ? new RectF(dynamicRectF) : currentRect);
            } else {
                // ✅ Pass current animated rectF to all other views
                viewLayer.setRect(currentRect);
            }
        }
    }


    private void playUpAnim() {
        if (isAnimationOn || (System.currentTimeMillis()-lastDownTime) < 150){
            new Thread(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(this::playUpAnim);

            }).start();
            return;
        }

        isAnimationOn = true;
        float midPointX = rectF.left + mWidth/2f;
        float midPointY = rectF.top + mHeight/2f;
        pressAnimation = new PressAnimation(false, System.currentTimeMillis(), 130, midPointX, midPointY, SHRINK_SCALE);

    }


    public static interface OnClickListener{
        void onClick(String id);
    }

    public static interface OnLongClickListener {
        void onLongClick(String id);
    }


////////////////////////////////////////////////// Touch methods //////////////////////////////////////////////////////////////////////////////////////////////


/*
  example :

          Position position = new Position(
                  this,
                  Position.HorizontalMarginFrom.LEFT,
                  Position.VerticalMarginFrom.TOP,
                  48f,
                  450f
          );
          ArrayList<ViewLayer> viewLayers = new ArrayList<>();
          viewLayers.add(BitmapView.get(freeCoin, position));
          viewLayers.add(LottieView.get(this.getContext(), "emoji_lottie_1", position));
          AnimatedButton.addButton(buttonViewComplexAnimatorArrayList, new AnimatedButton.Builder(this.getContext(), FREE_COIN, viewLayers, position)
                  .setClickListener(this)
                  .setProxySoundPlay(this::playPopupSound)
                  .build());
 */

}
