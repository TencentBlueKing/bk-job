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
  <div class="smart-style-item">
    <div
      class="smart-arrow"
      :style="arrowStyles" />
    <div class="smart-item-content">
      <slot />
    </div>
  </div>
</template>

<script>
  import _ from 'lodash';

  export default {
    props: {
      sourceTypeList: {
        type: Array,
        default: () => [],
      },
      fileSourceType: {
        type: String,
        default: '',
      },
    },
    computed: {
      arrowStyles() {
        const index = _.findIndex(this.sourceTypeList, _ => _.code === this.fileSourceType);
        if (index < 0) {
          return {
            display: 'none',
          };
        }
        return {
          left: `${30 + (index + 1) * 120}px`,
        };
      },
    },
  };
</script>

<style lang="postcss">
  .smart-style-item {
    .smart-arrow {
      position: relative;
      width: 0;
      height: 0;
      border: 8px solid transparent;
      border-bottom-color: #f5f6fa;
    }

    .smart-item-content {
      padding: 15px 30px;
      margin: 0 -30px 10px;
      background-color: #f5f6fa;
    }
  }
</style>
