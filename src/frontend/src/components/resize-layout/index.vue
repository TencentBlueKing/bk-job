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
    ref="root"
    class="jb-resize-layout">
    <div
      ref="left"
      class="jb-resize-layout-left"
      :style="leftStyles">
      <scroll-faker>
        <slot />
      </scroll-faker>
    </div>
    <div
      class="jb-resize-layout-right"
      :style="rightStyles">
      <div
        ref="handle"
        class="right-content-placeholder"
        :style="passiveStyles">
        <scroll-faker>
          <div ref="rightContent">
            <slot name="right" />
          </div>
        </scroll-faker>
        <div
          v-if="renderWidth > 5"
          class="jb-resize-layout-line"
          :style="lineStyles"
          @mousedown="handleMousedown">
          <icon type="many-dot" />
        </div>
      </div>
    </div>
  </div>
</template>
<script>
  import _ from 'lodash';

  export default {
    name: '',
    props: {
      rightWidth: {
        type: Number,
        required: true,
      },
      minWidth: {
        type: Number,
      },
      maxWidth: {
        type: Number,
      },
      rightFixed: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        renderWidth: this.rightWidth,
      };
    },
    computed: {
      leftStyles() {
        if (this.rightFixed) {
          if (this.renderWidth > this.rightWidth) {
            return {
              width: `calc(100% - ${this.rightWidth}px)`,
            };
          }
        }
        return {
          width: `calc(100% - ${this.renderWidth}px)`,
        };
      },
      rightStyles() {
        if (this.rightFixed) {
          if (this.renderWidth > this.rightWidth) {
            return {
              width: `${this.rightWidth}px`,
            };
          }
        }
        return {
          width: `${this.renderWidth}px`,
        };
      },
      passiveStyles() {
        return {
          position: 'absolute',
          top: 0,
          right: 0,
          bottom: 0,
          zIndex: 9,
          width: `${this.renderWidth}px`,
          padding: 'inherit',
          background: 'inherit',
        };
      },
      lineStyles() {
        return {
          right: `${this.renderWidth - 6}px`,
        };
      },
    },
    created() {
      this.moveStartWidth = this.width;
      this.isResizeable = false;
      this.handleMousemove = _.throttle(this.eventMousemove, 30);
    },
    mounted() {
      this.calcRightShowStatus();

      document.body.addEventListener('mousemove', this.handleMousemove);
      document.body.addEventListener('mouseup', this.handleMouseup);
      this.$once('hook:beforeDestroy', () => {
        document.body.removeEventListener('mousemove', this.handleMousemove);
        document.body.removeEventListener('mouseup', this.handleMouseup);
      });

      const observer = new MutationObserver(() => {
        this.calcRightShowStatus();
      });
      observer.observe(this.$refs.rightContent, {
        subtree: true,
        childList: true,
        attributeName: true,
      });
      this.$once('hook:beforeDestroy', () => {
        observer.takeRecords();
        observer.disconnect();
      });
    },
    methods: {
      calcRightShowStatus: _.throttle(function () {
        if (!this.$refs.rightContent) {
          return;
        }
        const { renderWidth } = this;
        this.renderWidth = 0;
        Array.from(this.$refs.rightContent.childNodes).forEach(($el) => {
          if ($el.getBoundingClientRect().height > 10) {
            if (renderWidth > 0) {
              this.renderWidth = renderWidth;
            } else {
              this.renderWidth = this.rightWidth;
            }
          }
        });
      }, 30),
      /**
       * @desc mousedown 事件，记录鼠标按下时容器的宽度
       * @param {Object} event
       */
      handleMousedown(event) {
        this.isResizeable = true;
        this.startClientX = event.clientX;
        this.parentWidth = this.$refs.root.getBoundingClientRect().width;
        this.moveStartWidth = this.$refs.handle.getBoundingClientRect().width;
        document.body.style.userSelect = 'none';
        this.$refs.left.style.pointerEvents = 'none';
      },
      /**
       * @desc mouseup 事件，取消可拖动特性
       */
      handleMouseup() {
        this.isResizeable = false;
        document.body.style.userSelect = '';
        this.$refs.left.style.pointerEvents = '';
      },
      /**
       * @desc mousemove 事件，动态更新容器宽度
       * @param {Object} event
       */
      eventMousemove(event) {
        if (!this.isResizeable) {
          return;
        }
        const MAX_WIDTH = this.maxWidth ? this.maxWidth : 0.8 * this.parentWidth;
        const MIN_WIDTH = this.minWidth ? this.minWidth : 0.1 * this.parentWidth;
        const { clientX } = event;
        let newWidth = 0;
        newWidth = this.startClientX - clientX + this.moveStartWidth;

        if (newWidth > MAX_WIDTH || newWidth < MIN_WIDTH) {
          return;
        }
        this.styles = {
          width: `${newWidth}px`,
        };
        this.renderWidth = newWidth;
        this.$emit('on-resize');
      },
    },
  };
</script>
<style lang="postcss">
  .jb-resize-layout {
    position: relative;
    display: flex;
    height: 100%;

    &::after {
      display: table;
      content: "";
    }

    .jb-resize-layout-left {
      float: left;
      height: 100%;
    }

    .jb-resize-layout-right {
      float: left;
      height: 100%;
    }

    .jb-resize-layout-line {
      position: absolute;
      top: 0;
      right: 300px;
      bottom: 0;
      z-index: 9;
      display: flex;
      width: 10px;
      font-size: 23px;
      color: #c4c6cc;
      cursor: ew-resize;
      border: 3px solid transparent;
      border-top: none;
      border-bottom: none;
      align-items: center;

      &:hover {
        &::before {
          background: #3a84ff;
        }
      }

      &::before {
        display: block;
        width: 1px;
        height: 100%;
        background: #dcdee5;
        content: "";
      }
    }
  }
</style>
