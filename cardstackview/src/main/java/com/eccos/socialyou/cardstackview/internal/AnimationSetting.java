package com.eccos.socialyou.cardstackview.internal;

import android.view.animation.Interpolator;

import com.eccos.socialyou.cardstackview.Direction;

public interface AnimationSetting {
    Direction getDirection();
    int getDuration();
    Interpolator getInterpolator();
}
