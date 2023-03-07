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
    class="step-view-global-variable"
    @click="handlerView">
    <div class="flag">
      <icon type="host" />
    </div>
    <div
      class="name"
      :title="name">
      {{ name }}
    </div>
    <jb-dialog
      v-model="isShowDetail"
      :cancel-text="$t('template.关闭')"
      class="global-host-variable-detail-dialog"
      ok-text=""
      :width="1020">
      <div class="variable-title">
        <span>{{ title }}</span>
      </div>
      <div class="content-wraper">
        <empty
          v-if="isEmpty"
          style="height: 100%;"
          :title="$t('template.变量值为空')" />
        <scroll-faker v-else>
          <ip-selector
            readonly
            show-view
            :value="hostNodeInfo" />
        </scroll-faker>
      </div>
    </jb-dialog>
  </div>
</template>
<script>
  import TaskHostNodeModel from '@model/task-host-node';

  import Empty from '@components/empty';
  import ScrollFaker from '@components/scroll-faker';

  import I18n from '@/i18n';

  export default {
    name: 'StepViewGlobalVariable',
    components: {
      ScrollFaker,
      // ServerPanel,
      Empty,
    },
    props: {
      type: {
        type: String,
        default: '',
      },
      name: {
        type: String,
        required: true,
      },
      data: {
        type: Array,
        default: () => [],
      },
    },
    data() {
      const { hostNodeInfo } = new TaskHostNodeModel({});

      return {
        isShowDetail: false,
        hostNodeInfo,
      };
    },
    computed: {
      title() {
        if (this.type) {
          return this.type;
        }
        return `${I18n.t('template.全局变量.label')} - ${this.name}`;
      },
      isEmpty() {
        return TaskHostNodeModel.isHostNodeInfoEmpty(this.hostNodeInfo);
      },
    },
    methods: {
      handlerView() {
        const curVariable = this.data.find(item => item.name === this.name);
        this.hostNodeInfo = Object.freeze(curVariable.defaultTargetValue.hostNodeInfo);

        this.isShowDetail = true;
      },
      handleClose() {
        this.isShowDetail = false;
      },
    },
  };
</script>
<style lang="postcss">
  .global-host-variable-detail-dialog {
    .bk-dialog-tool {
      display: none;
    }

    .bk-dialog-header,
    .bk-dialog-footer {
      position: relative;
      z-index: 99999;
      background: #fff;
    }

    .bk-dialog-header {
      padding: 0;
      border-bottom: 1px solid #dcdee5;
    }

    .bk-dialog-wrapper .bk-dialog-header .bk-dialog-header-inner {
      height: 40px;
      padding-left: 24px;
      font-size: 20px;
      line-height: 40px;
      color: #000;
      text-align: left;
    }

    .variable-title {
      position: relative;
      margin-top: 18px;
      font-size: 20px;
      line-height: 28px;
      color: #313238;
      text-align: left;
    }

    .content-wraper {
      height: 450px;
      max-height: 450px;
      min-height: 450px;
      margin-top: 12px;
    }
  }
</style>
<style lang='postcss' scoped>
  .step-view-global-variable {
    display: inline-flex;
    height: 24px;
    padding-right: 10px;
    line-height: 1;
    cursor: pointer;
    background: #fff;

    .flag {
      display: flex;
      height: 24px;
      font-size: 13px;
      color: #fff;
      background: #3a84ff;
      border-bottom-left-radius: 2px;
      border-top-left-radius: 2px;
      flex: 0 0 24px;
      align-items: center;
      justify-content: center;
    }

    .name {
      display: flex;
      padding: 0 10px;
      white-space: nowrap;
      border: 1px solid #dcdee5;
      border-left: none;
      border-top-right-radius: 2px;
      border-bottom-right-radius: 2px;
      align-items: center;
    }
  }
</style>
