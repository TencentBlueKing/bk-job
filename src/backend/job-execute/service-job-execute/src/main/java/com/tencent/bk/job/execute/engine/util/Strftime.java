/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.execute.engine.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Converts dates to strings using the same format specifiers as strftime
 * <p>
 * Note: This does not mimic strftime perfectly.  Certain strftime commands,
 * are not supported, and will convert as if they were literals.
 * <p>
 * Certain complicated commands, like those dealing with the week of the year
 * probably don't have exactly the same behavior as strftime.
 * <p>
 * These limitations are due to use SimpleDateTime.  If the conversion was done
 * manually, all these limitations could be eliminated.
 * <p>
 * The interface looks like a subset of DateFormat.  Maybe someday someone will make this class
 * extend DateFormat.
 *
 * @author Bip Thelin
 * @author Dan Sandberg
 * @version $Revision: 467222 $, $Date: 2006-10-24 05:17:11 +0200 (Tue, 24 Oct 2006) $
 */
public class Strftime {
    protected static Properties translate;

    /**
     * Initialize our pattern translation
     */
    static {
        translate = new Properties();
        translate.put("a", "EEE");
        translate.put("A", "EEEE");
        translate.put("b", "MMM");
        translate.put("B", "MMMM");
        translate.put("c", "EEE MMM d HH:mm:ss yyyy");

        //There's no way to specify the century in SimpleDateFormat.  We don't want to hard-code
        //20 since this could be wrong for the pre-2000 files.
        //translate.put("C", "20");
        translate.put("d", "dd");
        translate.put("D", "MM/dd/yy");
        translate.put("e", "dd"); //will show as '03' instead of ' 3'
        translate.put("F", "yyyy-MM-dd");
        translate.put("g", "yy");
        translate.put("G", "yyyy");
        translate.put("H", "HH");
        translate.put("h", "MMM");
        translate.put("I", "hh");
        translate.put("j", "DDD");
        translate.put("k", "HH"); //will show as '07' instead of ' 7'
        translate.put("l", "hh"); //will show as '07' instead of ' 7'
        translate.put("m", "MM");
        translate.put("M", "mm");
        translate.put("n", "\n");
        translate.put("p", "a");
        translate.put("P", "a");  //will show as pm instead of PM
        translate.put("r", "hh:mm:ss a");
        translate.put("R", "HH:mm");
        //There's no way to specify this with SimpleDateFormat
        //translate.put("s","seconds since ecpoch");
        translate.put("S", "ss");
        translate.put("t", "\t");
        translate.put("T", "HH:mm:ss");
        //There's no way to specify this with SimpleDateFormat
        //translate.put("u","day of week ( 1-7 )");

        //There's no way to specify this with SimpleDateFormat
        //translate.put("U","week in year with first sunday as first day...");

        translate.put("V", "ww"); //I'm not sure this is always exactly the same

        //There's no way to specify this with SimpleDateFormat
        //translate.put("W","week in year with first monday as first day...");

        //There's no way to specify this with SimpleDateFormat
        //translate.put("w","E");
        translate.put("X", "HH:mm:ss");
        translate.put("x", "MM/dd/yy");
        translate.put("y", "yy");
        translate.put("Y", "yyyy");
        translate.put("Z", "z");
        translate.put("z", "Z");
        translate.put("%", "%");
    }

    protected SimpleDateFormat simpleDateFormat;


    /**
     * Create an instance of this date formatting class
     *
     * @see #Strftime(String, Locale)
     */
    public Strftime(String origFormat) {
        String convertedFormat = convertDateFormat(origFormat);
        simpleDateFormat = new SimpleDateFormat(convertedFormat);
    }

    /**
     * Create an instance of this date formatting class
     *
     * @param origFormat the strftime-style formatting string
     * @param locale     the locale to use for locale-specific conversions
     */
    public Strftime(String origFormat, Locale locale) {
        String convertedFormat = convertDateFormat(origFormat);
        simpleDateFormat = new SimpleDateFormat(convertedFormat, locale);
    }

    /**
     * Format the date according to the strftime-style string given in the constructor.
     *
     * @param date the date to format
     * @return the formatted date
     */
    public String format(Date date) {
        return simpleDateFormat.format(date);
    }

    /**
     * Get the timezone used for formatting conversions
     *
     * @return the timezone
     */
    public TimeZone getTimeZone() {
        return simpleDateFormat.getTimeZone();
    }

    /**
     * Change the timezone used to format dates
     *
     * @see SimpleDateFormat#setTimeZone
     */
    public void setTimeZone(TimeZone timeZone) {
        simpleDateFormat.setTimeZone(timeZone);
    }

    /**
     * Search the provided pattern and get the C standard
     * Date/Time formatting rules and convert them to the
     * Java equivalent.
     *
     * @param pattern The pattern to search
     * @return The modified pattern
     */
    protected String convertDateFormat(String pattern) {
        boolean inside = false;
        boolean mark = false;
        boolean modifiedCommand = false;

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);

            if (c == '%' && !mark) {
                mark = true;
            } else {
                if (mark) {
                    if (modifiedCommand) {
                        //don't do anything--we just wanted to skip a char
                        modifiedCommand = false;
                        mark = false;
                    } else {
                        inside = translateCommand(buf, pattern, i, inside);
                        //It's a modifier code
                        if (c == 'O' || c == 'E') {
                            modifiedCommand = true;
                        } else {
                            mark = false;
                        }
                    }
                } else {
                    if (!inside && c != ' ') {
                        //We start a literal, which we need to quote
                        buf.append("'");
                        inside = true;
                    }

                    buf.append(c);
                }
            }
        }

        if (buf.length() > 0) {
            char lastChar = buf.charAt(buf.length() - 1);

            if (lastChar != '\'' && inside) {
                buf.append('\'');
            }
        }
        return buf.toString();
    }

    protected String quote(String str, boolean insideQuotes) {
        String retVal = str;
        if (!insideQuotes) {
            retVal = '\'' + retVal + '\'';
        }
        return retVal;
    }

    /**
     * Try to get the Java Date/Time formatting associated with
     * the C standard provided.
     *
     * @param buf       The buffer
     * @param pattern   The date/time pattern
     * @param index     The char index
     * @param oldInside Flag value
     * @return True if new is inside buffer
     */
    protected boolean translateCommand(StringBuffer buf, String pattern, int index, boolean oldInside) {
        char firstChar = pattern.charAt(index);
        boolean newInside = oldInside;

        //O and E are modifiers, they mean to present an alternative representation of the next char
        //we just handle the next char as if the O or E wasn't there
        if (firstChar == 'O' || firstChar == 'E') {
            if (index + 1 < pattern.length()) {
                newInside = translateCommand(buf, pattern, index + 1, oldInside);
            } else {
                buf.append(quote("%" + firstChar, oldInside));
            }
        } else {
            String command = translate.getProperty(String.valueOf(firstChar));

            //If we don't find a format, treat it as a literal--That's what apache does
            if (command == null) {
                buf.append(quote("%" + firstChar, oldInside));
            } else {
                //If we were inside quotes, close the quotes
                if (oldInside) {
                    buf.append('\'');
                }
                buf.append(command);
                newInside = false;
            }
        }
        return newInside;
    }
}
