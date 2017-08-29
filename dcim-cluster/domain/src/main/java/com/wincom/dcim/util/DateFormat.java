package com.wincom.dcim.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by wangxy on 17-8-28.
 */
public class DateFormat {
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ssZ";
    public static final String TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSZ";

    public static final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
    public static final SimpleDateFormat datetimeFormat = new SimpleDateFormat(DATETIME_PATTERN);
    public static final SimpleDateFormat timestampFormat = new SimpleDateFormat(TIMESTAMP_PATTERN);

    public static String formatTimestamp(Long tks) {
        Date d = new Date(tks);
        return timestampFormat.format(d);
    }

    public static String formatTimestamp(Date d) {
        return timestampFormat.format(d);
    }

    public static Date parseTimestamp(String str) {
        try {
            return timestampFormat.parse(str);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
