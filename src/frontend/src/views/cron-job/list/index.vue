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
  <div class="cron-job-manage">
    <list-action-layout>
      <auth-button
        ref="create"
        v-test="{ type: 'button', value: 'createCrontab' }"
        auth="cron/create"
        class="w120"
        theme="primary"
        @click="handleCreate">
        {{ $t('cron.新建') }}
      </auth-button>
      <template #right>
        <jb-search-select
          ref="search"
          :data="searchSelect"
          :placeholder="$t('cron.搜索任务ID，任务名称，更新人...')"
          style="width: 480px;"
          @on-change="handleSearch" />
      </template>
    </list-action-layout>
    <render-list
      ref="list"
      v-test="{ type: 'list', value: 'crontab' }"
      :data-source="getCronJobList"
      :search-control="() => $refs.search"
      :size="tableSize">
      <bk-table-column
        v-if="allRenderColumnMap.id"
        key="id"
        align="left"
        label="ID"
        prop="id"
        width="100" />
      <bk-table-column
        v-if="allRenderColumnMap.name"
        key="name"
        align="left"
        :label="$t('cron.任务名称.colHead')"
        min-width="200"
        prop="name"
        show-overflow-tooltip
        sortable="custom">
        <template slot-scope="{ row }">
          <auth-component
            auth="cron/view"
            :permission="row.canManage"
            :resource-id="row.id">
            <span
              class="time-task-name"
              @click="handleViewDetail(row)">
              {{ row.name }}
            </span>
            <span slot="forbid">{{ row.name }}</span>
          </auth-component>
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allRenderColumnMap.taskPlanId"
        key="taskPlanId"
        align="left"
        :label="$t('cron.执行方案ID')"
        prop="taskPlanId"
        width="120">
        <template slot-scope="{ row }">
          <router-link
            target="_blank"
            :to="{
              name: 'viewPlan',
              params: {
                templateId: row.taskTemplateId,
              },
              query: {
                from: 'cronJob',
                viewPlanId: row.taskPlanId,
              },
            }">
            {{ row.taskPlanId }}
          </router-link>
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allRenderColumnMap.planName"
        key="planName"
        align="left"
        :label="$t('cron.执行方案名称')"
        min-width="200"
        show-overflow-tooltip>
        <template slot-scope="{ row }">
          <div
            v-if="row.isPlanLoading"
            class="sync-fetch">
            <div class="sync-fetch-loading">
              <icon
                style="color: #3a84ff;"
                svg
                type="sync-pending" />
            </div>
          </div>
          <router-link
            v-else
            class="task-plan-text"
            target="_blank"
            :to="{
              name: 'viewPlan',
              params: {
                templateId: row.taskTemplateId,
              },
              query: {
                from: 'cronJob',
                viewPlanId: row.taskPlanId,
              },
            }">
            {{ row.taskPlanName }}
          </router-link>
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allRenderColumnMap.policeText"
        key="policeText"
        align="left"
        :label="$t('cron.执行策略.colHead')"
        prop="policeText"
        width="180">
        <template slot-scope="{ row }">
          <span
            v-bk-tooltips.right="row.executeTimeTips"
            class="tips">
            {{ row.policeText }}
          </span>
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allRenderColumnMap.creator"
        key="creator"
        align="left"
        :label="$t('cron.创建人')"
        prop="creator"
        width="120" />
      <bk-table-column
        v-if="allRenderColumnMap.createTime"
        key="createTime"
        align="left"
        :label="$t('cron.创建时间')"
        prop="createTime"
        width="180" />
      <bk-table-column
        v-if="allRenderColumnMap.lastModifyUser"
        key="lastModifyUser"
        align="left"
        :label="$t('cron.更新人.colHead')"
        prop="lastModifyUser"
        sortable="custom"
        width="140" />
      <bk-table-column
        v-if="allRenderColumnMap.lastModifyTime"
        key="lastModifyTime"
        align="left"
        :label="$t('cron.更新时间')"
        prop="lastModifyTime"
        width="180" />
      <bk-table-column
        v-if="allRenderColumnMap.lastExecuteStatus"
        key="lastExecuteStatus"
        align="left"
        :label="$t('cron.最新执行结果')"
        prop="lastExecuteStatus"
        sortable="custom"
        width="150">
        <template slot-scope="{ row }">
          <icon
            style="font-size: 16px; vertical-align: middle;"
            svg
            :type="row.statusIconType" />
          <span style="vertical-align: middle;">{{ row.statusText }}</span>
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allRenderColumnMap.successRateText"
        key="successRateText"
        align="left"
        :label="$t('cron.周期成功率')"
        :render-header="renderHeader"
        width="150">
        <template slot-scope="{ row }">
          <div
            v-if="row.isStatictisLoading"
            class="sync-fetch">
            <div class="sync-fetch-loading">
              <icon
                style="color: #3a84ff;"
                svg
                type="sync-pending" />
            </div>
          </div>
          <template v-else>
            <template v-if="row.isRateEmpty">
              <p v-html="row.successRateText" />
            </template>
            <template v-else>
              <bk-popover
                placement="right"
                theme="light">
                <p
                  style="padding-right: 10px;"
                  v-html="row.successRateText" />
                <div
                  slot="content"
                  style="color: #63656e;">
                  <div v-html="row.successRateTips" />
                  <div
                    v-if="row.showMoreFailAcion"
                    class="more-fail-action">
                    <bk-button
                      text
                      @click="handleHistoryRecord(row, true)">
                      {{ $t('cron.更多失败记录') }}
                    </bk-button>
                  </div>
                </div>
              </bk-popover>
            </template>
          </template>
        </template>
      </bk-table-column>
      <bk-table-column
        key="action"
        align="left"
        fixed="right"
        :label="$t('cron.操作')"
        :resizable="false"
        width="200">
        <template slot-scope="{ row }">
          <bk-switcher
            v-test="{ type: 'button', value: 'toggleCrontabStatus' }"
            class="mr10"
            size="small"
            theme="primary"
            :value="row.enable"
            @change="value => handleStatusChange(value, row)" />
          <auth-button
            v-test="{ type: 'button', value: 'editCrontab' }"
            auth="cron/edit"
            class="time-task-edit mr10"
            :permission="row.canManage"
            :resource-id="row.id"
            text
            @click="handleEdit(row)">
            {{ $t('cron.编辑') }}
          </auth-button>
          <jb-popover-confirm
            :confirm-handler="instance => handleDelete(row)"
            :content="$t('cron.删除后不可恢复，请谨慎操作！')"
            :title="$t('cron.确定删除该定时任务？')">
            <auth-button
              v-test="{ type: 'button', value: 'deleteCrontab' }"
              auth="cron/delete"
              :permission="row.canManage"
              :resource-id="row.id"
              text>
              {{ $t('cron.删除') }}
            </auth-button>
          </jb-popover-confirm>
          <bk-button
            v-test="{ type: 'button', value: 'crontabExecRecord' }"
            text
            @click="handleHistoryRecord(row)">
            {{ $t('cron.执行记录') }}
          </bk-button>
        </template>
      </bk-table-column>
      <bk-table-column type="setting">
        <bk-table-setting-content
          :fields="tableColumn"
          :selected="selectedTableColumn"
          :size="tableSize"
          @setting-change="handleSettingChange" />
      </bk-table-column>
    </render-list>
    <jb-sideslider
      :is-show.sync="showOperation"
      v-bind="operationSidesliderInfo"
      :width="960">
      <task-operation
        v-if="showOperation"
        :data="cronJobDetailInfo"
        @on-change="handleCronChange" />
    </jb-sideslider>
    <jb-sideslider
      :is-show.sync="showDetail"
      :title="$t('cron.定时任务详情')"
      :width="960">
      <task-detail :data="cronJobDetailInfo" />
      <template #footer>
        <bk-button
          v-test="{ type: 'button', value: 'showCrontabDetail' }"
          theme="primary"
          @click="handleToggelEdit">
          {{ $t('cron.编辑') }}
        </bk-button>
      </template>
    </jb-sideslider>
    <jb-sideslider
      :is-show.sync="showHistoryRecord"
      quick-close
      :show-footer="false"
      transfer
      :width="960">
      <div slot="header">
        <span>{{ $t('cron.定时执行记录') }}</span>
        <span style="font-size: 12px; color: #313238;">（{{ cronJobDetailInfo.name }}）</span>
      </div>
      <history-record
        v-if="showHistoryRecord"
        :data="cronJobDetailInfo"
        :show-faild="showHistoryFailedRecord"
        @on-change="handleCronChange" />
    </jb-sideslider>
  </div>
</template>
<script>
  import NotifyService from '@service/notify';
  import TimeTaskService from '@service/time-task';

  import { listColumnsCache } from '@utils/cache-helper';

  import JbPopoverConfirm from '@components/jb-popover-confirm';
  import JbSearchSelect from '@components/jb-search-select';
  import JbSideslider from '@components/jb-sideslider';
  import ListActionLayout from '@components/list-action-layout';
  import RenderList from '@components/render-list';

  import TaskDetail from './components/detail';
  import HistoryRecord from './components/history-record';
  import TaskOperation from './components/operation';

  import I18n from '@/i18n';

  const TABLE_COLUMN_CACHE = 'cron_list_columns';

  export default {
    name: '',
    components: {
      ListActionLayout,
      RenderList,
      JbSearchSelect,
      JbSideslider,
      JbPopoverConfirm,
      TaskOperation,
      TaskDetail,
      HistoryRecord,
    },
    data() {
      return {
        showOperation: false,
        showDetail: false,
        showHistoryRecord: false,
        showHistoryFailedRecord: false,
        searchParams: [],
        cronData: {},
        loading: false,
        cronJobDetailInfo: {},
        currentOperate: 'create',
        historyRecordDialogTitle: '',
        selectedTableColumn: [],
        tableSize: 'small',
      };
    },
    computed: {
      isSkeletonLoading() {
        return this.$refs.list.isLoading;
      },
      allRenderColumnMap() {
        return this.selectedTableColumn.reduce((result, item) => {
          result[item.id] = true;
          return result;
        }, {});
      },
      operationSidesliderInfo() {
        if (this.cronJobDetailInfo.id) {
          return {
            title: I18n.t('cron.编辑定时任务'),
            okText: I18n.t('cron.保存'),
          };
        }
        return {
          title: I18n.t('cron.新建定时任务'),
          okText: I18n.t('cron.提交'),
        };
      },
    },
    watch: {
      '$route'() {
        this.initParseURL();
      },
    },
    created() {
      this.getCronJobList = TimeTaskService.timeTaskList;
      this.searchSelect = [
        {
          name: 'ID',
          id: 'cronJobId',
          description: I18n.t('cron.将覆盖其它条件'),
          validate(values, item) {
            const validate = (values || []).every(_ => /^(\d*)$/.test(_.name));
            return !validate ? I18n.t('cron.ID只支持数字') : true;
          },
        },
        {
          name: I18n.t('cron.任务名称.colHead'),
          id: 'name',
          default: true,
        },
        {
          name: I18n.t('cron.执行方案ID'),
          id: 'planId',
          default: true,
        },
        {
          name: I18n.t('cron.创建人'),
          id: 'creator',
          remoteMethod: NotifyService.fetchUsersOfSearch,
          inputInclude: true,
        },
        {
          name: I18n.t('cron.更新人.colHead'),
          id: 'lastModifyUser',
          remoteMethod: NotifyService.fetchUsersOfSearch,
          inputInclude: true,
        },
      ];
      this.tableColumn = [
        {
          id: 'id',
          label: 'ID',
        },
        {
          id: 'name',
          label: I18n.t('cron.任务名称.colHead'),
          disabled: true,
        },
        {
          id: 'taskPlanId',
          label: I18n.t('cron.执行方案ID'),
        },
        {
          id: 'planName',
          label: I18n.t('cron.执行方案名称'),
        },
        {
          id: 'policeText',
          label: I18n.t('cron.执行策略.colHead'),
          disabled: true,
        },
        {
          id: 'creator',
          label: I18n.t('cron.创建人'),
        },
        {
          id: 'createTime',
          label: I18n.t('cron.创建时间'),
        },
        {
          id: 'lastModifyUser',
          label: I18n.t('cron.更新人.colHead'),
        },
        {
          id: 'lastModifyTime',
          label: I18n.t('cron.更新时间'),
        },
        {
          id: 'lastExecuteStatus',
          label: I18n.t('cron.最新执行结果'),
          disabled: true,
        },
        {
          id: 'successRateText',
          label: I18n.t('cron.周期成功率'),
          disabled: true,
        },
      ];
      const columnsCache = listColumnsCache.getItem(TABLE_COLUMN_CACHE);
      if (columnsCache) {
        this.selectedTableColumn = Object.freeze(columnsCache.columns);
        this.tableSize = columnsCache.size;
      } else {
        this.selectedTableColumn = Object.freeze([
          { id: 'name' },
          { id: 'planName' },
          { id: 'policeText' },
          { id: 'lastModifyUser' },
          { id: 'lastModifyTime' },
          { id: 'lastExecuteStatus' },
          { id: 'successRateText' },
        ]);
      }
    },
    mounted() {
      this.initParseURL();
    },
    methods: {
      /**
       * @desc 获取列表数据
       */
      fetchData() {
        this.$refs.list.$emit('onFetch', this.searchParams);
      },
      /**
       * @desc 解析 URL 参数
       */
      initParseURL() {
        // 在列表通过url指定查看定时任务详情
        const {
          name,
          cronJobId,
          // mode 表示 url 访问的场景，
          // create: 展示新建定时任务弹框
          // detail: 展示定时任务详情弹框
          // edit: 展示编辑定时任务弹框
          mode,
        } = this.$route.query;
        if (mode === 'create') {
          this.handleCreate();
          return;
        }

        if (!name && !cronJobId) {
          return;
        }

        const unWatch = this.$watch(() => this.$refs.list.isLoading, (isLoading) => {
          if (!isLoading) {
            if (mode === 'detail') {
              setTimeout(() => {
                // 通过url默认打开定时任务详情
                const $firstTimeTaskName = this.$refs.list.$el.querySelector('.time-task-name');
                if ($firstTimeTaskName) {
                  $firstTimeTaskName.click();
                }
              });
            } else if (mode === 'edit') {
              setTimeout(() => {
                // // 通过url默认打开定时任务编辑
                const $firstTimeTask = this.$refs.list.$el.querySelector('.time-task-edit');
                if ($firstTimeTask) {
                  $firstTimeTask.click();
                }
              });
            }

            unWatch();
          }
        });
      },
      /**
       * @desc 表格表头渲染
       * @param { Function } h
       * @param { Object } data 表格配置信息
       * @returns { vNode }
       */
      renderHeader(h, data) {
        return (
                <span>
                    <span>{ data.column.label }</span>
                    <bk-popover>
                        <icon
                            type="circle-italics-info"
                            style="margin-left: 8px; font-size: 12px;" />
                        <div slot="content">
                            <div style="font-weight: bold">{ I18n.t('cron.「周期成功率」采样规则和计算公式') }</div>
                            <div style="margin-top: 8px; font-weight: bold">{ I18n.t('cron.采样规则：') }</div>
                            <div>{ I18n.t('cron.近 24小时执行次数 ＞10，则 “分母” 为近 24 小时执行总数') }</div>
                            <div>{ I18n.t('cron.近 24小时执行次数 ≤ 10，则 “分母” 为近 10 次执行任务') }</div>
                            <div style="margin-top: 6px; font-weight: bold">{ I18n.t('cron.计算公式：') }</div>
                            <div>{ I18n.t('cron.成功次数(分子) / 分母 * 100 = 周期成功率（%）') }</div>
                        </div>
                    </bk-popover>
                </span>
        );
      },
      /**
       * @desc 表格列自定义
       * @param { Object } 列信息
       */
      handleSettingChange({ fields, size }) {
        this.selectedTableColumn = Object.freeze(fields);
        this.tableSize = size;
        listColumnsCache.setItem(TABLE_COLUMN_CACHE, {
          columns: fields,
          size,
        });
      },
      /**
       * @desc 搜索
       * @param { Object } searchParams
       */
      handleSearch(searchParams) {
        this.searchParams = searchParams;
        this.fetchData();
      },
      /**
       * @desc 查看执行记录
       * @param { Object } crontabData 定时任务信息
       * @param { Boolean } showFailed 显示失败记录
       */
      handleHistoryRecord(crontabData, showFailed = false) {
        this.cronJobDetailInfo = crontabData;
        this.showHistoryFailedRecord = showFailed;
        this.historyRecordDialogTitle = `定时执行记录${crontabData.name}`;
        this.showHistoryRecord = true;
      },
      /**
       * @desc 定时任务详情
       * @param { Object } crontabData 定时任务信息
       */
      handleViewDetail(crontabData) {
        this.cronJobDetailInfo = crontabData;
        this.showDetail = true;
      },
      /**
       * @desc 新建定时任务
       */
      handleCreate() {
        this.cronJobDetailInfo = {};
        this.showOperation = true;
      },
      /**
       * @desc 编辑定时任务
       * @param { Object } crontabData 定时任务信息
       */
      handleEdit(crontabData) {
        this.cronJobDetailInfo = crontabData;
        this.showOperation = true;
      },
      /**
       * @desc 从详情切换为编辑状态
       */
      handleToggelEdit() {
        this.showDetail = false;
        this.showOperation = true;
      },
      /**
       * @desc 定时任务有更新刷新列表数据
       */
      handleCronChange() {
        this.fetchData();
      },
      /**
       * @desc 切换定时任务状态
       * @param { Boolean } enable 开启状态
       * @param { Object } crontabData 定时任务信息
       */
      handleStatusChange(enable, crontabData) {
        const enableMemo = crontabData.enable;
        crontabData.enable = enable;
        TimeTaskService.timeTaskStatusUpdate({
          id: crontabData.id,
          enable,
        }).then(() => {
          this.messageSuccess(enable ? I18n.t('cron.开启成功') : I18n.t('cron.关闭成功'));
        })
          .catch(() => {
            crontabData.enable = enableMemo;
          });
      },
      /**
       * @desc 删除定时任务
       * @param { Object } crontabData 定时任务信息
       *
       * 删除成功后刷新列表数据
       */
      handleDelete(crontabData) {
        return TimeTaskService.timeTaskDelete({
          id: crontabData.id,
        }).then(() => {
          this.messageSuccess(I18n.t('cron.删除定时任务成功'));
          this.fetchData();
          return true;
        });
      },
    },
  };
</script>
<style lang="postcss">
  @keyframes sync-fetch-loading {
    0% {
      transform: rotateZ(0);
    }

    100% {
      transform: rotateZ(360deg);
    }
  }

  .cron-job-manage {
    .expression {
      font-size: 14px;
      color: #c4c6cc;
    }

    .more-fail-action {
      text-align: right;

      .bk-button-text {
        font-size: 12px;
      }
    }

    .time-task-name {
      display: inline-block;
      height: 18px;
      max-width: 100%;
      overflow: hidden;
      color: #3a84ff;
      text-overflow: ellipsis;
      white-space: nowrap;
      vertical-align: bottom;
      cursor: pointer;
    }

    .execute-result-text {
      &.success {
        &::before {
          background: #2dcb56;
        }
      }

      &.fail {
        &::before {
          background: #ea3636;
        }
      }

      &.waiting {
        &::before {
          background: #dcdee5;
        }
      }

      &::before {
        display: inline-block;
        width: 8px;
        height: 8px;
        margin-right: 10px;
        border-radius: 50%;
        content: "";
      }
    }

    .sync-fetch {
      height: 13px;
    }

    .sync-fetch-loading {
      position: absolute;
      display: flex;
      width: 13px;
      height: 13px;
      animation: sync-fetch-loading 1s linear infinite;
    }

    .task-plan-text {
      display: inline-block;
      height: 18px;
      max-width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      vertical-align: bottom;
    }
  }

</style>
