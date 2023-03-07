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
    ref="jobForm"
    class="job-form"
    @click="handleApplyChange">
    <bk-form
      ref="bkForm"
      :model="model"
      v-bind="$attrs">
      <slot />
    </bk-form>
  </div>
</template>
<script>
  import _ from 'lodash';

  export default {
    name: 'JbForm',
    inheritAttrs: false,
    props: {
      fixed: {
        type: Boolean,
        default: false,
      },
      model: {
        type: Object,
      },
    },
    data() {
      return {
        max: 0,
        isApplyChange: false,
      };
    },
    watch: {
      model: {
        handler() {
          setTimeout(() => {
            if (this.isApplyChange) {
              window.changeFlag = true;
            }
          });
        },
        deep: true,
      },
    },
    created() {
      this.timer = Date.now();
    },
    updated() {
      this.calcLableWidth();
    },
    mounted() {
      this.resetDefaultFeature();
      setTimeout(() => {
        const isShowForm = this.$refs.jobForm && this.$refs.jobForm.getBoundingClientRect().width > 0;
        if (isShowForm) {
          this.calcLableWidth();
          this.autoFocus();
        }
      });
    },
    methods: {
      /**
       * @desc 动态计算label的宽度
       */
      calcLableWidth() {
        if (Date.now() - 20 > this.timer) {
          this.timer = Date.now();
        } else {
          return;
        }
        if (this.fixed) {
          return;
        }
        const labelPadding = 24;
        const safePadding = 2;
        const $formEle = this.$refs.jobForm;
        if (!$formEle) {
          return;
        }
        let max = 0;
        const $labelEleList = $formEle.querySelectorAll('.bk-label');
        $labelEleList.forEach((item) => {
          const { width } = item.querySelector('span').getBoundingClientRect();
          max = Math.max(max, width);
        });

        const labelWidth = max + labelPadding + safePadding;
        $labelEleList.forEach((item) => {
          item.style.width = `${labelWidth}px`;
        });
        $formEle.querySelectorAll('.bk-form-content').forEach((item) => {
          item.style.marginLeft = `${labelWidth}px`;
        });
        this.max = max;
      },
      /**
       * @desc 处理表单项的自动聚焦
       */
      autoFocus() {
        // 表单项自动获取焦点
        const $autoFocusItem = this.$refs.jobForm.querySelector('[autofocus="autofocus"]');
        if ($autoFocusItem) {
          setTimeout(() => {
            $autoFocusItem.focus();
          });
        }
      },
      /**
       * @desc 设置表单项的默认特性
       */
      resetDefaultFeature() {
        const $inputList = this.$refs.jobForm.querySelectorAll('input');
        $inputList.forEach((inputItem) => {
          inputItem.spellcheck = false;
        });
      },
      /**
       * @desc 标记用户是否主动操作过表单项
       */
      handleApplyChange() {
        this.isApplyChange = true;
      },
      /**
       * @desc 验证表单
       */
      validate() {
        return this.$refs.bkForm.validate().catch((error) => {
          this.$refs.jobForm.querySelector('.jb-form-item.is-error').scrollIntoView();
          return Promise.reject(error);
        });
      },
      /**
       * @desc 清除表单验证信息
       */
      clearError(fields) {
        if (!fields) {
          for (const fieldItem of this.$refs.bkForm.formItems) {
            fieldItem.clearValidator();
          }
          return;
        }
        const allFileds = !_.isArray(fields) ? [fields] : fields;
        for (const fieldItem of this.$refs.bkForm.formItems) {
          const curProperty = fieldItem.property;
          if (allFileds.includes(curProperty)) {
            fieldItem.clearValidator();
          }
        }
      },
    },
  };
</script>
<style lang='postcss'>
  .job-form {
    .bk-form {
      .bk-label {
        height: 32px;
        padding-right: 24px;
        word-break: keep-all;
        white-space: pre;
        box-sizing: border-box;
      }

      .bk-form-item {
        margin-bottom: 20px;
      }

      &.bk-form-vertical {
        .bk-form-item {
          margin: 0;
          margin-bottom: 20px;
        }
      }

      .bk-form-item.is-danger,
      .bk-form-item.is-error {
        .bk-sideslider {
          input[type="number"],
          input[type="password"],
          input[type="text"],
          input[type="url"],
          select,
          textarea {
            color: #63656e;
            border-color: #c4c6cc;

            &::placeholder {
              color: #63656e;
              border-color: #c4c6cc;
            }
          }

          .bk-select {
            border-color: #c4c6cc;

            &::before {
              color: #63656e;
            }
          }
        }
      }
    }
  }
</style>
