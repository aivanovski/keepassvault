package com.ivanovsky.passnotes.util;

import android.support.annotation.Nullable;

import java.util.Date;

public class DateUtils {

	@Nullable
	public static Date anyLast(@Nullable Date first, @Nullable Date second) {
		if (first == null) return second;
		if (second == null) return first;

		return (first.after(second)) ? first : second;
	}
}
