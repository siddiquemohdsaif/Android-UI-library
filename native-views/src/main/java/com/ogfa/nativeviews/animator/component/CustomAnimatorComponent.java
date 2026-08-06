package com.ogfa.nativeviews.animator.component;

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
import com.ogfa.nativeviews.animator.component.internal.PressAnimation;
import com.ogfa.nativeviews.animator.component.internal.Region;
import com.ogfa.nativeviews.animator.component.layer.BitmapLayer;
import com.ogfa.nativeviews.animator.component.layer.ComponentLayer;
import com.ogfa.nativeviews.animator.component.layer.DynamicLayer;
import com.ogfa.nativeviews.component.Position;

import java.util.ArrayList;
import java.util.Iterator;

public class CustomAnimatorComponent {
    private static final float SHRINK_SCALE_DEFAULT = 0.96f;
    public final ArrayList<ComponentLayer> layers;
    private final int left;
    private final int top;
    private float SHRINK_SCALE;

    Paint paint = new Paint();
    public RectF bounds ;
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

    private Region componentRegion;
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

    public RectF getBounds() {
        return new RectF(bounds);
    }

    // Primary constructor
    private CustomAnimatorComponent(Context context, OnClickListener clickListener, OnLongClickListener longClickListener, String id, int width, int height, int left, int top, boolean isClickable, boolean isLongClickable, float shrink, ArrayList<ComponentLayer> layers, Runnable proxySoundPlay) {
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
        bounds = new RectF(left, top, mWidth + left, mHeight + top);
        rectF_press = getPressRect(left, top);
        componentRegion = new Region(left, left + mWidth, top, top + mHeight, id);
        this.layers = layers;
        this.dynamicRectF = null;
    }

    public static void releaseResources(ArrayList<CustomAnimatorComponent> buttonViewAnimatorList) {
        for (int i=0; i < buttonViewAnimatorList.size(); i++){
            for (ComponentLayer viewLayer:buttonViewAnimatorList.get(i).layers) {
                viewLayer.release();
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
        private ArrayList<ComponentLayer> layers;

        public Builder(Context context, String id, ArrayList<ComponentLayer> layers, RectF bounds) {
            this.context = context;
            this.id = id;
            this.layers = layers;
            this.width = (int) (bounds.right - bounds.left);
            this.height = (int) (bounds.bottom - bounds.top);
            this.left = bounds.left;
            this.top = bounds.top;
        }

        /**
         * Creates a simple single-bitmap component with explicit runtime bounds.
         */
        public Builder(Context context, String id, Bitmap bitmap, RectF bounds) {
            this(context, id, createBitmapLayers(bitmap, bounds), bounds);
        }

        /**
         * Builds the touch bounds from the largest BitmapLayer and a host-bound Position.
         *
         * <p>The bitmap dimensions are treated as Figma-space dimensions, matching
         * {@link BitmapLayer#create(android.graphics.Bitmap, Position)}.</p>
         */
        public Builder(
                Context context,
                String id,
                ArrayList<ComponentLayer> layers,
                Position position
        ) {
            this(context, id, layers, resolveBitmapBounds(layers, position));
        }

        /**
         * Creates a simple single-bitmap component whose bounds are evaluated from Position.
         */
        public Builder(Context context, String id, Bitmap bitmap, Position position) {
            this(context, id, createBitmapLayers(bitmap, position), position);
        }

        public Builder(Context context, String id, ArrayList<ComponentLayer> layers, RectF bounds, RectF dynamicRectF) {
            this.context = context;
            this.id = id;
            this.layers = layers;
            this.width = (int) (bounds.right - bounds.left);
            this.height = (int) (bounds.bottom - bounds.top);
            this.left = bounds.left;
            this.top = bounds.top;
            this.dynamicRectF = dynamicRectF;
        }

        private static ArrayList<ComponentLayer> createBitmapLayers(Bitmap bitmap, RectF bounds) {
            if (bitmap == null) {
                throw new IllegalArgumentException("Component bitmap cannot be null.");
            }
            if (bounds == null) {
                throw new IllegalArgumentException("Component bounds cannot be null.");
            }

            ArrayList<ComponentLayer> layers = new ArrayList<>();
            layers.add(BitmapLayer.create(bitmap, bounds));
            return layers;
        }

        private static ArrayList<ComponentLayer> createBitmapLayers(
                Bitmap bitmap,
                Position position
        ) {
            if (bitmap == null) {
                throw new IllegalArgumentException("Component bitmap cannot be null.");
            }
            if (position == null) {
                throw new IllegalArgumentException("Position cannot be null.");
            }

            ArrayList<ComponentLayer> layers = new ArrayList<>();
            layers.add(BitmapLayer.create(bitmap, position));
            return layers;
        }

        private static RectF resolveBitmapBounds(
                ArrayList<ComponentLayer> layers,
                Position position
        ) {
            if (position == null) {
                throw new IllegalArgumentException("Position cannot be null.");
            }
            if (layers == null || layers.isEmpty()) {
                throw new IllegalArgumentException(
                        "At least one ComponentLayer is required to calculate component bounds."
                );
            }

            RectF largestBounds = null;
            float largestArea = -1f;

            for (ComponentLayer viewLayer : layers) {
                if (viewLayer instanceof BitmapLayer) {
                    BitmapLayer bitmapView = (BitmapLayer) viewLayer;
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
                    "Position-based Builder requires at least one BitmapLayer. "
                            + "Use the RectF constructor for components without a bitmap layer."
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
        public Builder setPressScale(float shrink) {
            this.shrink = shrink;
            return this;
        }


        public Builder setSoundAction(Runnable proxySoundPlay) {
            this.proxySoundPlay = proxySoundPlay;
            return this;
        }


        public CustomAnimatorComponent build() {
            CustomAnimatorComponent animator = new CustomAnimatorComponent(
                    context, clickListener, longClickListener, id,
                    width, height, (int) left, (int) top,
                    isClickable, isLongClickable, shrink, layers, proxySoundPlay
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

    public void draw(Canvas canvas) {

        if (!isAnimationOn) {
            if (mIsPressed || (System.currentTimeMillis() - lastDownTime) < 250) {
                canvas.save();
                float midPointX = bounds.left + mWidth/2f;
                float midPointY = bounds.top + mHeight/2f;
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
        for (ComponentLayer viewLayer: layers) {
            viewLayer.draw(canvas);
        }
    }



////////////////////////////////////////////////// Util method //////////////////////////////////////////////////////////////////////////////////////////////
    public static void draw(Canvas canvas, ArrayList<CustomAnimatorComponent> buttonViewAnimators) {
        try {
            Iterator<CustomAnimatorComponent> iterator = buttonViewAnimators.iterator();

            while (iterator.hasNext()) {
                CustomAnimatorComponent buttonViewAnimator = iterator.next();
                buttonViewAnimator.draw(canvas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void drawVisible(Canvas canvas, ArrayList<CustomAnimatorComponent> buttonViewAnimators, View scrollView) {
        try {

            ArrayList<CustomAnimatorComponent> buttonViewAnimatorsShow = new ArrayList<>();
            getVisible(buttonViewAnimators,buttonViewAnimatorsShow,scrollView);
            draw(canvas,buttonViewAnimatorsShow);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void getVisible(ArrayList<CustomAnimatorComponent> buttonViewAnimators,  ArrayList<CustomAnimatorComponent> buttonViewAnimatorsShow, View view) {
        try {
            buttonViewAnimatorsShow.clear();
            // Get the visible rectangle of the ScrollView
            Rect scrollViewVisibleRect = new Rect();
            Rect buttonRect = new Rect();

            view.getLocalVisibleRect(scrollViewVisibleRect);

            Iterator<CustomAnimatorComponent> iterator = buttonViewAnimators.iterator();

            while (iterator.hasNext()) {
                CustomAnimatorComponent buttonViewAnimator = iterator.next();

                // Check if the buttonViewAnimator's rect intersects with the ScrollView's visible rect
                buttonViewAnimator.bounds.round(buttonRect);
                if (Rect.intersects(scrollViewVisibleRect, buttonRect)) {
                    buttonViewAnimatorsShow.add(buttonViewAnimator);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addComponent(ArrayList<CustomAnimatorComponent> buttonViewAnimators, CustomAnimatorComponent buttonViewAnimator) {
        buttonViewAnimators.add(buttonViewAnimator);
    }


    public static void removeComponent(String id, ArrayList<CustomAnimatorComponent> buttonViewAnimatorArrayList) {
        for (int i=0; i<buttonViewAnimatorArrayList.size(); i++) {
            if (buttonViewAnimatorArrayList.get(i).id.equals(id)){
                for (ComponentLayer viewLayer:buttonViewAnimatorArrayList.get(i).layers) {
                    viewLayer.release();
                }
                buttonViewAnimatorArrayList.remove(i);
                return;
            }
        }
    }

    public static CustomAnimatorComponent findComponentById(String id, ArrayList<CustomAnimatorComponent> buttonViewAnimatorArrayList) {
        for (CustomAnimatorComponent buttonViewAnimator : buttonViewAnimatorArrayList) {
            if (buttonViewAnimator.id.equals(id)) {
                return buttonViewAnimator;
            }
        }
        return null; // Return null if no component with the specified id is found
    }
////////////////////////////////////////////////// Util method //////////////////////////////////////////////////////////////////////////////////////////////






////////////////////////////////////////////////// Touch methods //////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean handleTouch(MotionEvent event, ArrayList<CustomAnimatorComponent> buttonViewAnimatorList) {

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

    public static void handleTouchScrollChanged( ArrayList<CustomAnimatorComponent> buttonViewAnimatorList) {
        try {
            for (CustomAnimatorComponent buttonViewAnimator: buttonViewAnimatorList) {
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
            if (componentRegion.isRegionClicked(x,y)){
                if (proxySoundPlay != null){
                    proxySoundPlay.run();
                }else {
                    NativeViewsSoundPlayer.playButtonSound(context);
                }

                if (isLongClickable && (System.currentTimeMillis()-lastDownTime) > 500){
                    mLongClickListener.onLongClick(componentRegion.id);
                }else {
                    mClickListener.onClick(componentRegion.id);
                }

                mIsPressed = false;
                //do up animation
                playUpAnim();

                return true;
            }
        }

        if (isDown && !mIsPressed && (System.currentTimeMillis()-lastDownTime) > 300){
            if (componentRegion.regionClickedDown(x,y)){
                mIsPressed = true;
                //do down animation
                playDownAnim();

                return true;
            }

        }else if (isDown && mIsPressed ){
            if (componentRegion.regionClickedDown(x,y)){
                return true;
            }
        }

        if (!isUp && !isDown && mIsPressed){ //move
            if (componentRegion.regionClickedMove(x,y)){
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
        float midPointX = bounds.left + mWidth/2f;
        float midPointY = bounds.top + mHeight/2f;
        pressAnimation = new PressAnimation(true, System.currentTimeMillis(), 130, midPointX, midPointY, SHRINK_SCALE);

    }

    public void animateToPositionWithValueAnimator(float targetLeft, float targetTop, long duration, View parentView, Runnable onComplete) {
        float startX = bounds.left;
        float startY = bounds.top;

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

            bounds.offsetTo(currentX, currentY);
            rectF_press = getPressRect(currentX, currentY);
            componentRegion.updateRegion((int) currentX, (int)(currentX + mWidth), (int) currentY, (int)(currentY + mHeight));

            // 💡 Animate dynamicRectF alongside bounds
            if (dynamicRectF != null) {
                float dynX = dynStartX + (targetLeft - startX) * fraction;
                float dynY = dynStartY + (targetTop - startY) * fraction;
                dynamicRectF.offsetTo(dynX, dynY);
            }

            updateLayerPositions();

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


    private void updateLayerPositions() {
        RectF currentRect = new RectF(bounds);

        for (ComponentLayer viewLayer : layers) {
            if (viewLayer instanceof DynamicLayer) {
                // ✅ Pass dynamicRectF if it exists, else fallback to bounds
                viewLayer.setBounds(dynamicRectF != null ? new RectF(dynamicRectF) : currentRect);
            } else {
                // ✅ Pass current animated bounds to all other views
                viewLayer.setBounds(currentRect);
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
        float midPointX = bounds.left + mWidth/2f;
        float midPointY = bounds.top + mHeight/2f;
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
          ArrayList<ComponentLayer> layers = new ArrayList<>();
          layers.add(BitmapLayer.create(freeCoin, position));
          layers.add(LottieLayer.create(this.getContext(), "emoji_lottie_1", position));
          CustomAnimatorComponent.addComponent(buttonViewComplexAnimatorArrayList, new CustomAnimatorComponent.Builder(this.getContext(), FREE_COIN, layers, position)
                  .setClickListener(this)
                  .setSoundAction(this::playPopupSound)
                  .build());
 */

}
