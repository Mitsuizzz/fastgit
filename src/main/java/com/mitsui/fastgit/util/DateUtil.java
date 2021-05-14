package com.mitsui.fastgit.util;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateUtil {

    public static Date convert2Date(int timestamp) {
        long temp = (long)timestamp * 1000L;
        return new Date(temp);
    }

    public static Date convert2Date(String dateStr, String format) {
        SimpleDateFormat simple = new SimpleDateFormat(format);

        try {
            simple.setLenient(false);
            return simple.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    public static String convert2String(Date date, String format) {
        SimpleDateFormat formater = new SimpleDateFormat(format);

        try {
            return formater.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static Timestamp convertSqlTime(Date date) {
        Timestamp timestamp = new Timestamp(date.getTime());
        return timestamp;
    }

    public static java.sql.Date convertSqlDate(Date date) {
        java.sql.Date Datetamp = new java.sql.Date(date.getTime());
        return Datetamp;
    }

    public static String getCurrentDate(String format) {
        return (new SimpleDateFormat(format)).format(new Date());
    }

    public static long getTimestamp() {
        return System.currentTimeMillis();
    }

    public static int getDaysOfMonth(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1);
        return calendar.getActualMaximum(5);
    }

    public static int getYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(1);
    }

    public static int getMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(2) + 1;
    }

    public static int getDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(5);
    }

    public static int getHour(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(10);
    }

    public static int getMinute(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(12);
    }

    public static int getSecond(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(13);
    }

    public static int getWeekDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(7);
        return dayOfWeek - 1;
    }

    public static int getMaxWeekNumOfYear(int year) {
        Calendar c = new GregorianCalendar();
        c.set(year, 11, 31, 23, 59, 59);
        return getWeekNumOfYear(c.getTime());
    }

    public static int getWeekNumOfYear(Date date) {
        Calendar c = new GregorianCalendar();
        c.setFirstDayOfWeek(2);
        c.setMinimalDaysInFirstWeek(7);
        c.setTime(date);
        return c.get(3);
    }

    public static Date getFirstDayOfWeek(Date date) {
        Calendar c = new GregorianCalendar();
        c.setFirstDayOfWeek(2);
        c.setTime(date);
        c.set(7, c.getFirstDayOfWeek());
        return c.getTime();
    }

    public static Date getLastDayOfWeek(Date date) {
        Calendar c = new GregorianCalendar();
        c.setFirstDayOfWeek(2);
        c.setTime(date);
        c.set(7, c.getFirstDayOfWeek() + 6);
        return c.getTime();
    }

    public static Date getFirstDayOfWeek(int year, int week) {
        Calendar calFirst = Calendar.getInstance();
        calFirst.set(year, 0, 7);
        Date firstDate = getFirstDayOfWeek(calFirst.getTime());
        Calendar firstDateCal = Calendar.getInstance();
        firstDateCal.setTime(firstDate);
        Calendar c = new GregorianCalendar();
        c.set(1, year);
        c.set(2, 0);
        c.set(5, firstDateCal.get(5));
        Calendar cal = (GregorianCalendar)c.clone();
        cal.add(5, (week - 1) * 7);
        firstDate = getFirstDayOfWeek(cal.getTime());
        return firstDate;
    }

    public static Date getLastDayOfWeek(int year, int week) {
        Calendar calLast = Calendar.getInstance();
        calLast.set(year, 0, 7);
        Date firstDate = getLastDayOfWeek(calLast.getTime());
        Calendar firstDateCal = Calendar.getInstance();
        firstDateCal.setTime(firstDate);
        Calendar c = new GregorianCalendar();
        c.set(1, year);
        c.set(2, 0);
        c.set(5, firstDateCal.get(5));
        Calendar cal = (GregorianCalendar)c.clone();
        cal.add(5, (week - 1) * 7);
        Date lastDate = getLastDayOfWeek(cal.getTime());
        return lastDate;
    }

    private static Date add(Date date, int calendarField, int amount) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        } else {
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            c.add(calendarField, amount);
            return c.getTime();
        }
    }

    public static Date addYears(Date date, int amount) {
        return add(date, 1, amount);
    }

    public static Date addMonths(Date date, int amount) {
        return add(date, 2, amount);
    }

    public static Date addWeeks(Date date, int amount) {
        return add(date, 3, amount);
    }

    public static Date addDays(Date date, int amount) {
        return add(date, 5, amount);
    }

    public static Date addHours(Date date, int amount) {
        return add(date, 11, amount);
    }

    public static Date addMinutes(Date date, int amount) {
        return add(date, 12, amount);
    }

    public static Date addSeconds(Date date, int amount) {
        return add(date, 13, amount);
    }

    public static Date addMilliseconds(Date date, int amount) {
        return add(date, 14, amount);
    }

    public static long diffTimes(Date before, Date after) {
        return after.getTime() - before.getTime();
    }

    public static long diffSecond(Date before, Date after) {
        return (after.getTime() - before.getTime()) / 1000L;
    }

    public static int diffMinute(Date before, Date after) {
        return (int)(after.getTime() - before.getTime()) / 1000 / 60;
    }

    public static int diffHour(Date before, Date after) {
        return (int)(after.getTime() - before.getTime()) / 1000 / 60 / 60;
    }

    public static int diffDay(Date before, Date after) {
        return Integer.parseInt(String.valueOf((after.getTime() - before.getTime()) / 86400000L));
    }


    public static int diffYear(Date before, Date after) {
        return getYear(after) - getYear(before);
    }

    public static Date setEndDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(11, 23);
        calendar.set(12, 59);
        calendar.set(13, 59);
        return calendar.getTime();
    }

    public static Date setStartDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        return calendar.getTime();
    }
}
