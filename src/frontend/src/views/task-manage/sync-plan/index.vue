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
  <div
    v-bkloading="{ isLoading }"
    class="do-sync-plan">
    <div class="sync-plan-step">
      <bk-steps
        class="step-process"
        :cur-step.sync="curStep"
        :steps="stepList" />
    </div>
    <div class="step-wraper">
      <component
        :is="stepCom"
        v-if="!isLoading"
        :plan-info="planInfo"
        :template-info="templateInfo"
        @on-cancel="handleCancel"
        @on-change="handleStepChange" />
    </div>
  </div>
</template>
<script>
  import TaskPlanService from '@service/task-plan';

  import Step1 from './pages/step1';
  import Step2 from './pages/step2';
  import Step3 from './pages/step3';

  import I18n from '@/i18n';

  export default {
    name: '',
    components: {
      Step1,
      Step2,
      Step3,
    },
    data() {
      return {
        isLoading: true,
        curStep: 1,
        templateInfo: {},
        planInfo: {},
      };
    },
    computed: {
      stepCom() {
        const comMap = {
          1: Step1,
          2: Step2,
          3: Step3,
        };
        return comMap[this.curStep];
      },
    },
    created() {
      this.id = this.$route.params.id;
      this.templateId = this.$route.params.templateId;

      this.stepList = [
        { title: I18n.t('template.差异总览'), icon: 1 },
        { title: I18n.t('template.差异明细'), icon: 2 },
        { title: I18n.t('template.确认定时任务'), icon: 3 },
      ];
      // 只查看diff信息，不做确认定时任务操作
      const isView = this.$route.query.mode === 'view';
      if (isView) {
        this.stepList.pop();
      }
      this.fetchData();
    },
    methods: {
      fetchData(id) {
        this.$request(TaskPlanService.fetchSyncInfo({
          id: this.id,
          templateId: this.templateId,
        }, {
          permission: 'page',
        }), () => {
          this.isLoading = true;
        }).then(({ templateInfo, planInfo }) => {
          // 变量值不会同步
          // 对比展示时以执行方案的变量值为准

          const planVariableMap = {};
          planInfo.variableList.forEach((variable) => {
            variable.realId = variable.id;
            planVariableMap[variable.realId] = {
              defaultTargetValue: variable.defaultTargetValue,
              defaultValue: variable.defaultValue,
            };
          });
          planInfo.stepList.forEach((step) => {
            step.realId = step.templateStepId;
          });
          this.planInfo = Object.freeze(planInfo);

          templateInfo.variables.forEach((variable) => {
            variable.realId = variable.id;
            if (planVariableMap[variable.realId]) {
              variable.defaultTargetValue = planVariableMap[variable.realId].defaultTargetValue;
              variable.defaultValue = planVariableMap[variable.realId].defaultValue;
            }
          });
          templateInfo.stepList.forEach((step) => {
            step.realId = step.id;
          });
          this.templateInfo = Object.freeze(templateInfo);
        })
          .catch((error) => {
            if ([1241002, 400].includes(error.code)) {
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
      handleStepChange(payload) {
        this.curStep = payload;
      },
      handleCancel() {
        this.routerBack();
      },

      routerBack() {
        const { from } = this.$route.query;
        if (from === 'viewPlan') {
          this.$router.push({
            name: 'viewPlan',
            params: {
              templateId: this.templateId,
            },
            query: {
              viewPlanId: this.id,
            },
          });
          return;
        }
        this.$router.push({
          name: 'planList',
          query: {
            viewTemplateId: this.templateId,
            viewPlanId: this.id,
          },
        });
      },
    },
  };
</script>
<style lang='postcss'>
  .do-sync-plan {
    .sync-plan-step {
      height: 54px;
      padding-top: 15px;
      background: #fff;
      border-bottom: 1px solid #e2e2e2;

      .bk-steps {
        .bk-step {
          display: flex;

          &::after {
            top: 50%;
            left: 0;
            width: calc(100% - 10px);
            height: 0;
            border-top: 1px dotted #c4c6cc;
            transform: translateY(-50%);
          }

          &.current {
            .bk-step-number {
              color: #fff;
            }
          }

          &.done {
            .bk-step-number {
              display: flex;
              font-size: 0;
              background: #c4c6cc;
              border-color: #c4c6cc;
              align-items: center;
              justify-content: center;

              .icon-finish {
                color: #fff;
              }
            }
          }
        }

        .bk-step-number {
          line-height: 19px;
          color: #979ba5;
        }

        .bk-step-indicator {
          width: 21px;
          height: 21px;
          font-size: 12px;
          border-width: 1px;
        }

        .bk-step-title {
          position: relative;
          z-index: 1;
          padding-right: 10px;
          padding-left: 9px;
          margin-top: 0;
          line-height: 21px;
          color: #313238;
          background: #fff;
        }
      }
    }

    .step-process {
      width: 686px;
      margin: 0 auto;
    }
  }
</style>
