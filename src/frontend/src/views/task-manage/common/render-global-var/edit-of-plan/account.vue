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
  <jb-form :model="formData">
    <jb-form-item :label="$t('template.变量名称')">
      <bk-input
        v-model="formData.name"
        disabled />
    </jb-form-item>
    <jb-form-item :label="$t('template.变量值')">
      <account-select
        v-model="formData.defaultValue"
        :disabled="valDisabled"
        :placeholder="$t('template.请输入变量的初始值_可选')" />
    </jb-form-item>
    <jb-form-item :label="$t('template.变量描述')">
      <bk-input
        v-model="formData.description"
        disabled
        maxlength="100"
        :row="5"
        type="textarea" />
    </jb-form-item>
    <jb-form-item>
      <bk-checkbox
        v-model="formData.required"
        disabled
        :false-value="0"
        :true-value="1">
        {{ $t('template.执行时必填') }}
      </bk-checkbox>
    </jb-form-item>
  </jb-form>
</template>
<script setup>
  import { reactive, watch } from 'vue';

  import AccountSelect from '@components/account-select';

  const getDefaultData = () => ({
    id: 0,
    delete: 0,
    // 变量名
    name: '',
    // 默认值（执行账号ID）
    defaultValue: '',
    // 变量描述
    description: '',
    // 必填 0-非必填 1-必填
    required: 0,
  });

  const props = defineProps({
    data: {
      type: Object,
      default: () => ({}),
    },
    valDisabled: {
      type: Boolean,
      default: false,
    },
  });

  const formData = reactive(getDefaultData());

  watch(() => props.data, (value) => {
    if (Object.keys(value).length) {
      const { name, defaultValue, description, required, id } = value;
      const del = value.delete;
      // 回显需要number类型
      Object.assign(formData, {
        name,
        defaultValue: Number(defaultValue),
        description,
        required,
        id,
      });
      formData.delete = del;
    }
  }, {
    immediate: true,
  });

  const submit = () => Promise.resolve({
    ...formData,
    type: 7,
  });

  const reset = () => {
    Object.assign(formData, getDefaultData());
  };

  defineExpose({
    submit,
    reset,
  });
</script>
