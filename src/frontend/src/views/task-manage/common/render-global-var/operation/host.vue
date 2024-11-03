<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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
    <jb-form
      ref="varHostForm"
      :model="formData"
      :rules="rules">
      <jb-form-item
        :label="$t('template.变量名称')"
        property="name"
        required>
        <jb-input
          v-model="formData.name"
          :maxlength="30"
          :placeholder="$t('template.变量名仅支持大小写英文字母或下划线_必填')" />
      </jb-form-item>
      <jb-form-item
        ref="defaultTargetValue"
        :desc="$t('template.仅作用于创建执行方案时的初始变量值，后续更改不会同步到执行方案')"
        :label="$t('template.初始值')"
        property="defaultTargetValue">
        <div>
          <bk-button
            class="mr10"
            @click="handleOpenChooseIp">
            <icon type="plus" />
            {{ $t('template.选择主机') }}
          </bk-button>
          <bk-button
            v-if="isShowClear"
            @click="handleClearDefault">
            {{ $t('template.清空') }}
          </bk-button>
        </div>
        <ip-selector
          :original-value="originalExecuteObjectsInfo"
          :show-dialog="isShowChooseIp"
          show-view
          :value="formData.defaultTargetValue.executeObjectsInfo"
          @change="handleExecuteObjectsInfoChange"
          @close-dialog="handleCloseIPSelector" />
      </jb-form-item>
      <jb-form-item :label="$t('template.变量描述')">
        <bk-input
          v-model="formData.description"
          maxlength="100"
          :placeholder="$t('template.这里可以备注变量的用途、使用说明等信息_可选')"
          type="textarea" />
      </jb-form-item>
      <jb-form-item style="margin-bottom: 0;">
        <bk-checkbox
          v-model="formData.required"
          :false-value="0"
          :true-value="1">
          {{ $t('template.执行时必填') }}
        </bk-checkbox>
      </jb-form-item>
    </jb-form>
  </div>
</template>
<script>
  import ExecuteTargetModel from '@model/execute-target';
  import TaskGlobalVariableModel from '@model/task/global-variable';

  import {
    globalVariableNameRule,
  } from '@utils/validator';

  import JbInput from '@components/jb-input';

  import I18n from '@/i18n';

  export default {
    name: 'VarHost',
    components: {
      JbInput,
    },
    props: {
      variable: {
        type: Array,
        default: () => [],
      },
      data: {
        type: Object,
        default: () => ({}),
      },
    },
    data() {
      return {
        formData: { ...this.data },
        isShowChooseIp: false,
      };
    },
    computed: {
      /**
       * @desc 显示清空按钮
       * @returns { Boolean }
       */
      isShowClear() {
        return !ExecuteTargetModel.isExecuteObjectsInfoEmpty(this.formData.defaultTargetValue.executeObjectsInfo);
      },
    },
    created() {
      if (this.$route.name !== 'templateCreate') {
        this.originalExecuteObjectsInfo = ExecuteTargetModel.cloneExecuteObjectsInfo(this.formData.defaultTargetValue.executeObjectsInfo);
      } else {
        this.originalExecuteObjectsInfo = null;
      }

      this.rules = {
        name: [
          {
            required: true,
            message: I18n.t('template.变量名称必填'),
            trigger: 'blur',
          },
          {
            validator: globalVariableNameRule.validator,
            message: globalVariableNameRule.message,
            trigger: 'blur',
          },
          {
            validator: val => !this.variable.some(item => item.name === val),
            message: I18n.t('template.变量名称已存在，请重新输入'),
            trigger: 'blur',
          },
        ],
      };
    },
    methods: {
      /**
       * @desc 编辑主机信息
       * @param { executeObjectsInfo } 主机信息
       */
      handleExecuteObjectsInfoChange(executeObjectsInfo) {
        this.formData.defaultTargetValue.executeObjectsInfo = executeObjectsInfo;
      },
      /**
       * @desc 显示 IP 选择器
       */
      handleOpenChooseIp() {
        this.isShowChooseIp = true;
      },
      handleCloseIPSelector() {
        this.isShowChooseIp = false;
      },
      /**
       * @desc 清空主机信息
       */
      handleClearDefault() {
        const { executeObjectsInfo } = new ExecuteTargetModel({});
        this.formData.defaultTargetValue.executeObjectsInfo = executeObjectsInfo;
      },
      /**
       * @desc 保存变量
       */
      submit() {
        return this.$refs.varHostForm.validate()
          .then(() => {
            this.$emit('on-change', {
              ...this.formData,
              type: TaskGlobalVariableModel.TYPE_HOST,
            });
          }, validator => Promise.reject(validator.content));
      },
    },
  };
</script>
<style lang="postcss" scoped>
  .view-server-panel {
    margin-top: 10px;
  }
</style>
