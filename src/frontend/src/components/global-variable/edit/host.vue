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
    class="variable-type-host"
    :class="{ 'variable-value-error': isError }">
    <div>
      <div>
        <bk-button
          v-bk-tooltips="descPopover"
          class="mr10"
          :disabled="readonly"
          @click="handleChooseIp">
          <icon type="plus" />
          {{ $t('添加服务器') }}
        </bk-button>
        <bk-button
          v-show="isNotEmpty"
          :disabled="readonly"
          @click="handleClear">
          {{ $t('清空') }}
        </bk-button>
      </div>
      <ip-selector
        ref="ipSelector"
        :original-value="originalExecuteObjectsInfo"
        :show-dialog="isShowChooseIp"
        show-view
        :show-view-diff="showViewDiff"
        :value="executeObjectsInfo"
        @change="handleChange"
        @close-dialog="handleCloseIpSelector" />
      <p
        v-if="isError"
        class="variable-error">
        {{ $t('该变量的值必填') }}
      </p>
    </div>
  </div>
</template>

<script>
  import ExecuteTargetModel from '@model/execute-target';

  import {
    removeIllegalHostFromExecuteObjectsInfo,
  } from '@utils/assist';

  export default {
    props: {
      data: {
        type: Object,
        required: true,
      },
      readonly: {
        type: Boolean,
        default: false,
      },
      showViewDiff: {
        type: Boolean,
        default: false,
      },
      withValidate: {
        type: Boolean,
        default: true,
      },
    },
    data() {
      return {
        isShowChooseIp: false,
        executeObjectsInfo: {},
        originalExecuteObjectsInfo: {},
      };
    },
    computed: {
      isNotEmpty() {
        return !ExecuteTargetModel.isExecuteObjectsInfoEmpty(this.executeObjectsInfo);
      },
      isError() {
        if (this.data.required !== 1) {
          return false;
        }
        return !this.isNotEmpty;
      },
      descPopover() {
        return {
          theme: 'light',
          extCls: 'variable-desc-tippy',
          trigger: 'click mouseenter',
          hideOnClick: false,
          content: this.data.description,
          disabled: !this.data.description,
        };
      },
    },
    watch: {

      data: {
        handler() {
          this.init();
        },
        immediate: true,
      },
    },
    methods: {
      /**
       * @desc 解析默认值
       */
      init() {
        if (!this.data.defaultTargetValue.isEmpty) {
          this.executeObjectsInfo = this.data.defaultTargetValue.executeObjectsInfo;
        } else {
          this.executeObjectsInfo = this.data.targetValue.executeObjectsInfo;
        }
        this.originalExecuteObjectsInfo = Object.freeze({
          ...this.executeObjectsInfo,
        });
      },
      /**
       * @desc 外部调用——移除无效主机
       */
      removeAllInvalidHost() {
        window.changeFlag = true;
        this.$refs.ipSelector.removeInvalidData();
      },
      /**
       * @desc 编辑主机列表
       */
      handleChooseIp() {
        this.isShowChooseIp = true;
      },
      handleCloseIpSelector() {
        this.isShowChooseIp = false;
      },
      /**
       * @desc 清空主机列表
       */
      handleClear() {
        const { executeObjectsInfo } = new ExecuteTargetModel({});
        this.executeObjectsInfo = executeObjectsInfo;
        window.changeFlag = true;
      },
      /**
       * @desc 提交编辑的数据
       */
      handleChange(executeObjectsInfo) {
        this.executeObjectsInfo = Object.freeze(executeObjectsInfo);
        window.changeFlag = true;
      },
      /**
       * @desc 外部调用——还原默认值
       */
      reset() {
        this.init();
      },
      /**
       * @desc 外部调用——移除无效主机
       */
      removeIllegalHost() {
        this.executeObjectsInfo = Object.freeze(removeIllegalHostFromExecuteObjectsInfo(this.executeObjectsInfo));
        this.originalExecuteObjectsInfo = Object.freeze({
          ...this.executeObjectsInfo,
        });
      },
      /**
       * @desc 外部调用——值验证
       * @returns {Promise}
       */
      validate() {
        const { type, id, name } = this.data;

        return new Promise((resolve, reject) => {
          if (this.withValidate && this.isError) {
            return reject(new Error('host error'));
          }
          resolve({
            id,
            name,
            type,
            value: '',
            targetValue: {
              executeObjectsInfo: this.executeObjectsInfo,
            },
          });
        });
      },
    },
  };
</script>
<style lang='postcss' scoped>
  .variable-type-host {
    .host-value-panel {
      margin-top: 10px;
    }
  }

</style>
