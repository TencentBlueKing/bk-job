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
            <div class="step-name-text">
              {{ data.name }}
            </div>
            <execution-history-select
              ref="executionHistorySelect"
              :batch="params.batch"
              :retry-count="params.retryCount"
              :step-instance-id="params.id"
              @on-change="handleRetryCountChange" />
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
        <div class="log-search-box">
          <compose-form-item>
            <bk-select
              v-model="searchModel"
              :clearable="false"
              style="width: 100px;">
              <bk-option
                id="log"
                :name="$t('history.搜索日志')" />
              <bk-option
                id="ip"
                :name="$t('history.搜索 IP')" />
            </bk-select>
            <bk-input
              v-if="searchModel === 'log'"
              key="log"
              :disabled="isFile"
              right-icon="bk-icon icon-search"
              style="width: 292px;"
              :tippy-tips="isFile ? $t('history.分发文件步骤不支持日志搜索') : ''"
              :value="params.keyword"
              @keyup="handleLogSearch" />
            <bk-input
              v-if="searchModel === 'ip'"
              key="ip"
              right-icon="bk-icon icon-search"
              style="width: 292px;"
              :value="params.searchIp"
              @keyup="handleIPSearch" />
          </compose-form-item>
          <div
            v-if="isLogSearching"
            class="search-loading">
            <icon
              class="loading-flag"
              type="loading" />
          </div>
          <div
            v-if="isIPSearching"
            class="search-loading">
            <icon
              class="loading-flag"
              type="loading" />
          </div>
        </div>
        <export-log
          :is-file="isFile"
          :step-instance-id="params.id" />
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
          <ip-list
            :data="dispalyGroup.agentTaskExecutionDetail"
            :list-loading="isLoading"
            :name="`${data.stepInstanceId}_${dispalyGroup.groupName}_${params.retryCount}_${params.keyword}_${params.searchIp}`"
            :pagination-loading="paginationChangeLoading"
            :search-value="`${params.keyword}_${params.searchIp}`"
            :total="dispalyGroup.agentTaskSize"
            @on-change="handleHostChange"
            @on-clear-search="handleClearSearch"
            @on-copy="handleCopyHost"
            @on-pagination-change="handlePaginationChange"
            @on-sort="handleSort" />
        </div>
        <div class="container-right">
          <!-- 执行日志 -->
          <execution-info
            v-if="data.stepInstanceId"
            :host="currentHost"
            :is-file="isFile"
            :is-task="isTask"
            :log-filter="params.keyword"
            :name="`${params.id}_${params.retryCount}_${dispalyGroup.groupName}_${currentHost.key}_${params.keyword}`"
            :retry-count="params.retryCount"
            :step-instance-id="data.stepInstanceId"
            @on-search="handleLogSearch" />
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
    </execution-status-bar>
  </div>
</template>
<script>
  import _ from 'lodash';

  import TaskExecuteService from '@service/task-execute';

  import {
    execCopy,
  } from '@utils/assist';

  import ComposeFormItem from '@components/compose-form-item';

  import ExecutionStatusBar from '../common/execution-status-bar';
  import StepAction from '../common/step-action';

  import ExecutionHistorySelect from './components/execution-history-select';
  import ExecutionInfo from './components/execution-info';
  import ExportLog from './components/export-log';
  import GroupTab from './components/group-tab';
  import IpList from './components/ip-list';
  import mixins from './components/mixins';
  import RollingBatch from './components/rolling-batch';
  import TaskStatus from './components/task-status';
  import ViewGlobalVariable from './components/view-global-variable';
  import ViewOperationRecord from './components/view-operation-record';
  import ViewStepInfo from './components/view-step-info';

  import I18n from '@/i18n';

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
      ComposeFormItem,
      ExecutionStatusBar,
      StepAction,
      RollingBatch,
      TaskStatus,
      ExecutionHistorySelect,
      GroupTab,
      IpList,
      ExecutionInfo,
      ExportLog,
      ViewGlobalVariable,
      ViewOperationRecord,
      ViewStepInfo,

    },
    mixins: [
      mixins,
    ],
    data() {
      return {
        isLoading: true,
        isLogSearching: false,
        isIPSearching: false,
        defailContainerStyles: {},
        // 搜索模式
        searchModel: 'log',
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
          retryCount: '',
          maxIpsPerResultGroup: 0,
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
        currentHost: {
          ip: '',
          retryCount: 0,
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
          agentTaskExecutionDetail: [],
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
        if (this.params.id < 1 || this.params.maxIpsPerResultGroup < 1) {
          return;
        }

        this.isLoading = true;
        TaskExecuteService.fetchStepExecutionResult({
          ...this.params,
          resultType: this.currentGroup.resultType,
          tag: this.currentGroup.tag,
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
            this.isLogSearching = false;
            this.isIPSearching = false;
            this.paginationChangeLoading = false;
          });
      }, 100),
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
          id: payload.stepInstanceId,
          retryCount: payload.retryCount,
        };
        this.taskInstanceId = payload.taskInstanceId;
        this.isTask = payload.isTask;
        this.taskStepList = Object.freeze(payload.taskStepList);
        this.taskExecution = payload.taskExecution;
        appendURLParams({
          retryCount: payload.retryCount,
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
       * @param {Number} retryCount 重试次数
       *
       * 刷新步骤执行详情
       */
      handleRetryCountChange(retryCount) {
        this.params = {
          ...this.params,
          retryCount,
        };
        appendURLParams({
          retryCount: this.params.retryCount,
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
       * @param {Object} host 选中的主机
       */
      handleHostChange(host) {
        this.currentHost = Object.freeze(host);
      },
      /**
       * @desc 组件列表分页
       * @param {Number} pageSize 每页条数
       */
      handlePaginationChange(pageSize) {
        this.params.maxIpsPerResultGroup = pageSize;
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
        this.isLogSearching = true;
        this.fetchStep();
      },
      /**
       * @desc 复制所有主机IP
       * @param { String } fieldName 复制的字段 IP | IPv6
       *
       * 主机列表是分页加载，复制全部主机时需要全量请求一次
       */
      handleCopyHost(fieldName) {
        TaskExecuteService.fetchStepGroupHost({
          ...this.params,
          resultType: this.currentGroup.resultType,
          tag: this.currentGroup.tag,
        }).then((data) => {
          const fieldDataList = data.reduce((result, item) => {
            if (item[fieldName]) {
              result.push(item[fieldName]);
            }
            return result;
          }, []);

          if (fieldDataList.length < 1) {
            this.$bkMessage({
              theme: 'warning',
              message: `${I18n.t('history.没有可复制内容')}`,
              limit: 1,
            });
            return;
          }
          const fieldNameText = fieldName === 'ip' ? 'IP' : 'IPv6';
          const successMessage = `${I18n.t('history.复制成功')}（${fieldDataList.length} ${I18n.t('history.个')}${fieldNameText}）`;
          execCopy(fieldDataList.join('\n'), successMessage);
        });
      },
      /**
       * @desc 导出脚本执行日志
       */
      handleExportExecutionLog() {
        TaskExecuteService.fetchStepExecutionLogFile({
          id: this.params.id,
        }).then(() => {
          this.$bkMessage({
            theme: 'success',
            message: I18n.t('history.导出日志操作成功'),
          });
        });
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
          this.params.retryCount = data.retryCount;
          this.$refs.taskStatus.reLoading();
          this.$refs.executionHistorySelect.reLoading();
          this.$bkMessage({
            limit: 1,
            theme: 'success',
            message: I18n.t('history.操作成功'),
          });
          appendURLParams({
            retryCount: this.params.retryCount,
          });
          this.fetchStep();
          return true;
        });
      },
      /**
       * @desc ip搜索
       * @param {String} value 搜索值，支持模糊搜索
       * @param {Object} event input交互事件
       *
       * 1，与日志搜索互斥
       * 2，enter建触发搜索
       * 3，重置页码（ip-list组件处理）
       */
      handleIPSearch(value, event) {
        if (event.isComposing) {
          // 跳过输入法复合事件
          return;
        }

        // 输入框的值被清空直接触发搜索
        // enter键开始搜索
        if ((value === '' && value !== this.params.searchIp)
          || event.keyCode === 13) {
          this.params.keyword = '';
          this.params.searchIp = value;
          this.isIPSearching = true;
          this.fetchStep();
        }
      },
      /**
       * @desc 日志搜索
       * @param {String} value 搜索值，支持模糊搜索
       * @param {Object} event input交互事件
       *
       * 1，与ip搜索互斥
       * 2，enter建触发搜索
       * 3，重置页码（ip-list组件处理）
       */
      handleLogSearch(value, event) {
        if (event.isComposing) {
          // 跳过输入法复合事件
          return;
        }

        // 输入框的值被清空直接触发搜索
        // enter键开始搜索
        if ((value === '' && value !== this.params.keyword)
          || event.keyCode === 13
          || event.type === 'click') {
          this.params.keyword = value;
          this.params.searchIp = '';
          this.isLogSearching = true;
          this.searchModel = 'log';
          this.fetchStep();
        }
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
      padding: 45px 24px 12px;
      background: #f5f6fa;

      .step-info-wraper {
        flex: 1;
        margin-top: -25px;

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

      .log-search-box {
        position: relative;
        display: flex;
        flex: 0 0 391px;
        background: #fff;

        .search-loading {
          position: absolute;
          top: 1px;
          right: 13px;
          bottom: 1px;
          display: flex;
          align-items: center;
          color: #c4c6cc;
          background: #fff;

          .loading-flag {
            animation: list-loading-ani 1s linear infinite;
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
        overflow: auto;
        background: #fff;
        border: 1px solid #dcdee5;
        border-bottom-left-radius: 2px;
        border-top-left-radius: 2px;
      }

      .container-right {
        display: flex;
        height: 100%;
        min-width: 800px;
        overflow: hidden;
        flex-direction: column;
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
</style>
