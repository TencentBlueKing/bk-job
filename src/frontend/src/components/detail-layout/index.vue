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
  <div
    ref="detailLayout"
    class="jb-detail-layout"
    :class="[mode, layout]">
    <slot />
  </div>
</template>
<script>
  export default {
    name: 'JbDetailLayout',
    props: {
      mode: {
        type: String,
        default: 'normal', // normal, see
      },
      layout: {
        type: String,
        default: 'horizontal', // horizontal, vertical
      },
      isWarp: {
        type: Boolean,
      },
    },
    created() {
      this.childrenNum = this.$slots.default;
    },
    updated() {
      const childrenNum = this.$slots.default;
      if (this.childrenNum !== childrenNum) {
        this.init();
        this.childrenNum = childrenNum;
      }
    },
    mounted() {
      const isShowLayout = this.$refs.detailLayout.getBoundingClientRect().width > 0;
      if (isShowLayout) {
        this.init();
      }
    },
    methods: {
      init() {
        if (this.layout === 'vertical') {
          return;
        }
        const $layoutEle = this.$refs.detailLayout;
        const $layoutDetailList = $layoutEle.querySelectorAll('.detail-label');

        let max = 0;
        $layoutDetailList.forEach((item) => {
          const { width } = item.getBoundingClientRect();
          max = Math.max(max, width);
        });
        $layoutDetailList.forEach((item) => {
          item.style.width = `${max}px`;
        });
      },
    },
  };
</script>
<style lang='postcss'>
  .jb-detail-layout {
    &.see {
      .detail-label {
        color: #b2b5bd;
      }
    }

    &.vertical {
      .detail-label {
        justify-content: flex-start;
      }

      .detail-item {
        flex-direction: column;
        align-items: stretch;
      }
    }
  }
</style>
