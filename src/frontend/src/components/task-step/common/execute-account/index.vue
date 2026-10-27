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
    :label="$t('执行账号')"
    :property="field"
    required
    :rules="rules">
    <compose-form-item>
      <template v-if="supportAccountVariable">
        <bk-select
          :clearable="false"
          :style="selectorStyle"
          :value="selectType"
          @change="handleTypeChange">
          <bk-option
            id="variable"
            :name="$t('全局变量')" />
          <bk-option
            id="account"
            :name="$t('手动添加')" />
        </bk-select>
        <template v-if="isVariableType">
          <bk-select
            class="account-global-variable-select"
            :clearable="false"
            :placeholder="$t('请选择账号变量')"
            :value="localVariable"
            @change="handleVariableChange">
            <bk-option
              v-for="(item, index) in accountVariable"
              :id="item.name"
              :key="index"
              :name="item.name" />
          </bk-select>
        </template>
        <template v-else>
          <account-select
            class="var-acc-select"
            :type="realAccountType"
            :value="localAccount"
            @change="handleAccountChange" />
        </template>
      </template>
      <account-select
        v-else
        class="form-item-content"
        :type="realAccountType"
        :value="localAccount"
        @change="handleAccountChange" />
    </compose-form-item>
  </jb-form-item>
</template>
<script setup>
  import { computed, ref, watch } from 'vue';

  import GlobalVariableModel from '@model/task/global-variable';

  import {
    formatScriptTypeValue,
  } from '@utils/assist';

  import AccountSelect from '@components/account-select';
  import ComposeFormItem from '@components/compose-form-item';

  import i18n from '@/i18n';

  const props = defineProps({
    field: {
      type: String,
      required: true,
    },
    formData: {
      type: Object,
      required: true,
    },
    // 可选的账号全局变量
    variable: {
      type: Array,
      default: () => [],
    },
    scriptLanguageField: {
      type: String,
    },
    accountType: {
      type: String,
      default: 'system',
    },
    // 账号变量字段名，用于回显账号变量（默认 `${field}Var`）
    accountVarField: {
      type: String,
      default: '',
    },
    // 是否需要【执行账号】全局变量
    supportAccountVariable: {
      type: Boolean,
      default: false,
    },
  });

  const emits = defineEmits(['on-change']);

  // 账号变量字段名，未显式传入时默认 `${field}Var`
  const accountVarField = computed(() => props.accountVarField || `${props.field}Var`);

  // variable：账号全局变量；account：手动添加
  const selectType = ref('account');
  const localVariable = ref('');
  const localAccount = ref('');

  const realAccountType = computed(() => {
    if (props.scriptLanguageField
      && formatScriptTypeValue(props.formData[props.scriptLanguageField]) === 'SQL') {
      return 'db';
    }
    return props.accountType;
  });
  /**
   * @desc 是否选择全局变量
   */
  const isVariableType = computed(() => selectType.value === 'variable');
  /**
   * @desc 可选的账号全局变量
   */
  const accountVariable = computed(() => props.variable.filter(item => item.type === GlobalVariableModel.TYPE_ACCOUNT));

  /**
   * @desc 切换选择方式的展示样式
   */
  const selectorStyle = computed(() => ({
    width: i18n.locale === 'en-US' ? '156px' : '120px',
  }));
  /**
   * @desc 当前字段对应的表单值（手动添加的账号 id）
   */
  const fieldValue = computed(() => props.formData[props.field]);
  /**
   * @desc 账号变量字段对应的表单值（变量名）
   */
  const accountVarValue = computed(() => props.formData[accountVarField.value]);
  /**
   * @desc 同时监听账号与账号变量字段
   */
  const accountState = computed(() => [
    fieldValue.value,
    accountVarValue.value,
  ]);

  const rules = [
    {
      message: i18n.t('执行账号必填'),
      trigger: 'blur',
      validator: () => {
        const accountVal = props.formData[props.field];
        const accountVarVal = props.formData[accountVarField.value];
        const isEmpty = val => val === '' || val === undefined || val === null;
        return !(isEmpty(accountVal) && isEmpty(accountVarVal));
      },
    },
  ];

  watch(accountState, ([accountVal, accountVarVal]) => {
    // 编辑态，账号变量优先于手动填写的账号
    if (accountVarVal && typeof accountVarVal === 'string'
      && accountVariable.value.find(_ => _.name === accountVarVal)) {
      selectType.value = 'variable';
      localVariable.value = accountVarVal;
    } else if (accountVal !== '' && accountVal !== undefined && accountVal !== null) {
      selectType.value = 'account';
      localAccount.value = accountVal;
    }
  }, {
    immediate: true,
  });

  /**
   * @desc 触发变更
   * 手动添加时回写 account，选择变量时回写 accountVar
   */
  const triggerChange = () => {
    if (isVariableType.value) {
      emits('on-change', props.field, {
        activeField: accountVarField.value,
        activeValue: localVariable.value,
        inactiveField: props.field,
      });
    } else {
      emits('on-change', props.field, {
        activeField: props.field,
        activeValue: localAccount.value,
        inactiveField: accountVarField.value,
      });
    }
  };
  /**
   * @desc 切换选择方式（全局变量 / 手动添加）
   */
  const handleTypeChange = (value) => {
    selectType.value = value;
    triggerChange();
  };
  /**
   * @desc 选择账号全局变量
   */
  const handleVariableChange = (value) => {
    localVariable.value = value;
    triggerChange();
  };
  /**
   * @desc 选择具体账号
   */
  const handleAccountChange = (value) => {
    localAccount.value = value;
    triggerChange();
  };
</script>
<style lang="postcss">
  .compose-form-item {
    .account-global-variable-select {
      width: 376px;
      float: left;
    }

    .var-acc-select {
      float: left;
      width: 376px;
      margin-left: -1px;
    }
  }

  html[lang="en-US"] {
    .account-global-variable-select,
    .var-acc-select {
      width: 341px;
    }
  }
</style>
