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
  <div class="execute-dashboard">
    <div class="layout-left">
      <total-count v-bind="$attrs" />
      <fail-count v-bind="$attrs" />
    </div>
    <div class="layout-right">
      <statistics v-bind="$attrs" />
    </div>
  </div>
</template>
<script>
  import FailCount from './fail-count';
  import Statistics from './statistics';
  import TotalCount from './total-count';

  export default {
    name: '',
    components: {
      TotalCount,
      FailCount,
      Statistics,
    },
    data() {
      return {};
    },

    methods: {},
  };
</script>
<style lang='postcss'>
  @import url("@/css/mixins/media");

  .execute-dashboard {
    display: flex;
    flex: 1;
    background: #fff;

    .layout-left {
      flex: 0 0 260px;
      border-right: 1px solid #f0f1f5;

      .dashboard-card-layout {
        @media (--huge-viewports) {
          width: 400px;
        }

        @media (--large-viewports) {
          width: 300px;
        }

        @media (--medium-viewports) {
          width: 280px;
        }

        @media (--small-viewports) {
          width: 260px;
        }

        &:nth-child(n+2) {
          border-top: 1px solid #f0f1f5;
        }
      }
    }

    .layout-right {
      flex: 1;
    }
  }
</style>
