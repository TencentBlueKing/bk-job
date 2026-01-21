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
  <div
    ref="listRef"
    class="step-execute-container-list"
    :style="rootStyles">
    <bk-table
      v-if="tableMaxHeight"
      :data="list"
      :max-height="tableMaxHeight"
      :outer-border="false"
      :row-class-name="rowClassNameCallback"
      @row-click="handleRowClick"
      @scroll-end="handleScrollEnd"
      @sort-change="handleSortChange">
      <bk-table-column
        v-if="allShowColumn.includes('podName')"
        :label="$t('history.所属 Pod 名称')"
        :min-width="300"
        prop="podName"
        :render-header="renderPodNameHeader"
        show-overflow-tooltip>
        <template slot-scope="{ row }">
          <div
            class="ip-box"
            :class="row.result">
            {{ row.executeObject.container.podName || '--' }}
          </div>
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allShowColumn.includes('name')"
        :label="$t('history.容器名称')"
        :min-width="300"
        prop="name"
        :render-header="renderContainerNameHeader">
        <template slot-scope="{ row }">
          {{ row.executeObject.container.name }}
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allShowColumn.includes('totalTime')"
        :label="$t('history.耗时(s)')"
        prop="totalTime"
        sortable
        :width="100">
        <template slot-scope="{ row }">
          {{ row.executeObject.container.totalTime || '--' }}
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allShowColumn.includes('exitCode')"
        :label="$t('history.返回码')"
        prop="exitCode"
        sortable
        :width="100" />
      <bk-table-column
        v-if="allShowColumn.includes('clusterName')"
        :label="$t('history.集群名')"
        prop="clusterName"
        sortable
        :width="150">
        <template slot-scope="{ row }">
          {{ row.executeObject.container.clusterName || '--' }}
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allShowColumn.includes('namespace')"
        :label="$t('history.命名空间')"
        prop="namespace"
        sortable
        :width="180">
        <template slot-scope="{ row }">
          {{ row.executeObject.container.namespace || '--' }}
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allShowColumn.includes('workloadType')"
        :label="$t('history.工作负载')"
        prop="workloadType"
        sortable
        :width="120">
        <template slot-scope="{ row }">
          {{ row.executeObject.container.workloadType || '--' }}
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allShowColumn.includes('nodeIp')"
        :label="$t('history.节点IP')"
        prop="nodeIp"
        sortable
        :width="120">
        <template slot-scope="{ row }">
          {{ row.executeObject.container.nodeIp || '--' }}
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allShowColumn.includes('nodeHostId')"
        :label="$t('history.节点主机ID')"
        prop="nodeHostId"
        sortable
        :width="120">
        <template slot-scope="{ row }">
          {{ row.executeObject.container.nodeHostId || '--' }}
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allShowColumn.includes('uid')"
        :label="$t('history.容器 ID')"
        prop="uid"
        :render-header="renderContainerIdHeader"
        :width="600">
        <template slot-scope="{ row }">
          {{ row.executeObject.container.uid || '--' }}
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allShowColumn.includes('id')"
        label="ID"
        prop="id"
        sortable
        :width="100">
        <template slot-scope="{ row }">
          {{ row.executeObject.container.id || '--' }}
        </template>
      </bk-table-column>
      <bk-table-column
        fixed="right"
        label-class-name="setting-column"
        :render-header="renderSettingHeader"
        :resizable="false"
        :width="46">
        <template slot-scope="{ row }">
          <icon
            v-if="row.key === selectRowKey"
            style="color: #979ba5;"
            type="arrow-full-right" />
        </template>
      </bk-table-column>
      <template
        v-if="hasMore"
        #append>
        <div
          ref="loading"
          class="list-loading">
          <div class="loading-flag">
            <icon type="loading-circle" />
          </div>
          <div>{{ $t('history.加载中') }}</div>
        </div>
      </template>
      <template
        v-if="list.length < 1 && !listLoading"
        #empty>
        <empty v-if="!searchValue" />
        <empty
          v-else
          type="search">
          <div style="font-size: 14px; color: #63656e;">
            {{ $t('搜索结果为空') }}
          </div>
          <div style="margin-top: 8px; font-size: 12px; line-height: 16px; color: #979ba5;">
            <span>{{ $t('可以尝试调整关键词') }}</span>
            <span>{{ $t('或') }}</span>
            <bk-button
              text
              @click="handleClearSearch">
              {{ $t('清空搜索条件') }}
            </bk-button>
          </div>
        </empty>
      </template>
    </bk-table>
  </div>
</template>
<script setup>
  import {
    shallowRef,
  } from 'vue';

  import {
    execCopy,
  } from '@utils/assist';

  import Empty from '@components/empty';

  import I18n from '@/i18n';

  import useList from '../hooks/use-list';

  import ColumnSetting from './column-setting';

  const COLUMN_CACHE_KEY = 'STEP_EXECUTE_CONTAINER_COLUMN';

  const columnConfig = [
    {
      label: I18n.t('history.所属 Pod 名称'),
      name: 'podName',
      width: 240,
      disabled: true,
    },
    {
      label: I18n.t('history.容器名称'),
      name: 'name',
      width: 240,
      disabled: true,
    },
    {
      label: I18n.t('history.耗时(s)'),
      name: 'totalTime',
      orderField: 'totalTime',
      width: 100,
    },
    {
      label: I18n.t('history.返回码'),
      name: 'exitCode',
      width: 100,
    },
    {
      label: I18n.t('history.集群名'),
      name: 'clusterName',
      width: 100,
    },
    {
      label: I18n.t('history.命名空间'),
      name: 'namespace',
      width: 100,
    },
    {
      label: I18n.t('history.工作负载'),
      name: 'workloadType',
      width: 100,
    },
    {
      label: I18n.t('history.节点IP'),
      name: 'nodeIp',
      width: 100,
    },
    {
      label: I18n.t('history.节点主机ID'),
      name: 'nodeHostId',
      width: 100,
    },
    {
      label: I18n.t('history.容器 ID'),
      name: 'uid',
      width: 300,
    },
    {
      label: 'ID',
      name: 'id',
      width: 100,
    },
  ];

  let defaultShowColumn = [
    'name',
    'totalTime',
    'exitCode',
  ];
  if (localStorage.getItem(COLUMN_CACHE_KEY)) {
    defaultShowColumn = JSON.parse(localStorage.getItem(COLUMN_CACHE_KEY));
  }

  const props = defineProps({
    name: {
      type: [
        String,
        Number,
      ],
      required: true,
    },
    data: {
      type: Array,
      default: () => [],
    },
    listLoading: {
      type: Boolean,
      default: false,
    },
    total: {
      type: Number,
      default: 0,
    },
    searchValue: String,
    getAllTaskList: {
      type: Function,
      required: true,
    },
  });

  defineEmits([
    'on-pagination-change',
    'on-sort',
    'on-change',
    'on-clear-search',
    'on-copy',
  ]);

  const columnList = shallowRef([...columnConfig]);
  const allShowColumn = shallowRef(defaultShowColumn);

  const {
    listRef,
    list,
    tableMaxHeight,
    selectRowKey,
    hasMore,
    rootStyles,
    rowClassNameCallback,
    handleRowClick,
    handleScrollEnd,
    handleSortChange,
    handleClearSearch,
  } = useList(props, columnList, allShowColumn);

  const handleCopyContainerName = () => {
    props.getAllTaskList()
      .then((data) => {
        const containerNameList = data.reduce((result, item) => {
          if (item.container.name) {
            result.push(item.container.name);
          }
          return result;
        }, []);

        if (containerNameList.length < 1) {
          this.$bkMessage({
            theme: 'warning',
            message: I18n.t('history.没有可复制的容器名称'),
            limit: 1,
          });
          return;
        }
        const successMessage = `${I18n.t('history.复制成功')}（${containerNameList.length} ${I18n.t('history.个')} ${I18n.t('history.容器名称')}）`;
        execCopy(containerNameList.join('\n'), successMessage);
      });
  };

  const handleCopyContainerId = () => {
    props.getAllTaskList()
      .then((data) => {
        const containerIdList = data.reduce((result, item) => {
          if (item.container.uid) {
            result.push(item.container.uid);
          }
          return result;
        }, []);

        if (containerIdList.length < 1) {
          this.$bkMessage({
            theme: 'warning',
            message: I18n.t('history.容器 ID'),
            limit: 1,
          });
          return;
        }
        const successMessage = `${I18n.t('history.复制成功')}（${containerIdList.length} ${I18n.t('history.个')} ${I18n.t('history.容器 ID')}）`;
        execCopy(containerIdList.join('\n'), successMessage);
      });
  };

  const handleCopyContainerPodName = () => {
    props.getAllTaskList()
      .then((data) => {
        const containerPodNameList = data.reduce((result, item) => {
          if (item.container.podName) {
            result.push(item.container.podName);
          }
          return result;
        }, []);

        if (containerPodNameList.length < 1) {
          this.$bkMessage({
            theme: 'warning',
            message: I18n.t('history.没有可复制的所属 Pod 名称'),
            limit: 1,
          });
          return;
        }
        const successMessage = `${I18n.t('history.复制成功')}（${containerPodNameList.length} ${I18n.t('history.个')} ${I18n.t('history.所属 Pod 名称')}）`;
        execCopy(containerPodNameList.join('\n'), successMessage);
      });
  };

  /**
   * @desc 自定义 containerName 列的头
   */
  const renderContainerNameHeader = (h, { column }) => (
    <div>
      {column.label}
      <span
        v-bk-tooltips={I18n.t('history.没有可复制的容器名称')}
        class="copy-ip-btn"
        onClick={handleCopyContainerName}>
        <icon type="step-copy" />
      </span>
    </div>
  );
  /**
   * @desc 自定义 containerName 列的头
   */
  const renderContainerIdHeader = (h, { column }) => (
    <div>
      {column.label}
      <span
        v-bk-tooltips={I18n.t('history.容器 ID')}
        class="copy-ip-btn"
        onClick={handleCopyContainerId}>
        <icon type="step-copy" />
      </span>
    </div>
  );

  /**
   * @desc 自定义所属 Pod 名称列的头
   */
  const renderPodNameHeader = (h, { column }) => (
    <div>
      {column.label}
      <span
        v-bk-tooltips={I18n.t('history.所属 Pod 名称')}
        class="copy-ip-btn"
        onClick={handleCopyContainerPodName}>
        <icon type="step-copy" />
      </span>
    </div>
  );

  /**
   * @desc 自定义表格设置列头
   */
  const renderSettingHeader = () => (
    <ColumnSetting
      column-list={columnList.value}
      value={allShowColumn.value}
      onChange={handleSubmitSetting} />
  );

  /**
   * @desc 保存列配置
   */
  const handleSubmitSetting = (showColumnList) => {
    allShowColumn.value = showColumnList;
    localStorage.setItem(COLUMN_CACHE_KEY, JSON.stringify(showColumnList));
  };

</script>
<style lang='postcss'>
  @keyframes list-loading-ani {
    0% {
      transform: rotateZ(0);
    }

    100% {
      transform: rotateZ(360deg);
    }
  }

  .step-execute-container-list {
    width: 287px;
    height: 100%;
    max-height: 100%;
    min-height: 100%;

    .bk-table{
      height: 100%;
      scrollbar-gutter: stable;

      .bk-table-row{
        cursor: pointer;

        &.active{
          background: #f0f1f5;
        }
      }

      th,
      td {
        &:first-child {
          .cell{
            padding-left: 35px;
          }
        }

        .cell{
          color: #63656e;
        }
      }

      .bk-table-fixed-right{
        bottom: 0 !important;
        width: 45px !important;

        .bk-table-row-last{
          td.is-last{
            border-bottom: 1px solid #dfe0e5;
          }
        }
      }
    }

    .ip-box{
      position: relative;
      cursor: pointer;

      &.success,
      &.fail,
      &.running,
      &.waiting {
        &::before {
          position: absolute;
          top: 3px;
          width: 3px;
          height: 12px;
          margin-right: 1em;
          margin-left: -13px;
          background: #2dc89d;
          content: "";
        }
      }

      &.fail {
        &::before {
          background: #ea3636;
        }
      }

      &.running {
        &::before {
          background: #699df4;
        }
      }

      &.waiting {
        &::before {
          background: #dcdee5;
        }
      }
    }

    .copy-ip-btn {
      margin-left: 2px;
      font-size: 12px;
      font-weight: normal;
      cursor: pointer;

      &:hover {
        color: #3a84ff;
      }
    }

    .setting-column {
      .cell{
        padding: 0;
      }
    }

    .list-loading {
      display: flex;
      height: 40px;
      font-size: 12px;
      color: #979ba5;
      text-align: center;
      align-items: center;
      justify-content: center;

      .loading-flag {
        display: flex;
        width: 20px;
        height: 20px;
        animation: list-loading-ani 1s linear infinite;
        align-items: center;
        justify-content: center;
        transform-origin: center center;
      }
    }
  }

  .step-execution-history-ip-list-setting-theme {
    padding: 0 !important;
  }
</style>
