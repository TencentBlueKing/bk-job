<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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
  <jb-form
    ref="form"
    form-type="vertical"
    :model="formData"
    :rules="rules">
    <jb-form-item
      :label="$t('dangerousRule.语法检测表达式_label')"
      property="expression"
      required>
      <bk-input v-model="formData.expression" />
    </jb-form-item>
    <jb-form-item
      :label="$t('dangerousRule.规则说明_label')"
      property="description"
      required>
      <bk-input v-model="formData.description" />
    </jb-form-item>
    <jb-form-item
      :label="$t('dangerousRule.脚本类型_label')"
      property="scriptTypeList"
      required>
      <bk-select
        v-model="formData.scriptTypeList"
        :clearable="false"
        multiple
        show-select-all>
        <bk-option
          v-for="item in scriptTypeList"
          :id="item.id"
          :key="item.id"
          :name="item.name" />
      </bk-select>
    </jb-form-item>
    <jb-form-item
      :label="$t('dangerousRule.动作_label')"
      property="action"
      required>
      <bk-select
        v-model="formData.action"
        :clearable="false">
        <bk-option
          :id="1"
          :name="$t('dangerousRule.扫描')" />
        <bk-option
          :id="2"
          :name="$t('dangerousRule.拦截')" />
      </bk-select>
    </jb-form-item>
  </jb-form>
</template>
<script>
  import DangerousRuleService from '@service/dangerous-rule';
  import PublicScriptManageService from '@service/public-script-manage';

  import I18n from '@/i18n';

  const generatorDefautlData = () => ({
    expression: '',
    description: '',
    scriptTypeList: 1,
    action: 1,
  });

  export default {
    data() {
      return {
        formData: generatorDefautlData(),
        scriptTypeList: [],
      };
    },
    created() {
      this.fetchScriptType();

      this.rules = {
        expression: [
          {
            required: true,
            message: I18n.t('dangerousRule.语法检测表达式必填'),
            trigger: 'change',
          },
        ],
        description: [
          {
            required: true,
            message: I18n.t('dangerousRule.规则说明必填'),
            trigger: 'change',
          },
        ],
        scriptTypeList: [
          {
            validator: value => value.length > 0,
            message: I18n.t('dangerousRule.脚本类型必填'),
            trigger: 'change',
          },
        ],
        action: [
          {
            required: true,
            message: I18n.t('dangerousRule.动作必填'),
            trigger: 'change',
          },
        ],
      };
    },
    methods: {
      /**
       * @desc 获取脚本类型列表
       */
      fetchScriptType() {
        PublicScriptManageService.scriptTypeList()
          .then((data) => {
            this.scriptTypeList = data;
          });
      },
      /**
       * @desc 提交用户数据
       */
      submit() {
        return this.$refs.form.validate()
          .then(() => DangerousRuleService.create({
            ...this.formData,
          }).then(() => {
            this.messageSuccess(I18n.t('dangerousRule.新增成功'));
            this.$emit('on-change');
          }));
      },
    },
  };
</script>
