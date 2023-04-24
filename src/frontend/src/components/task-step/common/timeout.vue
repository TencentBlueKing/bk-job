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
  <div>
    <jb-form-item :label="$t('超时时长')">
      <div
        class="form-item-content"
        @mouseenter="handleMouseenter"
        @mouseleave="handleMouseleave">
        <bk-input
          ref="input"
          :max="86400"
          :min="1"
          :palceholder="$t('此处留空将默认为7200s')"
          :precision="0"
          type="number"
          :value="formData[field]"
          @blur="handleInputBlur"
          @change="handleChange"
          @focus="handleInputFocus">
          <template slot="append">
            <div class="group-text">
              s
            </div>
          </template>
        </bk-input>
      </div>
    </jb-form-item>
    <div style="display: none;">
      <div
        ref="tips"
        @mouseenter="handleTipsMouseenter"
        @mouseleave="handleMouseleave">
        {{ $t('允许最小 1s，最大为 86400s') }}
      </div>
    </div>
  </div>
</template>
<script>
  import Tippy from 'bk-magic-vue/lib/utils/tippy';

  export default {
    props: {
      field: {
        type: String,
        required: true,
      },
      formData: {
        type: Object,
        default: () => ({}),
      },
    },
    beforeDestroy() {
      if (this.popperInstance) {
        this.popperInstance.hide();
        this.popperInstance.destroy();
      }
    },
    methods: {
      /**
       * @desc 显示tips
       */
      showTips() {
        if (!this.popperInstance) {
          this.popperInstance = Tippy(this.$refs.input.$el, {
            arrow: true,
            placement: 'right',
            trigger: 'manual',
            theme: 'light',
            interactive: true,
            hideOnClick: false,
            animation: 'slide-toggle',
            lazy: false,
            size: 'small',
            boundary: 'window',
            distance: 20,
            zIndex: window.__bk_zIndex_manager.nextZIndex(), // eslint-disable-line no-underscore-dangle
          });
          this.popperInstance.setContent(this.$refs.tips);
        }

        this.popperInstance.show();
      },
      /**
       * @desc 隐藏tips
       */
      hideTips() {
        if (this.isMouseenter) {
          return;
        }
        this.popperInstance.hide();
      },
      /**
       * @desc 鼠标移入的时候显示tips
       */
      handleMouseenter() {
        clearTimeout(this.hideTimer);
        this.showTips();
      },
      /**
       * @desc 鼠标移出的时候隐藏tips
       */
      handleMouseleave() {
        this.isMouseenter = false;
        this.hideTimer = setTimeout(() => {
          if (!this.isInputFocus) {
            this.hideTips();
          }
        }, 100);
      },
      /**
       * @desc 获得焦点是显示ips
       */
      handleInputFocus() {
        this.isInputFocus = true;
        this.showTips();
      },
      /**
       * @desc 失去焦点是显示ips
       */
      handleInputBlur() {
        this.isInputFocus = false;
        this.hideTips();
      },
      /**
       * @desc 鼠标在tips内部时取消隐藏tips定时器
       */
      handleTipsMouseenter() {
        clearTimeout(this.hideTimer);
        this.isMouseenter = true;
      },
      handleChange(value) {
        this.$emit('on-change', this.field, value);
      },
    },
  };
</script>
