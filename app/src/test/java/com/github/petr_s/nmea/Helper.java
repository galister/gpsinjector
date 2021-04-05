package com.github.petr_s.nmea;

import org.hamcrest.Description;
import org.mockito.ArgumentMatcher;

import java.util.List;
import java.util.Set;

public class Helper {
    public static ArgumentMatcher<Double> roughlyEq(final double expected) {
        return roughlyEq(expected, 0.0001);
    }

    public static ArgumentMatcher<Double> roughlyEq(final double expected, final double delta) {
        return new ArgumentMatcher<Double>() {
            @Override
            public boolean matches(Object argument) {
                return Math.abs(expected - (Double) argument) <= delta;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(Double.toString(expected) + "±" + Double.toString(delta));
            }
        };
    }

    public static ArgumentMatcher<Float> roughlyEq(final float expected) {
        return roughlyEq(expected, 0.0001f);
    }

    public static ArgumentMatcher<Float> roughlyEq(final float expected, final float delta) {
        return new ArgumentMatcher<Float>() {
            @Override
            public boolean matches(Object argument) {
                return Math.abs(expected - (Float) argument) <= delta;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(Float.toString(expected) + "±" + Float.toString(delta));
            }
        };
    }

    public static <T> ArgumentMatcher<Set> eq(final Set<T> expected) {
        return new ArgumentMatcher<Set>() {
            @Override
            public boolean matches(Object argument) {
                return argument.equals(expected);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(expected.toString());
            }
        };
    }

    public static <T> ArgumentMatcher<List> eq(final List<T> expected) {
        return new ArgumentMatcher<List>() {
            @Override
            public boolean matches(Object argument) {
                return argument.equals(expected);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(expected.toString());
            }
        };
    }
}
