package com.keylesspalace.tusky.util

import org.junit.Assert
import org.junit.Test

class ViewPager2FixTest {

    @Test
    fun viewPagerRecyclerViewFieldTest() {
        try {
            val f = ViewPager2Fix.getViewPagerRecyclerViewField()
        } catch (e: Exception) {
            Assert.fail("asdf")
        }
    }

    @Test
    fun recyclerViewTouchSlopFieldTest() {
        try {
            val f = ViewPager2Fix.getRecyclerViewTouchSlopField()
        } catch (e: Exception) {
            Assert.fail("asdf")
        }
    }
}
