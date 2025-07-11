<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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
    <div class="name">
      <span>{{ data.name }}</span>
      <span
        class="remove-flag"
        @click="handleRemove">
        <icon type="reduce-fill" />
      </span>
    </div>
    <div>
      <bk-button
        v-if="isValueEmpty"
        v-bk-tooltips="descPopover"
        style="width: 160px;"
        @click="handleChooseIp">
        <icon type="plus" />
        {{ $t('template.添加服务器') }}
      </bk-button>
      <div
        v-else
        class="host-value-text"
        @click="handleChooseIp">
        <div class="host-type">
          <icon type="host" />
        </div>
        <div>
          {{ valueText }}
        </div>
        <icon
          class="host-edit"
          type="edit-2" />
      </div>
    </div>
    <jb-ip-selector
      :original-value="originalValue"
      :show-dialog="isShowChooseIp"
      :value="executeObjectsInfo"
      @change="handleExecuteObjectsInfoChange"
      @close-dialog="handleCloseIpSelector" />
  </div>
</template>
<script>
  import ExecuteTargetModel from '@model/execute-target';

  export default {
    props: {
      data: {
        type: Object,
        required: true,
      },
      value: {
        type: Object,
        default: () => new ExecuteTargetModel({}),
      },
    },
    data() {
      return {
        isShowChooseIp: false,
        executeObjectsInfo: {},
      };
    },
    computed: {
      isValueEmpty() {
        return ExecuteTargetModel.isExecuteObjectsInfoEmpty(this.value.executeObjectsInfo);
      },
      valueText() {
        return new ExecuteTargetModel(this.value).text;
      },
      descPopover() {
        return {
          theme: 'light',
          extCls: 'variable-desc-tippy',
          trigger: 'click mouseenter',
          placement: 'left',
          hideOnClick: false,
          content: `<div style="max-width: 340px">${this.data.description}</div>`,
          disabled: !this.data.description,
        };
      },
    },
    created() {
      this.originalValue = ExecuteTargetModel.cloneExecuteObjectsInfo(this.value.executeObjectsInfo);
    },
    methods: {
      handleRemove() {
        this.$emit('on-remove');
      },
      handleChooseIp() {
        this.isShowChooseIp = true;
        this.executeObjectsInfo = this.value.executeObjectsInfo;
      },
      handleCloseIpSelector() {
        this.isShowChooseIp = false;
      },
      handleClear() {
        this.$emit('on-change', new ExecuteTargetModel({}));
      },
      handleExecuteObjectsInfoChange(executeObjectsInfo) {
        this.$emit('on-change', {
          executeObjectsInfo,
        });
      },
    },
  };
</script>
<style lang='postcss' scoped>
  .host-value-text {
    display: flex;
    width: 333px;
    height: 32px;
    overflow: hidden;
    font-size: 12px;
    color: #63656e;
    cursor: pointer;
    background: #fff;
    border: 1px solid #c4c6cc;
    border-radius: 3px;
    transition: all 0.1s;
    align-items: center;

    .host-type {
      display: flex;
      width: 32px;
      height: 32px;
      margin-right: 10px;
      font-size: 17px;
      color: #fff;
      background: #c4c6cc;
      transition: all 0.1s;
      align-items: center;
      justify-content: center;
    }

    .host-edit {
      margin-right: 8px;
      margin-left: auto;
      font-size: 16px;
      color: #3a84ff;
      opacity: 0%;
    }

    &:hover {
      border-color: #3a84ff;

      .host-type {
        background: #3a84ff;
      }

      .host-edit {
        opacity: 100%;
      }
    }
  }
</style>
