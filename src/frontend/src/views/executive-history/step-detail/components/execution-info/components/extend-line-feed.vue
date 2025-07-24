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
  <div
    v-if="activePanel === 'scriptLog'"
    class="extend-item"
    style="padding-left: 16px; border-left: 1px solid #262626;">
    <bk-switcher
      size="small"
      theme="primary"
      :value="value"
      @change="handleChange" />
    <span style="padding-left: 7px; font-size: 12px; color: #979ba5;">{{ $t('history.自动换行') }}</span>
  </div>
</template>
<script setup>
  import { onMounted } from 'vue';
  const props = defineProps({
    value: {
      type: Boolean,
      default: false,
    },
    activePanel: {
      type: String,
      required: true,
    },
  });

  const emits = defineEmits([
    'change',
    'input',
  ]);

  const SCRIPT_LOG_AUTO_LINE_FEED = 'script_log_line_feed';

  const handleChange = () => {
    const result  = !props.value;
    if (result) {
      localStorage.setItem(SCRIPT_LOG_AUTO_LINE_FEED, true);
    } else {
      localStorage.removeItem(SCRIPT_LOG_AUTO_LINE_FEED);
    }

    emits('input', result);
    emits('change', result);
  };

  onMounted(() => {
    emits('input', Boolean(localStorage.getItem(SCRIPT_LOG_AUTO_LINE_FEED)));
  });
</script>

