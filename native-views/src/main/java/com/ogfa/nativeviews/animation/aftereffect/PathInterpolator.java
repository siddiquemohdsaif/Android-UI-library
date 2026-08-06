package com.ogfa.nativeviews.animation.aftereffect;

import android.content.Context;
import android.graphics.PointF;

import com.ogfa.nativeviews.internal.util.BackgroundRunner;


import com.ogfa.nativeviews.animation.aftereffect.Effect.Interpolator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class PathInterpolator {


    public static final String Linear = "Linear";
    public static final String CubicEasyEase = "CubicEasyEase";
    public static final String CubicEasyEaseIn = "CubicEasyEaseIn";
    public static final String CubicEasyEaseOut = "CubicEasyEaseOut";
    public static final String EasyEase = "EasyEase";
    public static final String EasyEaseIn = "EasyEaseIn";
    public static final String EasyEaseOut = "EasyEaseOut";


    private Path path;
    private String interpolatorName;
    private Point basePoint;
    private Interpolator interpolator;
    private static volatile ConcurrentHashMap<String, Path> preloadedPaths = new ConcurrentHashMap<>();

    public static void preloadPaths(Context context) {
        BackgroundRunner.run(new Runnable() {
            @Override
            public void run() {
                // Define animations to preload here
                preloadPath("coin1_1_path", "coin1_1_path.json", context);
                preloadPath("coin1_2_path", "coin1_2_path.json", context);
                preloadPath("coin1_3_path", "coin1_3_path.json", context);
                preloadPath("coin1_4_path", "coin1_4_path.json", context);
                preloadPath("coin1_5_path", "coin1_5_path.json", context);
                preloadPath("coin2_1_path", "coin2_1_path.json", context);
                preloadPath("coin2_2_path", "coin2_2_path.json", context);
                preloadPath("coin2_3_path", "coin2_3_path.json", context);
                preloadPath("coin2_4_path", "coin2_4_path.json", context);
                preloadPath("coin2_5_path", "coin2_5_path.json", context);
                preloadPath("curve_path_data", "curve_path_data.json", context);
                preloadPath("puck_path_1_1", "puck_path_1_1.json", context);
                preloadPath("puck_path_1_2", "puck_path_1_2.json", context);
                preloadPath("puck_path_1_3", "puck_path_1_3.json", context);
                preloadPath("puck_path_1_4", "puck_path_1_4.json", context);
                preloadPath("puck_path_2_1", "puck_path_2_1.json", context);
                preloadPath("puck_path_2_2", "puck_path_2_2.json", context);
                preloadPath("puck_path_2_3", "puck_path_2_3.json", context);
                preloadPath("puck_path_2_4", "puck_path_2_4.json", context);
                preloadPath("puck_path_3_1", "puck_path_3_1.json", context);
                preloadPath("puck_path_3_2", "puck_path_3_2.json", context);
                preloadPath("puck_path_3_3", "puck_path_3_3.json", context);
                preloadPath("puck_path_3_4", "puck_path_3_4.json", context);
            }
        });
    }

    private static void preloadPath(String id, String animationName, Context context) {
        preloadedPaths.put(id, PathInterpolator.Path.loadPath(context, "paths/"+animationName));
    }

    public static Path get(String pathName){
        return preloadedPaths.get(pathName);
    }

    public PathInterpolator(Path path ,String interpolatorName ) {
        this.path = path;
        this.interpolatorName = interpolatorName;
    }

    public void getPosition(float progress, PointF positionForUpdate) {
        // Ensure progress is within bounds
        if (progress < 0.0f || progress > 1.0f || path.points.length < 2) {
            throw new IllegalArgumentException("Invalid progress or path length");
        }

        progress = interpolator.getInterpolation(progress); // apply animation effect

        // Calculate the index of the first point based on progress
        int index = (int)(progress * (path.points.length - 1));
        // Ensure we don't exceed array bounds
        int nextIndex = Math.min(index + 1, path.points.length - 1);

        // Get the two points for interpolation
        Point startPoint = path.points[index];
        Point endPoint = path.points[nextIndex];

        // Calculate the local progress between these two points
        float localProgress = (progress * (path.points.length - 1)) - index;

        // Interpolate x and y values
        positionForUpdate.x = interpolate(startPoint.x, endPoint.x, localProgress) + basePoint.x;
        positionForUpdate.y = interpolate(startPoint.y, endPoint.y, localProgress) + basePoint.y;
        //Log.d("anim_debug", "progress: " + progress + " x:" + positionForUpdate.x + " y:" + positionForUpdate.y);
    }

    // Helper method for linear interpolation
    private float interpolate(float startValue, float endValue, float fraction) {
        return startValue + fraction * (endValue - startValue);
    }

    public void transform(Interpolator posXInterpolator, Interpolator posYInterpolator) {
        Point startPoint, endPoint;
        startPoint = new Point(posXInterpolator.getInterpolation(0), posYInterpolator.getInterpolation(0));
        endPoint = new Point(posXInterpolator.getInterpolation(1), posYInterpolator.getInterpolation(1));
        this.path = Path.transformPath(this.path, startPoint, endPoint);
        this.basePoint = startPoint;
        switch (interpolatorName) {
            case CubicEasyEase:
                interpolator = com.ogfa.nativeviews.animation.aftereffect.Effect.CubicEasyEase.get(0, 1);
                break;
            case CubicEasyEaseIn:
                interpolator = com.ogfa.nativeviews.animation.aftereffect.Effect.CubicEasyEaseIn.get(0, 1);
                break;
            case CubicEasyEaseOut:
                interpolator = com.ogfa.nativeviews.animation.aftereffect.Effect.CubicEasyEaseOut.get(0, 1);
                break;
            case EasyEase:
                interpolator = com.ogfa.nativeviews.animation.aftereffect.Effect.EasyEase.get(0, 1);
                break;
            case EasyEaseIn:
                interpolator = com.ogfa.nativeviews.animation.aftereffect.Effect.EasyEaseIn.get(0, 1);
                break;
            case EasyEaseOut:
                interpolator = com.ogfa.nativeviews.animation.aftereffect.Effect.EasyEaseOut.get(0, 1);
                break;
            default:
                interpolator = com.ogfa.nativeviews.animation.aftereffect.Effect.Linear.get(0, 1);
                break;
        }
    }


    public static class Path {
        public Point points[];

        public Path(Point[] point) {
            this.points = point;
        }

        public Path copy() {
            Point[] copiedPoints = new Point[this.points.length];
            for (int i = 0; i < this.points.length; i++) {
                copiedPoints[i] = new Point(this.points[i].x, this.points[i].y);
            }
            return new Path(copiedPoints);
        }

        public static Path transformPath(Path pathIn, Point startPoint, Point endPoint){
            Path path = pathIn.copy();

            translatePathToOrigin(path);


            //scale
            //path
            float Pdx = path.points[path.points.length-1].x - path.points[0].x;
            float Pdy = path.points[path.points.length-1].y - path.points[0].y;
            //canvas
            float Cdx = endPoint.x - startPoint.x;
            float Cdy = endPoint.y - startPoint.y;
            // scale constant
            float Sx = Cdx/Pdx;
            float Sy = Cdy/Pdy;
            Scale(path, Sx, Sy);

            return path;
        }


        public static void Scale(Path path, float scaleX, float scaleY) {
            if (path.points == null) {
                return; // No points to scale
            }

            for (Point point : path.points) {
                point.x *= scaleX;
                point.y *= scaleY;
            }
        }



        public static void translatePathToOrigin(Path path) {
            if (path.points == null || path.points.length == 0) {
                return; // No points to translate
            }

            // Calculate how much to translate
            float translateX = -path.points[0].x;
            float translateY = -path.points[0].y;

            // Apply the translation to each point
            for (Point point : path.points) {
                point.x += translateX;
                point.y += translateY;
            }
        }



        public static Path loadPath(Context context, String path) {
            try {
                String jsonStr = loadJSONFromAsset(context, path);
                JSONArray jsonArray = new JSONArray(jsonStr);

                Point[] points = new Point[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    int x = obj.getInt("x");
                    int y = obj.getInt("y");
                    points[i] = new Point(x, y);
                }

                return new Path(points);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private static String loadJSONFromAsset(Context context, String fileName) {
            String json = null;
            try {
                InputStream is = context.getAssets().open(fileName);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                json = new String(buffer, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
            return json;
        }

        public void print() {
            for (int i = 0; i< points.length; i++) {
                Point point = points[i];
//                Log.d("point_print " + i, "  x: "+ point.x + " y:" + point.y);
            }
        }
    }


    public static class Point {
        public float x;
        public float y;
        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }


    // for test //
    public static PathInterpolator.Path loadPathFromString(String jsonString) {
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            PathInterpolator.Point[] points = new PathInterpolator.Point[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                float x = (float) obj.getDouble("x");
                float y = (float) obj.getDouble("y");
                points[i] = new PathInterpolator.Point(x, y);
            }
            return new PathInterpolator.Path(points);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String pathToString(PathInterpolator.Path path) {
        StringBuilder sb = new StringBuilder();
        for (PathInterpolator.Point point : path.points) {
            sb.append("(").append(point.x).append(", ").append(point.y).append(") ");
        }
        return sb.toString().trim();
    }




}
