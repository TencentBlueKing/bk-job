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
    v-bkloading="{ isLoading: isLoading, opacity: .1 }"
    class="step-execute-variable-view">
    <scroll-faker theme="dark">
      <table>
        <thead>
          <tr>
            <th style="width: 250px;">
              {{ $t('history.变量名称') }}
            </th>
            <th style="width: 90px;">
              {{ $t('history.变量类型') }}
            </th>
            <th>{{ $t('history.变量值') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(item, index) in variableList"
            :key="index">
            <td>{{ item.name }}</td>
            <td>{{ item.typeText }}</td>
            <td class="variable-value">
              {{ item.value }}
            </td>
          </tr>
        </tbody>
      </table>
    </scroll-faker>
  </div>
</template>
<script>
  import TaskExecuteService from '@service/task-execute';

  export default {
    name: '',
    inheritAttrs: false,
    props: {
      name: String,
      stepInstanceId: {
        type: Number,
        required: true,
      },
      ip: {
        type: String,
      },
      host: {
        type: Object,
      },
    },
    data() {
      return {
        isLoading: true,
        variableList: [],
      };
    },
    watch: {
      name: {
        handler() {
          this.isLoading = true;
          this.fetchStepVariables();
        },
        immediate: true,
      },
    },
    methods: {
      // 步骤使用的变量
      fetchStepVariables() {
        if (!this.ip) {
          this.isLoading = false;
          return;
        }
        TaskExecuteService.fetchStepVariables({
          stepInstanceId: this.stepInstanceId,
          hostId: this.host.hostId,
          ip: `${this.host.cloudAreaId}:${this.host.ipv4}`,
        }).then((data) => {
          this.variableList = Object.freeze(data);
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
    },
  };
</script>
<style lang='postcss'>
  .step-execute-variable-view {
    height: 100%;
    padding: 0 20px;
    font-family: Monaco, Menlo, "Ubuntu Mono", Consolas, source-code-pro, monospace;
    color: #c4c6cc;

    table {
      width: 100%;
    }

    th,
    td {
      height: 40px;
      padding-right: 10px;
      padding-left: 10px;
      text-align: left;
      border-bottom: 1px solid #3b3c42;
    }

    th {
      font-weight: normal;
      color: #ccc;
    }

    td {
      color: #979ba5;
      white-space: pre-line;

      &.variable-value {
        word-break: break-word;
      }
    }
  }
</style>
