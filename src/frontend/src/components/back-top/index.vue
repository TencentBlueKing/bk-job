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
    v-if="isShow"
    v-bk-tooltips="$t('回到顶部')"
    class="job-back-top"
    :class="classes"
    :style="styles"
    @click="handleBackTop">
    <icon type="up-to-top" />
  </div>
</template>
<script>
  import _ from 'lodash';

  import {
    scrollTopSmooth,
  } from '@utils/assist';

  export default {
    name: '',
    props: {
      // 可滚动容器元素
      target: {
        type: Function,
        default: () => document.querySelector('.jb-navigation-main').querySelector('.scroll-faker-content'),
      },
      size: {
        type: String,
        default: 'normal',
      },
      fixed: {
        type: Boolean,
        default: true,
      },
    },
    data() {
      return {
        isShow: false,
      };
    },
    computed: {
      classes() {
        return `theme-${this.size}`;
      },
      styles() {
        if (!this.fixed) {
          return {};
        }
        return {
          position: 'fixed',
          right: '34px',
          bottom: '92px',
        };
      },
    },
    mounted() {
      this.smartPosition();
      window.addEventListener('resize', this.smartPosition);
      const observer = new MutationObserver((payload) => {
        this.smartPosition();
      });
      observer.observe(this.target(), {
        subtree: true,
        childList: true,
        attributeName: true,
        characterData: true,
      });
      this.$once('hook:beforeDestroy', () => {
        observer.takeRecords();
        observer.disconnect();
        window.removeEventListener('resize', this.smartPosition);
      });
    },
    methods: {
      /**
       * @desc 计算位置
       */
      smartPosition: _.debounce(function () {
        const $srollContainer = this.target();
        if (!$srollContainer) {
          return;
        }
        const selfHeight = $srollContainer.getBoundingClientRect().height;
        const { scrollHeight } = $srollContainer;
        this.isShow = scrollHeight > selfHeight + 30;
      }, 300),
      /**
       * @desc 点击按钮目标容器滚动到顶部
       */
      handleBackTop() {
        const $srollContainer = this.target();
        scrollTopSmooth($srollContainer, 0);
      },
    },
  };
</script>
<style lang='postcss'>
  .job-back-top {
    display: flex;
    width: 28px;
    height: 28px;
    color: #fff;
    cursor: pointer;
    background: rgb(0 0 0 / 25%);
    border-radius: 50%;
    align-items: center;
    justify-content: center;

    &:hover {
      background: rgb(0 0 0 / 40%);
    }

    &.theme-normal {
      width: 36px;
      height: 36px;
      font-size: 20px;
    }

    &.theme-small {
      width: 28px;
      height: 28px;
      font-size: 16px;
    }
  }
</style>
