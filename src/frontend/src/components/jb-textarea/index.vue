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
    class="job-textarea"
    :class="{
      fouced: isFocused,
    }"
    :style="boxStyles">
    <div
      ref="wraper"
      class="job-textarea-wraper"
      :style="wraperStyles">
      <div style="min-height: 30px; word-break: break-all; white-space: pre-wrap; visibility: hidden;">
        {{ localValue }}
      </div>
      <textarea
        ref="textarea"
        class="job-textarea-edit"
        resize="none"
        spellcheck="false"
        :value="localValue"
        @blur="handleBlur"
        @focus="handleInputGetFocus"
        @input="handleInputChange" />
      <div
        v-if="maxlength > 0 && isFocused"
        class="value-length">
        <span style="color: #63656e;">{{ localValue.length }}</span>
        <span>/</span>
        <span>{{ maxlength }}</span>
      </div>
    </div>
    <div
      v-if="showPlaceholder"
      class="job-textarea-placeholder"
      @click="handleInputGetFocus">
      {{ placeholder }}
    </div>
  </div>
</template>
<script>
  const rowHeight = 18;

  export default {
    name: '',
    props: {
      value: {
        type: String,
        default: '',
      },
      placeholder: String,
      resize: {
        type: Boolean,
        default: false,
      },
      // 默认展示多少行
      rows: {
        type: Number,
        default: 1,
      },
      maxlength: Number,
    },
    data() {
      return {
        localValue: this.value,
        isFocused: false,
      };
    },
    computed: {
      boxStyles() {
        const styles = {};
        const defaultHeight = this.rows * rowHeight + 12;
        styles.height = `${defaultHeight}px`;
        return styles;
      },
      wraperStyles() {
        const styles = {
          // eslint-disable-next-line no-underscore-dangle
          zIndex: window.__bk_zIndex_manager.nextZIndex(),
        };

        if (!this.isFocused) {
          styles['max-height'] = '100%';
        }
        return styles;
      },
      showPlaceholder() {
        if (this.isFocused) {
          return false;
        }
        return !this.localValue;
      },
    },
    methods: {
      /**
       * @desc 输入框聚焦
       */
      focus() {
        this.$refs.textarea.focus();
      },
      /**
       * @desc 输入框获得焦点
       */
      handleInputGetFocus() {
        this.isFocused = true;
        setTimeout(() => {
          this.$refs.textarea.selectionStart = this.localValue.length;
          this.$refs.textarea.selectionEnd = this.localValue.length;
        });
      },
      /**
       * @desc 用户输入
       */
      handleInputChange(event) {
        let localValue = event.target.value.trim();
        if (this.maxlength > 0 && localValue.length > this.maxlength) {
          localValue = localValue.slice(0, this.maxlength);
          this.$nextTick(() => {
            this.$refs.textarea.value = localValue;
          });
        }
        this.localValue = localValue;
        this.$emit('input', this.localValue);
        this.$emit('change', this.localValue);
      },
      /**
       * @desc 输入框失焦
       */
      handleBlur() {
        this.isFocused = false;
        this.$emit('blur', this.localValue);
      },
    },
  };
</script>
<style lang='postcss'>
  @import url("@/css/mixins/scroll");

  .job-textarea {
    position: relative;
    font-size: 12px;
    line-height: 18px;
    word-break: break-all;
    cursor: pointer;

    .job-textarea-wraper {
      position: absolute;
      top: 0;
      right: 0;
      left: 0;
      z-index: 1999;
      max-height: 300px;
      min-height: 100%;
      padding-bottom: 20px;

      .value-length {
        position: absolute;
        right: 20px;
        bottom: 6px;
        display: flex;
        color: #979ba5;
        cursor: default;
        background: inherit;
        border-radius: 8px;
        user-select: none;
      }
    }

    .job-textarea-edit {
      position: absolute;
      top: 0;
      right: 0;
      bottom: 0;
      left: 0;
      width: 100%;
      height: 100%;
      padding: 6px 10px 0;
      overflow-y: scroll;
      font-size: 12px;
      color: #63656e;
      background: #fff;
      border: 1px solid #c4c6cc;
      border-radius: 2px;
      outline: none;
      resize: none;

      &:focus {
        padding-bottom: 30px;
        background: #fff !important;
        border: 1px solid #3a84ff !important;
      }

      @mixin scroller;
    }

    .job-textarea-placeholder {
      position: absolute;
      top: 1px;
      right: 1px;
      bottom: 1px;
      left: 1px;
      height: 30px;
      padding: 6px 10px;
      overflow: hidden;
      color: #c4c6cc;
      text-overflow: ellipsis;
      white-space: nowrap;
      background: inherit;
    }
  }
</style>
