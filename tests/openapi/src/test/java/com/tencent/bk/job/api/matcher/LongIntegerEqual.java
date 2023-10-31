package com.tencent.bk.job.api.matcher;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class LongIntegerEqual<T> extends BaseMatcher<T> {
    private final Object expectedValue;

    public LongIntegerEqual(Object value) {
        this.expectedValue = value;
    }

    @Override
    public boolean matches(Object actualValue) {
        return areEqual(actualValue, expectedValue);
    }

    private static boolean areEqual(Object actual, Object expected) {
        if (actual == null) {
            return expected == null;
        }
        if (expected == null) {
            return false;
        }
        long actualValue;
        long expectedValue;
        if (actual instanceof Long) {
            actualValue = (Long) actual;
        } else if (actual instanceof Integer) {
            actualValue = (Integer) actual;
        } else {
            return false;
        }
        if (expected instanceof Long) {
            expectedValue = (Long) expected;
        } else if (expected instanceof Integer) {
            expectedValue = (Integer) expected;
        } else {
            return false;
        }
        return actualValue == expectedValue;
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(expectedValue);
    }

    public static <T> Matcher<T> longIntegerEqual(T operand) {
        return new LongIntegerEqual<T>(operand);
    }
}
