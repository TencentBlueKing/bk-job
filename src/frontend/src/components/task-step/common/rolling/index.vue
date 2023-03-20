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
  <div class="task-step-rolling">
    <jb-form-item :label="$t('滚动执行')">
      <bk-switcher
        theme="primary"
        :value="formData[enabledField]"
        @change="handleRollingEnableChange" />
    </jb-form-item>
    <div v-if="formData[enabledField]">
      <jb-form-item
        ref="expr"
        :label="$t('滚动策略')"
        :property="exprField"
        required
        :rules="rollingExprRule">
        <div class="form-item-content rolling-expr-field">
          <bk-input
            :value="formData[exprField]"
            @change="handleRollingExprChange" />
          <div
            v-if="tips"
            class="strategy-tips">
            {{ tips }}
          </div>
          <div
            v-if="errorMessage"
            class="strategy-error">
            {{ errorMessage }}
          </div>
          <div
            class="rolling-expr-gudie-btn"
            :class="{
              active: isShowGuide,
            }"
            @click="handleShowGuide">
            <icon
              v-bk-tooltips="`${$t('查看使用指引')}`"
              type="help-document-fill" />
          </div>
        </div>
      </jb-form-item>
      <jb-form-item
        ref="rollingMode"
        :label="$t('滚动机制')"
        required>
        <bk-select
          class="form-item-content"
          :clearable="false"
          :value="formData[modeField]"
          @change="handleRollingModeChange">
          <bk-option
            :id="1"
            :name="$t('默认（执行失败则暂停）')" />
          <bk-option
            :id="2"
            :name="$t('忽略失败，自动滚动下一批')" />
          <bk-option
            :id="3"
            :name="$t('不自动，每批次都人工确认')" />
        </bk-select>
      </jb-form-item>
    </div>
    <element-teleport
      v-if="isShowGuide"
      target="#rollingExprGuide">
      <guide @on-close="handleHideGuide" />
    </element-teleport>
  </div>
</template>
<script>
  import _ from 'lodash';
  import I18n from '@/i18n';
  import rollingExprParse from '@utils/rolling-expr-parse';

  import Guide from './guide';

  export default {
    name: '',
    components: {
      Guide,
    },
    props: {
      enabledField: {
        type: String,
        required: true,
      },
      exprField: {
        type: String,
        required: true,
      },
      modeField: {
        type: String,
        required: true,
      },
      formData: {
        type: Object,
        default: () => ({}),
      },
    },
    data() {
      return {
        isShowGuide: false,
        tips: '',
        errorMessage: '',
      };
    },
    computed: {
      /**
       * @desc 滚动策略验证规则，不需要滚动执行时不进行验证
       * @returns { Array }
       */
      rollingExprRule() {
        if (!this.formData[this.enabledField]) {
          return [];
        }
        return [
          {
            validator: (value) => {
              this.errorMessage = '';
              return Boolean(value);
            },
            message: I18n.t('滚动策略必填'),
            trigger: 'blur',
          },
          {
            validator: (value) => {
              this.errorMessage = '';
              try {
                rollingExprParse(value);
                return true;
              } catch {
                return false;
              }
            },
            message: I18n.t('滚动策略格式不正确'),
            trigger: 'blur',
          },
        ];
      },
    },
    watch: {
      formData: {
        handler(formData) {
          this.validatorExpr(formData[this.exprField]);
        },
        immediate: true,
        deep: true,
      },
    },
    methods: {
      /**
       * @desc 验证滚动规则
       * @param { String } expr
       */
      validatorExpr(expr) {
        try {
          this.errorMessage = '';
          this.tips = rollingExprParse(expr);
        } catch (error) {
          this.tips = '';
          this.errorMessage = error.message;
        }
      },
      /**
       * @desc 是否启用滚动
       * @param { Boolean } enabled
       */
      handleRollingEnableChange(enabled) {
        this.$emit('on-change', this.enabledField, enabled);
        if (!enabled) {
          this.tips = '';
          this.errorMessage = '';
          this.$emit('on-reset', {
            [this.exprField]: '',
            [this.modeField]: 1,
          });
        }
        // 滚动策略默认 10%
        this.$emit('on-reset', {
          [this.exprField]: '10%',
          [this.modeField]: 1,
        });
        this.$nextTick(() => {
          if (this.formData[this.enabledField]) {
            this.$refs.rollingMode.$el.scrollIntoView();
          }
        });
      },
      /**
       * @desc 滚动策略更新
       * @param { String } expr 滚动表达式
       */
      handleRollingExprChange: _.debounce(function (expr) {
        this.$refs.expr && this.$refs.expr.clearValidator();
        this.validatorExpr(expr);
        this.$emit('on-change', this.exprField, expr);
      }, 20),
      /**
       * @desc 滚动机制更新
       * @param { Number } rollingMode
       */
      handleRollingModeChange(rollingMode) {
        this.$emit('on-change', this.modeField, rollingMode);
      },
      handleShowGuide() {
        this.isShowGuide = !this.isShowGuide;
      },
      handleHideGuide() {
        this.isShowGuide = false;
      },
    },
  };
</script>
<style lang="postcss">
  .task-step-rolling {
    display: block;

    .rolling-expr-field {
      position: relative;

      .form-error-tip {
        display: none;
      }

      .rolling-expr-gudie-btn {
        position: absolute;
        top: 0;
        right: -22px;
        font-size: 14px;
        color: #979ba5;
        cursor: pointer;

        &:hover,
        &.active {
          color: #3a84ff;
        }
      }
    }

    .strategy-tips {
      margin-top: 4px;
      font-size: 12px;
      line-height: 16px;
      color: #979ba5;
    }

    .strategy-error {
      margin-top: 4px;
      font-size: 12px;
      line-height: 18px;
      color: #ea3636;
    }
  }
</style>
