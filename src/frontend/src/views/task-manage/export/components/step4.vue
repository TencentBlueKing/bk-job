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
    class="export-task-step4-page">
    <div>
      <div class="notice">
        <img src="/static/images/export.svg">
        <div class="title">
          <div v-if="isExportSuccess">
            <div>{{ $t('template.作业导出成功！请及时保存并妥善保管。') }}</div>
            <div class="export-download">
              <span>{{ $t('template.如果页面未出现提示保存窗口，请点击') }}</span>
              <span
                class="btn"
                @click="handleDownloadFile">{{ $t('template.重新下载文件') }}</span>
            </div>
          </div>
          <div v-else>
            {{ $t('template.正在导出作业，请稍候') }}<span class="loading" />
          </div>
        </div>
      </div>
      <div class="log-content">
        <div
          v-for="(item, index) in logList"
          :key="index">
          [{{ item.timestamp }}] {{ item.content }}
        </div>
      </div>
    </div>
    <div class="action-footer">
      <bk-button
        class="w120"
        :disabled="!isExportSuccess"
        :loading="isFinishing"
        theme="primary"
        @click="handleFinish">
        {{ $t('template.完成') }}
      </bk-button>
    </div>
  </div>
</template>
<script>
  import BackupService from '@service/backup';

  import {
    taskExport,
  } from '@utils/cache-helper';

  import I18n from '@/i18n';

  const TASK_STATUS_DEFAULT = 0;
  const TASK_STATUS_DOING = 5;
  const TASK_STATUS_SUCCESS = 6;
  const TASK_STATUS_FAILED = 7;
  const TASK_STATUS_CANCEL = 8;

  export default {
    name: 'Exporting',
    data() {
      return {
        isLoading: true,
        isFinishing: false,
        isConfirmLoading: false,
        id: taskExport.getItem('id') || '',
        logList: [],
        status: TASK_STATUS_DEFAULT,
      };
    },
    computed: {
      isExportSuccess() {
        return [
          TASK_STATUS_SUCCESS,
          TASK_STATUS_FAILED,
          TASK_STATUS_CANCEL,
        ].includes(this.status);
      },
    },

    created() {
      // 自定义路由切换确认框
      this.$route.meta.leavaConfirm = () => new Promise((resolve, reject) => {
        if (!window.changeFlag) {
          resolve();
          return;
        }
        let confirmDialog = null;
        const keepCallback = () => {
          window.changeFlag = false;
          resolve();
          confirmDialog.close();
          setTimeout(() => {
            this.$emit('on-cancle');
          });
        };
        const finishCallback = () => {
          // 导出完成——完成任务
          // 导出进行中——终止任务
          const requestHandler = this.isExportSuccess
            ? BackupService.updateExportComplete
            : BackupService.exportDelete;
          this.$refs.confirmBtn.loading = true;
          requestHandler({
            id: this.id,
          }).then(() => {
            window.changeFlag = false;
            resolve();
            confirmDialog.close();
            setTimeout(() => {
              this.$emit('on-cancle');
            });
          })
            .finally(() => {
              this.$refs.confirmBtn.loading = false;
            });
        };

        confirmDialog = this.$bkInfo({
          title: I18n.t('template.是否结束当前任务？'),
          subHeader: (() => (
                    <div>
                        <div style={{ color: '#63656e', fontSize: '14px', textAlign: 'center' }}>
                            <p>{I18n.t('template.你可以选择保留或结束当前任务')}</p>
                            <p>{I18n.t('template.（选择保留，后续可以从列表页重新进入）')}</p>
                        </div>
                        <div style={{ padding: '20px 0', textAlign: 'center' }}>
                            <bk-button class="mr10" theme="primary" onClick={keepCallback}>
                                {I18n.t('template.保留')}
                            </bk-button>
                            <bk-button ref="confirmBtn" onClick={finishCallback}>
                                {I18n.t('template.结束')}
                            </bk-button>
                        </div>
                    </div>
          ))(),
          showFooter: false,
        });
      });

      // 自动下载导出文件
      // 从第3步跳转过来的会有 templateInfo 缓存默认自动下载文件
      // 通过url访问时不会有 templateInfo 缓存默认不自动下载文件
      this.autoLoadPackage = taskExport.getItem('templateInfo');

      this.pollingQueue = [];
      taskExport.clearItem();
      window.changeFlag = true;
      this.fetchData();
      this.startTimer();
      this.$once('hook:beforeDestroy', () => {
        this.clearTimer();
      });
    },

    methods: {
      fetchData() {
        if (!this.id) {
          this.isLoading = false;
          return;
        }
        BackupService.fetchExportInfo({
          id: this.id,
        }).then((data) => {
          if (this.isClearTimer) {
            return;
          }
          this.logList = Object.freeze(data.log);
          this.status = data.status;
          if (this.status === TASK_STATUS_SUCCESS && this.autoLoadPackage) {
            this.handleDownloadFile();
            return;
          }
          if ([
            TASK_STATUS_DEFAULT,
            TASK_STATUS_DOING,
          ].includes(this.status)) {
            // 任务正在执行中——自动下载文件
            this.autoLoadPackage = true;
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
        const nextTask = this.pollingQueue.pop();
        if (nextTask) {
          nextTask();
        }
        setTimeout(() => {
          this.startTimer();
        }, 2000);
      },

      clearTimer() {
        this.isClearTimer = true;
      },

      handleDownloadFile() {
        BackupService.fetchExportFile({
          id: this.id,
        }).then(() => {
          this.messageSuccess(I18n.t('template.下载文件成功'));
        });
      },

      handleFinish() {
        this.isFinishing = true;
        BackupService.updateExportComplete({
          id: this.id,
        }).then(() => {
          window.changeFlag = false;
          this.$emit('on-cancle');
        })
          .finally(() => {
            this.isFinishing = false;
          });
      },
    },
  };
</script>
<style lang="postcss">
  @keyframes export-task-loading {
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

  .export-task-step4-page {
    .notice {
      margin-top: 50px;
      margin-bottom: 16px;
      text-align: center;

      img {
        width: 143px;
      }

      .title {
        font-size: 24px;
        color: #63656e;

        .loading {
          &::after {
            display: inline-block;
            content: ".";
            animation: export-task-loading 2s linear infinite;
          }
        }
      }

      .export-download {
        margin-top: 10px;
        font-size: 14px;
        color: #63656e;

        .btn {
          color: #3a84ff;
          cursor: pointer;
        }
      }
    }

    .log-content {
      width: 680px;
      max-height: calc(100vh - 475px);
      padding: 12px 16px;
      margin: 30px auto 0;
      overflow-y: auto;
      font-size: 12px;
      line-height: 20px;
      color: #979ba5;
      background-color: #fafbfd;
      border: 1px solid #dcdee5;
      border-radius: 2px;

      &::-webkit-scrollbar {
        width: 13px;
      }

      &::-webkit-scrollbar-thumb {
        background-color: #dcdee5;
      }
    }
  }
</style>
