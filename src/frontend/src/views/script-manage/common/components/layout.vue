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
    ref="layout"
    class="script-version-manage-layout">
    <div class="layout-header">
      <div class="header-title">
        <slot name="title" />
      </div>
      <div class="sub-header">
        <slot name="sub-header" />
      </div>
    </div>
    <div
      ref="content"
      class="layout-container"
      :style="contentStyles">
      <resizeable-box
        v-if="$slots.left"
        class="left"
        free-position="right">
        <slot name="left" />
      </resizeable-box>
      <div class="right">
        <slot />
      </div>
    </div>
    <div class="layout-footer">
      <slot name="footer" />
    </div>
  </div>
</template>
<script>
  import { getOffset } from '@utils/assist';

  const PAGE_PADDING_BOTTOM = 20;

  export default {
    name: '',
    data() {
      return {
        showContent: !this.loading,
        offsetLeft: 0,
        contentOffsetTop: 0,
        footerHeight: 46,
      };
    },
    computed: {
      contentStyles() {
        const offset = this.contentOffsetTop + PAGE_PADDING_BOTTOM + this.footerHeight;
        return {
          height: `calc(100vh - ${offset}px)`,
        };
      },
    },
    mounted() {
      this.contentOffsetTop = getOffset(this.$refs.content).top;
    },
  };
</script>
<style lang='postcss'>
  .script-version-manage-layout {
    position: relative;
    height: 100vh;
    overflow: hidden;
    background: #292929;

    .layout-header {
      display: flex;
      align-items: center;
      height: 40px;
      padding-left: 16px;
      font-size: 14px;
      color: #c4c6cc;
      background: #333;
      box-shadow: 0 2px 4px 0 rgb(0 0 0 / 16%);

      .sub-header {
        display: flex;
        height: 100%;
        padding-right: 40px;
        margin-left: auto;
        font-size: 16px;
        align-items: center;

        i {
          padding: 10px 9px;
          cursor: pointer;
        }
      }
    }

    .layout-container {
      position: relative;
      z-index: 0;
      display: flex;

      .left {
        position: relative;
        width: 312px;
        padding: 12px 16px;
        background: #292929;

        .line {
          position: absolute;
          top: 0;
          right: 0;
          bottom: 0;
        }
      }

      .right {
        flex: 1;
      }
    }

    .layout-footer {
      position: relative;
      z-index: 2;
      display: flex;
      height: 46px;
      padding: 0 16px;
      background: #242424;
      align-items: center;
      border-top: 1px solid #141414;
      box-shadow: 0 -2px 4px 0 rgb(0 0 0 / 16%);
    }

    .bk-label {
      height: 16px;
      min-height: 16px;
      margin-bottom: 8px;
      font-size: 12px;
      line-height: 16px;
      color: #c4c6cc;

      .bk-label-text {
        line-height: 16px;
      }
    }

    .bk-form-input,
    .bk-form-textarea {
      font-size: 12px;
      color: #c4c6cc !important;
      background: #292929 !important;
      border-color: #63656e !important;

      &[readonly] {
        background: #333 !important;
        border-color: #424242 !important;
      }

      &::placeholder {
        color: #63656e;
      }
    }

    .bk-button {
      &.bk-default {
        color: #979ba5;
        background: transparent;
        border-color: #5c5e66;
        transition: all 0.15s;

        &:hover {
          color: #b1b6c2;
          border-color: #878b94;
        }

        &.is-disabled {
          color: #63656e;
          border-color: #36373d;
        }
      }
    }
    /* stylelint-disable selector-class-pattern */
    .jd-ace-editor {
      .ace_editor {
        background: #1a1a1a;

        .ace_gutter {
          background: #1a1a1a;
        }
      }

      .jb-ace-action {
        display: none;
      }

      .jb-ace-history-panel {
        top: 0;
        right: 42px;
      }
    }

    .bk-loading {
      background: rgb(0 0 0 / 80%) !important;
    }

    span[data-script-status] {
      color: #c4c6cc !important;
      background: #63656e !important;
    }

    span[data-script-status="1"] {
      color: #59b383 !important;
      background: #1f4d29 !important;
    }
  }
</style>
