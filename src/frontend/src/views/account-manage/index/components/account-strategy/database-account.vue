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

<template functional>
    <div key="databaseAccount">
        <jb-form-item
            :label="parent.$t('account.数据库类型')"
            required>
            <bk-select
                style="width: 100%;"
                :readonly="props.isEdit"
                :clearable="false"
                :value="props.formData.type"
                @change="value => props.change('type', value)">
                <bk-option :id="9" name="Mysql" />
                <bk-option :id="10" name="Oracle" />
                <bk-option :id="11" name="DB2" />
            </bk-select>
        </jb-form-item>
        <jb-form-item
            :label="parent.$t('account.名称')"
            required
            property="account">
            <jb-input
                :placeholder="props.namePlaceholder"
                :readonly="props.isEdit"
                :value="props.formData.account"
                @change="value => props.change('account', value)" />
        </jb-form-item>
        <jb-form-item
            :label="parent.$t('account.别名')"
            required
            property="alias">
            <jb-input
                :placeholder="parent.$t('account.在出现同名账号的情况下，账号的别名显得格外重要...')"
                :value="props.formData.alias"
                @change="value => props.change('alias', value)"
                :maxlength="32" />
        </jb-form-item>
        <jb-form-item
            :label="parent.$t('account.密码')"
            property="dbPassword">
            <bk-input
                :placeholder="parent.$t('account.输入密码')"
                key="database-password"
                type="password"
                :value="props.formData.dbPassword"
                @change="value => props.change('dbPassword', value)" />
        </jb-form-item>
        <jb-form-item
            :label="parent.$t('account.确认密码')"
            property="rePassword">
            <bk-input
                :placeholder="parent.$t('account.输入确认密码')"
                key="database-repassword"
                type="password"
                :value="props.formData.rePassword"
                @change="value => props.change('rePassword', value)" />
        </jb-form-item>
        <jb-form-item
            :label="parent.$t('account.端口')"
            required
            property="dbPort">
            <bk-input
                :placeholder="parent.$t('account.输入确认端口')"
                type="number"
                :min="1"
                :value="props.formData.dbPort"
                @change="value => props.change('dbPort', value)" />
        </jb-form-item>
        <jb-form-item
            :label="parent.$t('account.依赖系统账号')"
            required
            property="dbSystemAccountId">
            <account-select
                :placeholder="parent.$t('account.选择依赖系统账号')"
                :value="props.formData.dbSystemAccountId"
                type="system"
                @change="value => props.change('dbSystemAccountId', value)" />
        </jb-form-item>
        <jb-form-item :label="parent.$t('account.描述')">
            <bk-input
                :placeholder="parent.$t('account.输入账号描述')"
                :value="props.formData.remark"
                type="textarea"
                :maxlength="200"
                @change="value => props.change('remark', value)" />
        </jb-form-item>
    </div>
</template>
