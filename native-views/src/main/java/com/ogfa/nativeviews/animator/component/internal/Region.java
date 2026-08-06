package com.ogfa.nativeviews.animator.component.internal;

import android.graphics.PointF;

public class Region {

    private float xMin;
    private float xMax;
    private float yMin;
    private float yMax;
    public String id;

    private PointF onClickDown;


    public Region(float xMin, float xMax, float yMin, float yMax, String id) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        this.id = id;
    }

    public boolean isRegionClicked(float x, float y){

        if (x>xMin && x<xMax && y>yMin && y<yMax){
            return onClickDown != null;
        }else {
            return false;
        }
    }

    private boolean isRegionClickedNative(float x, float y){

        if (x>xMin && x<xMax && y>yMin && y<yMax){
            return true;
        }else {
            return false;
        }
    }

    public boolean regionClickedDown(float x, float y ){

        if (isRegionClickedNative(x,y)){
            onClickDown = new PointF(x,y);
            return true;
        }else {
            onClickDown = null;
            return false;
        }
    }

    public boolean regionClickedMove(float x, float y ){

        if (isRegionClickedNative(x,y)){
            onClickDown = new PointF(x,y);
            return true;
        }else {
            onClickDown = null;
            return false;
        }
    }




    public float getX (){
        return xMin;
    }

    public float getY (){
        return yMin;
    }

    public void updateRegion(int left, int right, int top, int bottom) {
        this.xMin = left;
        this.xMax = right;
        this.yMin = top;
        this.yMax = bottom;
    }


}
