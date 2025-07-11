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
  <div class="executive-history-step">
    <task-status
      ref="taskStatus"
      @on-init="handleTaskInit">
      <rolling-batch
        v-if="data.isRollingTask"
        :data="data"
        :value="params.batch"
        @change="handleBatchChange"
        @on-confirm="operationCode => handleStatusUpdate(operationCode)" />
      <div class="step-info-header">
        <div class="step-info-wraper">
          <div class="step-type-text">
            {{ stepTypeText }}
          </div>
          <div class="step-name-box">
            <div
              v-bk-overflow-tips
              class="step-name-text">
              {{ data.name }}
            </div>
            <execution-history-select
              ref="executionHistorySelect"
              :batch="params.batch"
              :execute-count="params.executeCount"
              :step-instance-id="params.id"
              @on-change="handleExecuteCountChange" />
          </div>
        </div>
        <!-- 步骤执行操作 -->
        <div
          v-if="!params.batch || params.batch === data.runningBatchOrder"
          class="step-action-box">
          <step-action
            v-for="action in data.actions"
            :key="action"
            :confirm-handler="operationCode => handleStatusUpdate(operationCode)"
            display-style="step-detail"
            :name="action" />
        </div>
        <search-log
          :data="data"
          :searching="isSearching"
          :value="params"
          @change="handleSearchChange" />
        <export-log
          :is-file="isFile"
          :step-instance-id="params.id"
          :task-instance-id="taskInstanceId" />
        <div class="task-instance-action">
          <view-global-variable
            v-if="isTask"
            :task-instance-id="taskInstanceId" />
          <view-operation-record :task-instance-id="taskInstanceId" />
          <view-step-info
            :step-instance-id="params.id"
            :task-instance-id="taskInstanceId" />
        </div>
      </div>
      <!-- 主机分组 -->
      <group-tab
        :data="data.resultGroups"
        :value="currentGroup"
        @on-change="handelGroupChange" />
      <div
        ref="detailContainer"
        class="detail-container"
        :style="defailContainerStyles">
        <div
          v-bkloading="{ isLoading: isHostLoading }"
          class="container-left">
          <!-- 主机列表 -->
          <!-- eslint-disable max-len -->
          <result-task-list
            :data="dispalyGroup.tasks"
            :execute-object-type="data.executeObjectType"
            :get-all-task-list="getAllTaskList"
            :list-loading="isLoading"
            :name="`${data.stepInstanceId}_${dispalyGroup.groupName}_${params.executeCount}_${params.keyword}_${params.searchIp}`"
            :pagination-loading="paginationChangeLoading"
            :search-value="`${params.keyword}_${params.searchIp}`"
            :total="dispalyGroup.taskSize"
            @on-change="handleResultTaskChange"
            @on-clear-search="handleClearSearch"
            @on-pagination-change="handlePaginationChange"
            @on-sort="handleSort" />
        </div>
        <div class="container-right">
          <!-- 执行日志 -->
          <execution-info
            v-if="data.stepInstanceId"
            :execute-count="params.executeCount"
            :is-file="isFile"
            :is-task="isTask"
            :log-filter="params.keyword"
            :name="`${params.id}_${params.executeCount}_${dispalyGroup.groupName}_${currentResultTask.key}_${params.keyword}`"
            :step-instance-id="data.stepInstanceId"
            :task-execute-detail="currentResultTask"
            :task-instance-id="taskInstanceId" />
        </div>
      </div>
    </task-status>
    <!-- 步骤执行操作——强制终止 -->
    <execution-status-bar
      v-if="data.name"
      :data="data"
      type="step">
      <step-action
        v-if="data.isForcedEnable"
        key="forced"
        :confirm-handler="handleForceTask"
        display-style="step-detail"
        name="forced"
        @on-cancel="handleCancelForceTask"
        @on-show="handleStartForceTask" />
      <div
        class="task-redo-btn"
        @click="handleGoRetry">
        <icon
          style="margin-right: 4px; color: #C4C6CC;"
          type="circle-back-filled" />
        {{ $t('history.去重做') }}
      </div>
    </execution-status-bar>
  </div>
</template>
<script>
  import _ from 'lodash';

  import TaskExecuteService from '@service/task-execute';

  import I18n from '@/i18n';

  import ExecutionStatusBar from '../common/execution-status-bar';
  import StepAction from '../common/step-action';

  import ExecutionHistorySelect from './components/execution-history-select';
  import ExecutionInfo from './components/execution-info';
  import ExportLog from './components/export-log';
  import GroupTab from './components/group-tab';
  import mixins from './components/mixins';
  import ResultTaskList from './components/result-task-list/index.vue';
  import RollingBatch from './components/rolling-batch';
  import SearchLog from './components/search-log.vue';
  import TaskStatus from './components/task-status';
  import ViewGlobalVariable from './components/view-global-variable';
  import ViewOperationRecord from './components/view-operation-record';
  import ViewStepInfo from './components/view-step-info/index.vue';

  const appendURLParams = (params = {}) => {
    const curSearchParams = new URLSearchParams(window.location.search);
    Object.keys(params).forEach((key) => {
      if (curSearchParams.has(key)) {
        curSearchParams.set(key, params[key]);
      } else {
        curSearchParams.append(key, params[key]);
      }
    });
    window.history.replaceState({}, '', `?${curSearchParams.toString()}`);
  };

  export default {
    name: 'StepExecuteDetail',
    components: {
      ExecutionStatusBar,
      StepAction,
      RollingBatch,
      TaskStatus,
      ExecutionHistorySelect,
      GroupTab,
      ResultTaskList,
      ExecutionInfo,
      ExportLog,
      ViewGlobalVariable,
      ViewOperationRecord,
      ViewStepInfo,
      SearchLog,
    },
    mixins: [
      mixins,
    ],
    data() {
      return {
        isLoading: true,
        isSearching: false,
        defailContainerStyles: {},
        // 步骤所属作业的所有步骤列表
        taskStepList: [],
        // 任务信息
        data: {
          finished: false,
          resultGroups: [],
          actions: [],
          isForcedEnable: false,
        },
        // 接口参数
        params: {
          id: 0,
          batch: '',
          executeCount: '',
          maxTasksPerResultGroup: 0,
          keyword: '', // 日志的筛选值
          searchIp: '', // ip的筛选值
          orderField: '', // 排序字段
          order: '', // 排序字段，当前支持totalTime|cloudAreaId|exitCode
        },
        // 选中的分组信息
        currentGroup: {
          resultType: '',
          tag: '',
        },
        // 选中的主机信息
        currentResultTask: {
          type: 0,
          executeObject: {
            executeObjectResourceId: 0,
            executeCount: 0,
          },
        },
        // 作业执行步骤
        isTask: false,
        // 分发文件类型的步骤
        isFile: false,
        // 主机分页加载
        paginationChangeLoading: false,
      };
    },
    computed: {
      /**
       * @desc 骨架屏loading
       */
      isSkeletonLoading() {
        return this.isLoading;
      },
      /**
       * @desc 步骤类型描述
       *
       * 对作业里面的步骤需要显示当前步骤在作业中的索引
       */
      stepTypeText() {
        let text = '';
        if (this.isTask) {
          const index = _.findIndex(this.taskStepList, _ => _.stepInstanceId === this.params.id);
          if (index > -1) {
            text += `Step ${index + 1}/${this.taskStepList.length}`;
          }
        }
        if (this.isFile) {
          text = `${text} ${I18n.t('history.分发文件')}`;
        } else {
          text = `${text} ${I18n.t('history.脚本执行')}`;
        }

        return text;
      },
      /**
       * @desc 如果任务已经结束重新获取任务详情时加上主机loading效果
       *
       * 主要用于任务结束后切换分组的场景
       */
      isHostLoading() {
        return this.data.finished ? this.isLoading : false;
      },
      /**
       * @desc 展示被选中的分组信息
       */
      dispalyGroup() {
        const targetGroup = _.find(this.data.resultGroups, _ => _.groupName === this.currentGroup.groupName);
        if (targetGroup) {
          return targetGroup;
        }
        return {
          tasks: [],
          groupName: '',
        };
      },
    },
    created() {
      this.taskInstanceId = 0;
      this.params.batch = this.$route.query.batch || '';
      this.isForceing = false;
      this.$Progress.start();
    },
    mounted() {
      window.addEventListener('rezie', this.calcDetailContainerStyle);
      this.$once('hook:beforeDestroy', () => {
        window.removeEventListener('rezie', this.calcDetailContainerStyle);
      });
    },
    beforeDestroy() {
      this.$Progress.finish();
    },
    methods: {
      /**
       * @desc 步骤执行详情
       *
       * 1，任务没结束一直轮询
       * 2，分组信息在执行过程中会变，如果已选中的分组不存在了自动选中第一个分组
       */
      fetchStep: _.debounce(function () {
        if (this.params.id < 1 || this.params.maxTasksPerResultGroup < 1) {
          return;
        }

        this.isLoading = true;
        TaskExecuteService.fetchStepExecutionResult({
          ...this.params,
          resultType: this.currentGroup.resultType,
          tag: this.currentGroup.tag,
          taskInstanceId: this.taskInstanceId,
        }).then((data) => {
          if (this.isForceing) {
            return;
          }
          this.data = Object.freeze(data);

          this.calcDetailContainerStyle();

          this.isFile = data.isFile;
          //  已选中的分组不存在了——默认选中第一个分组
          const { resultGroups } = data;
          if (!_.find(resultGroups, group => group.resultType === this.currentGroup.resultType
            && group.tag === this.currentGroup.tag)) {
            /* eslint-disable prefer-destructuring */
            this.currentGroup = resultGroups[0];
          }
          if (data.finished) {
            this.$Progress.finish();
            return;
          }

          this.$pollingQueueRun(this.fetchStep);
        })
          .catch((error) => {
            if ([
              400,
              1244007,
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
            this.isSearching = false;
            this.paginationChangeLoading = false;
          });
      }, 100),
      // 获取完整的 taskList
      getAllTaskList() {
        return TaskExecuteService.fetchStepGroupHost({
          ...this.params,
          resultType: this.currentGroup.resultType,
          tag: this.currentGroup.tag,
        });
      },
      calcDetailContainerStyle: _.throttle(function () {
        const { top } = this.$refs.detailContainer.getBoundingClientRect();
        this.defailContainerStyles = {
          height: `calc(100vh - ${top}px)`,
        };
      }, 20),
      /**
       * @desc 步骤所属作业初始化
       * @param {Object} payload 步骤信息
       *
       * 前置获取步骤关联的作业执行详情
       */
      handleTaskInit(payload) {
        this.params = {
          ...this.params,
          taskInstanceId: payload.taskInstanceId,
          id: payload.stepInstanceId,
          executeCount: payload.executeCount || 0,
        };
        this.taskInstanceId = payload.taskInstanceId;
        this.isTask = payload.isTask;
        this.taskStepList = Object.freeze(payload.taskStepList);
        this.taskExecution = payload.taskExecution;

        appendURLParams({
          executeCount: payload.executeCount,
          stepInstanceId: payload.stepInstanceId,
        });
        this.fetchStep();
      },
      /**
       * @desc 滚动执行批次筛选
       * @param { Number | String } batch 0: 查看全部批次；’‘：查看当前最新批次
       *
       * 切换批次时不主动获取步骤执行数据，
       * 切换批次时会导致组件 execution-history-select 刷新数据，这个时候会主动获取步骤执行数据
       */
      handleBatchChange(batch) {
        this.params = {
          ...this.params,
          batch,
        };
        this.currentGroup = {
          resultType: '',
          tag: '',
        };
        appendURLParams({
          batch: this.params.batch,
        });
      },
      /**
       * @desc 执行历史
       * @param {Number} executeCount 重试次数
       *
       * 刷新步骤执行详情
       */
      handleExecuteCountChange(executeCount) {
        this.params = {
          ...this.params,
          executeCount,
        };
        appendURLParams({
          executeCount: this.params.executeCount,
        });
        this.fetchStep();
      },
      /**
       * @desc 分组切换
       * @param group [Number] 选中的分组
       *
       * 刷新步骤执行详情
       *
       */
      handelGroupChange(group) {
        this.currentGroup = group;
        this.fetchStep();
      },
      /**
       * @desc 选中主机
       * @param {Object} task 目标任务
       */
      handleResultTaskChange(resultTask) {
        this.currentResultTask = Object.freeze(resultTask);
      },
      /**
       * @desc 组件列表分页
       * @param {Number} pageSize 每页条数
       */
      handlePaginationChange(pageSize) {
        this.params.maxTasksPerResultGroup = pageSize;
        this.paginationChangeLoading = true;
        this.fetchStep();
      },
      /**
       * @desc 主机排序
       * @param {Object} payload 排序信息
       */
      handleSort(payload) {
        this.params = Object.assign({}, this.params, payload);
        this.fetchStep();
      },
      /**
       * @desc 清空搜索条件
       */
      handleClearSearch() {
        this.params.keyword = '';
        this.params.searchIp = '';
        this.isSearching = true;
        this.fetchStep();
      },
      /**
       * @desc 开始强制终止
       */
      handleStartForceTask() {
        this.isForceing = true;
        this.$Progress.start();
      },
      /**
       * @desc 取消强制终止
       */
      handleCancelForceTask() {
        this.isForceing = false;
        this.$Progress.start();
        this.fetchStep();
      },
      /**
       * @desc 强制终止任务
       *
       * 强制终止成功需要刷新作业的状态和执行历史记录
       */
      handleForceTask() {
        this.$Progress.start();
        return TaskExecuteService.updateTaskExecutionStepOperateTerminate({
          taskInstanceId: this.taskInstanceId,
        }).then(() => {
          this.messageSuccess(I18n.t('history.操作成功'));
          this.$refs.taskStatus.reLoading();
          this.$refs.executionHistorySelect.reLoading();
          this.fetchStep();
          return true;
        });
      },
      /**
       * @desc 更新步骤执行状态
       * @param {Number} operationCode 状态码
       *
       * 强制终止成功需要刷新作业的状态和执行历史记录
       */
      handleStatusUpdate(operationCode) {
        this.$Progress.start();
        return TaskExecuteService.updateTaskExecutionStepOperate({
          id: this.params.id,
          operationCode,
        }).then((data) => {
          this.params.executeCount = data.executeCount;
          this.$refs.taskStatus.reLoading();
          this.$refs.executionHistorySelect.reLoading();
          this.$bkMessage({
            limit: 1,
            theme: 'success',
            message: I18n.t('history.操作成功'),
          });
          appendURLParams({
            executeCount: this.params.executeCount,
          });
          this.fetchStep();
          return true;
        });
      },
      handleGoRetry() {
        this.isLoading = true;
        if (this.isTask) {
          TaskExecuteService.fetchTaskInstance({
            id: this.taskInstanceId,
          }).then(({ variables }) => {
            if (variables.length > 0) {
              // 有变量，去设置变量
              this.$router.push({
                name: 'redoTask',
                params: {
                  taskInstanceId: this.taskInstanceId,
                },
              });
              return;
            }
            // 没有变量直接执行
            this.$bkInfo({
              title: I18n.t('history.确认执行？'),
              subTitle: I18n.t('history.该方案未设置全局变量，点击确认将直接执行。'),
              confirmFn: () => {
                this.isLoading = true;
                TaskExecuteService.redoTask({
                  taskInstanceId: this.taskInstanceId,
                  taskVariables: [],
                }).then(({ taskInstanceId }) => {
                  this.$bkMessage({
                    theme: 'success',
                    message: I18n.t('history.执行成功'),
                  });
                  this.$router.push({
                    name: 'historyTask',
                    params: {
                      id: taskInstanceId,
                    },
                  });
                  this.taskInstanceId = taskInstanceId;
                })
                  .finally(() => {
                    this.isLoading = false;
                  });
              },
            });
          })
            .finally(() => {
              this.isLoading = false;
            });
        }
        // 快速分发文件
        // 去快速执行分发文件页面重做
        if (this.data.isFile) {
          this.$router.push({
            name: 'fastPushFile',
            params: {
              taskInstanceId: this.taskInstanceId,
            },
            query: {
              from: 'executiveHistory',
            },
          });
        } else {
          // 快速执行脚本
          // 去快速执行脚本页面重做
          this.$router.push({
            name: 'fastExecuteScript',
            params: {
              taskInstanceId: this.taskInstanceId,
            },
            query: {
              from: 'executiveHistory',
            },
          });
          return;
        }
      },
      /**
       * @desc 日志搜若
       * @param { keyword: string, searchIp: string } payload
       * @returns { Boolean }
       */
      handleSearchChange(payload) {
        this.params.keyword = payload.keyword;
        this.params.searchIp = payload.searchIp;
        this.isSearching = true;
        this.fetchStep();
      },

      /**
       * @desc 路由回退
       */
      routerBack() {
        const { from } = this.$route.query;
        if (from === 'historyTask') {
          this.$router.push({
            name: 'historyTask',
            params: {
              id: this.taskInstanceId,
            },
          });
          return;
        }
        if (from === 'planList') {
          this.$router.push({
            name: 'historyTask',
            params: {
              id: this.taskInstanceId,
            },
            query: {
              from: 'planList',
            },
          });
          return;
        }

        if (from === 'plan') {
          // 保留执行方案到步骤详情的操作路径
          this.$router.push({
            name: 'historyTask',
            params: {
              id: this.taskInstanceId,
            },
            query: {
              from: 'plan',
            },
          });
          return;
        }
        if (from === 'fastExecuteScript') {
          this.$router.push({
            name: 'fastExecuteScript',
            query: {
              from: this.$route.name,
            },
          });
          return;
        }
        if (from === 'fastPushFile') {
          this.$router.push({
            name: 'fastPushFile',
            query: {
              from: this.$route.name,
            },
          });
          return;
        }
        this.$router.push({
          name: 'historyList',
        });
      },
    },
  };
</script>
<style lang='postcss'>
  @import url("@/css/mixins/media");

  @keyframes search-loading {
    0% {
      transform: rotateZ(0);
    }

    100% {
      transform: rotateZ(360deg);
    }
  }

  .executive-history-step {
    .step-info-header {
      display: flex;
      width: 100%;
      padding: 45px 24px 12px;
      overflow: hidden;
      background: #f5f6fa;

      .step-info-wraper {
        flex: 1;
        margin-top: -25px;
        overflow: hidden;

        .step-type-text {
          font-size: 12px;
          line-height: 16px;
          color: #979ba5;
        }

        .step-name-box {
          display: flex;
          align-items: center;
          margin-top: 10px;
        }

        .step-name-text {
          height: 24px;
          max-width: calc(100% - 65px);
          overflow: hidden;
          font-size: 18px;
          line-height: 24px;
          color: #313238;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .step-action-box {
        display: flex;
        flex: 0 0 auto;
        justify-content: flex-end;
        max-width: 360px;

        .step-instance-action {
          border-radius: 2px;

          i {
            display: none;
          }
        }
      }

      .task-instance-action {
        display: flex;

        .action-btn {
          display: flex;
          width: 32px;
          height: 32px;
          margin-left: 8px;
          font-size: 16px;
          color: #979ba5;
          cursor: pointer;
          background: #fff;
          border: 1px solid #c4c6cc;
          border-radius: 2px;
          align-items: center;
          justify-content: center;
        }
      }
    }

    .detail-container {
      display: flex;
      height: calc(100vh - 234px);
      padding: 20px 24px;

      .container-left {
        position: relative;
        height: 100%;
        overflow: hidden;
        background: #fff;
        border: 1px solid #dcdee5;
        border-bottom-left-radius: 2px;
        border-top-left-radius: 2px;
      }

      .container-right {
        height: 100%;
        min-width: 800px;
        flex: 1;
      }
    }

    .step-action {
      display: flex;
      align-items: center;

      .action-btn {
        display: flex;
        height: 32px;
        padding: 0 7px;
        margin-left: 10px;
        font-size: 19px;
        color: #979ba5;
        cursor: pointer;
        background: #fff;
        border-radius: 16px;
        align-items: center;
        justify-content: center;

        &:hover {
          color: #3a84ff;
        }
      }
    }
  }

  .task-redo-btn{
    display: inline-flex;
    height: 32px;
    padding: 0 12px;
    color: #63656E;
    cursor: pointer;
    background: #FFF;
    border: 1px solid #C4C6CC;
    border-radius: 16px;
    justify-content: center;
    align-items: center;

    &:hover {
      border-color: #3a84ff;
    }
  }
</style>
