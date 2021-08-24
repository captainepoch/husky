package com.keylesspalace.tusky.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.junit.runner.RunWith;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.RecyclerView;
import org.junit.runners.Parameterized;
import java.lang.reflect.*;
import java.lang.*;
import com.keylesspalace.tusky.util.ViewPager2Fix;

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

