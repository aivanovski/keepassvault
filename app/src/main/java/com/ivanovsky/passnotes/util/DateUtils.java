package com.ivanovsky.passnotes.util;

import androidx.annotation.Nullable;

import java.util.Date;

public class DateUtils {

    @Nullable
    public static Date anyLast(@Nullable Date first, @Nullable Date second) {
        if (first == null) return second;
        if (second == null) return first;

        return (first.after(second)) ? first : second;
    }

    @Nullable
    public static Long anyLastTimestamp(@Nullable Long first, @Nullable Long second) {
        if (first == null) return second;
        if (second == null) return first;

        return (first > second) ? first : second;
    }

    @Nullable
    public static Long anyLastTimestamp(@Nullable Date first, @Nullable Date second) {
        Long result = null;

        Date date = anyLast(first, second);
        if (date != null) {
            result = date.getTime();
        }

        return result;
    }
}
