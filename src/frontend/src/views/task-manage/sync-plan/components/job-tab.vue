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
  <div class="job-tab">
    asdasd
    <icon
      class="job-check"
      type="check" />
    <bk-switcher
      class="job-switch"
      size="min"
      theme="primary" />
  </div>
</template>
<script>
  export default {
    name: '',

    data() {
      return {};
    },

    methods: {},
  };
</script>
<style lang='postcss' scoped>
  .job-tab {
    display: flex;
    height: 40px;
    padding: 0 13px 0 24px;
    align-items: center;
    font-size: 14px;
    color: #63656e;
    border-right: 1px solid #dcdee5;
    border-bottom: 1px solid #dcdee5;

    .job-check {
      margin-left: auto;
      font-size: 24px;
      color: #2dcb56;
    }

    .job-switch {
      margin-left: 20px;
    }
  }
</style>
