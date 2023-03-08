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
  <div class="task-import-step2">
    <div class="package-upload">
      <bk-upload
        v-if="!uploadFilename"
        :custom-request="handleUploadRequest"
        :multiple="false"
        :size="Infinity"
        :tip="$t('template.仅支持上传来自作业平台专属导出的压缩包')"
        url="/" />
      <div
        v-if="uploadFilename"
        class="upload-result"
        :class="uploadStatus">
        <div class="file-pic">
          <img src="/static/images/package.svg">
        </div>
        <div class="file-info">
          <div class="file-name">
            {{ uploadFilename }}
          </div>
          <div
            v-if="uploadStatus === 'waiting'"
            class="upload-progress">
            <div
              class="progress-bar"
              :style="{ width: uploadProgress }" />
          </div>
          <div
            v-if="uploadStatus === 'success'"
            class="file-status">
            {{ $t('template.上传成功') }}
          </div>
          <div
            v-if="uploadStatus === 'failed'"
            class="file-status">
            {{ $t('template.上传失败') }}
          </div>
          <div
            v-if="uploadStatus === 'error'"
            class="file-status">
            {{ uploadErrorMsg }}
          </div>
        </div>
        <icon
          v-if="uploadStatus === 'failed'"
          class="file-refresh"
          type="refresh"
          @click="handleFileRefresh" />
        <icon
          class="file-delete"
          type="close-big"
          @click="hanleFileDelete" />
      </div>
    </div>
    <div
      v-if="isShowLog"
      v-bkloading="{ isLoading: isLogLoading }"
      class="upload-log-box"
      @click="handleLogAction">
      <div
        v-for="(item, index) in importInfo.log"
        :key="index"
        v-html="renderLogRow(item, index, importInfo.log)" />
    </div>
    <action-bar>
      <bk-button
        class="mr10"
        @click="handleCancel">
        {{ $t('template.取消') }}
      </bk-button>
      <bk-button
        class="mr10"
        @click="handleLast">
        {{ $t('template.上一步') }}
      </bk-button>
      <bk-button
        class="w120"
        :disabled="!isUploadSuccess"
        theme="primary"
        @click="handleNext">
        {{ $t('template.下一步') }}
      </bk-button>
    </action-bar>
    <lower-component
      :custom="isShowPassword"
      level="custom">
      <jb-dialog
        v-model="isShowPassword"
        class="setting-password-dialog"
        :esc-close="false"
        header-position="left"
        :mask-close="false"
        render-directive="if"
        :title="$t('template.文件包解密确认')"
        :width="480">
        <jb-form
          ref="passwordForm"
          form-type="vertical"
          :model="passwordFormData"
          :rules="rules">
          <jb-form-item
            :label="$t('template.文件包密码')"
            property="password"
            required>
            <bk-input
              v-model="passwordFormData.password"
              :native-attributes="{ autofocus: 'autofocus' }"
              :placeholder="$t('template.请输入上传的文件包密码，完成后点提交验证')"
              type="password"
              @keydown="handleEnter" />
          </jb-form-item>
        </jb-form>
        <div
          slot="footer"
          class="setting-password-footer">
          <bk-button
            class="mr10"
            :loading="isPasswordSubmiting"
            theme="primary"
            @click="handleSubmitPassword">
            {{ $t('template.提交') }}
          </bk-button>
          <bk-button @click="handleClosePassword">
            {{ $t('template.取消') }}
          </bk-button>
        </div>
      </jb-dialog>
    </lower-component>
  </div>
</template>
<script>
  import BackupService from '@service/backup';

  import { prettyDateTimeFormat } from '@utils/assist';
  import { taskImport } from '@utils/cache-helper';

  import ActionBar from '../components/action-bar';

  import I18n from '@/i18n';

  const escapeHTML = str => str.replace(/&/g, '&#38;').replace(/"/g, '&#34;')
    .replace(/'/g, '&#39;')
    .replace(/</g, '&#60;');

  const TASK_STATUS_DEFAULT = 0;
  const TASK_STATUS_PACKAGE_PARSE_SUCCESS = 1;
  const TASK_STATUS_NEED_PASSWORD = 2;
  const TASK_STATUS_ERROR_PASSWORD = 3;
  const TASK_STATUS_PENDING = 5;
  const TASK_STATUS_CANCEL = 8;

  const LOG_TYPE_TASK_EXPIRE = 2;
  const LOG_TYPE_NEED_PASSWORD = 6;
  const LOG_TYPE_ERROR_PASSWORD = 7;

  export default {
    name: '',
    components: {
      ActionBar,
    },
    data() {
      return {
        isShowPassword: false,
        isShowLog: false,
        isLogLoading: false,
        isPasswordSubmiting: false,
        id: '',
        importInfo: {
          id: '',
          log: [],
          status: 0,
          templateInfo: [],
        },
        passwordFormData: {
          password: '',
        },
        uploadProgress: '0',
        uploadFilename: '',
        uploadStatus: '', // waiting: 上传中；failed: 上传失败；success: 上传成功；
      };
    },

    computed: {
      isUploadSuccess() {
        return this.importInfo.status === TASK_STATUS_PACKAGE_PARSE_SUCCESS;
      },
    },

    created() {
      this.fileMemo = null;
      this.uploadErrorMsg = '';
      this.pollingQueue = [];
      this.rules = {
        password: [
          { required: true, message: I18n.t('template.请输入文件包密码'), trigger: 'blur' },
        ],
      };
      this.uploadRequestCancelSource = null;
      const { id, uploadFilename } = taskImport.getItem() || {};
      if (id && uploadFilename) {
        this.uploadFilename = uploadFilename;
        this.uploadStatus = 'success';
        this.importInfo.status = TASK_STATUS_PACKAGE_PARSE_SUCCESS;
        this.id = id;
        this.fetchImportInfo();
        this.startTimer();
      }
    },

    beforeDestroy() {
      this.clearTimer();
    },

    methods: {
      fetchImportInfo() {
        this.isShowLog = true;
        this.$request(BackupService.fetchImportInfo({
          id: this.id,
        }), () => {
          this.isLogLoading = true;
        }).then((data) => {
          if (this.isClearTimer) {
            return;
          }
          this.importInfo = Object.freeze(data);
          if (data.status === TASK_STATUS_NEED_PASSWORD) {
            // 需要密码自动弹框
            this.handleInputPassword();
            return;
          }
          if (data.status === TASK_STATUS_ERROR_PASSWORD) {
            // 密码错误
            return;
          }
          if ([
            TASK_STATUS_DEFAULT,
            TASK_STATUS_PENDING,
            TASK_STATUS_CANCEL,
          ].includes(data.status)) {
            // 循环获取日志
            this.pollingQueue.push(this.fetchImportInfo);
          }
        })
          .finally(() => {
            this.isLogLoading = false;
          });
      },

      renderLogRow(row, index, list) {
        // eslint-disable-next-line max-len
        const logContent = `<span class="log-header">[ ${escapeHTML(row.timestamp)} ]</span> ${escapeHTML(row.content)}`;
        const errorTypeMap = {
          // eslint-disable-next-line max-len
          [LOG_TYPE_NEED_PASSWORD]: `<span class="action" data-action="passwordRetry">${I18n.t('template.输入密码')}</span>`,
          // eslint-disable-next-line max-len
          [LOG_TYPE_ERROR_PASSWORD]: `<span class="action" data-action="passwordRetry">${I18n.t('template.重新输入密码')}</span>`,
        };
        if (row.type === LOG_TYPE_TASK_EXPIRE) {
          return `<span class="error">${logContent}</span>`;
        }
        if (index === list.length - 1 && errorTypeMap[row.type]) {
          return `<span class="error">${logContent}${errorTypeMap[row.type]}</span>`;
        }
        if (index === list.length - 1 && this.isUploadSuccess) {
          // eslint-disable-next-line max-len
          return `<span>${logContent}${I18n.t('template.请点')}<span class="action" data-action="success">${I18n.t('template.下一步')}</span></span>`;
        }
        return logContent;
      },

      startTimer() {
        if (this.isClearTimer) {
          return;
        }
        const currentTask = this.pollingQueue.pop();
        if (currentTask) {
          currentTask();
        }
        setTimeout(() => {
          this.startTimer();
        }, 1000);
      },

      clearTimer() {
        this.isClearTimer = true;
      },

      handleUploadRequest(option) {
        this.uploadFilename = option.fileObj.name;

        if (!/\.jobexport$/.test(option.fileObj.name)) {
          this.uploadStatus = 'error';
          this.uploadErrorMsg = I18n.t('template.文件类型不支持');
          return;
        }
        this.fileMemo = option;
        this.isShowLog = false;
        this.uploadStatus = 'waiting';
        const formData = new FormData();
        formData.append('uploadFile', option.fileObj.origin);

        BackupService.uploadImportFile(formData, {
          onUploadProgress: (event) => {
            const { loaded, total } = event;
            this.uploadProgress = `${Math.ceil(loaded / total * 100)}%`;
          },
          setCancelSource: (source) => {
            this.uploadRequestCancelSource = source;
          },
        }).then((data) => {
          this.id = data.id;
          taskImport.setItem('id', data.id);
          taskImport.setItem('uploadFilename', this.uploadFilename);
          this.fetchImportInfo();
          this.startTimer();
          this.uploadStatus = 'success';
        })
          .catch(() => {
            this.uploadStatus = 'failed';
          });
      },

      handleFileRefresh() {
        this.handleUploadRequest(this.fileMemo);
      },

      hanleFileDelete() {
        if (this.uploadRequestCancelSource) {
          this.uploadRequestCancelSource.cancel(I18n.t('template.上传任务已取消'));
        }
        this.uploadStatus = '';
        this.uploadFilename = '';
        this.id = '';
        this.importInfo = {
          id: '',
          log: [],
          status: 0,
          templateInfo: [],
        };
        this.isShowLog = false;
        taskImport.clearItem();
      },
      handleInputPassword() {
        this.passwordFormData.password = '';
        this.isShowPassword = true;
      },
      handleClosePassword() {
        this.isShowPassword = false;
        window.changeFlag = false;
        this.$refs.passwordForm.clearError();
        this.importInfo.log.push({
          content: I18n.t('template.导入任务已取消！需要重试请点击'),
          linkText: null,
          linkUrl: null,
          planId: null,
          templateId: null,
          timestamp: prettyDateTimeFormat(Date.now()),
          type: LOG_TYPE_ERROR_PASSWORD,
        });
      },
      handleEnter(value, event) {
        if (event.isComposing) {
          // 跳过输入发复合时间
          return;
        }
        if (event.keyCode !== 13) {
          // 非enter键
          return;
        }
        this.handleSubmitPassword();
      },
      handleSubmitPassword() {
        this.isPasswordSubmiting = true;
        this.$refs.passwordForm.validate()
          .then(() => BackupService.checkImportPassword({
            ...this.passwordFormData,
            id: this.id,
          }).then(() => {
            this.isShowPassword = false;
            window.changeFlag = false;
            this.fetchImportInfo();
          }))
          .finally(() => {
            this.isPasswordSubmiting = false;
          });
      },
      handleLogAction(event) {
        const $target = event.target;
        if (!$target.classList.contains('action')) {
          return;
        }
        const actionType = $target.getAttribute('data-action');
        switch (actionType) {
        case 'passwordRetry':
          this.handleInputPassword();
          break;
        case 'success':
          this.handleNext();
          break;
        default:
        }
      },
      handleCancel() {
        this.hanleFileDelete();
        this.$emit('on-cancle');
      },
      handleLast() {
        this.$emit('on-change', 1);
      },
      handleNext() {
        this.$emit('on-change', 3);
      },
    },
  };
</script>
<style lang='postcss'>
  .task-import-step2 {
    width: 680px;
    margin: 60px auto 0;

    .upload-log-box {
      width: 680px;
      max-height: calc(100vh - 344px);
      padding: 10px 16px;
      margin-top: 20px;
      overflow-y: scroll;
      font-size: 12px;
      line-height: 22px;
      color: #979ba5;
      background: #fafbfd;
      border: 1px solid #dcdee5;
      border-radius: 2px;

      .action {
        color: #3a84ff;
        cursor: pointer;
      }
    }

    .bk-upload {
      .all-file {
        display: none;
      }
    }

    .upload-result {
      position: relative;
      display: flex;
      height: 60px;
      padding: 12px 10px;
      cursor: pointer;
      background: #fff;
      border: 1px solid #c4c6cc;
      border-radius: 2px;

      &:hover {
        background: #f0f1f5;

        .file-delete {
          display: block;
        }
      }

      &.success {
        .file-status {
          color: #2dcb56;
        }
      }

      &.error,
      &.failed {
        background: rgb(254 221 220 / 40%);
        border-color: #ff5656;

        &:hover {
          .file-delete {
            display: block;
          }
        }

        .file-status {
          color: #ff5656;
        }

        .file-refresh,
        .file-delete {
          top: 20px;
          color: #ff5656;
        }

        .file-delte {
          right: 12px;
        }
      }

      &.failed {
        &:hover {
          .file-refresh {
            display: block;
          }
        }
      }

      .file-pic {
        width: 36px;
        height: 36px;
        background: skyblue;
      }

      .file-info {
        padding-left: 11px;
        font-size: 12px;
        line-height: 20px;
        color: #63656e;
        flex: 1;
      }

      .file-status {
        height: 16px;
        font-size: 12px;
        line-height: 16px;
      }

      .file-refresh,
      .file-delete {
        position: absolute;
        display: none;
        font-size: 20px;
        color: #979ba5;
      }

      .file-refresh {
        top: 20px;
        right: 41px;
      }

      .file-delete {
        top: 4px;
        right: 4px;
      }

      .progress-text {
        position: absolute;
        top: 18px;
        right: 18px;
        font-weight: bold;
        line-height: 16px;
        color: #63656e;
        user-select: none;
      }

      .upload-progress {
        position: relative;
        height: 2px;
        margin-top: 6px;
        background: #dcdee5;

        .progress-bar {
          position: absolute;
          top: 0;
          bottom: 0;
          left: 0;
          width: 100px;
          background: #3a84ff;
        }
      }
    }
  }

  .setting-password-dialog {
    .bk-dialog-header {
      padding-bottom: 0 !important;
    }

    .bk-form-item:last-child {
      margin-bottom: 0 !important;
    }

    .setting-password-footer {
      display: flex;
      justify-content: flex-end;
    }
  }
</style>
