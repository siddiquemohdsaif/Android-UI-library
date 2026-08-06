package com.ogfa.nativeviews.animation.aftereffect;

import android.graphics.PointF;

import java.util.ArrayList;

public class KeyFrameAggregator{

    private ArrayList<Layer.KeyFrameTimeLineDefinition> definitions;

    public KeyFrameAggregator(ArrayList<Layer.KeyFrameTimeLineDefinition> definitions) {
        this.definitions = definitions;
    }


    public void getPosition(long elapsedTime, float animWindowX, float animWindowY, PointF positionForUpdate) {
        if (definitions.size() == 1){
            Layer.KeyFrameTimeLineDefinition definition = definitions.get(0);
            // Compute the progress
            float progress = (float) (elapsedTime - definition.startTimeInMillis) / (definition.endTimeInMillis - definition.startTimeInMillis);
            definition.keyFrameAnimation.getPosition(progress,animWindowX, animWindowY, positionForUpdate);
        } else {
            PointF posAvg = new PointF();
            boolean found = false;
            for (Layer.KeyFrameTimeLineDefinition definition : definitions){
                // Compute the progress
                float progress = (float) (elapsedTime - definition.startTimeInMillis) / (definition.endTimeInMillis - definition.startTimeInMillis);
                if (definition.keyFrameAnimation.posXInterpolator_Set || definition.keyFrameAnimation.posYInterpolator_Set){
                    found = true;
                    definition.keyFrameAnimation.getPosition(progress,animWindowX, animWindowY, positionForUpdate);
                    posAvg.x += positionForUpdate.x;
                    posAvg.y += positionForUpdate.y;
                }
            }
            if (!found){
                positionForUpdate.x = definitions.get(0).keyFrameAnimation.posX; // default
                positionForUpdate.y = definitions.get(0).keyFrameAnimation.posY; // default
            }else {
                positionForUpdate.x = posAvg.x;
                positionForUpdate.y = posAvg.y;
            }
        }
    }

    public void getScale(long elapsedTime, float animWindowX, float animWindowY, PointF saleDimensionForUpdate) {
        if (definitions.size() == 1){
            Layer.KeyFrameTimeLineDefinition definition = definitions.get(0);
            // Compute the progress
            float progress = (float) (elapsedTime - definition.startTimeInMillis) / (definition.endTimeInMillis - definition.startTimeInMillis);
            definition.keyFrameAnimation.getScale(progress,animWindowX, animWindowY, saleDimensionForUpdate);
        } else {
            PointF scaleAvg = new PointF();
            boolean found = false;
            for (Layer.KeyFrameTimeLineDefinition definition : definitions){
                // Compute the progress
                float progress = (float) (elapsedTime - definition.startTimeInMillis) / (definition.endTimeInMillis - definition.startTimeInMillis);
                if (definition.keyFrameAnimation.scaleXInterpolator_Set || definition.keyFrameAnimation.scaleYInterpolator_Set){
                    found = true;
                    definition.keyFrameAnimation.getScale(progress,animWindowX, animWindowY, saleDimensionForUpdate);
                    scaleAvg.x += saleDimensionForUpdate.x;
                    scaleAvg.y += saleDimensionForUpdate.y;
                }
            }
            if (!found){
                saleDimensionForUpdate.x = 100; // default
                saleDimensionForUpdate.y = 100; // default
            }else {
                saleDimensionForUpdate.x = scaleAvg.x;
                saleDimensionForUpdate.y = scaleAvg.y;
            }
        }
    }

    public float getRotation(long elapsedTime) {
        if (definitions.size() == 1){
            Layer.KeyFrameTimeLineDefinition definition = definitions.get(0);
            // Compute the progress
            float progress = (float) (elapsedTime - definition.startTimeInMillis) / (definition.endTimeInMillis - definition.startTimeInMillis);
            return definition.keyFrameAnimation.getRotation(progress);
        } else {
            float rotationAvg = 0;
            boolean found = false;
            for (Layer.KeyFrameTimeLineDefinition definition : definitions){
                // Compute the progress
                float progress = (float) (elapsedTime - definition.startTimeInMillis) / (definition.endTimeInMillis - definition.startTimeInMillis);
                if (definition.keyFrameAnimation.rotationInterpolator_Set){
                    found = true;
                    rotationAvg += definition.keyFrameAnimation.getRotation(progress);
                }
            }
            if (!found){
                rotationAvg = 0; // default
            }
            return rotationAvg;
        }
    }

    public float getAlpha(long elapsedTime) {
        if (definitions.size() == 1){
            Layer.KeyFrameTimeLineDefinition definition = definitions.get(0);
            // Compute the progress
            float progress = (float) (elapsedTime - definition.startTimeInMillis) / (definition.endTimeInMillis - definition.startTimeInMillis);
            return definition.keyFrameAnimation.getAlpha(progress);
        } else {
            float alphaAvg = 0;
            boolean found = false;
            for (Layer.KeyFrameTimeLineDefinition definition : definitions){
                // Compute the progress
                float progress = (float) (elapsedTime - definition.startTimeInMillis) / (definition.endTimeInMillis - definition.startTimeInMillis);
                if (definition.keyFrameAnimation.alphaInterpolator_Set){
                    found = true;
                    alphaAvg += definition.keyFrameAnimation.getAlpha(progress);
                }
            }
            if (!found){
                alphaAvg = 100; // default
            }
            return alphaAvg;
        }
    }
}
