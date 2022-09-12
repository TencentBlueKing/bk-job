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
    <div class="dangerous-rule-manage-page">
        <table
            v-test="{ type: 'list', value: 'dangerousRule' }"
            class="rule-table">
            <thead>
                <tr>
                    <th style="width: 200px;">
                        {{ $t('dangerousRule.语法检测表达式') }}
                    </th>
                    <th>{{ $t('dangerousRule.规则说明') }}</th>
                    <th style="width: 300px;">
                        {{ $t('dangerousRule.脚本类型') }}
                    </th>
                    <th style="width: 300px;">
                        <span>{{ $t('dangerousRule.动作') }}</span>
                        <bk-popover placement="right">
                            <Icon
                                class="action-tips"
                                type="info" />
                            <div slot="content">
                                <div>{{ $t('dangerousRule.【扫描】') }}</div>
                                <div>{{ $t('dangerousRule.命中规则的脚本执行任务仅会做记录，不会拦截') }}</div>
                                <div style="margin-top: 8px;">
                                    {{ $t('dangerousRule.【拦截】') }}
                                </div>
                                <div>{{ $t('dangerousRule.命中规则的脚本执行任务会被记录，并中止运行') }}</div>
                            </div>
                        </bk-popover>
                    </th>
                    <th style="width: 180px;">
                        {{ $t('dangerousRule.操作') }}
                        <Icon
                            v-bk-tooltips="{
                                theme: 'dark',
                                content: $t('dangerousRule.规则的顺位越高，表示执行优先度越高'),
                            }"
                            class="action-tips"
                            type="info" />
                    </th>
                </tr>
            </thead>
            <table-action-row @on-change="handleAdd" />
            <tbody
                v-for="(rule, index) in list"
                :key="rule.id">
                <tr>
                    <td>
                        <jb-edit-input
                            field="expression"
                            mode="block"
                            :remote-hander="val => handleUpdate(rule, val)"
                            :rules="formRules.expression"
                            :value="rule.expression" />
                    </td>
                    <td>
                        <jb-edit-input
                            field="description"
                            mode="block"
                            :remote-hander="val => handleUpdate(rule, val)"
                            :rules="formRules.description"
                            :value="rule.description" />
                    </td>
                    <td>
                        <jb-edit-select
                            field="scriptTypeList"
                            :list="scriptTypeList"
                            mode="block"
                            multiple
                            :remote-hander="val => handleUpdate(rule, val)"
                            :rules="formRules.scriptTypeList"
                            show-select-all
                            :value="rule.scriptTypeList" />
                    </td>
                    <td>
                        <edit-action
                            :value="rule.action"
                            @on-change="action => handleUpdate(rule, { action })" />
                    </td>
                    <td>
                        <div class="action-box">
                            <bk-switcher
                                v-test="{ type: 'button', value: 'toggleRuleStatus' }"
                                class="mr10"
                                :false-value="0"
                                size="small"
                                theme="primary"
                                :true-value="1"
                                :value="rule.status"
                                @update="status => handleUpdate(rule, { status })" />
                            <bk-button
                                v-bk-tooltips.top="$t('dangerousRule.上移')"
                                v-test="{ type: 'button', value: 'upMoveRule' }"
                                class="arrow-btn mr10"
                                :disabled="index === 0"
                                text
                                @click="handleMove(index, -1)">
                                <Icon type="increase-line" />
                            </bk-button>
                            <bk-button
                                v-bk-tooltips.top="$t('dangerousRule.下移')"
                                v-test="{ type: 'button', value: 'downMoveRule' }"
                                class="arrow-btn mr10"
                                :disabled="index + 1 === list.length"
                                text
                                @click="handleMove(index, 1)">
                                <Icon type="decrease-line" />
                            </bk-button>
                            <jb-popover-confirm
                                :confirm-handler="() => handleDelete(rule.id)"
                                :content="$t('dangerousRule.脚本编辑器中匹配该规则将不会再收到提醒')"
                                :title="$t('dangerousRule.确定删除该规则？')">
                                <bk-button
                                    v-test="{ type: 'button', value: 'deleteRule' }"
                                    text>
                                    {{ $t('dangerousRule.删除') }}
                                </bk-button>
                            </jb-popover-confirm>
                        </div>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</template>
<script>
    import DangerousRuleService from '@service/dangerous-rule';
    import PublicScriptManageService from '@service/public-script-manage';

    import JbEditInput from '@components/jb-edit/input';
    import JbEditSelect from '@components/jb-edit/select';
    import JbPopoverConfirm from '@components/jb-popover-confirm';

    import EditAction from './components/edit-action';
    import TableActionRow from './components/table-action-row';

    import I18n from '@/i18n';

    export default {
        name: '',
        components: {
            JbEditInput,
            JbEditSelect,
            JbPopoverConfirm,
            TableActionRow,
            EditAction,
        },
        data () {
            return {
                isLoading: true,
                list: [],
                scriptTypeList: [],
            };
        },
        computed: {
            isSkeletonLoading () {
                return this.isLoading;
            },
        },
        created () {
            this.editRule = {};
            this.fetchData();
            this.fetchScriptType();

            this.formRules = {
                expression: [
                    {
                        required: true,
                        message: I18n.t('dangerousRule.语法检测表达式不能为空'),
                    },
                ],
                description: [
                    {
                        required: true,
                        message: I18n.t('dangerousRule.规则说明不能为空'),
                    },
                ],
                scriptTypeList: [
                    {
                        validator: value => value.length > 0,
                        message: I18n.t('dangerousRule.脚本类型不能为空'),
                    },
                ],
            };
        },
        methods: {
            /**
             * @desc 获取高危语句规则
             */
            fetchData () {
                this.isLoading = true;
                DangerousRuleService.fetchList({}, {
                    permission: 'page',
                })
                    .then((data) => {
                        this.list = data;
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 获取支持的脚本类型列表
             */
            fetchScriptType () {
                PublicScriptManageService.scriptTypeList()
                    .then((data) => {
                        this.scriptTypeList = data;
                    });
            },
            /**
             * @desc 更新脚本类型
             * @param {String} rule 高危语句规则
             * @param {Array} scriptTypeList 脚本语言列表哦
             */
            handleScriptTypeUpdate (rule, scriptTypeList) {
                this.editRule = {
                    ...rule,
                    scriptTypeList,
                };
            },
            /**
             * @desc 脚本语言下拉框收起时提交更新
             * @param {Boolean} toggle 脚本语言下拉框收起状态
             */
            handleSubmitScriptTypeChange (toggle) {
                if (!toggle
                    && this.editRule.scriptTypeList
                    && this.editRule.scriptTypeList.length > 0) {
                    DangerousRuleService.update({
                        ...this.editRule,
                    }).then(() => {
                        this.messageSuccess(I18n.t('dangerousRule.编辑成功'));
                    });
                }
            },
            /**
             * @desc 更新高危语句配置
             * @param {Object} rule 高危语句规则
             * @param {Object} payload 脚本语言列表哦
             */
            handleUpdate (rule, payload) {
                return DangerousRuleService.update({
                    ...rule,
                    ...payload,
                }).then(() => {
                    this.messageSuccess(I18n.t('dangerousRule.编辑成功'));
                    Object.assign(rule, payload);
                });
            },
            /**
             * @desc 添加一条高危语句
             */
            handleAdd () {
                this.fetchData();
            },
            /**
             * @desc 移动高危语句的顺序
             * @param {Number} index 当前语句的位置索引
             * @param {Number} step 移动的步数
             */
            handleMove (index, step) {
                this.isLoading = true;
                DangerousRuleService.updateSort({
                    id: this.list[index].id,
                    dir: step,
                }).then(() => {
                    const current = this.list[index];
                    const change = this.list[index + step];
                    this.list.splice(index, 1, change);
                    this.list.splice(index + step, 1, current);
                    this.messageSuccess(step < 0 ? I18n.t('dangerousRule.上移成功') : I18n.t('dangerousRule.下移成功'));
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 删除高危语句
             * @param {Number} id 高危语句的id
             */
            handleDelete (id) {
                return DangerousRuleService.remove({
                    id,
                }).then(() => {
                    this.messageSuccess(I18n.t('dangerousRule.删除成功'));
                    this.fetchData();
                });
            },
        },
    };
</script>
<style lang='postcss'>
    .dangerous-rule-manage-page {
        .rule-table {
            width: 100%;
            border: 1px solid #dcdee5;
            border-radius: 2px;
            table-layout: fixed;

            th,
            td {
                height: 40px;
                padding-right: 15px;
                padding-left: 15px;
                font-size: 12px;
                color: #63656e;
                text-align: left;
                border-top: 1px solid #dcdee5;
            }

            th {
                font-weight: normal;
                color: #313238;
                background: #fafbfd;
            }

            td {
                background: #fff;
            }

            .bk-button-text {
                font-size: 12px;

                .icon-plus {
                    font-size: 18px;
                }
            }
        }

        .input {
            width: 100%;

            .bk-form-input {
                height: 26px;
            }
        }

        .action-tips {
            color: #c4c6cc;
        }

        .action-box {
            display: flex;
            align-items: center;

            .arrow-btn {
                font-size: 16px;
            }
        }

        .script-type-edit {
            &.bk-select {
                margin-left: -10px;
                border-color: transparent;

                &.is-focus {
                    border-color: #3a84ff;
                }

                &:hover {
                    background: #f0f1f5;

                    .bk-select-angle {
                        display: block;
                    }
                }

                .bk-select-angle {
                    display: none;
                }
            }
        }

        .bk-select {
            line-height: 24px;

            .bk-select-name {
                height: 24px;
            }

            .bk-select-angle {
                top: 2px;
            }
        }
    }
</style>
