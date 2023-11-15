package com.tencent.bk.job.api.util;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 时间处理
 */
public class DateUtils {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER_WITH_ZONE =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");

    /**
     * 格式化Timestamp，yyyy-MM-dd HH:mm:ss
     *
     * @param timestamp
     * @return
     */
    public static String defaultFormatTimestamp(Timestamp timestamp) {
        return formatTimestamp(timestamp, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 根据给定的pattern格式化Timestamp
     *
     * @param timestamp
     * @param pattern
     * @return
     */
    public static String formatTimestamp(Timestamp timestamp, String pattern) {
        Instant now = Instant.ofEpochMilli(timestamp.getTime());
        LocalDateTime dateTime = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 根据给定的pattern格式化Timestamp
     *
     * @param unixTimestamp UNIX 时间戳
     * @param unit          时间单位，支持ChronoUnit.SECONDS,ChronoUnit.MILLILS
     * @param pattern       格式化样式
     * @param zone          时区,如果不传默认使用系统时区
     * @return
     */
    public static String formatUnixTimestamp(long unixTimestamp, ChronoUnit unit, String pattern, ZoneId zone) {
        return parseUnixTimestamp(unixTimestamp, unit, zone).format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String formatUnixTimestamp(long unixTimestamp, ChronoUnit unit) {
        return parseUnixTimestamp(unixTimestamp, unit, null).format(DATE_FORMATTER);
    }

    private static ZonedDateTime parseUnixTimestamp(long unixTimestamp, ChronoUnit unit, ZoneId zone) {
        if (unit != null && (unit != ChronoUnit.SECONDS && unit != ChronoUnit.MILLIS)) {
            throw new UnsupportedOperationException("Unsupported conversion with unit:" + unit.name());
        }
        Instant instant;
        unit = (unit == null ? ChronoUnit.SECONDS : unit);
        if (unit == ChronoUnit.MILLIS) {
            instant = Instant.ofEpochMilli(unixTimestamp);
        } else {
            instant = Instant.ofEpochSecond(unixTimestamp);
        }
        ZonedDateTime dateTime;
        if (zone != null) {
            dateTime = ZonedDateTime.ofInstant(instant, zone);
        } else {
            dateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        }
        return dateTime;
    }

    /**
     * @param datetime      时间
     * @param pattern       时间格式
     * @param convertToUnit 转换之后的单位，支持ChronoUnit.SECONDS和ChronoUnit.MILLIS。如果不传入，默认使用ChronoUnit.SECONDS
     * @param zone          时区；如果未传入，默认使用系统时区
     * @return UnixTimestamp
     */
    public static long convertUnixTimestampFromDateTimeStr(String datetime, String pattern, ChronoUnit convertToUnit,
                                                           ZoneId zone) {
        LocalDateTime localDateTime = LocalDateTime.parse(datetime, DateTimeFormatter.ofPattern(pattern));
        return convertUnixTimestampFromLocalDateTime(localDateTime, convertToUnit, zone);
    }

    /**
     * @param datetime      时间
     * @param convertToUnit 转换之后的单位，支持ChronoUnit.SECONDS和ChronoUnit.MILLIS。如果不传入，默认使用ChronoUnit.SECONDS
     * @param zone          时区；如果未传入，默认使用系统时区
     * @return
     */
    public static long convertUnixTimestampFromLocalDateTime(LocalDateTime datetime, ChronoUnit convertToUnit,
                                                             ZoneId zone) {
        if (convertToUnit != null && (convertToUnit != ChronoUnit.SECONDS && convertToUnit != ChronoUnit.MILLIS)) {
            throw new UnsupportedOperationException("Unsupported conversion with unit:" + convertToUnit.name());
        }
        Instant now = Instant.now();
        ZoneId targetZone = zone == null ? ZoneId.systemDefault() : zone;
        ZoneOffset currentOffsetForMyZone = targetZone.getRules().getOffset(now);


        long unixTimestamp;
        if (convertToUnit != null) {
            if (convertToUnit == ChronoUnit.MILLIS) {
                unixTimestamp = datetime.toInstant(currentOffsetForMyZone).toEpochMilli();
            } else {
                unixTimestamp = datetime.toInstant(currentOffsetForMyZone).toEpochMilli() / 1000;
            }
        } else {
            unixTimestamp = datetime.toInstant(currentOffsetForMyZone).toEpochMilli() / 1000;
        }
        return unixTimestamp;
    }

    /**
     * 把日期字符(yyyy-MM-dd HH:mm:ss)转换成Timestamp
     *
     * @param datetime
     * @return
     */
    public static Timestamp convertDateTimeStrToTimestamp(String datetime, ZoneId currentZone) {
        Instant now = Instant.now();
        ZoneOffset currentOffsetForMyZone = currentZone.getRules().getOffset(now);

        LocalDateTime localDateTime = LocalDateTime.parse(datetime,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return new Timestamp(localDateTime.toInstant(currentOffsetForMyZone).toEpochMilli());
    }

    /**
     * 把LocalDateTime转换成Timestamp
     *
     * @param localDateTime datetime
     * @param currentZone   当前时区
     * @return
     */
    public static Timestamp convertLocalDateTimeToTimestamp(LocalDateTime localDateTime, ZoneId currentZone) {
        if (localDateTime == null) {
            return null;
        }
        ZoneId finalZone = currentZone;
        if (currentZone == null) {
            finalZone = ZoneId.systemDefault();
        }
        Instant now = Instant.now();
        assert finalZone != null;
        ZoneOffset currentOffsetForMyZone = finalZone.getRules().getOffset(now);
        return new Timestamp(localDateTime.toInstant(currentOffsetForMyZone).toEpochMilli());
    }

    public static String defaultLocalDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String formatLocalDateTime(LocalDateTime localDateTime, String pattern) {
        return localDateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static long calculateMillsBetweenDateTime(LocalDateTime from, LocalDateTime to) {
        Duration duration = Duration.between(from, to);
        return duration.toMillis();
    }

    public static LocalDateTime convertFromMillSeconds(long millSeconds) {
        return LocalDateTime.ofEpochSecond(millSeconds / 1000,
            (int) (millSeconds % 1000) * 1_000_000,
            ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
    }

    public static Timestamp to(LocalDateTime u) {
        return u == null ? null : Timestamp.valueOf(u);
    }

    public static long currentTimeSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 把UnixTimestamp转换成java.sql.Timestamp
     *
     * @param unixTimestamp UnixTimestamp,毫秒
     * @param unit          unixTimestamp的单位，支持ChronoUnit.SECONDS和ChronoUnit.MILLIS。如果不传入，默认使用ChronoUnit.SECONDS
     * @return
     */
    public static Timestamp convertUnixTimestampToSqlTimestamp(long unixTimestamp, ChronoUnit unit) {
        if (unit != null && (unit != ChronoUnit.SECONDS && unit != ChronoUnit.MILLIS)) {
            throw new UnsupportedOperationException("Unsupported conversion with unit:" + unit.name());
        }
        long unixTimestampMillis = 0L;
        if (unit != null) {
            if (unit == ChronoUnit.MILLIS) {
                unixTimestampMillis = unixTimestamp;
            } else {
                unixTimestampMillis = 1000 * unixTimestamp;
            }
        } else {
            unixTimestampMillis = 1000 * unixTimestamp;
        }

        return new Timestamp(unixTimestampMillis);
    }


}
