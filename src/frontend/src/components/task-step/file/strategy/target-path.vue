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
    <jb-form-item
      :label="$t('目标路径')"
      :property="field"
      required
      :rules="rules">
      <div
        class="form-item-content"
        @mouseenter="handleMouseenter"
        @mouseleave="handleMouseleave">
        <bk-input
          ref="input"
          :placeholder="$t('请填写分发路径')"
          :value="formData[field]"
          @blur="handleInputBlur"
          @change="handleChange"
          @focus="handleInputFocus" />
      </div>
    </jb-form-item>
    <div style="display: none;">
      <div
        ref="targetPathTips"
        class="target-path-tips"
        @mouseenter="handleTipsMouseenter"
        @mouseleave="handleMouseleave">
        <div class="row">
          {{ $t('传输至linux服务器需以\/开头的绝对路径，如：/data/xx') }}
        </div>
        <div class="row">
          {{ $t('传输至Windows服务器需包含盘符开头，如：D:\\tmp\\') }}
        </div>
        <div class="row">
          <div>{{ $t('目标路径支持的内置变量：（点击可直接复制使用）') }}</div>
          <table class="target-path-demo">
            <thead>
              <tr>
                <th>{{ $t('变量名称') }}</th>
                <th>{{ $t('含义') }}</th>
                <th>{{ $t('示例') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td @click="handleCopy('[FILESRCIP]')">
                  <div class="copy-box">
                    <span class="copy-value">[FILESRCIP]</span>
                    <span class="copy-tips">{{ $t('点击复制') }}</span>
                  </div>
                </td>
                <td>{{ $t('源服务器IP') }}</td>
                <td>0_192.168.0.100</td>
              </tr>
              <tr>
                <td @click="handleCopy('[DATE:yyyy-MM-dd]')">
                  <div class="copy-box">
                    <span class="copy-value">[DATE:yyyy-MM-dd]</span>
                    <span class="copy-tips">{{ $t('点击复制') }}</span>
                  </div>
                </td>
                <td>{{ $t('当前日期') }}</td>
                <td>2021-01-01</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="row">
          {{ $t('（日期变量可传入标准的日期时间格式，如 yyyy-MM-dd_HH:mm:ss）') }}
        </div>
      </div>
    </div>
  </div>
</template>
<script>
  import Tippy from 'bk-magic-vue/lib/utils/tippy';
  import _ from 'lodash';

  import {
    execCopy,
  } from '@utils/assist';
  import {
    filePathRule,
  } from '@utils/validator';

  import I18n from '@/i18n';

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
      tipsPlacement: {
        type: String,
        default: 'right-start',
      },
    },
    watch: {
      tipsPlacement(tipsPlacement) {
        this.popperInstance && this.popperInstance.set({
          placement: tipsPlacement,
        });
      },
    },
    created() {
      this.rules = [
        {
          required: true,
          message: I18n.t('目标路径必填'),
          trigger: 'blur',
        },
        {
          validator: filePathRule.validator,
          message: I18n.t('目标路径格式错误'),
          trigger: 'blur',
        },
      ];
      this.isMouseenter = false;
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
            placement: this.tipsPlacement,
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
          this.popperInstance.setContent(this.$refs.targetPathTips);
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
      /**
       * @desc 复制变量
       * @param {String} variable 目标路径支持的内置变量
       */
      handleCopy(variable) {
        execCopy(variable);
      },
      /**
       * @desc 更新字段值
       */
      handleChange(value) {
        this.$emit('on-change', this.field, _.trim(value));
      },
    },
  };
</script>
<style lang='postcss' scoped>
  .target-path-tips {
    font-size: 12px;
    line-height: 16px;
    color: #63656e;

    .row {
      position: relative;
      padding-left: 12px;
      margin-bottom: 6px;

      &::before {
        position: absolute;
        top: 6px;
        left: 0;
        width: 4px;
        height: 4px;
        background: currentcolor;
        border-radius: 50%;
        content: "";
      }
    }

    .target-path-demo {
      width: 100%;
      margin-top: 6px;

      th,
      td {
        height: 40px;
        padding: 0 10px;
        border-bottom: 1px solid #dfe0e5;
      }

      th {
        background-color: #fafbfd;
      }

      .copy-box {
        display: flex;
        width: 160px;
        height: 26px;
        padding: 0 10px;
        margin: 0 -10px;
        overflow: hidden;
        line-height: 26px;
        cursor: pointer;
        border-radius: 2px;

        &:hover {
          background: #f0f1f5;

          .copy-tips {
            display: block;
          }
        }

        .copy-value {
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .copy-tips {
          display: none;
          margin-left: auto;
          flex: 0 0 auto;
        }
      }
    }
  }
</style>
