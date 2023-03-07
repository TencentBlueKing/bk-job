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
    class="choose-ip-host-table"
    :class="{ empty: isEmpty }">
    <scroll-faker :style="styles">
      <table>
        <template v-if="!$slots.default">
          <thead>
            <th style="width: 20%;">
              {{ $t('主机IP') }}
            </th>
            <th style="width: 20%;">
              {{ $t('云区域') }}
            </th>
            <th style="width: 150px;">
              {{ $t('Agent 状态') }}
            </th>
            <th>{{ $t('主机名') }}</th>
            <th>{{ $t('操作系统名称') }}</th>
            <th
              v-if="editable"
              class="action-column">
              {{ $t('操作') }}
            </th>
          </thead>
          <slot name="appendBefore" />
          <tbody
            v-if="renderList.length > 0"
            class="valid-list">
            <tr
              v-for="(row) in renderList"
              :key="row.hostId"
              :class="diff[row.hostId]">
              <td class="table-cell">
                <span
                  v-if="row.repeat"
                  class="repeat"
                  :tippy-tips="$t('指和其他主机的IP地址相同，但云区域不同')">
                  {{ $t('重复') }}
                </span>
                <div class="cell-text">
                  {{ row.ip }}
                </div>
              </td>
              <td>
                <div class="cell-text">
                  {{ row.cloudAreaInfo.name || '--' }}
                </div>
              </td>
              <td>
                <span v-if="row.alive">
                  <icon
                    style="vertical-align: middle;"
                    svg
                    type="normal" />
                  <span style="vertical-align: middle;">{{ $t('正常') }}</span>
                </span>
                <span v-else>
                  <icon
                    style="vertical-align: middle;"
                    svg
                    type="abnormal" />
                  <span style="vertical-align: middle;">{{ $t('异常') }}</span>
                </span>
              </td>
              <td>
                <div class="cell-text">
                  {{ row.ipDesc || '--' }}
                </div>
              </td>
              <td>
                <div class="cell-text">
                  {{ row.os || '--' }}
                </div>
              </td>
              <td
                v-if="editable"
                class="action-column">
                <bk-button
                  text
                  @click="handleRemove(row)">
                  {{ $t('移除') }}
                </bk-button>
              </td>
            </tr>
          </tbody>
        </template>
        <template v-else>
          <slot name="appendBefore" />
          <slot />
        </template>
      </table>
      <div
        v-if="isShowMorePage"
        class="list-more"
        @click="handlePage">
        {{ $t('查看更多') }}
      </div>
    </scroll-faker>
    <empty
      v-if="isEmpty"
      :type="emptyType" />
  </div>
</template>
<script>
  import Empty from '@components/empty';

  export default {
    name: '',
    components: {
      Empty,
    },
    props: {
      list: {
        type: Array,
        required: true,
      },
      editable: {
        type: Boolean,
        default: false,
      },
      maxHeight: {
        type: Number,
        default: -1,
      },
      appendNums: {
        type: Number,
        default: 0,
      },
      diff: {
        type: Object,
        default: () => ({}),
      },
      isSearch: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        page: 1,
        offset: 30,
      };
    },
    computed: {
      styles() {
        const style = {
          width: '100%',
        };
        if (this.list.length + this.appendNums > 10 && this.maxHeight > 0) {
          style.height = '410px';
        }
        return style;
      },
      renderList() {
        return Object.freeze(this.list.slice(0, this.page * this.offset));
      },
      isShowMorePage() {
        return this.renderList.length < this.list.length;
      },
      isEmpty() {
        return this.list.length < 1 && !this.$slots.appendBefore;
      },
      emptyType() {
        if (this.isSearch) {
          return 'search';
        }
        return '';
      },
    },
    methods: {
      /**
       * @desc 移除主机
       * @param { Object } hostInfo
       */
      handleRemove(hostInfo) {
        this.$emit('on-change', hostInfo);
      },
      handlePage() {
        this.page += 1;
      },
    },
  };
</script>
<style lang='postcss'>
  .choose-ip-host-table {
    &.empty {
      display: flex;
      align-items: center;
      flex-direction: column;
      min-height: 300px;

      .job-empty {
        margin-top: 90px;
      }
    }

    table {
      width: 100%;
      color: #63656e;
      table-layout: fixed;

      .valid-list {
        tr:last-child {
          td {
            border: none;
          }
        }
      }

      th,
      td {
        height: 41px;
        padding: 0 10px;
        font-size: 12px;
        font-weight: normal;
        line-height: 20px;
        text-align: left;
        border-bottom: 1px solid #e7e8ed;

        &:first-child {
          padding-left: 60px;
        }
      }

      th {
        color: #313238;
        white-space: nowrap;
        background: #f5f6fa;
      }

      td {
        word-break: break-all;
      }

      .table-cell {
        position: relative;

        .repeat,
        .invalid {
          position: absolute;
          top: 10px;
          height: 18px;
          padding: 0 5px;
          margin-left: -6px;
          font-size: 12px;
          line-height: 18px;
          text-align: center;
          border-radius: 2px;
          transform: translateX(-100%);
        }

        .repeat {
          color: #fff;
          background: #fe9c00;
        }

        .invalid {
          color: #979ba5;
          background: #f0f1f5;
        }
      }

      .cell-text {
        /* stylelint-disable value-no-vendor-prefix */
        display: -webkit-box;
        overflow: hidden;
        text-overflow: ellipsis;
        word-break: break-all;
        white-space: normal;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
      }
    }

    .bk-button-text {
      font-size: 12px;
    }

    .action-column {
      width: 90px;
      padding-right: 20px;
      text-align: right;
    }

    .loading-status {
      font-size: 16px;
      color: #3a84ff;
      vertical-align: middle;
      animation: rotate-loading 1s linear infinite;
    }

    .list-more {
      height: 40px;
      font-size: 12px;
      line-height: 40px;
      color: #63656e;
      text-align: center;
      cursor: pointer;
      border-top: 1px solid #e7e8ed;
    }
  }
</style>
