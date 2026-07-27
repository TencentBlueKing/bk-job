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
  <jb-form-item
    ref="rollingType"
    :label="$t('滚动对象')"
    required>
    <bk-radio-group
      class="form-item-content radio-check"
      :value="formData[typeField]"
      @change="handleRollingTypeChange">
      <bk-radio :value="1">
        {{ $t('传输目标') }}
      </bk-radio>
      <bk-radio :value="2">
        {{ $t('源文件') }}
      </bk-radio>
    </bk-radio-group>
  </jb-form-item>
</template>

<script setup>
  const props = defineProps({
    typeField: {
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
   * @desc 滚动对象类型更新
   * @param { Number } rollingType
   */
  const handleRollingTypeChange = (rollingType) => {
    emit('on-change', props.typeField, rollingType);
  };
</script>

<style lang="postcss" scoped>
  .radio-check {
    display: flex;
    align-items: center;
    height: 32px;
    gap: 32px;
  }
</style>
