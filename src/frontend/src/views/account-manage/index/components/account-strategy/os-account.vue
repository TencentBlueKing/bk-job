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

<template functional>
  <div key="system-account">
    <jb-form-item
      :label="parent.$t('account.类型')"
      required>
      <div class="radio-button-group-wraper">
        <bk-radio-group
          :value="props.formData.type"
          @change="value => props.change('type', value)">
          <bk-radio-button
            class="account-type-radio"
            :disabled="props.isEdit && props.formData.type !== 1"
            :value="1">
            Linux
          </bk-radio-button>
          <bk-radio-button
            class="account-type-radio"
            :disabled="props.isEdit && props.formData.type !== 2"
            :value="2">
            Windows
          </bk-radio-button>
        </bk-radio-group>
      </div>
    </jb-form-item>
    <jb-form-item
      :label="parent.$t('account.名称')"
      property="account"
      required>
      <jb-input
        :placeholder="props.namePlaceholder"
        :readonly="props.isEdit"
        :value="props.formData.account"
        @change="value => props.change('account', value)" />
    </jb-form-item>
    <jb-form-item
      :label="parent.$t('account.别名')"
      property="alias"
      required>
      <jb-input
        :maxlength="32"
        :placeholder="parent.$t('account.在出现同名账号的情况下，账号的别名显得格外重要')"
        :value="props.formData.alias"
        @change="value => props.change('alias', value)" />
    </jb-form-item>
    <!-- Linux系统账号不需要密码 -->
    <template v-if="props.formData.type !== 1">
      <jb-form-item
        key="systemPassword"
        :label="parent.$t('account.密码')"
        property="password"
        required>
        <bk-input
          :placeholder="parent.$t('account.输入密码')"
          type="password"
          :value="props.formData.password"
          @change="value => props.change('password', value)" />
      </jb-form-item>
      <jb-form-item
        key="systemRepassword"
        :label="parent.$t('account.确认密码')"
        property="rePassword"
        required>
        <bk-input
          :placeholder="parent.$t('account.输入确认密码')"
          type="password"
          :value="props.formData.rePassword"
          @change="value => props.change('rePassword', value)" />
      </jb-form-item>
    </template>
    <jb-form-item :label="parent.$t('account.描述_label')">
      <bk-input
        :maxlength="200"
        :placeholder="parent.$t('account.输入账号描述')"
        type="textarea"
        :value="props.formData.remark"
        @change="value => props.change('remark', value)" />
    </jb-form-item>
  </div>
</template>
