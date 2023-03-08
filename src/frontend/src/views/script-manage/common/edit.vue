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
  <layout>
    <div slot="title">
      {{ $t('script.编辑脚本') }}
    </div>
    <template slot="sub-header">
      <icon
        v-bk-tooltips="$t('上传脚本')"
        v-test="{ type: 'button', value: 'uploadScript' }"
        type="upload"
        @click="handleUploadScript" />
      <icon
        v-bk-tooltips="$t('历史缓存')"
        v-test="{ type: 'button', value: 'scriptEditHistory' }"
        type="history"
        @click.stop="handleShowHistory" />
      <icon
        v-bk-tooltips="$t('全屏')"
        v-test="{ type: 'button', value: 'scriptEditFullscreen' }"
        type="full-screen"
        @click="handleFullScreen" />
    </template>
    <div slot="left">
      <jb-form
        ref="form"
        v-test="{ type: 'form', value: 'editScript' }"
        class="edit-script-form"
        form-type="vertical"
        :model="formData"
        :rules="rules">
        <jb-form-item
          :label="$t('script.版本号.label')"
          required>
          <bk-input
            readonly
            :value="formData.version" />
        </jb-form-item>
        <jb-form-item :label="$t('script.版本日志.label')">
          <bk-input
            v-model="formData.versionDesc"
            :maxlength="100"
            :rows="5"
            type="textarea" />
        </jb-form-item>
      </jb-form>
    </div>
    <div ref="content">
      <jb-form :model="formData">
        <ace-editor
          ref="aceEditor"
          v-model="formData.content"
          :height="contentHeight"
          :lang="formData.typeName"
          :options="formData.typeName"
          :readonly="!scriptInfo.isDraft" />
      </jb-form>
    </div>
    <template #footer>
      <bk-button
        v-test="{ type: 'button', value: 'editScriptSubmit' }"
        class="w120 mr10"
        :loading="isSubmiting"
        theme="primary"
        @click="handleSubmit">
        {{ $t('script.提交') }}
      </bk-button>
      <bk-button
        v-test="{ type: 'button', value: 'debugScript' }"
        class="mr10"
        @click="handleDebugScript">
        {{ $t('script.调试') }}
      </bk-button>
      <bk-button
        v-test="{ type: 'button', value: 'editScriptCancel' }"
        @click="handleCancel">
        {{ $t('script.取消') }}
      </bk-button>
    </template>
  </layout>
</template>
<script>
  import _ from 'lodash';

  import PublicScriptManageService from '@service/public-script-manage';
  import ScriptManageService from '@service/script-manage';

  import {
    checkPublicScript,
    getOffset,
    leaveConfirm,
    scriptErrorConfirm,
  } from '@utils/assist';
  import { debugScriptCache } from '@utils/cache-helper';

  import AceEditor from '@components/ace-editor';

  import Layout from './components/layout';

  import I18n from '@/i18n';

  const genDefaultFormData = () => ({
    id: '',
    name: '',
    scriptVersionId: '',
    typeName: 'Shell',
    version: '',
    versionDesc: '',
    type: 1,
    content: '',
  });

  export default {
    name: '',
    components: {
      AceEditor,
      Layout,
    },
    inheritAttrs: false,
    props: {
      scriptInfo: {
        type: Object,
        required: true,
      },
    },
    data() {
      return {
        isSubmiting: false,
        contentHeight: 0,
        formData: {},
      };
    },
    watch: {
      scriptInfo: {
        handler(scriptInfo) {
          if (!scriptInfo.id) {
            return;
          }
          const {
            id,
            name,
            scriptVersionId,
            version,
            versionDesc,
            type,
            typeName,
            content,
          } = scriptInfo;

          this.formData = {
            ...genDefaultFormData(),
            id,
            name,
            scriptVersionId,
            version,
            versionDesc,
            type,
            typeName,
            content,
          };
        },
        immediate: true,
      },
    },
    created() {
      this.publicScript = checkPublicScript(this.$route);
      this.serviceHandler = this.publicScript ? PublicScriptManageService : ScriptManageService;

      this.rules = {
        content: [
          {
            required: true,
            message: I18n.t('script.脚本内容不能为空'),
            trigger: 'change',
          },
          {
            validator: value => ScriptManageService.getScriptValidation({
              content: value,
              scriptType: this.formData.type,
            }).then((data) => {
              // 高危语句报错状态需要全局保存
              this.$store.commit('setScriptCheckError', data.some(_ => _.isDangerous));
              return true;
            }),
            message: I18n.t('script.脚本内容检测失败'),
            trigger: 'blur',
          },
        ],
      };

      window.addEventListener('resize', this.init);
      this.$once('hook:beforeDestroy', () => {
        window.removeEventListener('resize', this.init);
      });
    },
    mounted() {
      this.calcContentHeight();
      const handleResize = _.throttle(this.calcContentHeight, 60);
      window.addEventListener('resize', handleResize);
      this.$once('hook:beforeDestroy', () => {
        window.removeEventListener('resize', handleResize);
      });
    },
    methods: {
      /**
       * @desc 计算内容区高度
       */
      calcContentHeight() {
        const contentOffsetTop = getOffset(this.$refs.content).top;
        this.contentHeight = window.innerHeight - contentOffsetTop - 26;
      },
      handleUploadScript() {
        this.$refs.aceEditor.handleUploadScript();
      },
      handleShowHistory() {
        this.$refs.aceEditor.handleShowHistory();
      },
      handleFullScreen() {
        this.$refs.aceEditor.handleFullScreen();
      },
      /**
       * @desc 保存脚本
       */
      handleSubmit() {
        if (!this.formData.content) {
          this.messageError(I18n.t('script.脚本内容不能为空'));
          return;
        }
        this.isSubmiting = true;
        this.$refs.form.validate()
          .then(() => scriptErrorConfirm())
          .then(() => {
            this.serviceHandler.scriptUpdate({
              ...this.formData,
              scriptVersionId: this.scriptInfo.scriptVersionId,
            }).then(() => {
              window.changeFlag = false;
              this.$emit('on-edit', {
                scriptVersionId: this.scriptInfo.scriptVersionId,
              });
              this.messageSuccess(I18n.t('script.操作成功'));
            });
          })
          .finally(() => {
            this.isSubmiting = false;
          });
      },
      /**
       * @desc 跳转到快速执行脚本页面调试脚本
       */
      handleDebugScript() {
        debugScriptCache.setItem(this.formData.content);
        const { href } = this.$router.resolve({
          name: 'fastExecuteScript',
          query: {
            model: 'debugScript',
          },
        });
        window.open(href);
      },
      /**
       * @desc 取消编辑
       */
      handleCancel() {
        leaveConfirm()
          .then(() => {
            this.$emit('on-edit-cancel', {
              scriptVersionId: this.scriptInfo.scriptVersionId,
            });
          });
      },
    },
  };
</script>
