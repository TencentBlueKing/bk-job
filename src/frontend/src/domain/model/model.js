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

export default class Model {
  getDefaultValue(value) {
    return value || '--';
  }

  getTimeZone() {
    const { USER_TIME_ZONE, BUSINESS_TIME_ZONE, DEFAULT_DISPLAY_TIME_ZONE } = window.PROJECT_CONFIG;
    return USER_TIME_ZONE || BUSINESS_TIME_ZONE || DEFAULT_DISPLAY_TIME_ZONE;
  }

  getFullTimeZone(defaultTimezone) {
    const timezone = defaultTimezone || this.getTimeZone();
    const { country, offset } = timezonesList[timezone] || {};
    return `${timezone} ${country} ${offset}`;
  }

  getTime(options) {
    const {
      timestamp,
      timezone,
      format = 'YYYY-MM-DD HH:mm:ss',
    } = options;

    if (!timestamp) return '--';
    return dayjs.tz(timestamp, timezone || this.getTimeZone())
      .format(format);
  }

  getTimeTooltip(time) {
    return `${time} ${this.getFullTimeZone()}`;
  }

  get createTimeText() {
    return this.getTime({ timestamp: this.createTime });
  }

  get lastModifyTimeText() {
    return this.getTime({ timestamp: this.lastModifyTime });
  }

  get startTimeText() {
    return this.getTime({ timestamp: this.startTime });
  }

  get endTimeText() {
    return this.getTime({ timestamp: this.endTime });
  }

  get createTimeTooltipsText() {
    return this.getTimeTooltip(this.createTimeText);
  }

  get lastModifyTimeTooltipsText() {
    return this.getTimeTooltip(this.lastModifyTimeText);
  }

  get startTimeTooltipsText() {
    return this.getTimeTooltip(this.startTimeText);
  }

  get endTimeTooltipsText() {
    return this.getTimeTooltip(this.endTimeText);
  }
}
