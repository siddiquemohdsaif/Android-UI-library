package com.ogfa.nativeviews.internal.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AssetHandler {

    public static Bitmap getBitmapFromAsset(String strName, Context context) {
        try {
            return BitmapFactory.decodeStream(context.getAssets().open(strName));
        } catch (IOException e) {
            Log.d("LottieViewAnimator", "getBitmapFromAsset:1 "+e);
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject loadJSONFromAsset(String filename, Context context) {
        String json = null;
        JSONObject jsonObject = null;
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            Log.e("JSONHandler", "Error reading asset " + filename, ex);
            return null;
        }

        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException ex) {
            Log.e("JSONHandler", "Error parsing JSON from asset " + filename, ex);
        }

        return jsonObject;
    }

    public static boolean isImageAssetExists(String assetName, Context context) {
        try {
            String[] assets = context.getAssets().list("images");  // List only from 'images' folder
            if (assets != null) {
                for (String asset : assets) {
                    if (asset.equals(assetName)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
