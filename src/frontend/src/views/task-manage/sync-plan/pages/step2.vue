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
  <layout class="sync-step2">
    <div class="container">
      <div class="sync-wraper">
        <div class="sync-layout sync-header">
          <div class="sync-before">
            <div class="title">
              {{ $t('template.同步前') }}
            </div>
          </div>
          <div class="sync-after">
            <div class="title">
              {{ $t('template.同步后') }}
            </div>
          </div>
        </div>
        <div class="sync-content">
          <scroll-faker id="asynContent">
            <div class="sync-layout">
              <div class="sync-before block-title">
                {{ $t('template.全局变量.label') }}
                <span class="global-variable-tips">{{ $t('template.全局变量的 “初始值” 不会被同步到执行方案') }}</span>
              </div>
              <div class="sync-after block-title">
                {{ $t('template.全局变量.label') }}
                <span class="global-variable-tips">{{ $t('template.全局变量的 “初始值” 不会被同步到执行方案') }}</span>
              </div>
            </div>
            <div
              v-for="index in templateVariableList.length"
              :key="`${'variable' + index}`"
              class="sync-layout">
              <div class="sync-before">
                <diff-global-variable
                  v-if="planVariableList[index - 1]"
                  :data="planVariableList[index - 1]"
                  :diff="beforeVariableDiff" />
              </div>
              <div class="sync-after">
                <diff-global-variable
                  v-if="templateVariableList[index - 1]"
                  :data="templateVariableList[index - 1]"
                  :diff="variableDiff"
                  type="sync-after" />
              </div>
            </div>
            <div class="sync-layout">
              <div class="sync-before block-title">
                {{ $t('template.作业步骤.label') }}
              </div>
              <div class="sync-after block-title">
                {{ $t('template.作业步骤.label') }}
              </div>
            </div>
            <div
              v-for="index in templateStepList.length"
              :key="`${'step' + index}`"
              class="sync-layout">
              <div class="sync-before">
                <diff-task-step
                  v-if="planStepList[index - 1]"
                  :account="accountList"
                  :data="planStepList[index - 1]"
                  :diff="beforeStepDiff"
                  type="sync-before" />
              </div>
              <div class="sync-after">
                <diff-task-step
                  v-if="templateStepList[index - 1]"
                  :account="accountList"
                  :data="templateStepList[index - 1]"
                  :diff="stepDiff"
                  type="sync-after" />
              </div>
            </div>
          </scroll-faker>
        </div>
      </div>
      <side-anchor
        class="side-anchor"
        :step="templateStepList"
        :variable="templateVariableList" />
    </div>
    <template #footer>
      <bk-button @click="handleCancel">
        {{ $t('template.取消') }}
      </bk-button>
      <bk-button @click="handleLast">
        {{ $t('template.上一步') }}
      </bk-button>
      <bk-button
        v-if="!isView"
        class="w120"
        theme="primary"
        @click="handleNext">
        {{ $t('template.下一步') }}
      </bk-button>
    </template>
  </layout>
</template>
<script>
  import _ from 'lodash';

  import AccountManageService from '@service/account-manage';

  import ScrollFaker from '@components/scroll-faker';

  import DiffGlobalVariable from '../components/diff/global-variable';
  import DiffTaskStep from '../components/diff/task-step';
  import Layout from '../components/layout';
  import SideAnchor from '../components/side-anchor';
  import {
    composeList,
    diffStep,
    diffVariable,
  } from '../components/utils';

  export default {
    name: 'SyncPlanStep2',
    components: {
      ScrollFaker,
      Layout,
      DiffGlobalVariable,
      DiffTaskStep,
      SideAnchor,
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
        accountList: [],
        templateVariableList: [],
        templateStepList: [],
        planVariableList: [],
        planStepList: [],
        beforeVariableDiff: {},
        variableDiff: {},
        beforeStepDiff: {},
        stepDiff: {},
      };
    },
    created() {
      this.isView = this.$route.query.mode === 'view';
      this.fetchAccount();
      this.init();
    },
    methods: {
      /**
       * @desc 计算差异
       */
      init() {
        const templateInfoVariableList = _.cloneDeep(this.templateInfo.variables);
        const templateInfoStepList = _.cloneDeep(this.templateInfo.stepList);
        const planInfoVariableList = _.cloneDeep(this.planInfo.variableList);
        const planInfoStepList = _.cloneDeep(this.planInfo.stepList);

        const [
          templateVariableList,
          planVariableList,
        ] = composeList(templateInfoVariableList, planInfoVariableList);
        this.templateVariableList = Object.freeze(templateVariableList);
        this.planVariableList = Object.freeze(planVariableList);

        const [
          templateStepList,
          planStepList,
        ] = composeList(templateInfoStepList, planInfoStepList);
        this.templateStepList = Object.freeze(templateStepList);
        this.planStepList = Object.freeze(planStepList);

        const variableDiff = Object.freeze(diffVariable(templateInfoVariableList, planInfoVariableList));
        this.beforeVariableDiff = Object.keys(variableDiff).reduce((result, key) => {
          if (variableDiff[key].type !== 'delete' && variableDiff[key].type !== 'new') {
            result[key] = variableDiff[key];
          }
          return result;
        }, {});
        this.variableDiff = variableDiff;

        const stepDiff = Object.freeze(diffStep(templateInfoStepList, planInfoStepList));
        this.beforeStepDiff = Object.keys(stepDiff).reduce((result, key) => {
          if (stepDiff[key].type !== 'delete' && stepDiff[key].type !== 'new') {
            result[key] = stepDiff[key];
          }
          return result;
        }, {});
        this.stepDiff = stepDiff;
      },
      /**
       * @desc 获取账号列表
       */
      fetchAccount() {
        AccountManageService.fetchAccountWhole()
          .then((data) => {
            this.accountList = data;
          });
      },
      /**
       * @desc 回退到上一步
       */
      handleLast() {
        this.$emit('on-change', 1);
      },
      /**
       * @desc 下一步
       */
      handleNext() {
        this.$emit('on-change', 3);
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
<style lang='postcss'>
  .sync-step2 {
    position: relative;

    .container {
      display: flex;
    }

    .sync-wraper {
      flex: 1;
    }

    .sync-header {
      .sync-before,
      .sync-after {
        padding: 0 !important;
      }
    }

    .sync-layout {
      display: flex;

      &:nth-child(2n+1) {
        background: #f7f8fa;
      }

      &:nth-child(2n) {
        background: #fff;
      }
    }

    .sync-content {
      height: calc(100vh - 199px);

      .sync-before,
      .sync-after {
        padding-right: 14px;
        padding-left: 24px;
      }
    }

    .sync-before {
      border-right: 1px solid #e2e2e2;
    }

    .sync-before,
    .sync-after {
      flex: 1;
      padding: 24px 14px 20px 24px;

      .title {
        height: 40px;
        font-size: 14px;
        line-height: 40px;
        color: #fff;
        text-align: center;
        background: rgb(0 0 0 / 60%);
      }
    }

    .block-title {
      padding-top: 20px;
      padding-bottom: 20px;
      font-size: 18px;
      line-height: 24px;
      color: #313238;
    }

    .global-variable-tips {
      font-size: 12px;
      color: #979ba5;
    }
  }
</style>
