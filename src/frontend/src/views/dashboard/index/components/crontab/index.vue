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
  <div class="crontab-dashboard">
    <crontab-status v-bind="$attrs" />
    <crontab-type v-bind="$attrs" />
  </div>
</template>
<script>
  import CrontabStatus from './status';
  import CrontabType from './type';

  export default {
    name: '',
    components: {
      CrontabStatus,
      CrontabType,
    },
    data() {
      return {};
    },

    methods: {},
  };
</script>
<style lang='postcss'>
  @import url("@/css/mixins/media");

  .crontab-dashboard {
    display: flex;

    .dashboard-card-layout {
      width: 260px;

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

      &:nth-child(n + 2) {
        border-left: 1px solid #f0f1f5;
      }
    }
  }
</style>
