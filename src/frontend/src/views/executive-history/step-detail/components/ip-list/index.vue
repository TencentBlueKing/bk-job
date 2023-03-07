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
    class="step-execute-host-list"
    :style="styles">
    <list-head
      :columns="columnList"
      :show-columns="allShowColumn"
      @on-copy="handleCopyIP"
      @on-show-setting="handleShowSetting"
      @on-sort="handleSort" />
    <div
      ref="list"
      class="ip-list-body">
      <scroll-faker @on-scroll="handleScroll">
        <list-body
          :columns="columnList"
          :data="list"
          :show-columns="allShowColumn"
          @on-row-select="handleSelect" />
        <div
          v-if="hasMore"
          ref="loading"
          class="list-loading">
          <div class="loading-flag">
            <icon type="loading-circle" />
          </div>
          <div>{{ $t('history.加载中') }}</div>
        </div>
        <template v-if="list.length < 1 && !listLoading">
          <empty
            v-if="!searchValue"
            style="height: 100%;" />
          <empty
            v-else
            style="height: 100%;"
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
      </scroll-faker>
    </div>
    <div ref="setting">
      <column-setting
        :column-list="columnList"
        :value="allShowColumn"
        @change="handleSubmitSetting"
        @close="handleHideSetting" />
    </div>
  </div>
</template>
<script>
  import _ from 'lodash';

  import {
    getOffset,
    makeMap,
  } from '@utils/assist';

  import Empty from '@components/empty';

  import ColumnSetting from './column-setting';
  import ListBody from './list-body';
  import ListHead from './list-head';

  import I18n from '@/i18n';

  const COLUMN_CACHE_KEY = 'STEP_EXECUTE_IP_COLUMN3';
  const LIST_ROW_HEIGHT = 40; // 每列高度

  const columnList = [
    {
      label: I18n.t('history.IP'),
      name: 'ipv4',
      width: 140,
      checked: true,
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
      order: '',
      width: 100,
      checked: true,
    },
    {
      label: I18n.t('history.云区域'),
      name: 'cloudAreaName',
      orderField: 'cloudAreaId',
      order: '',
      width: 120,
      checked: true,
    },
    {
      label: I18n.t('history.返回码'),
      name: 'exitCode',
      orderField: 'exitCode',
      order: '',
      width: 100,
      checked: true,
    },
    {
      label: 'Agent ID',
      name: 'agentId',
      width: 140,
      checked: true,
    },
    {
      label: 'Host ID',
      name: 'hostId',
      orderField: 'hostId',
      order: '',
      width: 90,
      checked: true,
    },
  ];

  export default {
    name: '',
    components: {
      Empty,
      ListHead,
      ListBody,
      ColumnSetting,
    },
    props: {
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
      paginationLoading: {
        type: Boolean,
        default: false,
      },
      total: {
        type: Number,
        default: 0,
      },
      searchValue: String,
    },
    data() {
      let allShowColumn = [
        'ipv4',
        'ipv6',
        'totalTime',
        'cloudAreaName',
        'exitCode',
      ];
      if (localStorage.getItem(COLUMN_CACHE_KEY)) {
        allShowColumn = JSON.parse(localStorage.getItem(COLUMN_CACHE_KEY));
      }
      return {
        list: [],
        columnList: Object.freeze(columnList),
        page: 1,
        pageSize: 0,
        isShowColumnSetting: false,
        allShowColumn,
        tempAllShowColumn: allShowColumn,
      };
    },
    computed: {
      /**
       * @desc IP 列表样式判断
       * @return {Object}
       */
      styles() {
        const allShowColumnMap = makeMap(this.allShowColumn);
        const allShowColumnWidth = columnList.reduce((result, item) => {
          if (allShowColumnMap[item.name]) {
            return result + item.width;
          }
          return result;
        }, 65);

        return {
          width: `${Math.max(allShowColumnWidth, 217)}px`,
        };
      },
      /**
       * @desc 列选择是否半选状态
       * @return {Boolean}
       */
      hasMore() {
        return this.page * this.pageSize < this.total;
      },
    },

    watch: {
      /**
       * @desc IP 列表名称变化时重置翻页
       */
      name() {
        this.page = 1;
      },
      data: {
        handler(data) {
          // 切换分组时最新的分组数据一定来自API返回数据
          // listLoading为false说明是本地切换不更新列表
          if (!this.listLoading) {
            return;
          }
          this.list = Object.freeze(data);
        },
        immediate: true,
      },
    },
    mounted() {
      this.initSettingPopover();
      this.calcPageSize();
      window.addEventListener('resize', this.handleScroll);
      this.$once('hook:beforeDestroy', () => {
        window.removeEventListener('resize', this.handleScroll);
      });
    },
    methods: {
      initSettingPopover() {
        this.settingPopover = this.$bkPopover(document.querySelector('#stepDetailIpListSettingBtn'), {
          theme: 'light step-execution-history-ip-list-setting',
          arrow: true,
          interactive: true,
          placement: 'bottom-start',
          content: this.$refs.setting,
          animation: 'slide-toggle',
          trigger: 'click',
          width: '450px',
        });
        this.$once('hook:beforeDestroy', () => {
          this.settingPopover.destroy();
          this.settingPopover.hide();
          this.settingPopover = null;
        });
      },
      /**
       * @desc 根据屏幕高度计算单页 pageSize
       */
      calcPageSize() {
        const { top } = getOffset(this.$refs.list);
        const windowHeight = window.innerHeight;
        const listHeight = windowHeight - top - 20;
        this.pageSize = parseInt(listHeight / LIST_ROW_HEIGHT + 6, 10);
        this.$emit('on-pagination-change', this.pageSize);
      },
      /**
       * @desc 滚动加载
       */
      handleScroll: _.throttle(function () {
        if (!this.hasMore) {
          return;
        }
        const windowHeight = window.innerHeight;
        const { top } = this.$refs.loading.getBoundingClientRect();

        if (top - 80 < windowHeight) {
          // 增加分页
          this.page += 1;
          this.$emit('on-pagination-change', this.page * this.pageSize);
        }
      }, 80),
      /**
       * @desc 复制ip
       * @param { String } type 要复制的字段，IP | IPv6
       */
      handleCopyIP(type) {
        this.$emit('on-copy', type);
      },
      /**
       * @desc 显示列配置面板
       */
      handleShowSetting() {
        this.isShowColumnSetting = true;
      },
      /**
       * @desc 保存列配置
       */
      handleSubmitSetting(showColumnList) {
        this.allShowColumn = showColumnList;
        this.isShowColumnSetting = false;
        localStorage.setItem(COLUMN_CACHE_KEY, JSON.stringify(this.allShowColumn));
      },
      /**
       * @desc 隐藏列配置面板
       */
      handleHideSetting() {
        this.isShowColumnSetting = false;
        this.settingPopover.hide();
      },
      /**
       * @desc 表格排序
       * @param {Object} column 操作列数据
       */
      handleSort(column) {
        const {
          orderField,
          order,
        } = column;
        const newOrder = order === 1 ? 0 : 1;
        column.order = newOrder;

        this.columnList = Object.freeze(this.columnList.map((item) => {
          item.order = '';
          if (item.orderField === orderField) {
            item.order = newOrder;
          }
          return { ...item };
        }));
        this.$emit('on-sort', {
          orderField,
          order: newOrder,
        });
        this.$emit('on-pagination-change', this.pageSize);
      },
      /**
       * @desc 选择表格一行数据
       * @param {Object} row 选择数据
       */
      handleSelect(row) {
        this.$emit('on-change', row);
      },
      /**
       * @desc 清空搜索
       */
      handleClearSearch() {
        this.$emit('on-clear-search');
      },
    },
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

  .step-execute-host-list {
    width: 287px;
    height: 100%;
    max-height: 100%;
    min-height: 100%;
    transition: all 0.3s;

    .ip-list-body {
      height: calc(100% - 41px);
    }

    .ip-table {
      width: 100%;
      table-layout: fixed;

      tbody {
        tr {
          &.active {
            background: #f0f1f5;

            .active-flag {
              font-size: 14px;
            }
          }

          .active-flag {
            padding: 0;
            font-size: 0;
            color: #979ba5;
            text-align: center;
          }
        }
      }

      th,
      td {
        height: 40px;
        padding-left: 16px;
        line-height: 40px;
        text-align: left;
        border-bottom: 1px solid #dcdee5;

        &:first-child {
          padding-left: 34px;
        }
      }

      th {
        position: relative;
        font-weight: normal;
        color: #313238;

        &.sort {
          cursor: pointer;
        }

        .sort-box {
          position: absolute;
          top: 0;
          display: inline-flex;
          height: 100%;
          margin-left: 9px;
          font-size: 6px;
          color: #c4c6cc;
          justify-content: center;
          flex-direction: column;

          .top,
          .bottom {
            width: 0;
            height: 0;
            margin: 1px 0;
            border: 5px solid transparent;

            &.active {
              color: #3a84ff;
            }
          }

          .top {
            border-bottom-color: currentcolor;
          }

          .bottom {
            border-top-color: currentcolor;
          }
        }
      }

      td {
        position: relative;
        color: #63656e;
        cursor: pointer;

        &.success,
        &.fail,
        &.running,
        &.waiting {
          &::before {
            position: absolute;
            top: 14px;
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

        .cell-text {
          height: 20px;
          overflow: hidden;
          line-height: 20px;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
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
