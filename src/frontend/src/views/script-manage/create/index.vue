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
  <smart-action
    class="create-script-page"
    offset-target="bk-form-content">
    <jb-form
      ref="form"
      v-test="{ type: 'form', value: 'create_script' }"
      :model="formData"
      :rules="rules">
      <jb-form-item
        :label="$t('script.脚本名称.label')"
        property="name"
        required>
        <div class="script-name input">
          <jb-input
            v-model="formData.name"
            :maxlength="60"
            :placeholder="$t('script.推荐按照该脚本逻辑提供的使用场景来取名...')" />
        </div>
      </jb-form-item>
      <jb-form-item
        :label="$t('script.场景标签.label')"
        property="tags">
        <jb-tag-select
          v-model="formData.tags"
          class="input"
          :placeholder="$t('script.标签对资源的分类管理有很大帮助')" />
      </jb-form-item>
      <jb-form-item :label="$t('script.描述')">
        <bk-input
          v-model="formData.description"
          class="input"
          :maxlength="200"
          :placeholder="$t('script.在此处标注该脚本的备注和使用说明')"
          type="textarea" />
      </jb-form-item>
      <jb-form-item
        :label="$t('script.版本号.label')"
        property="version"
        required>
        <jb-input
          v-model="formData.version"
          class="input"
          :maxlength="30"
          :placeholder="$t('script.输入版本号')" />
      </jb-form-item>
      <jb-form-item
        :label="$t('script.脚本内容.label')"
        property="content"
        required>
        <div ref="content">
          <ace-editor
            v-model="formData.content"
            v-bkloading="{ isLoading: isContentLoading, opacity: .2 }"
            :height="contentHeight"
            :lang="scriptType"
            @on-mode-change="handleTypeChange" />
        </div>
      </jb-form-item>
    </jb-form>
    <template #action>
      <bk-button
        class="w120 mr10"
        :loading="isSbumiting"
        theme="primary"
        @click="handleSubmit">
        {{ $t('script.提交') }}
      </bk-button>
      <bk-button
        theme="default"
        @click="handleCancel">
        {{ $t('script.取消') }}
      </bk-button>
    </template>
  </smart-action>
</template>
<script>
  import _ from 'lodash';

  import PublicScriptService from '@service/public-script-manage';
  import ScriptService from '@service/script-manage';

  import {
    checkPublicScript,
    formatScriptTypeValue,
    getOffset,
    scriptErrorConfirm,
  } from '@utils/assist';
  import {
    scriptNameRule,
    scriptVersionRule,
  } from '@utils/validator';

  import AceEditor from '@components/ace-editor';
  import JbInput from '@components/jb-input';
  import JbTagSelect from '@components/jb-tag-select';

  import I18n from '@/i18n';

  export default {
    name: '',
    components: {
      AceEditor,
      JbTagSelect,
      JbInput,
    },
    data() {
      return {
        isContentLoading: false,
        isSbumiting: false,
        scriptType: 'Shell',
        contentHeight: 480,
        formData: {
          name: '',
          tags: [],
          description: '',
          version: '',
          type: 1,
          content: '',
        },
      };
    },
    created() {
      this.publicScript = checkPublicScript(this.$route);
      this.serviceHandler = this.publicScript ? PublicScriptService : ScriptService;

      this.rules = {
        name: [
          {
            required: true,
            message: I18n.t('script.脚本名称必填'),
            trigger: 'blur',
          },
          {
            validator: scriptNameRule.validator,
            message: scriptNameRule.message,
            trigger: 'blur',
          },
        ],
        version: [
          {
            required: true,
            message: I18n.t('script.脚本版本必填'),
            trigger: 'blur',
          },
          {
            validator: scriptVersionRule.validator,
            message: scriptVersionRule.message,
            trigger: 'blur',
          },
        ],
        desc: [
          {
            max: 200,
            message: I18n.t('script.最多仅可 200个字符'),
            trigger: 'blur',
          },
        ],
        content: [
          {
            required: true,
            message: I18n.t('script.脚本内容不能为空'),
            trigger: 'change',
          },
          {
            validator: value => ScriptService.getScriptValidation({
              content: value,
              scriptType: this.formData.type,
            }).then((data) => {
              // 高危语句报错状态需要全局保存
              // 高危语句报错状态需要全局保存
              const dangerousContent = _.find(data, _ => _.isDangerous);
              this.$store.commit('setScriptCheckError', dangerousContent);
              return true;
            }),
            message: I18n.t('script.脚本内容检测失败'),
            trigger: 'blur',
          },
        ],
      };
    },
    mounted() {
      this.init();
    },
    methods: {
      /**
       * @desc 计算内容区的高度
       */
      init() {
        const contentOffsetTop = getOffset(this.$refs.content).top;
        const contentHeight = window.innerHeight - contentOffsetTop + 20;
        this.contentHeight = contentHeight > 480 ? contentHeight : 480;
      },
      /**
       * @desc 脚本语言类型切换
       * @param {String} scriptType 脚本语言
       */
      handleTypeChange(scriptType) {
        this.scriptType = scriptType;
        this.formData.type = formatScriptTypeValue(scriptType);
      },
      /**
       * @desc 保存脚本
       */
      handleSubmit() {
        this.isSbumiting = true;
        this.$refs.form.validate()
          .then(scriptErrorConfirm)
          .then(() => this.serviceHandler.scriptUpdate(this.formData)
            .then((data) => {
              window.changeFlag = false;
              this.messageSuccess(I18n.t('script.操作成功'), () => {
                this.$router.push({
                  name: this.publicScript ? 'publicScriptVersion' : 'scriptVersion',
                  params: {
                    id: data.id,
                  },
                  query: {
                    scriptVersionId: data.scriptVersionId,
                  },
                });
              });
            }))
          .finally(() => {
            this.isSbumiting = false;
          });
      },
      /**
       * @desc 取消新建
       */
      handleCancel() {
        this.routerBack();
      },
      /**
       * @desc 路由回退
       */
      routerBack() {
        if (this.publicScript) {
          this.$router.push({
            name: 'publicScriptList',
          });
          return;
        }
        this.$router.push({
          name: 'scriptList',
        });
      },
    },
  };
</script>
<style lang='postcss' scoped>
  .create-script-page {
    .input {
      width: 510px;
      background: #fff;
    }
  }

  .script-name-tips {
    padding-right: 12px;
    font-size: 12px;
    line-height: 16px;
    color: #63656e;

    .row {
      position: relative;
      padding-right: 12px;
      padding-left: 12px;

      &::before {
        position: absolute;
        top: 6px;
        left: 0;
        width: 4px;
        height: 4px;
        background: currentcolor;
        border-radius: 50%;
        content: "";
      }
    }
  }
</style>
