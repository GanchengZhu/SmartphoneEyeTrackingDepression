package com.example.gaze.record.motion;

import java.security.InvalidParameterException;

public class MotionFactory {

    public static BaseMotion getInstance(String type) {
//        if ("smooth".equals(type)) return new SmoothMotion();
//        else if ("lissajous".equals(type)) return new LissajousMotion();
//        else if ("five_point".equals(type)) return new FivePointMotion();
//        else if ("random".equals(type)) return new RandomMotion();
        if ("SmoothPursuit".equals(type)) return new SmoothPursuitMotion();
        else if ("HorizontalSinusoid".equals(type)) return new HorizontalSinusoidMotion();
        else if ("FixationStability".equals(type)) return new FixationStabilityMotion();
        else if ("VerticalSinusoid".equals(type)) return new VerticalSinusoidMotion();
        else throw new InvalidParameterException("Invalid parameter given: " + type);
    }

    public static BaseMotion getInstance() {
        return MotionFactory.getInstance("SmoothPursuit");
    }


}
