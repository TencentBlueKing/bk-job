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
  <div>
    <div class="name">
      <span>{{ data.name }}</span>
      <span
        class="remove-flag"
        @click="handleRemove">
        <icon type="reduce-fill" />
      </span>
    </div>
    <account-select
      v-bk-tooltips="descPopover"
      :value="value"
      @change="handleChange" />
  </div>
</template>
<script setup>
  import DOMPurify from 'dompurify';
  import { computed } from 'vue';

  import AccountSelect from '@components/account-select';

  const props = defineProps({
    data: {
      type: Object,
      required: true,
    },
    value: {
      type: [
        Number, String,
      ],
    },
  });
  const emits = defineEmits(['on-remove', 'on-change']);

  const descPopover = computed(() => ({
    theme: 'light',
    extCls: 'variable-desc-tippy',
    trigger: 'click mouseenter',
    placement: 'left',
    hideOnClick: false,
    content: `<div style="max-width: 340px">${DOMPurify.sanitize(props.data.description)}</div>`,
    disabled: !props.data.description,
  }));

  const handleRemove = () => {
    emits('on-remove');
  };
  const handleChange = (value) => {
    emits('on-change', value);
  };
</script>
