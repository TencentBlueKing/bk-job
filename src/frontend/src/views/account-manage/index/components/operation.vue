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
    <div class="operation-account" v-bkloading="{ isLoading }">
        <jb-form
            ref="operateAccountForm"
            :model="formData"
            :rules="rules"
            :key="`${formData.category}_${formData.type}`"
            form-type="vertical"
            v-test="{ type: 'form', value: 'createAccount' }">
            <jb-form-item :label="$t('account.用途')" required style="margin-bottom: 20px;">
                <div class="radio-button-group-wraper">
                    <bk-radio-group
                        :value="formData.category"
                        @change="handleCategoryChange">
                        <bk-radio-button
                            v-for="item in categoryList"
                            :key="item.value"
                            :value="item.value"
                            :disabled="isEdit && formData.category !== item.value"
                            class="account-type-radio">
                            {{ item.name }}
                        </bk-radio-button>
                    </bk-radio-group>
                </div>
            </jb-form-item>
            <component
                :is="accountCom"
                :key="formData.category"
                :form-data="formData"
                :is-edit="isEdit"
                :name-placeholder="namePlaceholder"
                :change="handleFieldChange" />
        </jb-form>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import QueryGlobalSettingService from '@service/query-global-setting';
    import AccountManageService from '@service/account-manage';
    import {
        accountAliasNameRule,
    } from '@utils/validator';
    import JbInput from '@components/jb-input';
    import AccountSelect from '@components/account-select';
    import AccountDatabase from './account-strategy/database-account';
    import AccountSystem from './account-strategy/system-account';

    const ACCOUNT_TYPE_SYSTEM = 1;
    const ACCOUNT_TYPE_DATABASE = 2;
    
    const generatorDefault = () => ({
        id: '',
        account: '',
        alias: '',
        category: ACCOUNT_TYPE_SYSTEM,
        dbPassword: '',
        dbPort: '',
        dbSystemAccountId: '',
        grantees: [],
        os: '',
        password: '',
        remark: '',
        type: 1,
        rePassword: '',
    });

    export default {
        name: 'OperationAccount',
        components: {
            JbInput,
            AccountSelect,
        },
        props: {
            data: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                isLoading: true,
                isEdit: false,
                isRulesLoadingError: false,
                formData: generatorDefault(),
                currentRules: Object.freeze({
                    linux: {
                        expression: '.',
                    },
                    windows: {
                        expression: '.',
                    },
                    db: {
                        expression: '.',
                    },
                }),
            };
        },
        computed: {
            accountCom () {
                const comMap = {
                    1: AccountSystem,
                    2: AccountDatabase,
                };
                return comMap[this.formData.category];
            },
            namePlaceholder () {
                if (this.isLoading) {
                    return '';
                }
                if (this.formData.category === ACCOUNT_TYPE_DATABASE) {
                    return this.currentRules.db.description;
                }
                if (this.formData.type === 1) {
                    return this.currentRules.linux.description;
                }
                return this.currentRules.windows.description;
            },
            rules () {
                if (this.isLoading) {
                    return {};
                }
                const baseRule = {
                    account: [
                        {
                            required: true,
                            message: I18n.t('account.名称必填'),
                            trigger: 'blur',
                        },
                        
                    ],
                    alias: [
                        {
                            required: true,
                            message: I18n.t('account.别名必填'),
                            trigger: 'blur',
                        },
                        {
                            validator: accountAliasNameRule.validator,
                            message: accountAliasNameRule.message,
                            trigger: 'blur',
                        },
                    ],
                };
                // db账号
                if (this.formData.category === ACCOUNT_TYPE_DATABASE) {
                    baseRule.account.push({
                        validator: (value) => {
                            const regx = new RegExp(this.currentRules.db.expression);
                            return regx.test(value);
                        },
                        message: this.currentRules.db.description,
                        trigger: 'blur',
                    });
                    return {
                        ...baseRule,
                        dbPassword: [
                            {
                                validator: value => !/[\u4e00-\u9fa5]/.test(value),
                                message: I18n.t('account.密码不支持中文'),
                                trigger: 'blur',
                            },
                        ],
                        rePassword: [
                            {
                                validator: value => this.formData.rePassword === this.formData.dbPassword,
                                message: I18n.t('account.密码不一致'),
                                trigger: 'blur',
                            },
                        ],
                        dbPort: [
                            {
                                required: true,
                                message: I18n.t('account.端口必填'),
                                trigger: 'blur',
                            },
                        ],
                        dbSystemAccountId: [
                            {
                                required: true,
                                message: I18n.t('account.依赖系统账号必填'),
                                trigger: 'blur',
                            },
                        ],
                    };
                }
                // Linux系统账号不需要密码
                if (this.formData.type === 1) {
                    baseRule.account.push({
                        validator: (value) => {
                            const regx = new RegExp(this.currentRules.linux.expression);
                            return regx.test(value);
                        },
                        message: this.currentRules.linux.description,
                        trigger: 'blur',
                    });
                    return baseRule;
                }
                // 非Linux系统账号需要密码
                baseRule.account.push({
                    validator: (value) => {
                        const regx = new RegExp(this.currentRules.windows.expression);
                        return regx.test(value);
                    },
                    message: this.currentRules.windows.description,
                    trigger: 'blur',
                });
                return {
                    ...baseRule,
                    password: [
                        {
                            required: true,
                            message: I18n.t('account.密码必填'),
                            trigger: 'blur',
                        },
                        {
                            validator: value => !/[\u4e00-\u9fa5]/.test(value),
                            message: I18n.t('account.密码不支持中文'),
                            trigger: 'blur',
                        },
                    ],
                    rePassword: [
                        {
                            validator: value => value === this.formData.password,
                            message: I18n.t('account.密码不一致'),
                            trigger: 'blur',
                        },
                    ],
                };
            },
        },
        created () {
            this.categoryList = [
                { value: ACCOUNT_TYPE_SYSTEM, name: I18n.t('account.系统账号') },
                { value: ACCOUNT_TYPE_DATABASE, name: I18n.t('account.数据库账号') },
            ];
            if (this.data.id) {
                this.isEdit = true;
                const {
                    id,
                    account,
                    alias,
                    category,
                    dbPassword,
                    dbPort,
                    dbSystemAccountId,
                    grantees,
                    os,
                    password,
                    remark,
                    type,
                } = this.data;
                let rePassword = dbPassword;
                if (password) {
                    rePassword = password;
                }
                this.formData = {
                    id,
                    account,
                    alias,
                    category,
                    dbPassword,
                    dbPort,
                    dbSystemAccountId,
                    grantees,
                    os,
                    password,
                    remark,
                    type,
                    rePassword,
                };
            }
            this.fetchRules();
        },
        methods: {
            fetchRules () {
                if (this.data.id) {
                    this.isLoading = false;
                    return Promise.resolve();
                }
                return QueryGlobalSettingService.fetchAllNameRule()
                    .then((data) => {
                        const { currentRules } = data;
                        this.currentRules = Object.freeze(currentRules.reduce((result, item) => {
                            result[item.osTypeKey] = item;
                            return result;
                        }, {}));
                    })
                    .catch(() => {
                        this.isRulesLoadingError = false;
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            
            createAccount () {
                const params = { ...this.formData };
                delete params.rePassword;
                return AccountManageService.createAccount(params)
                    .then(() => {
                        this.messageSuccess(I18n.t('account.新建账号成功'));
                        this.$emit('on-change');
                    });
            },
            updateAccount () {
                if (this.isRulesLoadingError) {
                    this.messageWarn(I18n.t('account.命名规则请求失败无法执行当前操作，请刷新页面'));
                    return Promise.reject(Error('rule error'));
                }
                const params = { ...this.formData };
                delete params.rePassword;
                return AccountManageService.updateAccount(params)
                    .then(() => {
                        this.messageSuccess(I18n.t('account.编辑账号成功'));
                        this.$emit('on-change');
                    });
            },
            handleCategoryChange (value) {
                this.formData = generatorDefault();
                this.formData.category = value;
                this.formData.type = value === ACCOUNT_TYPE_SYSTEM ? 1 : 9;
                this.$refs.operateAccountForm.clearError();
            },
            handleFieldChange (key, value) {
                this.formData[key] = value;
            },
            submit () {
                return this.$refs.operateAccountForm.validate()
                    .then(() => {
                        if (this.formData.id) {
                            return this.updateAccount();
                        }
                        return this.createAccount();
                    });
            },
        },
    };
</script>
<style lang="postcss">
    .operation-account {
        .radio-button-group-wraper {
            position: relative;
            z-index: 1;
        }

        .account-type-radio {
            width: 120px;

            .bk-radio-button-text {
                width: 100%;
            }
        }
    }
</style>
