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
  <div class="internal-variable-wrapper">
    <icon
      class="close-dialog"
      type="close"
      @click.stop="handleClose(false)" />
    <header>{{ $t('setting.内置变量列表') }}</header>
    <bk-tab
      :active="variableType"
      :before-toggle="handleTabChange"
      class="variable-tab"
      type="unborder-card">
      <bk-tab-panel
        :label="$t('setting.通用变量')"
        name="general" />
      <bk-tab-panel
        :label="$t('setting.作业变量')"
        name="job" />
      <bk-tab-panel
        :label="$t('setting.定时任务变量')"
        name="cron" />
    </bk-tab>
    <bk-table
      class="variable-table"
      :data="renderList">
      <bk-table-column
        :label="$t('setting.变量名称')"
        prop="name" />
      <bk-table-column
        :label="$t('setting.含义')"
        prop="meaning" />
      <bk-table-column
        :label="$t('setting.示例')"
        prop="examples"
        :width="320" />
    </bk-table>
  </div>
</template>
<script>
  import {
    InternalVariables,
  } from './variables';

  export default {
    props: {
      handleClose: {
        type: Function,
      },
    },
    data() {
      return {
        variableType: 'general',
      };
    },
    computed: {
      renderList() {
        return InternalVariables[this.variableType];
      },
    },
    methods: {
      handleTabChange(tab) {
        this.variableType = tab;
      },
    },
  };
</script>

<style lang="postcss">
  .internal-variable-wrapper {
    height: 586px;
    padding-top: 16px;

    .close-dialog {
      position: absolute;
      top: 10px;
      right: 6px;
      font-size: 28px;
      color: #979ba5;
      cursor: pointer;
    }

    header {
      font-size: 20px;
      color: #313238;
    }

    .variable-tab {
      margin-top: 10px;

      .bk-tab-section {
        display: none;
      }
    }

    .variable-table {
      margin-top: 20px;
      border: none;
    }

    .bk-table-outer-border::after {
      display: none;
    }
  }
</style>
