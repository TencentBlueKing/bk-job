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
    <bk-select
        :clearable="false"
        style="width: 495px;"
        :value="formData[field]"
        @change="handleChange"
        searchable>
        <bk-option
            v-for="option in accountList"
            :key="option.id"
            :id="option.id"
            :name="option.alias" />
    </bk-select>
</template>
<script>
    import AccountManageService from '@service/account-manage';

    export default {
        props: {
            field: {
                type: String,
                required: true,
            },
            scriptLanguageField: {
                type: String,
            },
            formData: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                accountType: '',
                systemAccountList: [],
                dbAccountList: [],
            };
        },
        computed: {
            accountList () {
                return this.systemAccountList;
            },
        },
        created () {
            this.fetchSystemAccount();
            this.fetchDbAccount();
        },
        methods: {
            /**
             * @desc 获取系统账号
             */
            fetchSystemAccount () {
                AccountManageService.fetchAccountWhole({
                    category: 1,
                }).then((data) => {
                    this.systemAccountList = data;
                    setTimeout(() => {
                        this.initDefaultAccount();
                    });
                })
                    .finally(() => {
                        this.requestQueue.pop();
                    });
            },
            /**
             * @desc 获取db账号
             */
            fetchDbAccount () {
                AccountManageService.fetchAccountWhole({
                    category: 2,
                }).then((data) => {
                    this.dbAccountList = data;
                    setTimeout(() => {
                        this.initDefaultAccount();
                    });
                })
                    .finally(() => {
                        this.requestQueue.pop();
                    });
            },
            /**
             * @desc 初始化默认值
             */
            initDefaultAccount () {
                if (this.isLoading) {
                    return;
                }
                //  编辑状态
                // 1, 现有账号不存在于当前账号列表中默认重置为空
                // 2, 切换脚本类型不做默认值处理
                if (this.formData.id) {
                    if (this.formData[this.field]
                        && !this.accountList.find(item => item.id === this.formData[this.field])) {
                        this.handleChange('');
                    }
                    return;
                }

                // 新建时账号不存在于当前账号列表中，默认使用账号列表的第一个
                if (!this.accountList.find(item => item.id === this.formData[this.field])) {
                    this.handleChange(this.accountList[0].id);
                }
            },
            handleChange (value) {
                this.$emit('on-change', this.field, value);
            },
        },
    };
</script>
