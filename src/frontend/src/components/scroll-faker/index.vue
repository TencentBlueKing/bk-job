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
    ref="scrollBox"
    class="scroll-faker"
    :class="{
      [`theme-${theme}`]: theme,
    }"
    :style="boxStyles"
    @mouseenter="calcScroller">
    <div
      ref="scrollContent"
      class="scroll-faker-content"
      @mouseenter="handleContentMouseenter"
      @mouseleave="handleContentMouseleave"
      @scroll="handleContentScroll">
      <slot />
    </div>
    <div
      v-if="isRenderVerticalScroll"
      ref="verticalScroll"
      class="scrollbar-vertical"
      @mouseenter="handleVerticalMouseenter"
      @mouseleave="handleVerticalMouseleave"
      @scroll="handleVerticalScroll">
      <div
        class="scrollbar-inner"
        :style="{ height: `${contentScrollHeight}px` }">
&nbsp;
      </div>
    </div>
    <div
      v-if="isRenderHorizontalScrollbar"
      ref="horizontalScrollbar"
      class="scrollbar-horizontal"
      @mouseenter="handleHorizontalMouseenter"
      @mouseleave="handleHorizontalMouseleave"
      @scroll="handleHorizontalScroll">
      <div
        class="scrollbar-inner"
        :style="{ width: `${contentScrollWidth}px` }">
&nbsp;
      </div>
    </div>
  </div>
</template>
<script>
  import _ from 'lodash';

  export default {
    name: 'ScrollFaker',
    props: {
      theme: {
        type: String,
        default: '',
      },
    },
    data() {
      return {
        isRenderVerticalScroll: false,
        isRenderHorizontalScrollbar: false,
        boxStyles: {},
        contentScrollHeight: 1,
        contentScrollWidth: 0,
        contentScrollTop: 0,
      };
    },
    created() {
      this.calcScroller = _.throttle(this.eventCalcScroller, 100);
      this.handleContentScroll = _.throttle(this.eventContentScroll, 30);
      this.handleVerticalScroll = _.throttle(this.eventVerticalScroll, 30);
      this.handleHorizontalScroll = _.throttle(this.eventHorizontalScroll, 30);
    },
    mounted() {
      window.addEventListener('resize', this.calcScroller);
      const observer = new MutationObserver(() => {
        this.calcScroller();
      });
      observer.observe(this.$refs.scrollContent, {
        subtree: true,
        childList: true,
        attributeName: true,
      });
      this.$once('hook:beforeDestroy', () => {
        observer.takeRecords();
        observer.disconnect();
        window.removeEventListener('resize', this.calcScroller);
      });
      // setTimeout 保证手动赋值的 style 能生效，然后再动态计算 boxStyles
      setTimeout(() => {
        this.calcScroller();
      });
    },
    methods: {
      /**
       * @desc 初始化，计算内容的滚动高度和宽度
       */
      eventCalcScroller() {
        if (this.$refs.scrollBox && this.$refs.scrollContent) {
          const {
            scrollHeight,
            scrollWidth,
          } = this.$refs.scrollContent;
          this.contentScrollHeight = scrollHeight;
          this.contentScrollWidth = scrollWidth;

          const {
            width: boxWidth,
            height: boxHeight,
          } = this.$refs.scrollBox.getBoundingClientRect();
          // 内容区高度大于容器高度显示垂直滚动条
          this.isRenderVerticalScroll = Math.ceil(this.contentScrollHeight) > Math.ceil(boxHeight);
          // 内容区宽度大于容器宽度显示水平滚动条
          this.isRenderHorizontalScrollbar = Math.ceil(this.contentScrollWidth) > Math.ceil(boxWidth);
          const boxStyles = {
            width: '100%',
            height: '100%',
          };
          // 计算滚动容器的展示宽高
          const {
            height: scrollBoxStyleHeight,
            maxHeight: scrollBoxStyleMaxHeight,
            width: scrollBoxStyleWidth,
            maxWidth: scrollBoxStyleMaxWidth,
          } = this.$refs.scrollBox.style;
          if (this.isRenderVerticalScroll) {
            if (scrollBoxStyleHeight) {
              boxStyles.height = scrollBoxStyleHeight;
            } else if (scrollBoxStyleMaxHeight) {
              boxStyles.maxHeight = scrollBoxStyleMaxHeight;
            }
          }
          if (this.isRenderHorizontalScrollbar) {
            if (scrollBoxStyleWidth) {
              boxStyles.width = scrollBoxStyleWidth;
            } else if (scrollBoxStyleMaxWidth) {
              boxStyles.maxWidth = scrollBoxStyleMaxWidth;
            }
          }

          this.boxStyles = Object.freeze(boxStyles);
        }
      },
      /**
       * @desc 外部调用，获取容器滚动位置
       */
      getScroll() {
        const {
          scrollLeft,
          scrollTop,
        } = this.$refs.scrollContent;
        return {
          scrollLeft,
          scrollTop,
        };
      },
      /**
       * @desc 外部调用，容器滚到指定位置
       * @param {Number} scrollLeft
       * @param {Number} scrollTop
       */
      scrollTo(scrollLeft, scrollTop) {
        this.$refs.scrollContent.scrollTo(scrollLeft, scrollTop);
      },
      /**
       * @desc 内容区跟随滚动
       * @param {Number} scrollLeft
       * @param {Number} scrollTop
       */
      contentScrollTo(scrollLeft, scrollTop) {
        if (!this.$refs.scrollContent) {
          return;
        }

        if (this.isHorizontalScroll && typeof scrollLeft !== 'undefined') {
          this.$refs.scrollContent.scrollLeft = scrollLeft;
        }
        if (this.isVerticalScroll && typeof scrollTop !== 'undefined') {
          this.$refs.scrollContent.scrollTop = scrollTop;
        }
      },
      /**
       * @desc 垂直滚动条跟随滚动
       * @param {Number} scrollLeft
       */
      verticalScrollTop(scrollTop) {
        if (this.isContentScroll && this.$refs.verticalScroll) {
          this.$refs.verticalScroll.scrollTo(0, scrollTop);
        }
      },
      /**
       * @desc 水平滚动条跟随滚动
       * @param {Number} scrollLeft
       */
      horizontalScrollLeft(scrollLeft) {
        if (this.isContentScroll && this.$refs.horizontalScrollbar) {
          this.$refs.horizontalScrollbar.scrollLeft = scrollLeft;
        }
      },
      /**
       * @desc 鼠标在内容区状态
       */
      handleContentMouseenter() {
        this.isContentScroll = true;
      },
      /**
       * @desc 鼠标离开内容区状态
       */
      handleContentMouseleave() {
        this.isContentScroll = false;
      },
      /**
       * @desc 内容区滚动
       * @param {Object} event 鼠标滚动事件
       */
      eventContentScroll(event) {
        const {
          scrollTop,
          scrollLeft,
        } = event.target;
        this.verticalScrollTop(scrollTop);
        this.horizontalScrollLeft(scrollLeft);
        this.$emit('on-scroll', event);
      },
      /**
       * @desc 鼠标在垂直滚动条区域
       */
      handleVerticalMouseenter() {
        this.isVerticalScroll = true;
      },
      /**
       * @desc 鼠标离开垂直滚动条区域
       */
      handleVerticalMouseleave() {
        this.isVerticalScroll = false;
      },
      /**
       * @desc 触发垂直滚动条滚动
       * @param {Object} event 鼠标滚动事件
       */
      eventVerticalScroll(event) {
        this.contentScrollTo('', event.target.scrollTop);
      },
      /**
       * @desc 鼠标在水平滚动条区域
       */
      handleHorizontalMouseenter() {
        this.isHorizontalScroll = true;
      },
      /**
       * @desc 鼠标离开水平滚动条区域
       */
      handleHorizontalMouseleave() {
        this.isHorizontalScroll = false;
      },
      /**
       * @desc 触发水平滚动条滚动
       * @param {Object} event 鼠标滚动事件
       */
      eventHorizontalScroll(event) {
        this.contentScrollTo(event.target.scrollLeft);
      },
    },
  };
</script>
<style lang='postcss'>
  .scroll-faker {
    position: relative;
    height: 100%;

    &:hover {
      & > .scrollbar-vertical,
      & > .scrollbar-horizontal {
        opacity: 100%;
      }
    }

    &.theme-dark {
      & > .scrollbar-vertical,
      & > .scrollbar-horizontal {
        &::-webkit-scrollbar-thumb {
          background-color: rgb(255 255 255 / 20%);
        }

        &:hover {
          &::-webkit-scrollbar-thumb {
            background-color: rgb(255 255 255 / 28%);
          }
        }
      }
    }

    & > .scroll-faker-content {
      height: 100%;
      overflow-x: scroll;
      overflow-y: scroll;

      &::-webkit-scrollbar {
        width: 0;
        height: 0;
      }
    }

    & > .scrollbar-vertical,
    & > .scrollbar-horizontal {
      cursor: pointer;
      opacity: 0%;
      transition: 0.15s;

      &::-webkit-scrollbar-thumb {
        background-color: rgb(151 155 165 / 80%);
        border-radius: 3px;
      }

      &:hover {
        z-index: 10000000;
        opacity: 100%;

        &::-webkit-scrollbar-thumb {
          background-color: rgb(151 155 165 / 90%);
          border-radius: 7px;
        }
      }
    }

    & > .scrollbar-vertical {
      position: absolute;
      top: 0;
      right: 0;
      bottom: 0;
      width: 14px;
      overflow-x: hidden;
      overflow-y: scroll;

      &::-webkit-scrollbar {
        width: 6px;
      }

      &:hover {
        &::-webkit-scrollbar {
          width: 14px !important;
        }
      }

      .scrollbar-inner {
        width: 100%;
      }
    }

    & > .scrollbar-horizontal {
      position: absolute;
      right: 0;
      bottom: 0;
      left: 0;
      height: 14px;
      overflow-x: scroll;
      overflow-y: hidden;

      &::-webkit-scrollbar {
        height: 6px;
      }

      &:hover {
        &::-webkit-scrollbar {
          height: 14px !important;
        }
      }

      .scrollbar-inner {
        height: 100%;
      }
    }
  }
</style>
