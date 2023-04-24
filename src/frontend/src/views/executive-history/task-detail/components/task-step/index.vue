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
  <component
    :is="themeCom"
    v-bind="$attrs"
    :choose="handleChoose"
    :data="data"
    :handle-change-status="handleChangeStatus" />
</template>
<script>
  import TaskExecuteService from '@service/task-execute';

  import StepAction from '../../../common/step-action';

  import ThemeApproval from './theme/approval';
  import ThemeNormal from './theme/normal';

  import I18n from '@/i18n';

  export default {
    name: 'TaskStep',
    components: {
      StepAction,
    },
    props: {
      data: {
        type: Object,
        required: true,
      },
    },
    computed: {
      themeCom() {
        if (this.data.isApproval && !this.data.isNotStart) {
          return ThemeApproval;
        }
        return ThemeNormal;
      },
    },
    methods: {
      handleChoose() {
        if (this.data.isApproval) {
          return;
        }
        if (this.data.isNotStart) {
          this.$bkMessage({
            theme: 'warning',
            message: I18n.t('history.该步骤还未执行'),
            limit: 1,
          });
          return;
        }

        this.$emit('on-select', this.data);
      },
      handleChangeStatus(operationCode, confirmReason) {
        return TaskExecuteService.updateTaskExecutionStepOperate({
          id: this.data.stepInstanceId,
          operationCode,
          confirmReason,
        }).then(() => {
          this.$bkMessage({
            limit: 1,
            theme: 'success',
            message: I18n.t('history.操作成功'),
          });
          this.$emit('on-update');
          return true;
        });
      },
    },
  };
</script>
<style lang="postcss">
  @keyframes ani-rotate {
    to {
      transform: rotateZ(360deg);
    }
  }

  .execution-step-box {
    position: relative;
    z-index: 1;
    display: flex;
    height: 42px;
    padding: 0 4px;
    margin: 14px 0;
    margin-left: 16px;
    background: #fff;
    border: 1px solid transparent;
    border-radius: 21px;
    box-shadow: 0 2px 6px #ddd;
    align-items: center;

    &.theme-start,
    &.theme-end {
      width: 44px;
    }

    &.theme-end {
      &.disabled {
        background: #f5f7fa;
        border: 1px solid #dcdee5;

        .step-icon {
          color: #f0f1f5;
          background-color: #dcdee5;
        }
      }
    }

    &.theme-normal {
      width: 546px;
      cursor: pointer;

      .theme-normal-wraper {
        position: relative;
        display: flex;
        height: 100%;
        align-items: center;
        flex: 1;

        &:hover {
          .step-desc {
            color: #3a84ff;
          }

          .step-info {
            margin-right: -20px;
            opacity: 100%;
            visibility: visible;
          }
        }

        .step-status-desc {
          margin-right: 30px;
          margin-left: auto;

          .time {
            color: #979ba5;
          }
        }
      }

      .name-text {
        max-width: 390px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .step-info {
        position: absolute;
        top: 50%;
        right: 0;
        z-index: 99;
        display: flex;
        max-width: 390px;
        padding: 8px 11px;
        margin-right: 0;
        font-size: 12px;
        line-height: 16px;
        color: #63656e;
        background: #fff;
        border: 1px solid #dcdee5;
        border-radius: 4px;
        opacity: 0%;
        visibility: hidden;
        transform: translate(100%, -50%);
        box-shadow: 0 0 5px 0 rgb(0 0 0 / 9%);
        transition: all 0.15s;
        user-select: none;
        flex-direction: column;

        &::before {
          position: absolute;
          top: 50%;
          left: -2px;
          width: 11px;
          height: 11px;
          background: #fff;
          border-top: 1px solid #dcdee5;
          border-left: 1px solid #dcdee5;
          content: "";
          transform: rotateZ(-45deg) translateY(-50%);
        }

        &::after {
          position: absolute;
          top: 0;
          width: 30px;
          height: 100%;
          margin-left: -30px;
          content: "";
        }

        .jb-detail-layout {
          .detail-item {
            margin-bottom: 0;
            font-size: 12px;
            line-height: 16px;
          }

          .detail-content {
            word-break: break-all;
          }
        }
      }

      .step-instance-action {
        box-shadow: 0 2px 6px 0 rgb(0 0 0 / 6%);
      }
    }

    &.theme-approval {
      display: block;
      height: auto;
      padding: 20px;
      margin-left: 0;

      &.success {
        .confirm-flag {
          display: block;
        }
      }

      .step-icon {
        margin-right: 18px;
      }

      .step-desc {
        margin-left: 0;
        font-size: 18px;
        color: #313238;
        align-items: center;
      }

      .approval-container {
        padding: 0 52px;
      }

      .approval-info {
        display: flex;
        margin-top: -2px;
        font-size: 12px;
        line-height: 20px;
        color: #b2b5bd;
        align-items: center;
        flex-wrap: wrap;
      }

      .approval-person {
        display: flex;
        flex-wrap: wrap;

        .persion-label,
        .person {
          margin-top: 10px;
        }

        .role-flag {
          margin-right: 4px;
        }
      }

      .person {
        display: inline-flex;
        height: 22px;
        padding: 0 6px;
        margin-right: 6px;
        font-size: 12px;
        color: #63656e;
        background: #f0f1f5;
        border-radius: 2px;
        align-items: center;
      }

      .approval-channel {
        margin-top: 10px;
      }

      .confirm-reason,
      .confirm-reason-text {
        margin-top: 20px;
      }

      .confirm-reason-text {
        font-size: 14px;
        line-height: 22px;
        color: #63656e;
      }

      .confirm-flag {
        display: none;
        height: 18px;
        padding: 0 5px;
        margin-left: 8px;
        font-size: 12px;
        line-height: 18px;
        color: #2dcb9d;
        background: rgb(45 203 157 / 14%);
        border-radius: 2px;
      }

      .step-message {
        padding-top: 14px;
        font-size: 14px;
        line-height: 26px;
        color: #63656e;
      }

      .step-action {
        position: relative;
        left: unset;
        height: auto;
        margin-top: 20px;
        justify-content: flex-end;

        .step-instance-action {
          margin-right: 0;
          margin-left: 10px;
          border: 1px solid #c4c6cc;

          &:hover {
            border-color: currentcolor;
          }
        }
      }

      .step-process {
        left: 36px;
      }
    }

    &.loading {
      color: #63656e;

      .step-icon {
        background: #3a84ff;
      }

      .step-desc {
        .time {
          color: #3a84ff;
        }
      }

      .loading-progress {
        display: block;
        animation: "ani-rotate" 2s linear infinite;
      }
    }

    &.ingore {
      .step-icon {
        background: #abd88a;
      }
    }

    &.disabled {
      color: #c4c6cc;
      cursor: default;
      background: #f5f7fa;
      border-color: #dcdee5;
      box-shadow: none;
      user-select: none;

      &:hover {
        .step-desc {
          color: #c4c6cc !important;
        }
      }

      .step-desc {
        color: #c4c6cc;
      }

      .step-icon {
        background: #dcdee5;
      }
    }

    &.fail,
    &.forced,
    &.confirm-forced {
      color: #979ba5;

      .theme-normal-wraper {
        &:hover {
          .step-desc {
            .time {
              display: block;
            }
          }

          .step-error-flag {
            display: none;
          }
        }
      }

      .step-icon {
        background: #ff5656;
      }

      .step-desc {
        .time {
          display: none;
        }
      }

      .step-error-flag {
        font-size: 18px;
        color: #ff5656;
      }
    }

    &.confirm {
      color: #979ba5;

      .step-icon {
        background: #ff9c01;
      }
    }

    .step-icon {
      display: flex;
      height: 34px;
      font-size: 14px;
      font-weight: 600;
      color: #fff;
      background: #2dcb9d;
      border-radius: 50%;
      align-items: center;
      justify-content: center;
      flex: 0 0 34px;
    }

    .step-desc {
      display: flex;
      margin-left: 18px;
      font-size: 14px;
      color: #979ba5;
      flex: 1;
    }

    .step-process {
      position: absolute;
      bottom: -28px;
      left: 21px;
      z-index: 1;
      font-size: 31px;
      color: #c4c6cc;
      transform: translateX(-50%);

      &.step-pending {
        bottom: -31px;
        left: 20px;
        z-index: -1;
      }
    }

    .loading-progress {
      position: absolute;
      top: 0;
      left: 1px;
      width: 40px;
      height: 40px;
    }

    .step-action {
      position: absolute;
      left: 566px;
      display: flex;
      height: 100%;
      white-space: pre;
      align-items: center;
    }
  }
</style>
