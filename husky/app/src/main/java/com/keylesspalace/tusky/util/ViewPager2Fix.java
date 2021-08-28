package com.keylesspalace.tusky.util;

import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.RecyclerView;
import java.lang.reflect.*;
import java.lang.*;

/**
 * ViewPager2 written by monkeys!
 */
public class ViewPager2Fix {
	/**
	 * Thanks to @al.e.shevelev@medium.com for solution
	 */
	public static Field getViewPagerRecyclerViewField() throws NoSuchFieldException {
		Field f = ViewPager2.class.getDeclaredField("mRecyclerView");
		f.setAccessible(true);
		return f;
	}
	
	public static Field getRecyclerViewTouchSlopField() throws NoSuchFieldException {
		Field f = RecyclerView.class.getDeclaredField("mTouchSlop");
		f.setAccessible(true);
		return f;
	}
	 
	public static void reduceVelocity(ViewPager2 pager, float val) {
		try {
			Field recyclerViewField = getViewPagerRecyclerViewField();
			Field touchSlopField = getRecyclerViewTouchSlopField();

			RecyclerView recyclerView = (RecyclerView)recyclerViewField.get(pager);
			int touchSlop = (int)touchSlopField.get(recyclerView);
			touchSlopField.setInt(recyclerView, (int)(touchSlop*val));
		} catch(Exception e) {
			// all possible exceptions must be caught during tests
			;
		}
	}
}
