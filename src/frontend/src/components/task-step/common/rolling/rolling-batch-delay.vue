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
  <div ref="batchStartWait">
    <jb-form-item
      v-for="item in fields"
      :key="item.field"
      :ref="item.field"
      :label="$t(item.label)"
      :property="item.field"
      required
      :rules="item.rules">
      <div class="form-item-content">
        <bk-input
          :max="3600000"
          :min="0"
          type="number"
          :value="formData[item.field]"
          @change="handleChange(item.field, $event)" />
      </div>
    </jb-form-item>
  </div>
</template>

<script setup>
  import I18n from '@/i18n';

  const props = defineProps({
    batchStartWaitFixedMsField: {
      type: String,
      required: true,
    },

    batchStartWaitRandomMinMsField: {
      type: String,
      required: true,
    },

    batchStartWaitRandomMaxMsField: {
      type: String,
      required: true,
    },

    formData: {
      type: Object,
      required: true,
    },
  });

  const emit = defineEmits(['on-change']);

  /**
   * 创建校验规则
   */
  const createRule = (message) => {
    return [
      {
        validator: value => {
          return value === 0 || (value && Number(value) > 0 ) 
        },
        message: I18n.t(message),
        trigger: 'blur',
      },
    ];
  };

  const fields = [
    {
      field: props.batchStartWaitFixedMsField,
      label: '批次间固定延迟（ms）',
      rules: createRule('批次间固定延迟必填'),
    },
    {
      field: props.batchStartWaitRandomMinMsField,
      label: '批次间随机延迟下限（ms）',
      rules: createRule('批次间随机延迟下限必填'),
    },
    {
      field: props.batchStartWaitRandomMaxMsField,
      label: '批次间随机延迟上限（ms）',
      rules: createRule('批次间随机延迟上限必填'),
    },
  ];

  const handleChange = (field, value) => {
    if(Number.isNaN(+value)) return;
    emit('on-change', field, value === '' ? '' : +value);
  };
</script>
