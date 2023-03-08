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
    ref="tips"
    class="sync-plan-side-anchor-tips"
    :style="styles">
    <div
      ref="content"
      class="wraper">
      {{ data.name }}
    </div>
  </div>
</template>
<script>
  import {
    getScrollParent,
  } from '@utils/assist';

  export default {
    props: {
      data: {
        type: Object,
        required: true,
      },
    },
    data() {
      return {
        position: 'left',
        top: 0,
        left: 0,
      };
    },
    computed: {
      styles() {
        return {
          position: 'absolute',
          top: `${this.top}px`,
          left: `${this.left}px`,
          'z-index': window.__bk_zIndex_manager.nextZIndex(), // eslint-disable-line no-underscore-dangle
        };
      },
    },
    mounted() {
      this.$target = document.querySelector('.sync-plan-side-anchor').querySelector(`[data-anchor="${this.data.target}"]`);
      const scrollParent = getScrollParent(this.$target);
      if (scrollParent) {
        scrollParent.addEventListener('scroll', this.calcPosition);
        this.$once('hook:beforeDestroy', () => {
          scrollParent.removeEventListener('scroll', this.calcPosition);
        });
      }

      this.init();
    },
    beforeDestroy() {
      try {
        if (this.$refs.detail) {
          document.body.removeChild(this.$refs.detail);
        }
      } catch (error) {
        console.log(error);
      }
    },
    methods: {
      init() {
        this.$nextTick(() => {
          this.calcPosition();
        });
        document.body.appendChild(this.$refs.tips);
      },
      calcPosition() {
        const tipsHeight = this.$refs.tips.getBoundingClientRect().height;
        const { top, left, height } = this.$target.getBoundingClientRect();
        this.top = top - (tipsHeight - height) / 2;
        this.left = left;
      },
    },
  };
</script>
<style lang='postcss'>
  .sync-plan-side-anchor-tips {
    position: relative;
    margin-left: -10px;
    color: #fff;
    border-radius: 4px;
    transform: translateX(-100%);

    &.arrow-position-right {
      &::before {
        right: 128px;
        left: unset;
      }
    }

    &::after {
      position: absolute;
      top: 50%;
      right: -5px;
      width: 11px;
      height: 11px;
      background: #333;
      content: "";
      transform: translateY(-50%) rotateZ(45deg);
      box-shadow: 0 0 5px 0 rgb(0 0 0 / 9%);
    }

    .wraper {
      position: relative;
      padding: 7px 14px;
      font-size: 12px;
      white-space: nowrap;
      background: #333;
      border-radius: 4px;
    }
  }
</style>
