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
  <div class="choose-ip-server-ip-input">
    <div @click="handleInputClick">
      <bk-input
        ref="input"
        :class="{
          'focus-error': isFocusError,
        }"
        :placeholder="$t('请输入 IP 地址，多IP可用 空格 换行 ; , |分隔 \n带云区域请用冒号分隔，如（ 0:192.168.1.101 ）')"
        :rows="inputRows"
        type="textarea"
        :value="ipInputText"
        @change="handleIPChange" />
    </div>
    <div class="input-action">
      <div
        v-if="isError"
        class="input-error">
        <div>{{ $t('以上内容存在错误：') }}</div>
        <div v-if="invalidIPList.length > 0">
          <span>{{ $t('IP 在本业务下不存在') }}</span>
          <icon
            v-bk-tooltips="$t('标识错误')"
            class="error-action"
            type="ip-audit"
            @click="handleHighlightInvilad" />
          <icon
            v-bk-tooltips="$t('一键清除')"
            class="error-action"
            type="delete"
            @click="handleRemoveInvalid" />
        </div>
        <div v-if="invalidIPList.length > 0 && errorIPList.length > 0">
          ；
        </div>
        <div v-if="errorIPList.length > 0">
          <span>{{ $t('内容格式错误，无法识别') }}</span>
          <icon
            v-bk-tooltips="$t('标识错误')"
            class="error-action"
            type="ip-audit"
            @click="handleHightlightError" />
          <icon
            v-bk-tooltips="$t('一键清除')"
            class="error-action"
            type="delete"
            @click="handleRemoveError" />
        </div>
      </div>
      <bk-button
        class="submit-btn"
        :loading="isSubmiting"
        outline
        theme="primary"
        @click="handleAddHost">
        <span>{{ $t('添加到已选择') }}</span>
        <div
          v-if="inputItemList.length > 0"
          ref="inputNumber"
          class="server-input-number">
          <span class="text">{{ inputItemList.length }}</span>
        </div>
      </bk-button>
    </div>
  </div>
</template>
<script>
  import _ from 'lodash';

  import HostManageService from '@service/host-manage';

  import { encodeRegexp } from '@utils/assist';

  export default {
    name: '',
    inheritAttrs: false,
    props: {
      previewId: {
        type: String,
        required: true,
      },
      dialogHeight: {
        type: Number,
        required: true,
      },
    },
    data() {
      return {
        isSubmiting: false,
        ipInputText: '',
        inputItemList: [],
        invalidIPList: [],
        errorIPList: [],
        isFocusError: false,
      };
    },
    computed: {
      inputRows() {
        const offsetTop = 65;
        const offsetBottom = 130;
        const inputPadding = 15;

        return Math.floor((this.dialogHeight - offsetTop - offsetBottom - inputPadding) / 18);
      },
      isError() {
        return this.invalidIPList.length > 0 || this.errorIPList.length > 0;
      },
    },
    methods: {
      /**
       * @desc 更新输入框内容
       */
      updateInputValue() {
        this.ipInputText = [
          ...this.invalidIPList,
          ...this.errorIPList,
        ].join('\n');
        // fix: bk-input组件更新问题
        setTimeout(() => {
          this.$refs.input.setCurValue(this.ipInputText);
        });
      },
      /**
       * @desc 用户点击输入框时切换选中文本样式
       */
      handleInputClick() {
        this.isFocusError = false;
      },
      /**
       * @desc 高亮输入框内无效的 IP 输入
       */
      handleHighlightInvilad() {
        this.isFocusError = true;
        const $inputEl = this.$refs.input.$el.querySelector('textarea');
        const errorText = this.invalidIPList.join('\n');
        $inputEl.focus();
        $inputEl.selectionStart = 0;
        $inputEl.selectionEnd = errorText.length;
      },
      /**
       * @desc 清空输入框内无效的 IP 输入
       */
      handleRemoveInvalid() {
        this.invalidIPList = [];
        this.updateInputValue();
      },
      /**
       * @desc 高亮输入框内错误格式的 IP 输入
       */
      handleHightlightError() {
        this.isFocusError = true;
        const $inputEl = this.$refs.input.$el.querySelector('textarea');
        $inputEl.focus();
        const invalidText = this.invalidIPList.join('\n');
        const errorText = this.errorIPList.join('\n');
        const startIndex = invalidText.length > 0 ? invalidText.length + 1 : 0;
        $inputEl.selectionStart = startIndex;
        $inputEl.selectionEnd = startIndex + errorText.length;
      },
      /**
       * @desc 清空输入框内错误格式的 IP 输入
       */
      handleRemoveError() {
        this.errorIPList = [];
        this.updateInputValue();
      },
      /**
       * @desc 输入框值改变
       * @param {String} value 用户输入值
       */
      handleIPChange: _.throttle(function (value) {
        const realValue = _.trim(value);
        this.invalidIPList = [];
        this.errorIPList = [];
        if (!realValue) {
          this.inputItemList = [];
          // 通知外部组件输入框没有值需要保存
          this.$emit('on-input-change', false);
        } else {
          this.inputItemList = value.split(/[;,；，\n|]+/).filter(_ => !!_);
          // 通知外部组件输入框有值还没被保存
          this.$emit('on-input-change', true);
        }
        // this.isError = false;
      }, 60),
      /**
       * @desc 提交输入结果
       */
      handleAddHost() {
        if (this.inputItemList.length < 1) {
          return;
        }
        const IPReg = /(((\d+:)?)(?:(?:25[0-5]|2[0-4]\d|((1\d{2})|([1-9]?\d)))\.){3}(?:25[0-5]|2[0-4]\d|((1\d{2})|([1-9]?\d))))/;
        const ipList = [];
        // 无法解析出 IP 的内容
        const errorIPList = [];
        this.inputItemList.forEach((IPText) => {
          const IPMatch = IPText.match(IPReg);
          if (IPMatch) {
            const [IPStr] = IPMatch;
            const errorIPRule = new RegExp(`([\\d:.]${encodeRegexp(IPStr)})|(${encodeRegexp(IPStr)}[\\d.])`);
            if (errorIPRule.test(IPText)) {
              // 无法完全正确的解析 IP（eq: 1.1.1.12.2.2.2，少了分隔造成的）
              errorIPList.push(IPText);
            } else {
              // 提取识别成功的 IP
              ipList.push(IPStr);
              // 将剩下的内容作为错误 IP 处理
              const errorText = IPText.replace(new RegExp(`(${encodeRegexp(IPStr)})|(\\s)`, 'g'), '');
              if (errorText) {
                errorIPList.push(errorText);
              }
            }
          } else {
            // 输入内容中不包含 IP
            errorIPList.push(IPText);
          }
        });

        this.isSubmiting = true;

        const params = {
          ipList,
        };
        if (window.IPInputScope) {
          params.actionScope = window.IPInputScope;
        }

        HostManageService.fetchHostOfHost(params)
          .then((data) => {
            // 输入的有效 IP
            const resultIPList = [];
            const hostIPMap = {};

            data.forEach((host) => {
              const {
                hostId,
                ip,
                cloudAreaInfo,
              } = host;
              resultIPList.push({ hostId });
              // 记录 IP 和 云区域 ID + IP 组成的检索
              hostIPMap[ip] = true;
              hostIPMap[`${cloudAreaInfo.id}:${ip}`] = true;
            });
            // 提交输入内容
            if (resultIPList.length > 0) {
              this.$emit('on-change', 'ipInput', resultIPList);
            }
            // 正确的 IP 输入，但是 IP 不存于当前业务下
            this.invalidIPList = ipList.reduce((result, IPItem) => {
              if (!hostIPMap[IPItem]) {
                result.push(IPItem);
              }
              return result;
            }, []);
            this.errorIPList = errorIPList;
            // 提交成功后重置用户输入内容的分隔符解析
            this.inputItemList = [];

            this.updateInputValue();
            this.$emit('on-input-change', false);
          })
          .finally(() => {
            this.isSubmiting = false;
          });
      },
    },
  };
</script>
<style lang="postcss">
  .choose-ip-server-ip-input {
    padding: 20px 24px;

    .focus-error {
      textarea {
        &::selection {
          color: #63656e;
          background: #fdd;
        }
      }
    }

    .input-action {
      display: flex;
      align-items: center;
      margin-top: 20px;
    }

    .input-error {
      display: flex;
      margin-right: auto;
      color: #ea3636;

      .error-action {
        font-size: 16px;
        color: #63656e;
        cursor: pointer;

        &:hover {
          color: #3a84ff;
        }
      }
    }

    .submit-btn {
      margin-left: auto;

      &:hover {
        .server-input-number .text {
          color: #3a84ff;
          background: #fff;
        }
      }
    }
  }

  .server-input-number,
  .server-input-number .text {
    transition: transform 0.5s;
  }

  .server-input-number {
    position: relative;
    z-index: 999999;
    display: inline-block;
    height: 17px;
    transition-timing-function: cubic-bezier(0.74, 0.95, 0.81, 0.92);
  }

  .server-input-number .text {
    display: inline-block;
    height: 17px;
    min-width: 17px;
    padding: 0 4px;
    font-size: 12px;
    line-height: 17px;
    color: #fff;
    text-align: center;
    background: #3a84ff;
    border-radius: 8px;
    transition-timing-function: cubic-bezier(0, -1.12, 0.94, -1.07);
  }
</style>
