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
  <jb-form
    ref="varHostForm"
    :model="formData">
    <jb-form-item :label="$t('template.变量名称')">
      <bk-input
        v-model="formData.name"
        disabled />
    </jb-form-item>
    <jb-form-item :label="$t('template.变量值')">
      <section>
        <bk-button @click="handleShowChooseIp">
          <icon type="plus" />
          {{ $t('template.选择主机') }}
        </bk-button>
        <bk-button
          v-if="isShowClear"
          style="margin-left: 10px;"
          @click="handleClear">
          {{ $t('template.清空') }}
        </bk-button>
      </section>
      <ip-selector
        :original-value="originalExecuteObjectsInfoInfo"
        :show-dialog="isShowChooseIp"
        show-view
        :value="formData.defaultTargetValue.executeObjectsInfo"
        @change="handleExecuteObjectsInfoChange"
        @close-dialog="handleCloseIPSelector" />
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
<script>
  import _ from 'lodash';

  import ExecuteTargetModel from '@model/execute-target';
  import TaskGlobalVariableModel from '@model/task/global-variable';

  const getDefaultData = () => ({
    id: 0,
    delete: 0,
    // 变量名
    name: '',
    // 执行目标信息
    defaultTargetValue: {
      executeObjectsInfo: {},
      variable: '',
    },
    // 变量描述
    description: '',
    // 必填 0-非必填 1-必填
    required: 0,
  });

  export default {
    name: 'VarHost',
    props: {
      data: {
        type: Object,
        default: () => ({}),
      },
    },
    data() {
      return {
        formData: {},
        isShowChooseIp: false,
      };
    },
    computed: {
      isShowClear() {
        return !ExecuteTargetModel.isExecuteObjectsInfoEmpty(this.formData.defaultTargetValue.executeObjectsInfo);
      },
    },
    watch: {
      data: {
        handler(data) {
          this.formData = new TaskGlobalVariableModel(data);
        },
        immediate: true,
      },
    },
    created() {
      this.originalExecuteObjectsInfoInfo = ExecuteTargetModel.cloneExecuteObjectsInfo(this.formData.defaultTargetValue.executeObjectsInfo);
    },
    methods: {
      handleExecuteObjectsInfoChange(executeObjectsInfo) {
        this.formData.defaultTargetValue.executeObjectsInfo = Object.freeze(executeObjectsInfo);
      },
      handleShowChooseIp() {
        this.isShowChooseIp = true;
      },
      handleCloseIPSelector() {
        this.isShowChooseIp = false;
      },

      handleClear() {
        const { executeObjectsInfo } = new ExecuteTargetModel({});
        this.formData.defaultTargetValue.executeObjectsInfo = executeObjectsInfo;
      },
      submit() {
        return Promise.resolve({
          ...this.formData,
          type: TaskGlobalVariableModel.TYPE_HOST,
        });
      },

      reset() {
        this.formData = new TaskGlobalVariableModel(getDefaultData);
      },
    },
  };
</script>
<style lang="postcss" scoped>
  .view-server-panel {
    margin-top: 10px;
  }
</style>
