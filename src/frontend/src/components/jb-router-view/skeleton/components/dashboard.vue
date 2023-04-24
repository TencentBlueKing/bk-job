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
  <div style="padding: 80px 22px 0;">
    <content-loader
      :height="400"
      :width="maxWidth"
      v-bind="$attrs">
      <rect
        fill="#D8D8D8"
        height="18"
        rx="2"
        width="49"
        x="0"
        y="0" />
      <!--  -->
      <rect
        fill="#D8D8D8"
        height="18"
        rx="2"
        width="80"
        x="19"
        y="54" />
      <rect
        fill="#D8D8D8"
        height="12"
        rx="2"
        width="80"
        x="19"
        y="133" />
      <rect
        fill="#D8D8D8"
        height="12"
        rx="2"
        width="80"
        x="19"
        y="159" />
      <rect
        fill="#D8D8D8"
        height="24"
        rx="2"
        width="54"
        x="19"
        y="91" />
      <rect
        fill="#D8D8D8"
        height="60"
        rx="2"
        width="120"
        x="120"
        y="111" />
      <!--  -->
      <rect
        fill="#D8D8D8"
        height="18"
        rx="2"
        width="80"
        x="280"
        y="54" />
      <rect
        fill="#D8D8D8"
        height="12"
        rx="2"
        width="80"
        x="280"
        y="133" />
      <rect
        fill="#D8D8D8"
        height="12"
        rx="2"
        width="80"
        x="280"
        y="159" />
      <rect
        fill="#D8D8D8"
        height="24"
        rx="2"
        width="54"
        x="280"
        y="91" />
      <rect
        fill="#D8D8D8"
        height="60"
        rx="2"
        width="120"
        x="380"
        y="111" />
      <!--  -->
      <rect
        fill="#D8D8D8"
        height="18"
        rx="2"
        width="80"
        x="559"
        y="54" />
      <rect
        fill="#D8D8D8"
        height="24"
        rx="2"
        width="102"
        x="559"
        y="91" />
      <rect
        fill="#D8D8D8"
        height="38"
        rx="2"
        width="40"
        x="559"
        y="133" />
      <!--  -->
      <rect
        fill="#D8D8D8"
        height="18"
        rx="2"
        width="80"
        :x="559 + leftBlockOffset"
        y="54" />
      <rect
        fill="#D8D8D8"
        height="24"
        rx="2"
        width="102"
        :x="559 + leftBlockOffset"
        y="91" />
      <rect
        fill="#D8D8D8"
        height="38"
        rx="2"
        width="40"
        :x="559 + leftBlockOffset"
        y="133" />
      <!--  -->
      <rect
        fill="#D8D8D8"
        height="18"
        rx="2"
        width="80"
        :x="559 + leftBlockOffset * 2"
        y="54" />
      <rect
        fill="#D8D8D8"
        height="24"
        rx="2"
        width="102"
        :x="559 + leftBlockOffset * 2"
        y="91" />
      <rect
        fill="#D8D8D8"
        height="38"
        rx="2"
        width="40"
        :x="559 + leftBlockOffset * 2"
        y="133" />
      <!--  -->
      <rect
        fill="#D8D8D8"
        height="18"
        rx="2"
        width="80"
        :x="559 + leftBlockOffset * 3"
        y="54" />
      <rect
        fill="#D8D8D8"
        height="24"
        rx="2"
        width="102"
        :x="559 + leftBlockOffset * 3"
        y="91" />
      <rect
        fill="#D8D8D8"
        height="38"
        rx="2"
        width="40"
        :x="559 + leftBlockOffset * 3"
        y="133" />
    </content-loader>
  </div>
</template>
<script>
  import {
    ContentLoader,
  } from 'vue-content-loader';
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
      leftBlockOffset() {
        return (this.maxWidth - 40 - 559) / 4;
      },
    },
  };
</script>
