//package com.ogfa.nativeviews.animation.aftereffect;
//
//import android.graphics.Canvas;
//import android.graphics.Matrix;
//import android.graphics.Paint;
//import android.graphics.PointF;
//
//import com.ogfa.nativeviews.internal.util.LogManager.Log;
//
//public class AfterEffectRenderer {
//
//    public static PointF totalPosition = new PointF(0,0);
//    public static PointF currentPosition = new PointF(0,0);
//
//    public static void render(Canvas canvas, AfterEffectAnimator afterEffectAnimator, Matrix matrix, Paint paint) {
//        long currentTime = System.currentTimeMillis();
//        if (afterEffectAnimator.loop && afterEffectAnimator.startTime + afterEffectAnimator.duration < currentTime){
//            afterEffectAnimator.startTime = currentTime;
//        }
//
//        long elapsedTime = currentTime - afterEffectAnimator.startTime;
//
//        float animWindowX = afterEffectAnimator.animationWindow.width;
//        float animWindowY = afterEffectAnimator.animationWindow.height;
//
//        for (Layer layer : afterEffectAnimator.layers) {
//
//            layer.playSound(afterEffectAnimator.duration, elapsedTime); // play sound
//
//            float totalAlpha = 0;
//            float totalScaleX = 0;
//            float totalScaleY = 0;
//            float totalRotation = 0;
//            totalPosition.x = 0;
//            totalPosition.y = 0;
//
//            for (Layer.KeyFrameTimeLineDefinition definition : layer.keyFrameTimeLineDefinitions) {
//                if (elapsedTime >= definition.startTimeInMillis && elapsedTime <= definition.endTimeInMillis) {
//                    float progress = (float) (elapsedTime - definition.startTimeInMillis) / (definition.endTimeInMillis - definition.startTimeInMillis);
//
//                    // Aggregate alpha, ensuring it doesn't exceed 100
//                    totalAlpha += definition.keyFrameAnimation.getAlpha(progress);
//                    totalAlpha = Math.min(totalAlpha, 100);
//
//                    // Aggregate scale
//                    totalScaleX += definition.keyFrameAnimation.getScaleXInterpolator().getInterpolation(progress) / 100f;
//                    totalScaleY += definition.keyFrameAnimation.getScaleYInterpolator().getInterpolation(progress) / 100f;
//
//                    // Aggregate rotation, and wrap around -360,360
//                    totalRotation += definition.keyFrameAnimation.getRotation(progress);
//
//                    // Aggregate position
//                    definition.keyFrameAnimation.getPosition(progress, animWindowX, animWindowY, currentPosition);
//                    totalPosition.x += currentPosition.x;
//                    totalPosition.y += currentPosition.y;
//
//                    if (totalAlpha == 0){
//                        Log.d("dddd", "alpha : 0" + " progress :" + progress);
//                    }
//                }
//            }
//
//
//
//            paint.setAlpha((int) (totalAlpha * 255f / 100f));
//
//            matrix.reset();
//            matrix.postTranslate(-layer.bitmap.getWidth() / 2.0f, -layer.bitmap.getHeight() / 2.0f); // Center the bitmap
//            matrix.postScale(totalScaleX, totalScaleY);
//            matrix.postRotate(totalRotation);
//            matrix.postTranslate(totalPosition.x, totalPosition.y);
//
//            // Draw the layer's bitmap on the canvas using the computed properties
//            canvas.drawBitmap(layer.bitmap, matrix, paint);
//        }
//    }
//
//    private static float normalizeRotation(float rotation) {
//        // Normalize rotation to be within -360 to 360 degrees
//        rotation %= 360;
//        if (rotation > 360) {
//            rotation -= 360;
//        } else if (rotation < -360) {
//            rotation += 360;
//        }
//        return rotation;
//    }
//}












package com.ogfa.nativeviews.animation.aftereffect;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;

import java.util.ArrayList;

public class AfterEffectRenderer {

    public static PointF positionForUpdate = new PointF(0,0);
    public static PointF scaleForUpdate = new PointF(0,0);

    public static void render(Canvas canvas, AfterEffectAnimator afterEffectAnimator, Matrix matrix, Paint paint) {
        long currentTime = System.currentTimeMillis();
        if (afterEffectAnimator.loop && afterEffectAnimator.startTime + afterEffectAnimator.duration <= currentTime){
            afterEffectAnimator.startTime = currentTime;
        }

        long elapsedTime = currentTime - afterEffectAnimator.startTime;

        float animWindowX = afterEffectAnimator.animationWindow.width;
        float animWindowY = afterEffectAnimator.animationWindow.height;

        for (Layer layer : afterEffectAnimator.layers) {

            layer.playSound(afterEffectAnimator.duration, elapsedTime); // play sound

            // Determine the current KeyFrameAnimation for this layer
            KeyFrameAggregator definition = null;
            ArrayList<Layer.KeyFrameTimeLineDefinition> definitions = new ArrayList<>();
            for (Layer.KeyFrameTimeLineDefinition def : layer.keyFrameTimeLineDefinitions) {
                if (elapsedTime >= def.startTimeInMillis && elapsedTime < def.endTimeInMillis) {
                    definitions.add(def);
                }
            }


            if (definitions.size() == 0){
                for (Layer.KeyFrameTimeLineDefinition def : layer.keyFrameTimeLineDefinitions) {
                    if (elapsedTime >= def.startTimeInMillis && elapsedTime <= def.endTimeInMillis) {
                        definitions.add(def);
                    }
                }
            }

            if (definitions.size()>0){
                definition = new KeyFrameAggregator(definitions);
            }

            if (definition == null) {
                continue; // No active KeyFrameTimeLineDefinition for this layer at the current time
            }

            // Compute the progress
            //float progress = (float) (elapsedTime - definition.startTimeInMillis) / (definition.endTimeInMillis - definition.startTimeInMillis);

            if (definitions.size() > 1){
//                Log.d("dddd", " size:" + definitions.size());
            }


            // Get the current properties for the animation
            float alpha = definition.getAlpha(elapsedTime);
            float rotation = definition.getRotation(elapsedTime);


            paint.setAlpha((int) (alpha * 255f/100f));

            matrix.reset();
            matrix.postTranslate(-layer.bitmap.getWidth() / 2.0f, -layer.bitmap.getHeight() / 2.0f); // Center the bitmap
            definition.getScale(elapsedTime,animWindowX, animWindowY, scaleForUpdate);
            matrix.postScale(scaleForUpdate.x / 100f,
                    scaleForUpdate.y / 100f);
            matrix.postRotate(rotation);
            definition.getPosition(elapsedTime,animWindowX, animWindowY, positionForUpdate);
            matrix.postTranslate(positionForUpdate.x,
                    positionForUpdate.y);

            // Draw the layer's bitmap on the canvas using the computed properties
            canvas.drawBitmap(layer.bitmap, matrix, paint);
        }
    }
}
