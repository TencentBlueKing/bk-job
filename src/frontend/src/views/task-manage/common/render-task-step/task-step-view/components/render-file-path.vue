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
  <div class="render-file-path-box">
    <bk-popover placement="left">
      <div
        v-for="(item, fileIndex) in renderData"
        :key="fileIndex"
        class="path-text-row">
        {{ item }}
      </div>
      <div v-if="hasMore">
        ...
      </div>
      <ul
        slot="content"
        class="source-file-tips-box">
        <li
          v-for="(item, fileIndex) in data"
          :key="fileIndex"
          class="row">
          <span class="dot" />
          {{ item }}
        </li>
      </ul>
    </bk-popover>
  </div>
</template>
<script>
  const DISPLAY_ROW_NUMS = 3;

  export default {
    name: '',
    props: {
      data: {
        type: Array,
        default: () => [],
      },
    },
    data() {
      return {};
    },
    computed: {
      renderData() {
        return this.data.slice(0, DISPLAY_ROW_NUMS);
      },
      hasMore() {
        return this.data.length > DISPLAY_ROW_NUMS;
      },
    },
  };
</script>
<style lang="postcss">
  @import url("@/css/mixins/scroll");

  .render-file-path-box {
    padding: 6px 10px;
    margin-left: -10px;

    &:hover {
      background: #f0f1f5;
    }
  }

  .source-file-tips-box {
    max-width: 300px;
    max-height: 280px;
    min-width: 60px;
    overflow-y: auto;

    @mixin scroller;

    .row {
      word-break: break-all;
    }

    .dot {
      display: inline-block;
      width: 6px;
      height: 6px;
      background: currentcolor;
      border-radius: 50%;
    }
  }
</style>
