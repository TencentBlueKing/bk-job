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
    :model-value="dateValue"
    :timezone.sync="timezone"
    @update:modelValue="handleDateChange" />
</template>
<script setup>
  import { computed, onMounted, ref } from 'vue';

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
    },
  });

  const dateValue = ref([]); // 只能接受时间戳，接受日期组件内部会在转换一次，会导致显示数据错误

  const emits = defineEmits(['changeDate', 'changeTimezone']);

  const timezone = computed({
    get() {
      return props.timezone;
    },
    set(timezone) {
      emits('changeTimezone', timezone);
    },
  });

  onMounted(() => {
    const { startTime, endTime } = props.date;
    if (!startTime) {
      const currentTime = new Date().getTime();
      const startDate = model.getTime({ timestamp: currentTime - props.day * 86400000, timezone: timezone.value });
      const endDate = model.getTime({ timestamp: currentTime, timezone: timezone.value });
      dateValue.value = [currentTime - props.day * 86400000, currentTime];
      emits('setDate', { startTime: startDate, endTime: endDate });
      return;
    }
    dateValue.value = [model.getTimestamp({ date: startTime, timezone: timezone.value }), model.getTimestamp({ date: endTime, timezone: timezone.value })];
  });

  const handleDateChange = (date, info) => {
    const [{ formatText: startTime }, { formatText: endTime }] = info;
    dateValue.value = date;
    emits('changeDate', { startTime, endTime });
  };
</script>
