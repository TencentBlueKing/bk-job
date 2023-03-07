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
  <table class="notify-channel-table">
    <thead>
      <tr>
        <th>
          <div class="split-header">
            <span class="split-item channel">{{ $t('setting.渠道') }}</span>
            <span class="split-item template">{{ $t('setting.模板') }}</span>
          </div>
        </th>
        <th
          v-for="(channel, index) in channleList"
          :key="index"
          class="channel-item"
          :class="[{ 'un-selected': !channelCode.includes(channel.code) }]"
          @click.stop="handleToggleChannel(channel.code)">
          <div class="channel-wraper">
            <img
              alt=""
              class="channel-icon"
              :src="channel.icon">
            <p class="channel-name">
              {{ channel.name }}
            </p>
          </div>
          <bk-checkbox
            :checked="channelCode.includes(channel.code)"
            class="channel-check" />
        </th>
      </tr>
    </thead>
    <tbody>
      <tr
        v-for="(template, index) in templateList"
        :key="index">
        <td>{{ template.name }}</td>
        <td
          v-for="(channl, colIndex) in channleList"
          :key="colIndex"
          :class="{ 'un-selected': !channelCode.includes(channl.code) }">
          <div class="setting-detail">
            <icon
              v-if="getConfigStatus(channl.templateInfoList, template.code)"
              class="setting-flag"
              type="check" />
            <span
              v-else
              class="un-set-up">{{ $t('setting.未设置') }}</span>
            <span
              class="edit-btn"
              @click.stop="handleEditTemplate(channl.code, template.code)">
              <icon
                class="edit-icon"
                type="edit-2" />{{ $t('setting.编辑模板') }}
            </span>
          </div>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<script>
  export default {
    props: {
      channleList: {
        type: Array,
        default: () => [],
      },
      channelCode: {
        type: Array,
        default: () => [],
      },
      handleToggleChannel: {
        type: Function,
      },
      handleEditTemplate: {
        type: Function,
      },
    },
    computed: {
      templateList() {
        return this.channleList.length && (this.channleList[0].templateInfoList || []);
      },
    },
    methods: {
      getConfigStatus(templateList, code) {
        return templateList.some(template => template.code === code && template.isConfiged);
      },
    },
  };
</script>

<style lang='postcss' scoped>
  .notify-channel-table {
    width: 100%;
    border: 1px solid #dcdee5;

    th,
    td {
      height: 42px;
      font-size: 12px;
      text-align: center;
      border-left: 1px solid #dcdee5;
    }

    th {
      position: relative;
      height: 100px;
      font-weight: normal;
      color: #313238;

      &:first-child {
        width: 202px;
      }
    }

    td {
      color: #63656e;
      border-top: 1px solid #dcdee5;
    }

    .split-header {
      height: 100%;
      text-align: left;

      &::after {
        position: absolute;
        top: 0;
        left: -12px;
        width: 224px;
        height: 49px;
        border-bottom: 1px solid #dcdee5;
        content: "";
        transform: rotateZ(26deg);
        transform-origin: bottom center;
      }
    }

    .split-item {
      position: relative;
      display: inline-block;
      width: 100%;
      text-align: center;

      &.channel {
        top: 18px;
        left: 14%;
      }

      &.template {
        top: 48px;
        right: 14%;
      }
    }

    .channel-item {
      position: relative;
      margin: 0 10px;
      font-size: 40px;
      cursor: pointer;
      border: 1px solid #dcdee5;
      border-radius: 4px;
      transition: border 0.1s;

      &.active {
        border-color: #3a84ff;
      }

      .channel-wraper {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
      }
    }

    .channel-icon {
      height: 40px;
    }

    .channel-check {
      position: absolute;
      top: 8px;
      right: 8px;
    }

    .channel-name {
      margin-top: 8px;
      font-size: 12px;
      color: #63656e;
    }

    .setting-detail {
      display: flex;
      height: 100%;
      min-width: 100px;
      font-size: 12px;
      align-items: center;
      justify-content: center;

      .edit-btn {
        display: none;
        color: #3a84ff;
        cursor: pointer;
      }

      &:hover {
        .edit-btn {
          display: inline-block;
        }

        .setting-flag,
        .un-set-up {
          display: none;
        }
      }
    }

    .setting-flag {
      font-size: 26px;
      color: #979ba5;
      cursor: pointer;
    }

    .un-selected {
      color: #d6d9e0;
      background-color: #fafbfd;

      .channel-name {
        color: #d6d9e0;
      }

      .setting-detail:hover {
        .edit-btn {
          display: none;
        }

        .setting-flag,
        .un-set-up {
          display: inline-block;
        }
      }

      .setting-flag {
        color: #d6d9e0;
      }
    }
  }
</style>
