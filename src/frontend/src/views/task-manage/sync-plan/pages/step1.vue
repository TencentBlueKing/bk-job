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
  <layout>
    <div class="sync-plan-step1">
      <detail-layout>
        <detail-item
          :label="$t('template.全局变量.label')"
          style="margin-bottom: 20px;">
          <render-global-var
            :default-field="$t('template.变量值')"
            :diff="variableDiff"
            :list="templateVariableList"
            mode="diff" />
        </detail-item>
        <detail-item :label="$t('template.执行步骤')">
          <render-task-step
            :diff="stepDiff"
            :list="templateStepList"
            mode="diff" />
        </detail-item>
      </detail-layout>
    </div>
    <template #footer>
      <bk-button
        @click="handleCancel">
        {{ $t('template.取消') }}
      </bk-button>
      <bk-button
        class="w120"
        theme="primary"
        @click="handleNext">
        {{ $t('template.下一步') }}
      </bk-button>
    </template>
  </layout>
</template>
<script>
  import DetailLayout from '@components/detail-layout';
  import DetailItem from '@components/detail-layout/item';

  import RenderGlobalVar from '../../common/render-global-var';
  import RenderTaskStep from '../../common/render-task-step';
  import Layout from '../components/layout';
  import {
    diffStepSimple,
    diffVariableSimple,
    mergeList,
  } from '../components/utils';

  export default {
    name: 'SyncPlanStep1',
    components: {
      DetailLayout,
      DetailItem,
      Layout,
      RenderGlobalVar,
      RenderTaskStep,
    },
    props: {
      templateInfo: {
        type: Object,
        default: () => ({
          variables: [],
          stepList: [],
        }),
      },
      planInfo: {
        type: Object,
        default: () => ({
          variableList: [],
          stepList: [],
        }),
      },
    },
    data() {
      return {
        templateVariableList: [],
        templateStepList: [],
        variableDiff: {},
        stepDiff: {},
      };
    },
    created() {
      this.templateVariableList = Object.freeze(mergeList(this.templateInfo.variables, this.planInfo.variableList));
      this.variableDiff = Object.freeze(diffVariableSimple(this.templateInfo.variables, this.planInfo.variableList));

      const [
        templateStepList,
        stepDiff,
      ] = diffStepSimple(this.templateInfo.stepList, this.planInfo.stepList);
      this.templateStepList = Object.freeze(templateStepList);
      this.stepDiff = Object.freeze(stepDiff);
    },
    methods: {
      /**
       * @desc 切换下一步
       */
      handleNext() {
        this.$emit('on-change', 2);
      },
      /**
       * @desc 取消同步
       */
      handleCancel() {
        this.$emit('on-cancel');
      },
    },
  };
</script>
<style lang="postcss">
  .sync-plan-step1 {
    .detail-label {
      padding-right: 24px;
    }
  }
</style>
<style lang='postcss' scoped>
  .sync-plan-step1 {
    padding: 24px 24px 60px;
  }
</style>
