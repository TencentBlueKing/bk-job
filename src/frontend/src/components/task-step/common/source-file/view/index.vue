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
    v-show="isShow"
    class="source-file-edit-view-box"
    :class="mode ? mode : 'normal'">
    <bk-collapse v-model="activeResult">
      <jb-collapse-item
        v-show="isShowLocalFile"
        :active="activeResult"
        content-hidden-type="hidden"
        name="local">
        <span>
          <span>
            <span>{{ $t('已选择.result') }}</span>
            <span class="number strong">{{ localFileList.length }}</span>
            <span>{{ $t('个本地文件') }}</span>
          </span>
          <action-extend>
            <div
              class="action-item"
              @click="handleRemoveAllLocal">
              {{ $t('移除全部') }}
            </div>
          </action-extend>
        </span>
        <template #content>
          <local-file
            ref="localFile"
            v-bind="$attrs"
            :data="localFileList"
            @on-change="handleLocalFileChange" />
        </template>
      </jb-collapse-item>
      <jb-collapse-item
        v-show="isShowSourceFile"
        :active="activeResult"
        content-hidden-type="hidden"
        name="source">
        <span>
          <span>{{ $t('已选择.result') }}</span>
          <span class="number strong">{{ sourceFileList.length }}</span>
          <span>{{ $t('个源文件文件') }}</span>
        </span>
        <template #content>
          <source-file
            ref="sourceFile"
            :data="sourceFileList"
            @on-change="handleSourceFileChange" />
        </template>
      </jb-collapse-item>
      <jb-collapse-item
        v-show="isShowServerFile"
        :active="activeResult"
        content-hidden-type="hidden"
        name="server">
        <span>
          <span>{{ $t('已选择.result') }}</span>
          <span class="number strong">{{ serverFileList.length }}</span>
          <span>{{ $t('个服务器文件') }}</span>
        </span>
        <template #content>
          <server-file
            v-bind="$attrs"
            :data="serverFileList"
            :mode="mode"
            @on-change="handleServerFileChange"
            @on-close="handleServerClose" />
        </template>
      </jb-collapse-item>
    </bk-collapse>
  </div>
</template>
<script>
  import JbCollapseItem from '@components/jb-collapse-item';

  import ActionExtend from '../components/action-extend';

  import LocalFile from './local';
  import ServerFile from './server';
  import SourceFile from './source';

  export default {
    name: 'SourceFileView',
    components: {
      JbCollapseItem,
      ActionExtend,
      LocalFile,
      ServerFile,
      SourceFile,
    },
    inheritAttrs: false,
    model: {
      prop: 'showAddServerFile',
      event: 'update',
    },
    props: {
      showAddServerFile: {
        type: Boolean,
        default: false,
      },
      isAddSourceFile: {
        type: Boolean,
        defaule: false,
      },
      /**
       * @desc 文件来源使用场景
       * @value onlyhost 快速执行执行场景，只会使用服务器主机
       * @value normal 作业文件分发步骤，使用主机和主机变量
       */
      mode: {
        type: String,
        default: '',
      },
      data: {
        type: Array,
        default: () => [],
      },
      account: {
        type: Array,
        default: () => [],
      },
    },
    data() {
      return {
        activeResult: [],
        localFileList: [],
        serverFileList: [],
        sourceFileList: [],
      };
    },
    computed: {
      isShow() {
        return this.isShowLocalFile || this.isShowServerFile || this.isShowSourceFile;
      },
      isShowLocalFile() {
        return this.localFileList.length > 0;
      },
      isShowServerFile() {
        return this.showAddServerFile;
      },
      isShowSourceFile() {
        return this.sourceFileList.length > 0;
      },
    },
    watch: {
      data: {
        handler(allSourceFile) {
          if (this.isInnerChange) {
            this.isInnerChange = false;
            return;
          }
          this.activeResult = [];
          this.localFileList = [];
          this.serverFileList = [];
          this.sourceFileList = [];
          allSourceFile.forEach((item) => {
            if (item.isServerFile) {
              this.serverFileList.push(item);
            } else if (item.isLocalFile) {
              this.localFileList.push(item);
            } else if (item.isSourceFile) {
              this.sourceFileList.push(item);
            }
          });
          if (this.localFileList.length > 0) {
            this.activeResult.push('local');
          }
          if (this.showAddServerFile || this.serverFileList.length > 0) {
            this.activeResult.push('server');
          }
          if (this.sourceFileList.length > 0) {
            this.activeResult.push('source');
          }
        },
        immediate: true,
      },
      showAddServerFile(value) {
        if (value) {
          this.activeResult.push('server');
        }
      },
      isAddSourceFile(value) {
        if (value) this.activeResult.push('source');
      },
    },
    methods: {
      trigger() {
        this.isInnerChange = true;
        this.$emit('on-change', [
          ...this.localFileList,
          ...this.serverFileList,
          ...this.sourceFileList,
        ]);
      },
      /**
       * @desc 隐藏添加服务器文件输入框
       */
      handleServerClose() {
        this.$emit('update', false);
      },
      /**
       * @desc 服务器文件更新
       * @param {Array} serverFileList
       */
      handleServerFileChange(serverFileList) {
        if (serverFileList.length > 0) {
          this.activeResult = [...new Set([...this.activeResult, 'server'])];
        }
        this.serverFileList = Object.freeze(serverFileList);
        this.trigger();
      },
      /**
       * @desc 添加文件源文件
       */
      handleAddSourceFile() {
        this.$refs.sourceFile.handleShowSourceDialog();
      },
      /**
       * @desc 文件源文件更新
       * @param {Array} sourceFileList
       */
      handleSourceFileChange(sourceFileList) {
        if (sourceFileList.length > 0) {
          this.activeResult = [...new Set([...this.activeResult, 'source'])];
        }
        this.sourceFileList = Object.freeze(sourceFileList);
        this.trigger();
      },
      /**
       * @desc 开始上传本地文件
       */
      startUploadLocalFile() {
        this.$refs.localFile.startUpload();
      },
      /**
       * @desc 本地文件更新
       * @param {Array} localFileList
       */
      handleLocalFileChange(localFileList) {
        if (localFileList.length > 0) {
          this.activeResult = [...new Set([...this.activeResult, 'local'])];
        }
        this.localFileList = Object.freeze(localFileList);
        this.trigger();
      },
      /**
       * @desc 移除所有本地文件
       */
      handleRemoveAllLocal() {
        this.localFileList = [];
        this.trigger();
      },
    },
  };
</script>
<style lang='postcss'>
  .source-file-edit-view-box {
    flex: 1;

    table {
      width: 100%;
      background: #fff;
      table-layout: fixed;

      tr:nth-child(n + 2) {
        td {
          border-top: 1px solid #dcdee5;
        }
      }

      th,
      td {
        height: 41px;
        padding: 5px 10px;
        font-size: 12px;
        text-align: left;
        box-sizing: border-box;

        &:first-child {
          width: 40%;
          padding-left: 60px;
        }

        &:nth-child(2) {
          width: 15%;
        }

        &:nth-child(4) {
          width: 20%;
        }

        &:last-child {
          width: 105px !important;
          text-align: left;

          .bk-button-text ~ .bk-button-text {
            margin-left: 10px;
          }
        }
      }

      th {
        font-weight: normal;
        color: #313238;
        border-bottom: 1px solid #dcdee5;
      }

      td {
        line-height: 18px;
        color: #63656e;
        word-break: break-all;
      }
    }

    &.normal {
      .render-server-agent {
        .agent-text {
          flex-direction: column;
          align-items: flex-start;
        }

        .splite {
          display: none;
        }
      }

      .upload-progress {
        position: absolute;
        width: 160px;
      }

      .file-server-agent,
      .server-edit-btn,
      .file-edit-server {
        white-space: pre;
      }
    }
    /* stylelint-disable selector-class-pattern */
    &.onlyHost {
      th,
      td {
        &:nth-child(3) {
          width: 20%;
        }
      }

      .upload-progress {
        position: absolute;
        width: 257px;
      }

      .sep-location {
        &::before {
          content: "，";
        }
      }
    }

    .bk-table-empty-block {
      display: none;
    }

    .bk-button,
    .bk-button-text {
      font-size: 12px;
    }
  }
</style>
