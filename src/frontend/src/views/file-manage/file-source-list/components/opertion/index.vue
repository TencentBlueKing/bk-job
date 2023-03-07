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
    class="file-source-create-form">
    <jb-form
      v-if="!isLoading"
      ref="fileSourceform"
      form-type="vertical"
      :model="formData"
      :rules="rules">
      <jb-form-item
        :label="$t('file.文件源标识.label')"
        property="code"
        required>
        <bk-input
          v-model="formData.code"
          :placeholder="$t('file.请输入文件源标识')" />
      </jb-form-item>
      <jb-form-item
        :label="$t('file.文件源别名.label')"
        property="alias"
        required>
        <bk-input
          v-model="formData.alias"
          :maxlength="32"
          :placeholder="$t('file.为文件源起一个可读性较好的别名')"
          show-word-limit />
      </jb-form-item>
      <jb-form-item
        :label="$t('file.类型.label')"
        required>
        <bk-radio-group v-model="formData.storageType">
          <bk-radio-button value="OSS">
            {{ $t('file.对象存储') }}
          </bk-radio-button>
        </bk-radio-group>
      </jb-form-item>
      <jb-form-item
        :label="$t('file.来源')"
        required>
        <bk-radio-group
          v-model="formData.fileSourceTypeCode"
          @change="handleFileSourceChange">
          <bk-radio-button
            v-for="item in sourceTypeList"
            :key="item.code"
            :value="item.code">
            <img
              :src="item.icon"
              style="width: 1em; height: 1em; vertical-align: middle;">
            <span style="vertical-align: middle;">{{ item.name }}</span>
          </bk-radio-button>
        </bk-radio-group>
      </jb-form-item>
      <render-file-source-param
        :file-source-type-code="formData.fileSourceTypeCode"
        :param-map="formData.fileSourceInfoMap"
        @on-change="handleFileSourceParamsChange" />
      <jb-form-item
        :label="$t('file.公共存储')"
        required>
        <bk-checkbox v-model="formData.publicFlag">
          {{ $t('file.设为公共存储') }}
        </bk-checkbox>
      </jb-form-item>
      <jb-form-item
        v-if="formData.publicFlag"
        :label="$t('file.共享对象')"
        property="sharedScopeList"
        required>
        <div class="share-object-box">
          <bk-select
            v-model="formData.sharedScopeList"
            class="share-app-select"
            :clearable="false"
            :disabled="formData.shareToAllApp"
            multiple
            searchable>
            <bk-option
              v-for="scopeItem in scopeList"
              :id="`#${scopeItem.scopeType}#${scopeItem.scopeId}`"
              :key="`#${scopeItem.scopeType}#${scopeItem.scopeId}`"
              :name="scopeItem.name" />
          </bk-select>
          <bk-checkbox v-model="formData.shareToAllApp">
            {{ $t('file.全业务') }}
          </bk-checkbox>
        </div>
      </jb-form-item>
      <jb-form-item
        :label="$t('file.身份凭证')"
        property="credentialId"
        required>
        <bk-select
          v-model="formData.credentialId"
          :clearable="false">
          <auth-option
            v-for="option in fileFourceTicketList"
            :id="option.id"
            :key="option.id"
            auth="ticket/use"
            :name="option.name"
            :permission="option.canUse"
            :resource-id="option.id" />
        </bk-select>
      </jb-form-item>
      <jb-form-item :label="$t('file.文件前缀名')">
        <bk-select
          v-model="filePrefixType"
          :clearable="false">
          <bk-option
            id="${UUID}"
            name="UUID" />
          <bk-option
            id="custom"
            :name="$t('file.自定义字符串')" />
        </bk-select>
        <bk-input
          v-if="isCustomFilePrefix"
          v-model="formData.filePrefix"
          style="margin-top: 10px;" />
      </jb-form-item>
      <jb-form-item
        :label="$t('file.接入点')"
        required>
        <div class="access-point-box">
          <bk-select
            v-model="formData.workerSelectScope"
            class="worker-select"
            :clearable="false">
            <bk-option
              v-for="option in workerSelectModeList"
              :id="option.id"
              :key="option.id"
              :name="option.name" />
          </bk-select>
          <bk-checkbox
            v-model="isWorkerSelectScopeAuto"
            :disabled="workersList.length < 1">
            {{ $t('file.自动选择接入点') }}
          </bk-checkbox>
        </div>
        <div v-if="!isWorkerSelectScopeAuto && workersList.length > 0">
          <bk-radio-group
            v-model="formData.workerId"
            class="worker-box">
            <bk-radio-button
              v-for="option in workersList"
              :key="option.id"
              :value="option.id">
              <div class="worker-item">
                <span>{{ option.innerIp }} </span>
                <span v-html="option.latencyHtml" />
              </div>
            </bk-radio-button>
          </bk-radio-group>
        </div>
      </jb-form-item>
    </jb-form>
  </div>
</template>
<script>
  import AppManageService from '@service/app-manage';
  import FileSourceManageService from '@service/file-source-manage';
  import FileSourceTypeService from '@service/file-source-type';
  import FileWorkerService from '@service/file-worker';
  import TicketManageService from '@service/ticket-manage';

  import FileSourceModel from '@model/file/file-source';

  import {
    fileSourceAliasNameRule,
  } from '@utils/validator';

  import RenderFileSourceParam from './components/render-file-source-param';

  import I18n from '@/i18n';

  const getDefaultData = () => ({
    // ID,更新文件源的时候需要传入，新建文件源不需要
    id: -1,
    // 文件源名称
    alias: '',
    // 文件源Code
    code: '',
    // 文件源凭证Id
    credentialId: '',
    // 文件前缀：后台自动生成UUID传${UUID}，自定义字符串直接传
    filePrefix: FileSourceModel.FILE_PERFIX_UUID,
    // 文件源参数
    fileSourceInfoMap: {},
    // 文件源类型Code
    fileSourceTypeCode: '',
    // 是否为公共文件源
    publicFlag: true,
    // 是否共享到全业务
    shareToAllApp: false,
    // 共享的业务Id列表
    sharedScopeList: [],
    // 存储类型
    storageType: 'OSS',
    // 接入点Id，手动选择时传入，自动选择不传
    workerId: '',
    // 接入点选择范围:APP/PUBLIC/ALL，分别为业务私有接入点/公共接入点/全部
    workerSelectScope: 'PUBLIC',
    // 接入点选择模式：AUTO/MANUAL，分别为自动/手动
    workerSelectMode: 'AUTO',
  });

  export default {
    name: 'SourceFileOpertion',
    components: {
      RenderFileSourceParam,
    },
    props: {
      data: {
        type: Object,
        default: () => ({}),
      },
    },
    data() {
      return {
        isLoading: false,
        formData: getDefaultData(),
        filePrefixType: FileSourceModel.FILE_PERFIX_UUID,
        // 文件源类型列表
        sourceTypeList: [],
        // 文件源参数
        fileSourceParamList: [],
        // 业务列表
        scopeList: [],
        // 文件源凭证列表
        fileFourceTicketList: [],
        // 自动选择接入点
        isWorkerSelectScopeAuto: true,
        // 接入点列表
        workersList: [],
      };
    },
    computed: {
      /**
       * @desc 自定义文件前缀名
       * @return {Boolean}
       */
      isCustomFilePrefix() {
        return this.filePrefixType !== FileSourceModel.FILE_PERFIX_UUID;
      },
    },
    watch: {
      /**
       * @desc 共享对象为全业务，清空 sharedScopeList
       */
      'formData.shareToAllApp'(newVal) {
        if (newVal) {
          this.formData.sharedScopeList = [];
        }
      },
      /**
       * @desc 接入点选择范围——获取文件接入点列表
       * 重置接入点为自动选择
       */
      'formData.workerSelectScope'() {
        this.fetchWorkersList();
        this.isWorkerSelectScopeAuto = true;
      },
      /**
       * @desc 自动选择接入点——workerId 为空
       */
      isWorkerSelectScopeAuto(isWorkerSelectScopeAuto) {
        if (isWorkerSelectScopeAuto) {
          this.formData.workerId = '';
        }
      },
    },
    created() {
      const taskQueue = [
        this.fetchSourceTypeList(),
        this.fetchScopeList(),
        this.fetchTicketList(),
        this.fetchWorkersList(),
      ];
      // 编辑文件源
      if (this.data.id) {
        this.formData.id = this.data.id;
        taskQueue.unshift(this.fetchFileSourceDetail());
      }
      this.isLoading = true;
      Promise.all(taskQueue)
        .finally(() => {
          this.isLoading = false;
        });
      // 接入点选择范围
      this.workerSelectModeList = [
        { id: 'APP', name: I18n.t('file.业务私有') },
        { id: 'PUBLIC', name: I18n.t('file.公共接入点') },
        { id: 'ALL', name: I18n.t('file.全部') },
      ];
      // 表单验证
      this.rules = {
        code: [
          {
            required: true,
            message: I18n.t('file.文件源标识必填'),
            trigger: 'blur',
          },
        ],
        alias: [
          {
            required: true,
            message: I18n.t('file.文件源别名必填'),
            trigger: 'blur',
          },
          {
            validator: fileSourceAliasNameRule.validator,
            message: fileSourceAliasNameRule.message,
            trigger: 'blur',
          },
          {
            validator: alias => FileSourceManageService.fetchAliasCheck({
              fileSourceId: this.formData.id,
              alias,
            }),
            message: I18n.t('file.文件源别名已存在，请重新输入'),
            trigger: 'blur',
          },
        ],
        sharedScopeList: [
          {
            validator: (sharedScopeList) => {
              if (this.formData.shareToAllApp) {
                return true;
              }
              return sharedScopeList.length > 0;
            },
            message: I18n.t('file.共享对象必填'),
            trigger: 'blur',
          },
        ],
        credentialId: [
          {
            required: true,
            message: I18n.t('file.身份凭证必填'),
            trigger: 'blur',
          },
        ],
      };
    },
    methods: {
      /**
       * @desc 获取文件源类型列表
       */
      fetchSourceTypeList() {
        return FileSourceTypeService.sourceTypeList()
          .then((data) => {
            this.sourceTypeList = Object.freeze(data);
            if (!this.formData.fileSourceTypeCode && this.sourceTypeList.length > 0) {
              this.formData.fileSourceTypeCode = this.sourceTypeList[0].code;
            }
          });
      },
      /**
       * @desc 获取业务列表数据
       *
       * 需过滤掉当前业务
       */
      fetchScopeList() {
        return AppManageService.fetchAppList()
          .then((data) => {
            const {
              SCOPE_TYPE,
              SCOPE_ID,
            } = window.PROJECT_CONFIG;
            this.scopeList = Object.freeze(data.reduce((result, item) => {
              if (item.scopeType === SCOPE_TYPE && item.scopeId === SCOPE_ID) {
                return result;
              }
              result.push(item);
              return result;
            }, []));
          });
      },
      /**
       * @desc 获取身份凭证列表数据
       */
      fetchTicketList() {
        return TicketManageService.fetchList()
          .then((res) => {
            this.fileFourceTicketList = Object.freeze(res.data);
          });
      },
      /**
       * @desc 获取接入点列表数据
       */
      fetchWorkersList() {
        return FileWorkerService.workersList({
          workerSelectScope: this.formData.workerSelectScope,
        }).then((data) => {
          this.workersList = Object.freeze(data);
        });
      },
      /**
       * @desc 获取文件源详情
       */
      fetchFileSourceDetail() {
        return FileSourceManageService.getSourceInfo({
          id: this.formData.id,
        }).then((data) => {
          const {
            alias,
            code,
            credentialId,
            fileSourceInfoMap,
            filePrefix,
            fileSourceType,
            publicFlag,
            storageType,
            shareToAllApp,
            sharedScopeList,
            workerId,
            workerSelectMode,
            workerSelectScope,
          } = data;

          this.formData = {
            ...this.formData,
            alias,
            code,
            credentialId,
            fileSourceInfoMap,
            filePrefix,
            fileSourceTypeCode: fileSourceType.code,
            publicFlag,
            storageType,
            shareToAllApp,
            sharedScopeList: sharedScopeList.map(({ type, id }) => `#${type}#${id}`),
            workerId,
            workerSelectMode,
            workerSelectScope,
          };
          // 没有接入点Id——自动选择接入点
          this.isWorkerSelectScopeAuto = !workerId;
          // 文件前缀名不等于 FileSourceModel.FILE_PERFIX_UUID 则是自定义文件前缀名
          this.filePrefixType = filePrefix === FileSourceModel.FILE_PERFIX_UUID ? filePrefix : 'custom';
        });
      },
      /**
       * @desc 文件源改变重置文件源参数
       */
      handleFileSourceChange() {
        this.formData.fileSourceInfoMap = {};
      },
      /**
       * @desc 文件源参数变化
       * @param {Object} params 文件源参数
       */
      handleFileSourceParamsChange(params) {
        this.formData.fileSourceInfoMap = params;
      },
      /**
       * @desc 新建、编辑确认
       *
       * 校验表单通过后,根据文件源ID是否存在判断新建或编辑
       */
      submit() {
        return this.$refs.fileSourceform.validate()
          .then(() => {
            const params = Object.assign({}, this.formData);

            // workerId 不为空手动选择接入点
            // workerId 为空自动选择接入点
            params.workerSelectMode = params.workerId ? 'MANUAL' : 'AUTO';
            if (this.filePrefixType === FileSourceModel.FILE_PERFIX_UUID) {
              params.filePrefix = FileSourceModel.FILE_PERFIX_UUID;
            }
            params.sharedScopeList = params.sharedScopeList.map((item) => {
              const [, type, id] = item.match(/^#([^#]+)#(.*)/);
              return {
                type,
                id,
              };
            });

            if (params.id < 0) {
              return FileSourceManageService.addSource(params)
                .then(() => {
                  this.messageSuccess(I18n.t('file.创建成功'));
                  this.$emit('on-change');
                });
            }
            return FileSourceManageService.updateSource(params)
              .then(() => {
                this.messageSuccess(I18n.t('file.更新成功'));
                this.$emit('on-change');
              });
          });
      },
    },
  };
</script>
<style lang="postcss">
  .file-source-create-form {
    min-height: 50vh;

    .share-object-box {
      display: flex;
      align-items: center;

      .share-app-select {
        flex: 1 1 auto;
        margin-right: 10px;
      }
    }

    .access-point-box {
      display: flex;
      align-items: center;

      .worker-select {
        width: 220px;
        margin-right: 10px;
      }
    }

    .worker-box {
      .bk-form-radio-button {
        width: 220px;
        margin-top: 10px;

        .bk-radio-button-text {
          width: 100%;
        }
      }

      .worker-item {
        display: flex;
        font-size: 12px;
        justify-content: space-between;

        .latency {
          color: #3fc06d;
        }
      }
    }
  }
</style>
