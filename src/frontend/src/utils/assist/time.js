/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 * ---------------------------------------------------
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
import dayjs from 'dayjs';

import timezonesList from '@/utils/world-timezones.json';

const timezone = require('dayjs/plugin/timezone');
const utc = require('dayjs/plugin/utc');

dayjs.extend(utc);
dayjs.extend(timezone);

export const prettyDateTimeFormat = (target) => {
  if (!target) {
    return '';
  }
  const formatStr = (str) => {
    if (String(str).length === 1) {
      return `0${str}`;
    }
    return str;
  };
  const d = new Date(target);
  const year = d.getFullYear();
  const month = formatStr(d.getMonth() + 1);
  const date = formatStr(d.getDate());
  const hours = formatStr(d.getHours());
  const minutes = formatStr(d.getMinutes());
  const seconds = formatStr(d.getSeconds());
  return `${year}-${month}-${date} ${hours}:${minutes}:${seconds}`;
};

export const prettyDateFormat = (target) => {
  if (!target) {
    return '';
  }
  const formatStr = (str) => {
    if (String(str).length === 1) {
      return `0${str}`;
    }
    return str;
  };
  const d = new Date(target);
  const year = d.getFullYear();
  const month = formatStr(d.getMonth() + 1);
  const date = formatStr(d.getDate());
  return `${year}-${month}-${date}`;
};

export const generatorDefaultCronTime = () => {
  const nextHourDate = new Date(Date.now() + 3600000);
  const formatStr = (str) => {
    if (String(str).length === 1) {
      return `0${str}`;
    }
    return str;
  };
  const d = new Date(nextHourDate);
  const year = d.getFullYear();
  const month = formatStr(d.getMonth() + 1);
  const date = formatStr(d.getDate());
  const hours = formatStr(d.getHours());

  return `${year}-${month}-${date} ${hours}:00:00`;
};

export const transformTimeFriendly = (target) => {
  const totalTime = parseFloat(target);
  if (totalTime < 60) {
    return `${totalTime}s`;
  }
  const dayUnit = 86400;
  const hourUnit = 3600;
  const minUnit = 60;
  const stack = [];
  const day = Math.floor(target / dayUnit);
  if (day) {
    stack.push(`${day}d`);
  }
  const hour = Math.floor((target % dayUnit) / hourUnit);
  if (hour) {
    stack.push(`${hour}h`);
  }
  const min = Math.floor((target % hourUnit) / minUnit);
  if (min) {
    stack.push(`${min}m`);
  }
  const second = Math.ceil(target % 60);
  stack.push(`${second}s`);
  return stack.join(' ');
};

// 通过字符串日期格式拆分出年月日时分秒
export function extractDateTime(dateTimeStr) {
  const regex = /^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$/;
  const match = dateTimeStr.match(regex);

  if (match) {
    return {
      year: match[1],
      month: match[2],
      day: match[3],
      hour: match[4],
      minute: match[5],
      second: match[6],
    };
  }
  console.warn('日期格式不匹配');
  return null;
}

// UTC+08:00 转换成0800
export function formatTimezoneOffset(utcOffset) {
  // 正则表达式匹配 UTC+/- 时区格式
  const regex = /UTC([+-])(\d{2}):(\d{2})/;
  const match = utcOffset.match(regex);

  if (match) {
    const sign = match[1]; // 获取符号 (+ 或 -)
    const hours = match[2]; // 获取小时
    const minutes = match[3]; // 获取分钟
    // 拼接成目标格式
    return sign === '+' ? hours + minutes : sign + hours + minutes;
  }
  // 如果输入格式不匹配，返回原字符串或抛出错误
  console.warn('输入的时区格式不支持:', utcOffset);
  return utcOffset || '0000';
}

export function getTimezoneInfo(defaultTimezone) {
  const timezone = defaultTimezone || getTimeZone();
  const { country = '', offset = '' } = timezonesList[timezone] || {};
  return { country, offset };
}

export function getTimeZone() {
  const { USER_TIME_ZONE, BUSINESS_TIME_ZONE, DEFAULT_DISPLAY_TIME_ZONE } = window.PROJECT_CONFIG;
  return USER_TIME_ZONE || BUSINESS_TIME_ZONE || DEFAULT_DISPLAY_TIME_ZONE;
}

export function getFullTimeZone(defaultTimezone) {
  const timezone = defaultTimezone || getTimeZone();
  const { country = '', offset = '' } = getTimezoneInfo(timezone);
  return `${timezone} ${country} ${offset}`;
}

// 时间戳转换成日期格式
export function getTime(options) {
  const {
    timestamp,
    timezone,
    format = 'YYYY-MM-DD HH:mm:ss',
  } = options;

  if (!timestamp) return '--';
  return dayjs.tz(timestamp, timezone || getTimeZone())
    .format(format);
}

// 日期格式转换成时间戳 转换的日期格式不能带时区
export function getTimestamp(options) {
  const {
    date,
    timezone,
  } = options;

  if (!date) return;
  return dayjs.tz(date, timezone || getTimeZone())
    .valueOf();
}

export function getTimeTooltip(time) {
  return `${time} ${getFullTimeZone()}`;
}
