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
  <jb-popover-confirm
    class="step-action-confirm"
    :confirm-handler="handleConfirm"
    :content="confirmInfo.desc"
    :title="confirmInfo.title"
    @cancel="handleConfirmCancel"
    @show="handleConfirmShow">
    <component
      :is="actionCom"
      class="step-instance-action"
      :class="displayStyle" />
  </jb-popover-confirm>
</template>
<script>
  import ActionAllRetry from './all-retry';
  import ActionConfirm from './confirm';
  import ActionConfirmForced from './confirm-forced';
  import ActionConfirmRetry from './confirm-retry';
  import ActionFailIpRetry from './fail-ip-retry';
  import ActionForced from './forced';
  import ActionForcedRetry from './forced-retry';
  import ActionForcedSkip from './forced-skip';
  import ActionNext from './next';
  import ActionSkip from './skip';

  import I18n from '@/i18n';

  const ACTION_FAIL_IP_RETRY = 2;
  const ACTION_SKIP = 3;
  const ACTION_CONFIRM = 6;
  const ACTION_ALL_RETRY = 8;
  const ACTION_CONFIRM_FORCED = 9;
  const ACTION_CONFIRM_RETRY = 10;
  const ACTION_NEXT = 11;
  const ACTION_FORCED_SKIP = 12;

  const actionMap = {
    confirm: {
      operationCode: ACTION_CONFIRM,
      title: I18n.t('history.确定继续执行？'),
      desc: I18n.t('history.将继续执行后面的步骤'),
    },
    confirmForced: {
      operationCode: ACTION_CONFIRM_FORCED,
      title: I18n.t('history.确定终止流程？'),
      desc: I18n.t('history.人工确认步骤终止后，需「重新发起确认」才可恢复'),
    },
    confirmRetry: {
      operationCode: ACTION_CONFIRM_RETRY,
      title: I18n.t('history.确定重新发起确认？'),
      desc: I18n.t('history.将会再次发送消息通知相关的确认人'),
    },
    allRetry: {
      operationCode: ACTION_ALL_RETRY,
      title: I18n.t('history.确定全部重试？'),
      desc: I18n.t('history.该步骤的所有IP 都将重新执行'),
    },
    failIpRetry: {
      operationCode: ACTION_FAIL_IP_RETRY,
      title: I18n.t('history.确定失败IP重试？'),
      desc: I18n.t('history.仅作用于本次执行失败的 IP'),
    },
    skip: {
      operationCode: ACTION_SKIP,
      title: I18n.t('history.确定进入下一步？'),
      desc: I18n.t('history.跳过当前步骤进入下一步'),
    },
    forced: {
      operationCode: '',
      title: I18n.t('history.确定终止执行任务？'),
      desc: I18n.t('history.终止动作仅对当前还未执行完成的IP有效'),
    },
    forcedRetry: {
      operationCode: ACTION_ALL_RETRY,
      title: I18n.t('history.确定重试并继续？'),
      desc: I18n.t('history.该步骤的所有IP 都将重新执行'),
    },
    forcedSkip: {
      operationCode: ACTION_FORCED_SKIP,
      title: I18n.t('history.确定跳过并进入下一步？'),
      desc: I18n.t('history.将不再等待强制终止动作的结果，直接进入下一步'),
    },
    next: {
      operationCode: ACTION_NEXT,
      title: I18n.t('history.确定进入下一步？'),
      desc: I18n.t('history.跳过当前步骤进入下一步'),
    },
  };

  export default {
    props: {
      name: {
        type: String,
        required: true,
      },
      displayStyle: {
        type: String,
        default: 'task',
      },
      confirmHandler: {
        type: Function,
        default: () => {},
      },
    },
    computed: {
      actionCom() {
        const comMap = {
          confirm: ActionConfirm,
          confirmForced: ActionConfirmForced,
          confirmRetry: ActionConfirmRetry,
          allRetry: ActionAllRetry,
          failIpRetry: ActionFailIpRetry,
          skip: ActionSkip,
          forced: ActionForced,
          forcedRetry: ActionForcedRetry,
          forcedSkip: ActionForcedSkip,
          next: ActionNext,
        };
        return comMap[this.name];
      },
      confirmInfo() {
        return actionMap[this.name];
      },
    },
    methods: {
      handleConfirm() {
        return this.confirmHandler(this.confirmInfo.operationCode);
      },
      handleConfirmShow() {
        this.$emit('on-show');
      },
      handleConfirmCancel() {
        this.$emit('on-cancel');
      },
    },
  };
</script>
<style lang='postcss'>
  .step-action-confirm {
    display: inline-flex;
  }

  .step-instance-action {
    display: flex;
    height: 32px;
    padding: 0 12px 0 8px;
    margin-right: 10px;
    font-size: 14px;
    color: #63656e;
    cursor: pointer;
    background: #fff;
    border-radius: 16px;
    align-items: center;
    justify-content: center;

    &:hover {
      i {
        color: currentcolor;
      }
    }

    &.task-detail {
      box-shadow: 0 2px 6px #ddd;
    }

    &.step-detail {
      border: 1px solid #c4c6cc;
    }

    &.stop,
    &.confirm-forced {
      &:hover {
        color: #ea3636;
        border-color: #c4c6cc;
      }
    }

    &.retry,
    &.skip,
    &.next,
    &.confirm {
      &:hover {
        color: #3a84ff;
        border-color: #3a84ff;
      }
    }

    i {
      margin-right: 6px;
      font-size: 16px;
      color: #979ba5;
    }
  }
</style>
