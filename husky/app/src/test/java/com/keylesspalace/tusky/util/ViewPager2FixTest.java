package com.keylesspalace.tusky.util;

import java.lang.reflect.Field;
import org.junit.Assert;
import org.junit.Test;

public class ViewPager2FixTest {

    @Test
    public void getViewPagerRecyclerViewFieldTest() {
        try {
            Field f = ViewPager2Fix.getViewPagerRecyclerViewField();
        } catch(Exception e) {
            Assert.fail("asdf");
        }
    }

    @Test
    public void getRecyclerViewTouchSlopFieldTest() {
        try {
            Field f = ViewPager2Fix.getRecyclerViewTouchSlopField();
        } catch(Exception e) {
            Assert.fail("asdf");
        }
    }
}
