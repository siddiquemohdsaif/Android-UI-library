package com.ogfa.nativeviews.animator.component;

import android.graphics.RectF;
import java.util.List;

public interface BoundsResolver {
    RectF resolve(RectF declaredBounds, List<RectF> layerBounds);
}
