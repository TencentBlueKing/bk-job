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
  <div style="width: 546px; padding-top: 27px; margin: 0 auto;">
    <content-loader
      :height="392"
      :width="546"
      v-bind="$attrs">
      <rect
        height="327"
        width="2"
        x="20"
        y="38" />
      <rect
        height="42"
        rx="21"
        width="546"
        x="0"
        y="70" />
      <rect
        height="42"
        rx="21"
        width="546"
        x="0"
        y="140" />
      <rect
        height="42"
        rx="21"
        width="546"
        x="0"
        y="210" />
      <rect
        height="42"
        rx="21"
        width="546"
        x="0"
        y="280" />
      <circle
        cx="21"
        cy="21"
        r="21" />
      <circle
        cx="21"
        cy="371"
        r="21" />
    </content-loader>
  </div>
</template>
<script>
  import { ContentLoader } from 'vue-content-loader';
  export default {
    components: {
      ContentLoader,
    },
    props: {
      maxWidth: {
        type: Number,
        required: true,
      },
    },
    computed: {
      xPosition1() {
        return this.maxWidth - 100;
      },
      xPosition2() {
        return this.maxWidth - 590;
      },
      blockWidth() {
        return Math.abs(this.maxWidth - 279);
      },
    },
  };
</script>
