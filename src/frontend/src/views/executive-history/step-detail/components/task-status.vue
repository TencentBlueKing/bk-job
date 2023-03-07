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
    class="task-execution-step-wraper"
    :class="{ column: taskExecutionDetail.taskExecution.isTask }">
    <div
      v-if="taskExecutionDetail.taskExecution.isTask"
      class="task-process">
      <div class="process-wraper">
        <scroll-faker>
          <task-step-start />
          <task-step
            v-for="step in taskExecutionDetail.stepExecution"
            :key="step.stepInstanceId"
            :active-id="currentStepInstanceId"
            :data="step"
            @on-select="handleChangeStep"
            @on-update="handleTaskStatusUpdate" />
          <task-step-end :disable="!taskExecutionDetail.taskExecution.isSuccess" />
        </scroll-faker>
      </div>
      <div class="execution-process">
        {{ currentStepOrder }} / {{ taskExecutionDetail.totalStep }}
      </div>
    </div>
    <div class="task-step-detail">
      <slot />
    </div>
    <div
      v-if="historyEnable"
      class="execution-history"
      :class="{ active: isShowHistory }">
      <div
        class="toggle-btn"
        @click="handleShowHistory">
        <icon
          class="toggle-flag"
          type="angle-double-left" />
        <div class="return-edit">
          {{ $t('history.返回编辑') }}
        </div>
      </div>
      <div class="history-content">
        <template v-if="taskExecutionDetail.taskExecution.isScript">
          <div
            v-for="item in scriptHistoryList"
            :key="item.id"
            class="item"
            @click="handleGoRedoScriptExec(item)">
            {{ item.name }}
          </div>
        </template>
        <template v-if="taskExecutionDetail.taskExecution.isFile">
          <div
            v-for="item in fileHistoryList"
            :key="item.id"
            class="item"
            @click="handleGoRedoFileExec(item)">
            {{ item.name }}
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
<script>
  import _ from 'lodash';

  import TaskExecuteService from '@service/task-execute';

  import {
    execScriptHistory,
    pushFileHistory,
  } from '@utils/cache-helper';

  import mixins from './mixins';
  import TaskStepEnd from './task-step/end';
  import TaskStep from './task-step/index';
  import TaskStepStart from './task-step/start';

  import I18n from '@/i18n';

  export default {
    name: '',
    components: {
      TaskStep,
      TaskStepStart,
      TaskStepEnd,
    },
    mixins: [
      mixins,
    ],
    data() {
      return {
        isLoading: false,
        isShow: false,
        taskExecutionDetail: {
          isFinished: false,
          taskExecution: {},
          stepExecution: [],
        },
        scriptHistoryList: [],
        fileHistoryList: [],
        isShowHistory: false,
        isFinished: false,
        currentStepInstanceId: '',
      };
    },
    computed: {
      currentStepOrder() {
        return _.findIndex(
          this.taskExecutionDetail.stepExecution,
          _ => _.stepInstanceId === this.currentStepInstanceId,
        ) + 1;
      },
      historyEnable() {
        if (this.taskExecutionDetail.taskExecution.isScript) {
          return this.scriptHistoryList.length > 0;
        }
        if (this.taskExecutionDetail.taskExecution.isFile) {
          return this.fileHistoryList.length > 0;
        }
        return false;
      },
    },
    created() {
      this.timer = '';

      const { taskInstanceId } = this.$route.params;
      const { stepInstanceId, retryCount = 0 } = this.$route.query;

      this.taskInstanceId = parseInt(taskInstanceId, 10);
      this.stepInstanceId = parseInt(stepInstanceId, 10);
      this.retryCount = parseInt(retryCount, 10);

      this.fetchData();
    },
    mounted() {
      this.initHistory();
      this.$once('hook:beforeDestroy', () => {
        clearTimeout(this.timer);
      });
    },
    methods: {
      fetchData(trigger = false) {
        this.isLoading = true;
        TaskExecuteService.fetchTaskExecutionResult({
          id: this.taskInstanceId,
        }).then((data) => {
          this.taskExecutionDetail = Object.freeze(data);
          if (!data.finished) {
            this.timer = setTimeout(() => {
              this.reLoading();
            }, 1000);
          }
          // 没有指定stepInstanceId默认取第一个步骤
          if (!this.stepInstanceId) {
            const [
              {
                stepInstanceId,
                retryCount,
              },
            ] = data.stepExecution;
            this.stepInstanceId = stepInstanceId;
            this.retryCount = retryCount;
          }

          this.currentStepInstanceId = this.stepInstanceId;
          this.$emit('on-init', {
            taskInstanceId: this.taskInstanceId,
            stepInstanceId: this.stepInstanceId,
            retryCount: this.retryCount,
            taskStepList: data.stepExecution,
            isTask: this.taskExecutionDetail.taskExecution.isTask,
            taskExecution: this.taskExecutionDetail.taskExecution,
          });
        })
          .catch((error) => {
            if ([
              400,
              1244006,
            ].includes(error.code)) {
              setTimeout(() => {
                this.$router.push({
                  name: 'historyList',
                });
              }, 3000);
            }
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      // 作业执行状态轮询
      reLoading() {
        // 是作业类型的任务才会轮询作业的状态
        if (!this.taskExecutionDetail.taskExecution.isTask) {
          return;
        }
        TaskExecuteService.fetchTaskExecutionResult({
          id: this.taskInstanceId,
        }).then((data) => {
          this.taskExecutionDetail = Object.freeze(data);
          if (!data.finished) {
            this.$pollingQueueRun(this.reLoading);
          }
        });
      },
      initHistory() {
        this.scriptHistoryList = Object.freeze(execScriptHistory.getItem());
        this.fileHistoryList = Object.freeze(pushFileHistory.getItem());
      },
      handleShowHistory() {
        this.isShowHistory = !this.isShowHistory;
      },
      handleGoRedoScriptExec(payload) {
        this.$router.push({
          name: 'fastExecuteScript',
          params: {
            taskInstanceId: payload.taskInstanceId,
          },
          query: {
            from: this.$route.name,
          },
        });
      },
      handleGoRedoFileExec(payload) {
        this.$router.push({
          name: 'fastPushFile',
          params: {
            taskInstanceId: payload.taskInstanceId,
          },
          query: {
            from: this.$route.name,
          },
        });
      },
      handleChangeStep(step) {
        if (step.stepInstanceId === this.currentStepInstanceId) {
          return;
        }

        if (step.isNotStart) {
          this.$bkMessage({
            theme: 'warning',
            message: I18n.t('history.该步骤还未执行'),
            limit: 1,
          });
          return;
        }

        if (step.isApproval) {
          this.$bkMessage({
            theme: 'warning',
            message: I18n.t('history.人工确认步骤不支持查看步骤详情'),
            limit: 1,
          });
          return;
        }

        const { stepInstanceId, retryCount } = step;
        this.currentStepInstanceId = stepInstanceId;

        this.$emit('on-init', {
          stepInstanceId,
          taskInstanceId: this.taskInstanceId,
          retryCount,
          taskStepList: this.taskExecutionDetail.stepExecution,
          isTask: this.taskExecutionDetail.taskExecution.isTask,
          taskExecution: this.taskExecutionDetail.taskExecution,
        });
      },
      handleTaskStatusUpdate(payload) {
        this.reLoading();
      },
    },
  };
</script>
<style lang='postcss'>
  html[lang="en-US"] {
    .return-edit {
      margin-top: 6px;
      transform: rotate(90deg);
    }
  }

  @keyframes ani-rotate {
    to {
      transform: rotateZ(360deg);
    }
  }

  .task-execution-step-wraper {
    background: #fff;

    &.column {
      display: flex;

      .task-step-detail {
        width: 0;
      }
    }

    .task-process {
      display: flex;
      height: calc(100vh - 104px);
      padding-bottom: 38px;
      background: #fff;
      border-right: 1px solid #e2e2e2;
      flex-direction: column;
      flex: 0 0 61px;

      .process-wraper {
        height: 100%;

        .scroll-faker-content {
          position: relative;
        }
      }

      .execution-process {
        position: absolute;
        right: 0;
        bottom: 0;
        left: 0;
        width: 60px;
        height: 28px;
        font-size: 14px;
        line-height: 28px;
        color: #fff;
        text-align: center;
        background: rgb(0 0 0 / 25%);
        user-select: none;
      }
    }

    .task-step-detail {
      flex: 1;
      background: #fff;
    }

    .execution-history {
      position: fixed;
      top: 127px;
      right: 0;
      z-index: 999;
      font-size: 12px;
      line-height: 30px;
      color: #c4c6cc;
      background: #63656e;
      border-bottom-left-radius: 2px;
      transform: translateX(100%);
      transition: all 0.35s;
      user-select: none;

      &.active {
        transform: translateX(0);

        .toggle-flag {
          transform: rotateZ(180deg);
        }
      }

      .toggle-btn {
        position: absolute;
        top: 0;
        left: -22px;
        display: flex;
        width: 22px;
        height: 88px;
        line-height: 13px;
        color: #dcdee5;
        text-align: center;
        cursor: pointer;
        background: #63656e;
        border-right: 1px solid #757783;
        border-bottom-left-radius: 8px;
        border-top-left-radius: 8px;
        flex-direction: column;
        justify-content: center;

        .toggle-flag {
          margin-bottom: 5px;
          color: #979ba5;
          transition: all 0.2s;
        }
      }

      .history-content {
        display: flex;
        min-height: 90px;
        padding: 12px 0;
        flex-direction: column;
        justify-content: center;
      }

      .item {
        padding-right: 16px;
        padding-left: 16px;
        cursor: pointer;
        transition: all 0.15s;

        &:hover {
          color: #fff;
          background: #4f515a;
        }
      }
    }
  }
</style>
