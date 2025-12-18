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
  <date-picker
    ref="datePicker"
    :model-value="defaultDateTime"
    :timezone.sync="timezone"
    @update:modelValue="handleDateChange" />
</template>
<script setup>
  import { computed, watch } from 'vue';

  import DatePicker from '@blueking/date-picker/vue2';

  import Model from '@/domain/model/model';

  import '@blueking/date-picker/vue2/vue2.css';

  const model = new Model();

  const props = defineProps({
    date: {
      type: Object,
      default: () => ({}),
    },
    timezone: String,
    day: { // 没有时间数据，默认查几天前的
      type: Number,
      default: 1,
    }
  });

  const emits = defineEmits(['changeDate', 'changeTimezone']);

  const timezone = computed({
    get() {
      return props.timezone;
    },
    set(timezone) {
      emits('changeTimezone', timezone);
    },
  });

  const defaultDateTime = computed(() => {
    const { startTime, endTime } = props.date;
    if (startTime) {
      // 如果有数据，则根据时区将此时间格式转换成时间戳传给组件 --todo 组件修复回显问题在看传入什么，现在直接传日期
      // return [model.getTimestamp({ date: startTime, timezone: timezone.value }), model.getTimestamp({ date: endTime, timezone: timezone.value })];
      // return
      return [startTime, endTime];
    }
    // 默认为近24小时的数据
    const currentTime = new Date().getTime();
    return [model.getTime({ timestamp: currentTime - props.day * 86400000, timezone: timezone.value }), model.getTime({ timestamp: currentTime, timezone: timezone.value })];
  });

  watch(() => defaultDateTime.value, (value) => {
    const [startTime, endTime] = value;
    emits('setDate', { startTime, endTime });
  }, {
    immediate: true,
  });

  const handleDateChange = (date, info) => {
    const [{ formatText: startTime }, { formatText: endTime }] = info;
    emits('changeDate', { startTime, endTime });
  };
</script>
