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
    id="app"
    :class="{
      'viewport-full': isViewportFull,
      fixed: sideFixed,
    }">
    <div class="jb-navigation">
      <div class="jb-navigation-header">
        <div
          class="header-left"
          :style="headerLeftStyles">
          <slot name="header" />
        </div>
        <div class="header-center">
          <slot name="headerCenter" />
        </div>
        <div class="header-right">
          <slot name="headerRight" />
        </div>
      </div>
      <div
        class="jb-navigation-side"
        :style="sideStyles"
        @mouseenter="handleSideMouseenter"
        @mouseleave="handleSideMouseleave">
        <slot name="sideAppendBefore" />
        <div class="side-wrapper">
          <scroll-faker theme="dark">
            <slot name="side" />
          </scroll-faker>
        </div>
        <div class="side-footer">
          <icon
            class="fixed-flag"
            type="expand-line"
            @click="handleSideFixedToggle" />
        </div>
      </div>
      <div
        class="jb-navigation-main"
        :style="mainStyles">
        <div
          class="jb-navigation-body-header"
          :style="bodyHeaderStyles">
          <slot name="contentHeader" />
        </div>
        <scroll-faker
          ref="contentScroll"
          :style="scrollStyles">
          <div
            class="jb-navigation-content"
            :style="contentStyles">
            <div class="navigation-content-wrapper">
              <slot />
            </div>
          </div>
        </scroll-faker>
      </div>
    </div>
  </div>
</template>
<script>
  import _ from 'lodash';

  const PAGE_MIN_WIDTH = 1366;
  const PAGE_MIDDLE_WIDTH = 1920;
  const SIDE_LEFT_EXPAND_SMALL_WIDTH = 220;
  const SIDE_LEFT_EXPAND_BIG_WIDTH = 280;
  const SIDE_LEFT_INEXPAND_WIDTH = 60;

  export default {
    name: 'JobSiteFrame',
    props: {
      sideFixed: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        isViewportFull: false,
        isSideHover: false,
        pageWidth: PAGE_MIN_WIDTH,
        sideLeftExpandWidth: 0,
      };
    },
    computed: {
      realSideWidth() {
        return this.sideFixed ? this.sideLeftExpandWidth : SIDE_LEFT_INEXPAND_WIDTH;
      },
      headerLeftStyles() {
        return {
          width: `${this.sideLeftExpandWidth}px`,
        };
      },
      sideStyles() {
        if (this.isSideHover) {
          return {
            width: `${this.sideLeftExpandWidth}px`,
            zIndex: 2000,
          };
        }
        return {
          width: `${this.realSideWidth}px`,
        };
      },
      mainStyles() {
        return {
          marginLeft: `${this.realSideWidth}px`,
          zIndex: 1999,
        };
      },
      bodyHeaderStyles() {
        return {
          left: `${this.realSideWidth}px`,
        };
      },
      scrollStyles() {
        const navigationHeaderHeight = 52;
        const contentHeaderHeight = 52;
        return {
          width: `calc(100vw - ${this.realSideWidth}px)`,
          height: `calc(100vh - ${navigationHeaderHeight + contentHeaderHeight}px)`,
        };
      },
      contentStyles() {
        return {
          width: `${this.pageWidth - this.realSideWidth}px`,
        };
      },
    },
    watch: {
      /**
       * @desc 页面标题
       */
      $route: {
        handler(route) {
          this.isViewportFull = Boolean(route.meta.full);
          setTimeout(() => {
            this.$refs.contentScroll.scrollTo(0, 0);
          });
        },
        immediate: true,
      },
    },
    mounted() {
      this.init();
      const resizeHandler = _.throttle(this.init, 100);
      window.addEventListener('resize', resizeHandler);
      this.$once('hook:beforeDestroy', () => {
        window.removeEventListener('resize', resizeHandler);
      });
    },
    methods: {
      /**
       * @desc 初始化动态计算布局尺寸
       */
      init() {
        const windowInnerWidth = window.innerWidth;
        this.pageWidth = windowInnerWidth < PAGE_MIN_WIDTH ? PAGE_MIN_WIDTH : windowInnerWidth;
        this.sideLeftExpandWidth = windowInnerWidth < PAGE_MIDDLE_WIDTH ? SIDE_LEFT_EXPAND_SMALL_WIDTH : SIDE_LEFT_EXPAND_BIG_WIDTH;
      },
      /**
       * @desc 鼠标移入
       */
      handleSideMouseenter() {
        clearTimeout(this.hoverTimer);
        this.hoverTimer = setTimeout(() => {
          this.isSideHover = true;
          this.$emit('on-side-expand', this.isSideHover);
        }, 50);
      },
      /**
       * @desc 鼠标移出
       */
      handleSideMouseleave() {
        clearTimeout(this.hoverTimer);
        this.hoverTimer = setTimeout(() => {
          this.isSideHover = false;
          this.$emit('on-side-expand', this.isSideHover);
        }, 50);
      },
      /**
       * @desc 切换左侧面板是否固定的状态
       */
      handleSideFixedToggle() {
        this.$emit('on-side-fixed');
      },
    },
  };
</script>
<style lang="postcss">
  #app {
    &.viewport-full {
      .navigation-content-wrapper {
        padding: 0 !important;
      }
    }

    &.fixed {
      .jb-navigation {
        .fixed-flag {
          transform: rotateZ(180deg) !important;
        }

        .jb-navigation-side {
          transition: none;
        }

        .jb-navigation-main {
          margin-left: 220px;
        }
      }
    }
  }

  .jb-navigation {
    line-height: 19px;

    .jb-navigation-header {
      position: fixed;
      top: 0;
      right: 0;
      left: 0;
      z-index: 2000;
      display: flex;
      align-items: center;
      height: 52px;
      font-size: 14px;
      color: #96a2b9;
      background: #182233;

      .header-left {
        display: flex;
        flex: 0 0 auto;
        align-items: center;
        padding-left: 16px;
      }

      .header-right {
        display: flex;
        align-items: center;
        padding-right: 16px;
        margin-left: auto;
      }
    }

    .jb-navigation-side {
      position: fixed;
      top: 52px;
      bottom: 0;
      left: 0;
      z-index: 2000;
      display: flex;
      width: 220px;
      padding-top: 10px;
      font-size: 14px;
      background: #131824;
      transition: all 0.3s;
      flex-direction: column;

      .side-wrapper {
        width: 100%;
        overflow: hidden;
        flex: 1 1 auto;
      }

      .side-footer {
        display: flex;
        height: 56px;
        margin-top: auto;
        margin-left: 16px;
        font-size: 16px;
        color: #747e94;
        align-items: center;

        .fixed-flag {
          padding: 8px;
          cursor: pointer;
          border-radius: 50%;
          transform: rotateZ(0deg);
          transition: all 0.15s;

          &:hover {
            color: #d3d9e4;
            background: linear-gradient(270deg, #253047, #263247);
          }
        }
      }
    }

    .jb-navigation-main {
      margin-top: 104px;
    }

    .jb-navigation-body-header {
      position: fixed;
      top: 52px;
      right: 0;
      left: 220px;
      z-index: 1999;
      display: flex;
      align-items: center;
      height: 52px;
      padding-right: 24px;
      padding-left: 24px;
      font-size: 16px;
      color: #313238;
      background: #fff;
      box-shadow: 0 2px 4px 0 rgb(0 0 0 / 10%);
    }

    .jb-navigation-content {
      background: #f5f6fa;

      .navigation-content-wrapper {
        position: relative;
        padding: 20px 24px 0;
      }
    }
  }
</style>
