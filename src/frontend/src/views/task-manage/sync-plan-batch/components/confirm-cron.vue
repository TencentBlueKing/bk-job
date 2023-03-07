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
  <jb-dialog
    class="plan-confirm-cron-dialog"
    fullscreen
    :value="isShow"
    @cancel="handleClose">
    <div
      v-bkloading="{ isLoading }"
      class="confirm-cron-wraper"
      @keyup.esc="handleClose">
      <confirm-cron
        v-if="!isLoading"
        :cron-job-list="cronJobInfoList"
        :plan-id="planId"
        :template-info="templateInfo"
        @on-change="handleConfirmCron" />
    </div>
  </jb-dialog>
</template>
<script>
  import TaskManageService from '@service/task-manage';

  import ConfirmCron from '../../common/plan/confirm-cron';

  export default {
    name: '',
    components: {
      ConfirmCron,
    },
    props: {
      isShow: {
        type: Boolean,
        default: false,
      },
      templateId: {
        type: Number,
        required: true,
      },
      planId: {
        type: Number,
        required: true,
      },
      cronJobInfoList: {
        type: Array,
        default: () => [],
      },
    },
    data() {
      return {
        isShowDialog: false,
        isLoading: true,
        templateInfo: {},
      };
    },
    watch: {
      templateId(templateId) {
        if (templateId < 0) {
          return;
        }
        this.fetchData();
      },
    },
    methods: {
      fetchData() {
        this.isLoading = true;
        TaskManageService.taskDetail({
          id: this.templateId,
        }).then((data) => {
          this.templateInfo = Object.freeze(data);
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      handleClose() {
        this.$emit('on-close');
      },
      handleConfirmCron(cronJonList) {
        this.$emit('on-change', cronJonList);
      },
    },
  };
</script>
<style lang='postcss'>
  .plan-confirm-cron-dialog {
    .bk-dialog-wrapper {
      .bk-dialog-body {
        padding: 0;
      }

      .bk-dialog.bk-dialog-fullscreen .bk-dialog-body {
        bottom: 0;
      }

      .bk-dialog-footer {
        display: none;
      }
    }

    .confirm-cron-wraper {
      min-height: 100vh;
    }

    .layout-left,
    .layout-right {
      height: 100vh;
    }
  }
</style>
