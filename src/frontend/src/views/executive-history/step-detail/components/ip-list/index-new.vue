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
    ref="listRef"
    class="step-execute-host-list"
    :style="styles">
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
        v-if="allShowColumn.includes('ipv4')"
        label="IP"
        :min-width="140"
        prop="ipv4"
        :render-header="renderIpHeader">
        <template slot-scope="{ row }">
          <div
            class="ip-box"
            :class="row.result">
            {{ row.ipv4 }}
          </div>
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allShowColumn.includes('ipv6')"
        label="IPv6"
        prop="ipv6"
        :render-header="renderIpv6Header"
        :width="300">
        <template slot-scope="{ row }">
          {{ row.ipv6 || '--' }}
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allShowColumn.includes('totalTime')"
        :label="$t('history.耗时(s)')"
        prop="totalTime"
        sortable
        :width="100">
        <template slot-scope="{ row }">
          {{ row.totalTime || '--' }}
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allShowColumn.includes('cloudAreaName')"
        :label="$t('history.管控区域')"
        prop="cloudAreaName"
        sortable
        :width="120">
        <template slot-scope="{ row }">
          {{ row.cloudAreaName || '--' }}
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allShowColumn.includes('exitCode')"
        :label="$t('history.返回码')"
        prop="exitCode"
        sortable
        :width="100">
        <template slot-scope="{ row }">
          {{ row.exitCode }}
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allShowColumn.includes('agentId')"
        label="Agent ID"
        prop="agentId"
        :width="240">
        <template slot-scope="{ row }">
          {{ row.agentId || '--' }}
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allShowColumn.includes('hostId')"
        label="Host ID"
        prop="hostId"
        sortable
        :width="100">
        <template slot-scope="{ row }">
          {{ row.hostId || '--' }}
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
  import _ from 'lodash';
  import {
    computed,
    onBeforeUnmount,
    onMounted,
    reactive,
    ref,
    shallowRef,
    watch,
  } from 'vue';

  import {
    getOffset,
    makeMap,
  } from '@utils/assist';

  import Empty from '@components/empty';

  import I18n from '@/i18n';

  import ColumnSetting from './column-setting-new';

  const COLUMN_CACHE_KEY = 'STEP_EXECUTE_IP_COLUMN3';

  const columnConfig = [
    {
      label: I18n.t('history.IP'),
      name: 'ipv4',
      width: 140,
      disabled: true,
    },
    {
      label: 'IPv6',
      name: 'ipv6',
      width: 300,
    },
    {
      label: I18n.t('history.耗时(s)'),
      name: 'totalTime',
      orderField: 'totalTime',
      width: 100,
    },
    {
      label: I18n.t('history.管控区域'),
      name: 'cloudAreaName',
      orderField: 'cloudAreaId',
      width: 120,
    },
    {
      label: I18n.t('history.返回码'),
      name: 'exitCode',
      width: 100,
    },
    {
      label: 'Agent ID',
      name: 'agentId',
      width: 240,
    },
    {
      label: 'Host ID',
      name: 'hostId',
      width: 100,
    },
  ];

  let defaultShowColumn = [
    'ipv4',
    'ipv6',
    'totalTime',
    'cloudAreaName',
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
  });

  const emits = defineEmits([
    'on-pagination-change',
    'on-sort',
    'on-change',
    'on-clear-search',
    'on-copy',
  ]);

  const listRef = ref();
  const list = shallowRef([]);
  const columnList = shallowRef([...columnConfig]);
  const allShowColumn = shallowRef(defaultShowColumn);
  const pagination = reactive({
    page: 1,
    pageSize: 0,
  });
  const tableMaxHeight = ref(0);
  const selectRowKey = ref('');
  const windowInnerWidth = ref(window.innerWidth);
  const positionLeftOffset = ref(0);

  const styles = computed(() => {
    const rightLogWidth = 800;
    const paddingLeft = 24;
    const maxWidth = windowInnerWidth.value - positionLeftOffset.value - rightLogWidth - paddingLeft;
    const allShowColumnMap = makeMap(allShowColumn.value);
    const allShowColumnWidth = columnList.value.reduce((result, item) => {
      if (allShowColumnMap[item.name]) {
        return result + item.width;
      }
      return result;
    }, 65);

    return {
      width: `${Math.min(Math.max(allShowColumnWidth, 217), maxWidth)}px`,
    };
  });

  const hasMore = computed(() => pagination.page * pagination.pageSize < props.total);

  watch(() => props.name, () => {
    pagination.page = 1;
    selectRowKey.value = '';
  });

  /**
   * @desc 点击行选择改行的主机
   */
  const handleRowClick = (row) => {
    selectRowKey.value = row.key;
    emits('on-change', row);
  };

  watch(() => props.data, () => {
    if (listRef.value) {
      positionLeftOffset.value = listRef.value.getBoundingClientRect().left;
    }
    // 切换分组时最新的分组数据一定来自API返回数据
    // listLoading为false说明是本地切换不更新列表
    if (!props.listLoading) {
      return;
    }
    list.value = props.data;

    if (props.data.length < 1) {
      handleRowClick({});
    } else if (!selectRowKey.value) {
      selectRowKey.value = props.data[0].key;
      handleRowClick(props.data[0]);
    }
  }, {
    immediate: true,
  });

  /**
   * @desc 根据屏幕高度计算单页 pageSize
   */
  const calcPageSize = () => {
    const { top } = getOffset(listRef.value);
    const windowHeight = window.innerHeight;
    const rowHeight = 40;
    const listHeight = windowHeight - top - 20;
    pagination.pageSize = parseInt(listHeight / rowHeight + 6, 10);
    emits('on-pagination-change', pagination.pageSize);
  };
  /**
   * @desc 自定义 IP 列的头
   */
  const renderIpHeader = (h, { column }) => (
    <div>
      {column.label}
      <span
        v-bk-tooltips={I18n.t('history.复制 IP')}
        class="copy-ip-btn"
        onClick={() => handleCopyIP('ip')}>
        <icon type="step-copy" />
      </span>
    </div>
  );
  /**
   * @desc 自定义 IPv6 列头
   */
  const renderIpv6Header = (h, { column }) => (
    <div>
      {column.label}
      <span
        v-bk-tooltips={I18n.t('history.复制 IPv6')}
        class="copy-ip-btn"
        onClick={() => handleCopyIP('ipv6')}>
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
   * @desc 表格行的样式 class
   */
  const rowClassNameCallback = ({ row }) => (selectRowKey.value === row.key ? 'active' : '');
  /**
   * @desc 表格滚动到底部
   */
  const handleScrollEnd = () => {
    if (!hasMore.value) {
      return;
    }
    // 增加分页
    pagination.page = pagination.page + 1;
    emits('on-pagination-change', pagination.page * pagination.pageSize);
  };

  /**
   * @desc 列表数据排序
   */
  const handleSortChange = ({ prop, order }) => {
    if (!order) {
      emits('on-sort', {
        orderField: '',
        order: '',
      });
    } else {
      emits('on-sort', {
        orderField: prop,
        order: order === 'descending' ? 0 : 1,
      });
    }

    emits('on-pagination-change', pagination.pageSize);
  };
  /**
   * @desc 复制ip
   * @param { String } type 要复制的字段，IP | IPv6
   */
  const handleCopyIP = (type) => {
    emits('on-copy', type);
  };
  /**
   * @desc 保存列配置
   */
  const handleSubmitSetting = (showColumnList) => {
    allShowColumn.value = showColumnList;
    localStorage.setItem(COLUMN_CACHE_KEY, JSON.stringify(showColumnList));
  };
  /**
   * @desc 清空搜索
   */
  const handleClearSearch = () => {
    emits('on-clear-search');
  };

  const handleWindowResize = _.throttle(() => {
    windowInnerWidth.value = window.innerWidth;
  }, 60);

  onMounted(() => {
    calcPageSize();
    tableMaxHeight.value = listRef.value.getBoundingClientRect().height;

    window.addEventListener('resize', handleWindowResize);
    onBeforeUnmount(() => {
      window.removeEventListener('resize', handleWindowResize);
    });
  });
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

  .step-execute-host-list {
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
