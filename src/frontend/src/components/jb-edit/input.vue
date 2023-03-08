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
    :key="value"
    class="jb-edit-input"
    :class="mode">
    <template v-if="!isEditing">
      <div
        class="render-value-box"
        @click.stop="handleBlockShowEdit">
        <div
          v-bk-overflow-tips
          class="value-text">
          <slot :value="newVal">
            <span>{{ newVal || '--' }}</span>
          </slot>
        </div>
        <div class="edit-action-box">
          <icon
            v-if="!isBlock && !isSubmiting"
            class="edit-action"
            type="edit-2"
            @click.self.stop="handleShowEdit" />
          <icon
            v-if="isSubmiting"
            class="edit-loading"
            type="loading-circle" />
        </div>
      </div>
    </template>
    <template v-else>
      <div
        class="edit-value-box"
        :class="{ 'edit-error': !!error }"
        @click.stop="">
        <bk-input
          ref="input"
          :value="newVal"
          @blur="handleInputBlur"
          @change="handleInputChange"
          @keyup="handleInputEnter" />
        <div
          v-if="error"
          v-bk-tooltips.top="error"
          class="input-edit-info">
          <icon type="info" />
        </div>
      </div>
    </template>
  </div>
</template>
<script>
  import I18n from '@/i18n';

  export default {
    name: 'JbEditInput',
    props: {
      /**
       * @value block 块级交互
       * @value ‘’ 默认鼠标点击编辑按钮
       */
      mode: {
        type: String,
        default: '',
      },
      /**
       * @desc 编辑操作对应的字段名称
       */
      field: {
        type: String,
        required: true,
      },
      /**
       * @desc 默认值
       */
      value: {
        type: String,
        default: '',
      },
      /**
       * @desc 宽度
       */
      width: {
        type: String,
        default: 'auto',
      },
      remoteHander: {
        type: Function,
        default: () => Promise.resolve(),
      },
      /**
       * @desc 值验证规则
       */
      rules: {
        type: Array,
        default: () => [],
      },
    },
    data() {
      return {
        newVal: this.value,
        error: '',
        isEditing: false,
        isSubmiting: false,
      };
    },
    computed: {
      styles() {
        return {
          width: this.width,
        };
      },
      isBlock() {
        return this.mode === 'block';
      },
    },
    watch: {
      value(newVal) {
        this.newVal = newVal;
      },
    },
    mounted() {
      this.isValidatoring = false;
      document.body.addEventListener('click', this.handleHideEdit);
      this.$once('hook:beforeDestroy', () => {
        document.body.removeEventListener('click', this.handleHideEdit);
      });
    },
    methods: {
      /**
       * @desc 值验证
       */
      doValidator() {
        const checkValidator = (rule, value) => new Promise((resolve, reject) => {
          if (rule.required && !value) {
            reject(rule.message);
          }
          // 通过自定义方法来检测
          if (rule.validator && (typeof rule.validator === 'function')) {
            const result = rule.validator(value);
            if (result.then) {
              result.then((data) => {
                if (data) {
                  return resolve();
                }
                return reject(rule.message);
              }).catch(() => {
                reject(rule.message);
              });
            } else if (result) {
              return resolve();
            } else {
              return reject(rule.message);
            }
          } else {
            resolve();
          }
        });

        const allPromise = this.rules.map(rule => checkValidator(rule, this.newVal));
        this.isValidatoring = true;
        return Promise.all(allPromise).finally(() => {
          this.isValidatoring = false;
        });
      },
      /**
       * @desc 提交编辑
       */
      triggerChange() {
        this.doValidator()
          .then(() => {
            this.isEditing = false;
            if (this.newVal === this.value) {
              return;
            }
            this.isSubmiting = true;
            this.remoteHander({
              [this.field]: this.newVal,
            }).then(() => {
              this.$emit('on-change', {
                [this.field]: this.newVal,
              });
              this.messageSuccess(I18n.t('编辑成功'));
            })
              .catch(() => {
                this.newVal = this.value;
              })
              .finally(() => {
                this.isSubmiting = false;
              });
          })
          .catch((error) => {
            this.error = error;
          });
      },
      handleBlockShowEdit() {
        if (!this.isBlock) {
          return;
        }
        this.handleShowEdit();
      },
      /**
       * @desc 显示input
       */
      handleShowEdit() {
        document.body.click();
        this.isEditing = true;
        this.$nextTick(() => {
          this.$refs.input.focus();
        });
      },
      /**
       * @desc input 值更新
       * @param {String} value 最新输入值
       */
      handleInputChange(value) {
        this.newVal = value.trim();
      },
      /**
       * @desc input 失去焦点开始提交
       */
      handleInputBlur() {
        this.triggerChange();
      },
      /**
       * @desc input enter 提交
       * @param {String} value 最新输入值
       * @param {Object} event dom 事件
       */
      handleInputEnter(value, event) {
        if (!this.isEditing) return;
        if (event.key === 'Enter' && event.keyCode === 13) {
          this.triggerChange();
        }
      },
      /**
       * @desc 隐藏 input 框
       * @param {Object} event dom 事件
       */
      handleHideEdit(event) {
        const eventPath = event.composedPath();
        if (this.isValidatoring || this.error) {
          return;
        }
        // eslint-disable-next-line no-plusplus
        for (let i = 0; i < eventPath.length; i++) {
          const target = eventPath[i];
          if (target.className === 'jb-edit-input') {
            return;
          }
        }
        this.isEditing = false;
      },
    },
  };
</script>
<style lang='postcss'>
  .jb-edit-input {
    &.block {
      position: relative;
      margin-left: -10px;
      cursor: pointer;

      .render-value-box {
        padding-left: 10px;

        &:hover {
          background: #f0f1f5;
        }
      }

      .edit-action-box {
        position: absolute;
        top: 0;
        right: 10px;
        width: 16px;
      }
    }

    .render-value-box {
      position: relative;
      display: flex;
      height: 30px;
      min-width: 36px;
      min-height: 28px;

      &:hover {
        .edit-action {
          opacity: 100%;
          transform: scale(1);
        }
      }
    }

    .value-text {
      overflow: hidden;
      line-height: 30px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .edit-action-box {
      display: flex;
      align-items: center;
      min-height: 1em;
      margin-right: auto;
      font-size: 16px;
      color: #979ba5;

      .edit-action {
        padding: 6px 0 6px 2px;
        cursor: pointer;
        opacity: 0%;
        transform: scale(0);
        transition: 0.15s;
        transform-origin: left center;

        &:hover {
          color: #3a84ff;
        }
      }

      .edit-loading {
        position: absolute;
        top: 9px;
        margin-left: 2px;
        animation: rotate-loading 1s linear infinite;
      }
    }

    .edit-value-box {
      position: relative;
      width: 100%;
      font-size: 0;

      &.edit-error {
        .bk-form-input {
          border-color: #ea3636 !important;
        }
      }

      .input-edit-info {
        position: absolute;
        top: 0;
        right: 0;
        bottom: 0;
        z-index: 1;
        display: flex;
        align-items: center;
        padding: 0 10px;
        font-size: 16px;
        color: #ea3636;
      }

      .bk-form-input {
        height: 30px;
      }
    }
  }
</style>
