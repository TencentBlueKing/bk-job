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
    <smart-action
      class="dubug-exex-plan-page"
      offset-target="detail-content">
      <detail-layout mode="see">
        <detail-item label="">
          <div
            class="detail-item-title"
            style="margin-bottom: 8px;">
            <span>{{ $t('template.全局变量.label') }}</span>
            <span>（ {{ selectedVariable.length }} / {{ variableList.length }} ）</span>
          </div>
          <render-global-var
            :list="variableList"
            mode="editOfPlan"
            :select-value="selectedVariable" />
        </detail-item>
        <detail-item
          class="task-step-item"
          label="">
          <div class="task-step-selection">
            <div
              class="detail-item-title"
              style="margin-bottom: 14px;">
              <span>{{ $t('template.选择要调试的步骤') }}</span>
              <span>（ {{ formData.enableSteps.length }} / {{ taskStepList.length }} ）</span>
            </div>
            <div class="step-check">
              <bk-button
                v-if="hasSelectAll"
                text
                @click="handleDeselectAll">
                {{ $t('template.取消全选') }}
              </bk-button>
              <bk-button
                v-else
                text
                @click="handleSelectAll">
                {{ $t('template.全选') }}
              </bk-button>
            </div>
          </div>
          <render-task-step
            :list="taskStepList"
            mode="select"
            :select-value="formData.enableSteps"
            @on-select="handleSelectStep" />
        </detail-item>
      </detail-layout>
      <template #action>
        <div class="action-wraper">
          <bk-button
            class="w120 mr10"
            :disabled="!enableStepsNotEmpty"
            theme="primary"
            @click="handleSubmitExec">
            {{ $t('template.去执行') }}
          </bk-button>
          <bk-button @click="handleCancle">
            {{ $t('template.取消') }}
          </bk-button>
        </div>
      </template>
    </smart-action>
    <back-top />
  </div>
</template>
<script>
  import TaskExecuteService from '@service/task-execute';
  import ExecPlanService from '@service/task-plan';

  import {
    findUsedVariable,
  } from '@utils/assist';

  import BackTop from '@components/back-top';
  import DetailLayout from '@components/detail-layout';
  import DetailItem from '@components/detail-layout/item';

  import RenderGlobalVar from '../common/render-global-var';
  import RenderTaskStep from '../common/render-task-step';

  import I18n from '@/i18n';

  const getDefaultData = () => ({
    id: 0,
    name: '',
    enableSteps: [],
    templateId: 0,
    variables: [],
  });

  export default {
    name: '',
    components: {
      BackTop,
      DetailLayout,
      DetailItem,
      RenderGlobalVar,
      RenderTaskStep,
    },
    data() {
      return {
        formData: getDefaultData(),
        variableList: [],
        taskStepList: [],
        isLoading: true,
      };
    },
    computed: {
      isSkeletonLoading() {
        return this.isLoading;
      },
      selectedVariable() {
        const selectedSteps = this.taskStepList.filter(step => this.formData.enableSteps.includes(step.id));
        return findUsedVariable(selectedSteps);
      },
      hasSelectAll() {
        return this.formData.enableSteps.length >= this.taskStepList.length;
      },
      enableStepsNotEmpty() {
        return this.formData.enableSteps.length > 0;
      },
    },
    created() {
      this.formData.templateId = Number(this.$route.params.id);

      this.fetchData();
    },
    methods: {
      /**
       * @desc 获取调试数据
       */
      fetchData() {
        ExecPlanService.fetchDebugInfo({
          templateId: this.formData.templateId,
        }).then((data) => {
          this.variableList = Object.freeze(data.variableList);
          this.taskStepList = Object.freeze(data.stepList);

          this.formData.name = data.name;
          this.formData.id = data.id;
          this.formData.enableSteps = data.stepList.map(item => item.id);
          this.formData.variables = data.variableList;
        })
          .catch((error) => {
            if ([
              1243027,
              400,
            ].includes(error.code)) {
              setTimeout(() => {
                this.$router.push({
                  name: 'taskList',
                });
              }, 3000);
            }
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      /**
       * @desc 执行调试任务
       */
      taskExecution() {
        this.isExecuting = true;
        TaskExecuteService.taskExecution({
          taskId: this.formData.id,
          taskVariables: [],
        }).then(({ taskInstanceId }) => {
          this.$bkMessage({
            theme: 'success',
            message: I18n.t('template.操作成功'),
          });
          this.$router.push({
            name: 'historyTask',
            params: {
              id: taskInstanceId,
            },
          });
        })
          .finally(() => {
            this.isExecuting = false;
          });
      },

      /**
       * @desc 选择模板步骤
       * @param {String} payload 模板步骤
       */
      handleSelectStep(payload) {
        const index = this.formData.enableSteps.findIndex(item => item === payload.id);
        if (index > -1) {
          this.formData.enableSteps.splice(index, 1);
        } else {
          this.formData.enableSteps.push(payload.id);
        }
      },
      /**
       * @desc 选择模板的所有步骤
       */
      handleSelectAll() {
        this.formData.enableSteps = this.taskStepList.map(item => item.id);
      },
      /**
       * @desc 情况模板步骤选择
       */
      handleDeselectAll() {
        this.formData.enableSteps = [];
      },
      /**
       * @desc 开始执行
       *
       * 1，任务没有全局变量时直接执行任务
       * 2，任务没有选举变量时需要先设置变量
       */
      handleSubmitExec() {
        this.isExecuting = true;
        ExecPlanService.planUpdate(this.formData)
          .then(() => {
            if (this.variableList.length < 1) {
              this.$bkInfo({
                title: I18n.t('template.确认执行？'),
                subTitle: I18n.t('template.未设置全局变量，点击确认将直接执行。'),
                confirmFn: () => {
                  this.taskExecution();
                },
              });
            } else {
              this.$router.push({
                name: 'settingVariable',
                params: {
                  id: this.formData.id,
                  templateId: this.formData.templateId,
                  debug: 1,
                },
                query: {
                  from: 'debugPlan',
                },
              });
            }
          })
          .finally(() => {
            this.isExecuting = false;
          });
      },

      /**
       * @desc 取消调试
       */
      handleCancle() {
        this.routerBack();
      },
      /**
       * @desc 路由回退
       */
      routerBack() {
        const { from } = this.$route.query;
        if (from === 'taskList') {
          this.$router.push({
            name: 'taskList',
          });
          return;
        }
        this.$router.push({
          name: 'templateDetail',
          params: {
            id: this.formData.templateId,
          },
        });
      },
    },
  };
</script>
<style lang='postcss'>
  @import url("@/css/mixins/media");

  .dubug-exex-plan-page {
    padding-bottom: 20px;

    .detail-item-title {
      font-size: 16px;
      line-height: 21px;
      color: #313238;
    }

    .task-step-item {
      margin-top: 40px;
      margin-bottom: 20px;
    }

    .task-step-selection {
      display: flex;
      width: 500px;

      .step-check {
        margin-left: auto;
      }
    }

    .task-step-selection,
    .action-wraper {
      @media (--small-viewports) {
        width: 500px;
      }

      @media (--medium-viewports) {
        width: 560px;
      }

      @media (--large-viewports) {
        width: 620px;
      }

      @media (--huge-viewports) {
        width: 680px;
      }
    }

    .action-wraper {
      display: flex;

      .plan-save {
        margin-left: auto;
      }
    }
  }

  .save-debug-plan-dialog {
    .bk-dialog-header {
      padding-bottom: 0 !important;
    }

    .bk-form-item:last-child {
      margin-bottom: 0 !important;
    }
  }
</style>
