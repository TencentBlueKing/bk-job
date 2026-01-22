<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
-->

<template>
  <jb-form-item :rules="rules">
    <bk-checkbox
      :value="isEndTime"
      @change="handleChange">
      {{ $t('cron.设置结束时间') }}
    </bk-checkbox>
    <div v-if="isEndTime">
      <bk-date-picker
        :clearable="false"
        :options="dateOptions"
        :placeholder="$t('cron.选择日期时间')"
        style="width: 100%;"
        transfer
        type="datetime"
        :value="formData.endTime"
        @change="handleEndTimeChange" />
    </div>
  </jb-form-item>
</template>
<script>
  import {
    prettyDateTimeFormat,
  } from '@utils/assist';

  import { getTimestamp } from '@/utils/assist/time';

  export default {
    name: '',
    props: {
      rules: Array,
      formData: {
        type: Object,
        required: true,
      },
      timezone: {
        type: String,
        required: true,
      },
    },
    data() {
      return {
        isEndTime: false,
      };
    },
    computed: {
      dateOptions() {
        const disabledDate = (date) => {
          const prettyDate = prettyDateTimeFormat(date); // 当前经过格式化的日期 --去除时区
          return getTimestamp({ date: prettyDate, timezone: this.timezone }) < Date.now() - 86400000;
        };
        return {
          disabledDate,
        };
      },
    },
    watch: {
      formData: {
        handler() {
          if (this.formData.endTime) {
            this.isEndTime = true;
          }
        },
        immediate: true,
      },
    },
    methods: {
      handleChange(value) {
        this.isEndTime = value;
        const endTime = value ? prettyDateTimeFormat(Date.now() + 86400000) : '';
        this.handleEndTimeChange(endTime);
      },
      handleEndTimeChange(value) {
        this.$emit('on-change', {
          endTime: value,
        });
      },
    },
  };
</script>
