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
  <div class="jb-popover-confirm">
    <slot />
    <div
      ref="popoverContent"
      class="jb-popover-content"
      :style="{
        width: parsedWidth,
        minWidth: parsedMinWidth,
      }">
      <h2
        ref="confirmTitle"
        class="confirm-title">
        <slot name="title">
          {{ title }}
        </slot>
      </h2>
      <p
        ref="confirmContent"
        class="confirm-content">
        <slot name="content">
          {{ content }}
        </slot>
      </p>
      <div class="confirm-options">
        <slot name="options">
          <bk-button
            class="confirm-option-button"
            :loading="pending"
            size="small"
            theme="primary"
            @click="handleConfirm">
            {{ confirmText }}
          </bk-button>
          <bk-button
            class="confirm-option-button"
            :disabled="pending"
            size="small"
            @click="handleCancel">
            {{ cancelText }}
          </bk-button>
        </slot>
      </div>
    </div>
  </div>
</template>
<script>
  import I18n from '@/i18n';

  export default {
    name: 'JbPopoverConfirm',
    props: {
      title: {
        type: String,
        default: '',
      },
      content: {
        type: String,
        default: '',
      },
      confirmText: {
        type: String,
        default: I18n.t('确定'),
      },
      cancelText: {
        type: String,
        default: I18n.t('取消'),
      },
      placement: {
        type: String,
        default: 'top',
      },
      theme: {
        type: String,
        default: 'light',
      },
      contentWidth: {
        type: [
          String,
          Number,
        ],
        default: 280,
      },
      contentMinWidth: {
        type: [
          String,
          Number,
        ],
        default: '',
      },
      disabled: Boolean,
      confirmHandler: Function,
      cancelHandler: Function,
    },
    data() {
      return {
        instance: null,
        pending: false,
      };
    },
    computed: {
      parsedWidth() {
        const width = parseInt(this.contentWidth, 10);
        return isNaN(width) ? 'auto' : (`${width}px`);
      },
      parsedMinWidth() {
        const minWidth = parseInt(this.contentMinWidth, 10);
        return isNaN(minWidth) ? 'auto' : (`${minWidth}px`);
      },
    },
    watch: {
      disabled(disabled) {
        if (this.instance) {
          disabled ? this.instance.disable() : this.instance.enable();
        }
      },
    },
    mounted() {
      this.init();
    },
    beforeDestroy() {
      this.instance = null;
    },
    methods: {
      init() {
        this.instance = this.$bkPopover(this.$el, {
          theme: `${this.theme} jb-popover-confirm`,
          interactive: true,
          placement: this.placement,
          content: this.$refs.popoverContent,
          trigger: 'click',
          arrow: true,
          onShow: () => {
            this.$emit('show');
          },
          onHidden: () => {
            this.$emit('cancel', this);
          },
        });
        this.disabled && this.instance.disable();
      },
      async handleConfirm() {
        if (typeof this.confirmHandler === 'function') {
          try {
            this.pending = true;
            await this.confirmHandler(this.instance);
          } catch (e) {
            console.error(e);
          } finally {
            this.pending = false;
          }
        }
        this.instance && this.hide();
        this.$emit('confirm', this);
      },
      hide() {
        this.instance && this.instance.hide();
      },
      handleCancel() {
        this.hide();
        this.$emit('cancel', this);
      },
    },
  };
</script>

<style lang="postcss">
  .jb-popover-confirm {
    display: inline-block;
  }

  .jb-popover-confirm-theme.tippy-tooltip {
    .tippy-content {
      padding: 20px !important;
    }

    .jb-popover-content {
      .confirm-title {
        font-size: 16px;
        font-weight: normal;
        line-height: 20px;
        color: #313238;
      }

      .confirm-content {
        margin: 10px 0 0;
        font-size: 12px;
        color: #63656e;
      }

      .confirm-options {
        display: flex;
        justify-content: flex-end;
        margin: 16px 0 0;
      }

      .confirm-option-button {
        height: 24px;
        min-width: 60px;
        margin: 0 0 0 10px;
        line-height: 22px;
      }
    }
  }
</style>
