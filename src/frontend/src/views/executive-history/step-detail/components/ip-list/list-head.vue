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
  <div class="ip-list-head">
    <table class="ip-table">
      <colgroup>
        <template v-for="item in columns">
          <col
            v-if="showColumns.includes(item.name)"
            :key="item.name"
            :name="item.name"
            :width="item.width">
        </template>
        <col
          key="setting"
          name="setting"
          width="40">
      </colgroup>
      <thead>
        <tr>
          <template v-for="item in columns">
            <th
              v-if="showColumns.includes(item.name)"
              :key="item.name"
              :class="{
                sort: item.orderField,
              }"
              @click="handleSort(item)">
              <span>{{ item.label }}</span>
              <span
                v-if="item.name === 'ipv4'"
                v-bk-tooltips="$t('history.复制 IP')"
                class="copy-ip-btn"
                @click="handleCopyIP">
                <icon type="step-copy" />
              </span>
              <span
                v-if="item.name === 'ipv6'"
                v-bk-tooltips="$t('history.复制 IPv6')"
                class="copy-ip-btn"
                @click="handleCopyIPv6">
                <icon type="step-copy" />
              </span>
              <span
                v-if="item.orderField"
                class="sort-box"
                :data-order="item.order">
                <span
                  class="top"
                  :class="{ active: item.order === 1 }" />
                <span
                  class="bottom"
                  :class="{ active: item.order === 0 }" />
              </span>
            </th>
          </template>
          <th class="right-fixed-column">
            <div
              id="stepDetailIpListSettingBtn"
              class="list-action"
              @click="handleShowSetting">
              <i class="bk-icon icon-cog-shape" />
            </div>
          </th>
        </tr>
      </thead>
    </table>
  </div>
</template>
<script>
  export default {
    name: '',
    props: {
      columns: {
        type: Array,
        required: true,
      },
      showColumns: {
        type: Array,
        required: true,
      },
    },
    methods: {
      /**
       * @desc 列排序
       * @param { Boolean } payload
       * @returns { undefined }
       */
      handleSort(payload) {
        if (payload.label === 'IP') {
          return;
        }
        this.$emit('on-sort', payload);
      },
      /**
       * @desc 复制IP
       */
      handleCopyIP() {
        this.$emit('on-copy', 'ip');
      },
      /**
       * @desc 复制IPv6
       */
      handleCopyIPv6() {
        this.$emit('on-copy', 'ipv6');
      },
      /**
       * @desc 显示表格配置
       */
      handleShowSetting() {
        this.$emit('on-show-setting');
      },
    },
  };
</script>
<style lang='postcss' scoped>
  .ip-list-head {
    user-select: none;

    .copy-ip-btn {
      font-size: 12px;
      font-weight: normal;
      cursor: pointer;

      &:hover {
        color: #3a84ff;
      }
    }

    th {
      .list-action {
        width: 40px;
        height: 40px;
        padding: 0;
        font-size: 14px;
        color: #979ba5;
        text-align: center;
        cursor: pointer;
        border-left: 1px solid #dcdee5;
      }
    }

    .right-fixed-column {
      position: sticky;
      top: 0;
      right: 0;
      padding: 0;
      background: #fff;
    }
  }
</style>
