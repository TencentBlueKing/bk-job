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
    v-bkloading="{ isLoading }"
    class="task-import-step5">
    <div class="flag">
      <img src="/static/images/import.svg">
    </div>
    <div class="title">
      <span v-if="isImportSuccess">{{ $t('template.作业导入完成！请及时检查。') }}</span>
      <template v-else>
        <span v-if="isImportFailed">{{ $t('template.作业导入出现异常，请稍后再试...') }}</span>
        <span v-else>{{ $t('template.正在导入作业，请稍候') }}<span class="loading" /></span>
      </template>
    </div>
    <div class="log-container">
      <div
        ref="log"
        class="log-box">
        <div
          v-for="(item, index) in log"
          :key="index">
          <span>[ {{ item.timestamp }} ]</span> {{ item.content }}
          <span
            v-if="item.type === 4"
            class="action"
            @click="handleLocationTemplate(item)">
            {{ $t('template.查看详情') }}
            <icon type="jump" />
          </span>
          <span
            v-if="item.type === 5"
            class="action"
            @click="handleLocationPlan(item)">
            {{ $t('template.查看详情') }}
            <icon type="jump" />
          </span>
        </div>
      </div>
      <icon
        class="log-copy"
        :tippy-tips="$t('template.复制日志')"
        type="log-copy"
        @click="handleCopyLog" />
    </div>
    <action-bar>
      <bk-button
        class="w120"
        :disabled="!isImportSuccess"
        theme="primary"
        @click="handleFinish">
        {{ $t('template.完成') }}
      </bk-button>
    </action-bar>
  </div>
</template>
<script>
  import BackupService from '@service/backup';

  import { execCopy } from '@utils/assist';
  import { taskImport } from '@utils/cache-helper';

  import ActionBar from '../components/action-bar';

  const TASK_STATUS_DEFAULT = 0;
  const TASK_STATUS_PENDING = 5;
  const TASK_STATUS_SUCCESS = 6;
  const TASK_STATUS_FAILED = 7;
  const TASK_STATUS_CANCEL = 8;

  export default {
    name: '',
    components: {
      ActionBar,
    },
    data() {
      return {
        isLoading: true,
        status: 0,
        log: [],
      };
    },
    computed: {
      isImportSuccess() {
        return [
          TASK_STATUS_SUCCESS,
          TASK_STATUS_CANCEL,
        ].includes(this.status);
      },
      isImportFailed() {
        return [
          TASK_STATUS_FAILED,
        ].includes(this.status);
      },
    },
    created() {
      this.id = taskImport.getItem('id');
      this.pollingQueue = [];
      taskImport.clearItem();
      this.fetchData();
      this.startTimer();
      this.$once('hook:beforeDestroy', () => {
        this.clearTimer();
      });
    },
    methods: {
      fetchData() {
        BackupService.fetchImportInfo({
          id: this.id,
        }).then((data) => {
          if (this.isClearTimer) {
            return;
          }
          this.log = Object.freeze(data.log);
          this.status = data.status;
          if ([
            TASK_STATUS_DEFAULT,
            TASK_STATUS_PENDING,
          ].includes(data.status)) {
            this.pollingQueue.push(this.fetchData);
          }
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      startTimer() {
        if (this.isClearTimer) {
          return;
        }
        const lastTask = this.pollingQueue.pop();
        if (lastTask) {
          lastTask();
        }
        setTimeout(() => {
          this.startTimer();
        }, 2000);
      },
      clearTimer() {
        this.isClearTimer = true;
      },
      handleLocationTemplate(payload) {
        const { href } = this.$router.resolve({
          name: 'templateDetail',
          params: {
            id: payload.templateId,
          },
        });
        window.open(href);
      },
      handleLocationPlan(payload) {
        const { href } = this.$router.resolve({
          name: 'viewPlan',
          params: {
            templateId: payload.templateId,
          },
          query: {
            viewPlanId: payload.planId,
          },
        });
        window.open(href);
      },
      handleCopyLog() {
        execCopy(this.$refs.log.innerText);
      },
      handleFinish() {
        this.$emit('on-cancle');
      },
    },
  };
</script>
<style lang='postcss'>
  @keyframes import-task-loading {
    0% {
      content: ".";
    }

    30% {
      content: "..";
    }

    60% {
      content: "...";
    }
  }

  .task-import-step5 {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-height: 100%;

    .flag {
      margin-top: 50px;
      margin-bottom: 16px;

      img {
        width: 143px;
      }
    }

    .title {
      font-size: 24px;
      line-height: 31px;
      color: #63656e;

      .loading {
        &::after {
          display: inline-block;
          content: ".";
          animation: import-task-loading 2s linear infinite;
        }
      }
    }

    .log-container {
      position: relative;
      margin-top: 30px;
    }

    .log-box {
      width: 680px;
      max-height: calc(100vh - 460px);
      padding: 12px 16px;
      overflow-y: auto;
      font-size: 12px;
      line-height: 20px;
      color: #979ba5;
      background: #fafbfd;
      border: 1px solid #dcdee5;
      border-radius: 2px;

      &::-webkit-scrollbar {
        width: 13px;
      }

      &::-webkit-scrollbar-thumb {
        background-color: #dcdee5;
      }

      .action {
        color: #3a84ff;
        cursor: pointer;
      }
    }

    .log-copy {
      position: absolute;
      top: 10px;
      right: 10px;
      font-size: 16px;
      color: #979ba5;
      cursor: pointer;
    }
  }
</style>
