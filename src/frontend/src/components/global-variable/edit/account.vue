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
    :class="{ 'variable-value-error': isError }"
    style="position: relative;">
    <account-select
      ref="selectRef"
      v-bk-tooltips="descPopover"
      :disabled="disabled"
      :value="value"
      @change="handleChange" />
    <p
      v-if="isError"
      class="variable-error">
      {{ $t('该变量的值必填') }}
    </p>
  </div>
</template>
<script setup>
  import DOMPurify from 'dompurify';
  import { computed, ref, watch } from 'vue';

  import AccountSelect from '@components/account-select';

  const props = defineProps({
    data: {
      type: Object,
      required: true,
    },
    placement: {
      type: String,
      required: true,
    },
    readonly: {
      type: Boolean,
      default: false,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    withValidate: {
      type: Boolean,
      default: true,
    },
  });


  const selectRef = ref(null);
  const value = ref('');

  const isError = computed(() => {
    if (props.data.required !== 1) {
      return false;
    }
    return !value.value;
  });

  const descPopover = computed(() => ({
    theme: 'light',
    extCls: 'variable-desc-tippy',
    trigger: 'click mouseenter',
    hideOnClick: false,
    content: DOMPurify.sanitize(props.data.description),
    disabled: !props.data.description,
  }));

  const init = () => {
    // defaultValue 为字符串，回显时需转换成number类型
    const val = props.data.defaultValue || props.data.value;
    value.value = val ? Number(val) : '';
  };

  init();

  const handleChange = (val) => {
    value.value = val;
    window.changeFlag = true;
  };

  const reset = () => {
    init();
  };

  const validate = () => {
    const { type, id, name } = props.data;
    const data = {
      id,
      name,
      type,
      value: value.value,
      targetValue: {},
    };
    return new Promise((resolve, reject) => {
      if (props.withValidate && isError.value) {
        return reject(new Error('account error'));
      }
      resolve(data);
    });
  };

  watch(() => props.placement, (newVal) => {
    if (!props.data.description) return;
    // eslint-disable-next-line no-underscore-dangle
    const tippy = selectRef.value && selectRef.value.$el && selectRef.value.$el._tippy;
    if (tippy) {
      tippy.set({ placement: newVal });
    }
  });

  defineExpose({
    reset,
    validate,
  });
</script>
