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
  <div class="step-distro-file">
    <jb-form
      ref="form"
      fixed
      :label-width="formMarginLeftWidth"
      :model="formData">
      <card-layout
        class="block"
        :title="$t('template.基本信息')">
        <item-factory
          field="name"
          :form-data="formData"
          name="stepName"
          :placeholder="$t('template.推荐按步骤实际处理的场景行为来取名...')"
          @on-change="handleChange" />
        <item-factory
          field="timeout"
          :form-data="formData"
          name="timeout"
          @on-change="handleChange" />
        <item-factory
          field="ignoreError"
          :form-data="formData"
          name="errorHandle"
          @on-change="handleChange" />
        <item-factory
          field="uploadSpeedLimit"
          :form-data="formData"
          :label="$t('template.上传限速')"
          name="speedLimit"
          @on-change="handleChange" />
        <item-factory
          field="downloadSpeedLimit"
          :form-data="formData"
          :label="$t('template.下载限速')"
          name="speedLimit"
          @on-change="handleChange" />
      </card-layout>
      <card-layout
        class="block"
        :title="$t('template.文件来源')">
        <item-factory
          field="fileSourceList"
          :form-data="formData"
          name="sourceFileOfTemplate"
          :variable="variable"
          @on-change="handleChange" />
      </card-layout>
      <card-layout
        class="block"
        style="margin-bottom: 20px;"
        :title="$t('template.传输目标')">
        <item-factory
          ref="targetPath"
          field="path"
          :form-data="formData"
          name="targetPath"
          tips-placement="top"
          @on-change="handleChange" />
        <item-factory
          field="transferMode"
          :form-data="formData"
          name="transferMode"
          @on-change="handleChange" />
        <item-factory
          field="account"
          :form-data="formData"
          name="executeAccount"
          @on-change="handleChange" />
        <item-factory
          field="server"
          :form-data="formData"
          name="targetServerOfTemplate"
          :variable="variable"
          @on-change="handleChange" />
      </card-layout>
    </jb-form>
  </div>
</template>
<script>
  import { mapState } from 'vuex';

  import TaskStepModel from '@model/task/task-step';
  import TaskHostNodeModel from '@model/task-host-node';

  import {
    compareHost,
    detectionSourceFileDupLocation,
  } from '@utils/assist';

  import CardLayout from '@components/task-step/file/card-layout';
  import ItemFactory from '@components/task-step/file/item-factory';

  import I18n from '@/i18n';

  const getDefaultData = () => ({
    id: -1,
    // 步骤名称
    name: '',
    // 源文件列表
    fileSourceList: [],
    // 超时
    timeout: 7200,
    // 上传文件限速
    uploadSpeedLimit: 0,
    // 传输模式： 1 - 严谨模式； 2 - 强制模式；3 - 安全模式
    transferMode: 2,
    // 忽略错误 0 - 不忽略 1 - 忽略
    ignoreError: 0,
    // 下载文件限速
    downloadSpeedLimit: 0,

    // 目标路径，通过三个输入框（account、path、server）赋值
    // 最终组合成一个 fileDestination
    // fileDestination: {
    //     account: '', // 执行账号
    //     path: '', // 目标路径
    //     server: {} // 执行目标
    // }
    account: '',
    path: '',
    server: new TaskHostNodeModel({}),
  });

  export default {
    name: '',
    components: {
      CardLayout,
      ItemFactory,
    },
    inheritAttrs: false,
    props: {
      variable: {
        type: Array,
        default: () => [],
      },
      data: {
        type: Object,
        default: () => ({}),
      },
    },
    data() {
      return {
        formData: getDefaultData(),
      };
    },
    computed: {
      ...mapState('distroFile', {
        isEditNewSourceFile: state => state.isEditNewSourceFile,
        isLocalFileUploading: state => state.isLocalFileUploading,
        isLocalFileUploadFailed: state => state.isLocalFileUploadFailed,
      }),
      formMarginLeftWidth() {
        return this.$i18n.locale === 'en-US' ? 140 : 110;
      },
    },
    watch: {
      data: {
        handler(newData) {
          // 本地新建的步骤id为-1，已提交后端保存的id大于0
          if (!newData.id) {
            this.formData = Object.assign({}, this.formData, newData);
            return;
          }
          const {
            account,
            path,
            server,
          } = newData.fileDestination;

          const originData = { ...newData };
          delete originData.fileDestination;

          this.formData = {
            ...this.formData,
            ...originData,
            account,
            path,
            server,
          };
          // 有数据需要自动验证一次
          setTimeout(() => {
            this.$refs.form.validate();
          });
        },
        immediate: true,
      },
    },
    mounted() {
      window.IPInputScope = 'FILE_DISTRIBUTION';
      this.$once('hook:beforeDestroy', () => {
        window.IPInputScope = '';
      });
    },
    methods: {
      /**
       * @desc 表单字段更新
       * @param {String} field 字段名
       * @param {Any} value 字段值
       */
      handleChange(field, value) {
        this.formData[field] = value;
      },
      /**
       * @desc 提交表单
       *
       * 1，首先检测是否有没保存的源文件
       * 2，表单验证
       *   - 表单验证失败检测是否有本地文件上传未完成或者本地文件上传失败
       */
      submit() {
        return Promise.resolve()
          // 检测没有保存的源文件
          .then(() => new Promise((resolve, reject) => {
            if (!this.isEditNewSourceFile) {
              return resolve();
            }
            this.$bkInfo({
              title: I18n.t('template.您有未保存的源文件'),
              type: 'warning',
              maskClose: false,
              escClose: false,
              closeIcon: false,
              okText: I18n.t('template.继续提交'),
              cancelText: I18n.t('template.去保存'),
              confirmFn: () => {
                resolve();
              },
              cancelFn: () => {
                reject(new Error('save'));
              },
            });
          }))
          // 检测服务器源文件的主机和执行目标服务器主机相同
          .then(() => new Promise((resolve, reject) => {
            let sameHost = false;
            // eslint-disable-next-line no-plusplus
            for (let i = 0; i < this.formData.fileSourceList.length; i++) {
              const currentFileSource = this.formData.fileSourceList[i];
              // 服务器源文件
              if (currentFileSource.fileType === TaskStepModel.fileStep.TYPE_SERVER) {
                if (compareHost(this.formData.server, currentFileSource.host)) {
                  sameHost = true;
                  break;
                }
              }
            }
            if (sameHost) {
              this.$bkInfo({
                title: I18n.t('template.源和目标服务器相同'),
                subTitle: I18n.t('template.检测到文件传输源和目标服务器是同一批，若是单台建议使用本地 cp 方式效率会更高，请问你是否确定参数无误？'),
                width: 500,
                maskClose: false,
                escClose: false,
                closeIcon: false,
                okText: I18n.t('template.好的，我调整一下'),
                cancelText: I18n.t('template.是的，确定无误'),
                confirmFn: () => {
                  reject(new Error('save'));
                },
                cancelFn: () => {
                  resolve();
                },
              });
            } else {
              resolve();
            }
          }))
          // 检测源文件的同名文件和目录
          .then(() => new Promise((resolve, reject) => {
            if (detectionSourceFileDupLocation(this.formData.fileSourceList)) {
              // 有重名目录和文件
              this.$bkInfo({
                title: I18n.t('template.源文件可能出现同名'),
                subTitle: I18n.t('template.多文件源传输场景下容易出现同名文件覆盖的问题，你可以在目标路径中使用 [源服务器IP] 的变量来尽可能规避风险。'),
                okText: I18n.t('template.好的，我调整一下'),
                cancelText: I18n.t('template.已知悉，确定执行'),
                maskClose: false,
                escClose: false,
                closeIcon: false,
                width: 500,
                confirmFn: () => {
                  // 聚焦到目标路径输入框
                  this.$refs.targetPath.$el.scrollIntoView();
                  this.$refs.targetPath.$el.querySelector('.bk-form-input').focus();
                  reject(new Error('transferMode change'));
                },
                cancelFn: () => {
                  resolve();
                },
              });
            } else {
              resolve();
            }
          }))
          // 提交步骤
          .then(() => {
            const {
              id,
              name,
              timeout,
              uploadSpeedLimit,
              downloadSpeedLimit,
              transferMode,
              ignoreError,
              fileSourceList,
              account,
              path,
              server,
            } = this.formData;

            const result = {
              id,
              name,
              delete: 0,
              type: 2,
              fileStepInfo: {
                timeout,
                uploadSpeedLimit,
                downloadSpeedLimit,
                transferMode,
                ignoreError,
                fileSourceList,
                fileDestination: {
                  account,
                  path,
                  server,
                },

              },
            };
            return this.$refs.form.validate()
              // 表单验证通过直接提交
              .then(() => {
                this.$emit('on-change', result, true);
              })
              // 表单验证失败时，检测本地文件上传状态
              .catch(() => new Promise((resolve, reject) => {
                let confirmInfo = null;
                const handleClose = () => {
                  confirmInfo.close();
                  reject(new Error('not save'));
                };
                const subHeader = () => (
                                    <div>
                                        <div style="text-align: center">
                                            <bk-button
                                                onClick={handleClose}
                                                style="width: 96px"
                                                theme="primary">
                                                { I18n.t('template.去处理') }
                                            </bk-button>
                                        </div>
                                    </div>
                );
                if (this.isLocalFileUploading) {
                  confirmInfo = this.$bkInfo({
                    type: 'error',
                    title: I18n.t('template.本地源文件上传未完成'),
                    subHeader: subHeader(),
                    showFooter: false,
                    closeIcon: false,
                  });
                } else if (this.isLocalFileUploadFailed) {
                  confirmInfo = this.$bkInfo({
                    type: 'error',
                    title: I18n.t('template.本地源文件上传失败'),
                    subHeader: subHeader(),
                    showFooter: false,
                    closeIcon: false,
                  });
                } else {
                  this.$emit('on-change', result, false);
                  resolve();
                }
              }));
          });
      },
    },
  };
</script>
<style lang='postcss'>
  .step-distro-file {
    .card-layout {
      padding-left: 0;
      margin-bottom: 6px;

      .card-layout-title {
        padding-left: 30px;
      }
    }
  }
</style>
