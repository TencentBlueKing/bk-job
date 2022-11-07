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
        class="task-import-step3">
        <div class="layout-wraper">
            <div class="layout-left">
                <scroll-faker>
                    <div
                        v-for="templateItem in templateInfoOrigin"
                        :key="templateItem.id"
                        class="task-box"
                        :class="{
                            active: templateItem.id === activeTemplateId,
                            disable: !templateInfoMap[templateItem.id].checked,
                        }"
                        @click="handleSelectTemplate(templateItem.id)">
                        <div class="task-name">
                            {{ templateNameMap[templateItem.id] }}
                        </div>
                        <bk-switcher
                            size="small"
                            theme="primary"
                            :value="templateInfoMap[templateItem.id].checked"
                            @change="value => handleTemplateChange(value, templateItem.id)"
                            @click.stop="" />
                    </div>
                </scroll-faker>
            </div>
            <div class="layout-right">
                <scroll-faker>
                    <div
                        v-if="activeTemplateId"
                        class="wraper">
                        <div class="task-header">
                            <span>{{ templateNameMap[activeTemplateId] }}</span>
                            <span
                                v-if="!templateInfoMap[activeTemplateId].checked"
                                class="invalid-flag">
                                {{ $t('template.不导入') }}
                            </span>
                        </div>
                        <template v-if="currentPlanList.length > 0">
                            <div class="task-plan-header">
                                <span>{{ $t('template.选择要导入的作业执行方案') }}</span>
                                <bk-button
                                    v-if="templateInfoMap[activeTemplateId].exportAll"
                                    class="whole-check"
                                    :disabled="!templateInfoMap[activeTemplateId].checked"
                                    text
                                    @click="handleCancelWholePlan">
                                    {{ $t('template.取消全选') }}
                                </bk-button>
                                <bk-button
                                    v-else
                                    class="whole-check"
                                    :disabled="!templateInfoMap[activeTemplateId].checked"
                                    text
                                    @click="handleSelectWholePlan">
                                    {{ $t('template.全选') }}
                                </bk-button>
                            </div>
                            <div class="task-plan-list">
                                <div
                                    v-for="planIdItem in currentPlanList"
                                    :key="planIdItem"
                                    class="plan-box"
                                    :class="{
                                        invalid: !templateInfoMap[activeTemplateId].planIdMap[planIdItem],
                                        disable: !templateInfoMap[activeTemplateId].checked,
                                    }"
                                    @click="handleTogglePlan(planIdItem)">
                                    <div class="plan-name">
                                        {{ planNameMap[planIdItem] }}
                                    </div>
                                    <div
                                        class="plan-check"
                                        :class="{
                                            active: templateInfoMap[activeTemplateId].planIdMap[planIdItem],
                                            disable: !templateInfoMap[activeTemplateId].checked,
                                        }" />
                                </div>
                            </div>
                        </template>
                        <empty
                            v-else
                            style="margin-top: 100px;"
                            :title="$t('template.暂无执行方案')" />
                    </div>
                </scroll-faker>
            </div>
        </div>
        <action-bar>
            <bk-button
                class="mr10"
                @click="handleCancel">
                {{ $t('template.取消') }}
            </bk-button>
            <bk-button
                class="mr10"
                @click="handleLast">
                {{ $t('template.上一步') }}
            </bk-button>
            <bk-button
                class="w120"
                theme="primary"
                @click="handleNext">
                {{ $t('template.下一步') }}
            </bk-button>
        </action-bar>
    </div>
</template>
<script>
    import BackupService from '@service/backup';

    import { taskImport } from '@utils/cache-helper';

    import Empty from '@components/empty';

    import ActionBar from '../components/action-bar';

    import I18n from '@/i18n';

    export default {
        name: '',
        components: {
            Empty,
            ActionBar,
        },
        data () {
            return {
                id: '',
                isLoading: false,
                activeTemplateId: 0,
                templateInfoOrigin: [],
                templateInfoMap: {},
                planNameMap: {},
                templateNameMap: {},
            };
        },
        computed: {
            currentPlanList () {
                return this.templateInfoMap[this.activeTemplateId].planIdOrigin;
            },
        },
        created () {
            this.id = taskImport.getItem('id');
            this.fetchData();
        },
        methods: {
            fetchData () {
                this.$request(BackupService.fetchImportInfo({
                    id: this.id,
                }), () => {
                    this.isLogLoading = true;
                }).then((data) => {
                    const { idNameInfo, templateInfo } = data;
                    this.templateInfoOrigin = Object.freeze(templateInfo);
                    this.templateInfoMap = templateInfo.reduce((result, item) => {
                        const originPlan = item.planId || [];
                        result[item.id] = {
                            checked: true,
                            id: item.id,
                            planIdOrigin: [
                                ...originPlan,
                            ],
                            planIdMap: originPlan.reduce((planMap, item) => {
                                planMap[item] = true;
                                return planMap;
                            }, {}),
                            exportAll: 1,
                        };
                        return result;
                    }, {});
                    if (templateInfo.length > 0) {
                        this.activeTemplateId = templateInfo[0].id;
                    }
                    this.planNameMap = Object.freeze(idNameInfo.planNameMap);
                    this.templateNameMap = Object.freeze(idNameInfo.templateNameMap);
                })
                    .finally(() => {
                        this.isLogLoading = false;
                    });
            },
            handleSelectTemplate (templateId) {
                this.activeTemplateId = templateId;
            },
            handleTemplateChange (checked, templateId) {
                this.templateInfoMap[templateId].checked = checked;
            },
            handleCancelWholePlan () {
                this.templateInfoMap[this.activeTemplateId].planIdMap = {};
                this.templateInfoMap[this.activeTemplateId].exportAll = 0;
            },
            handleSelectWholePlan () {
                const templateInfoMap = { ...this.templateInfoMap };
                const currentTemplate = templateInfoMap[this.activeTemplateId];
                currentTemplate.planIdOrigin.forEach((item) => {
                    currentTemplate.planIdMap[item] = true;
                });
                currentTemplate.exportAll = 1;
            },
            handleTogglePlan (planId) {
                if (!this.templateInfoMap[this.activeTemplateId].checked) {
                    return;
                }
                const templateInfoMap = { ...this.templateInfoMap };
                const currentTemplate = templateInfoMap[this.activeTemplateId];
                if (currentTemplate.planIdMap[planId]) {
                    delete currentTemplate.planIdMap[planId];
                    currentTemplate.exportAll = 0;
                } else {
                    currentTemplate.planIdMap[planId] = true;
                    currentTemplate.exportAll
                        = Number(Object.keys(currentTemplate.planIdMap).length === currentTemplate.planIdOrigin.length);
                }
                this.templateInfoMap = templateInfoMap;
            },
            handleCancel () {
                this.$emit('on-cancle');
            },
            handleLast () {
                this.$emit('on-change', 2);
            },
            handleNext () {
                const templateInfo = [];
                for (const templateId in this.templateInfoMap) {
                    const currentTemplate = this.templateInfoMap[templateId];
                    if (!currentTemplate.checked) {
                        continue;
                    }
                    templateInfo.push({
                        id: currentTemplate.id,
                        exportAll: currentTemplate.exportAll,
                        planId: Object.keys(currentTemplate.planIdMap),
                    });
                }
                if (templateInfo.length < 1) {
                    this.messageError(I18n.t('template.导入内容不能为空'));
                    return;
                }
                taskImport.setItem('id', this.id);
                taskImport.setItem('templateInfo', templateInfo);
                this.$emit('on-change', 4);
            },
        },
    };
</script>
<style lang='postcss'>
    .task-import-step3 {
        height: 100%;

        .layout-wraper {
            display: flex;
            height: 100%;
            background: #fff;
        }

        .layout-left,
        .layout-right {
            height: calc(100vh - 205px);
        }

        .layout-left {
            position: relative;
            flex: 0 0 360px;
            background: #fafbfd;

            &::after {
                position: absolute;
                top: 0;
                right: 0;
                bottom: 0;
                width: 1px;
                background: #dcdee5;
                content: "";
            }
        }

        .layout-right {
            flex: 1;
            padding-left: 50px;

            .wraper {
                width: 680px;
            }
        }

        .task-box {
            display: flex;
            height: 40px;
            padding: 0 22px;
            font-size: 14px;
            color: #63656e;
            cursor: pointer;
            border-bottom: 1px solid #dcdee5;
            transition: all 0.15s;
            align-items: center;

            &.active {
                position: relative;
                z-index: 1;
                background: #fff;
            }

            &.disable {
                color: #c4c6cc;
            }

            .task-name {
                margin-right: auto;
            }
        }

        .task-header {
            display: flex;
            padding: 40px 0 16px;
            font-size: 18px;
            line-height: 24px;
            color: #000;
            border-bottom: 1px solid #f0f1f5;

            .invalid-flag {
                height: 16px;
                padding: 0 5px;
                margin-top: 2px;
                margin-left: 8px;
                font-size: 12px;
                line-height: 16px;
                color: #63656e;
                background: #f0f1f5;
                border-radius: 2px;
            }
        }

        .task-plan-header {
            display: flex;
            padding: 20px 0 13px;
            font-size: 14px;
            color: #313238;

            .whole-check {
                margin-left: auto;
            }
        }

        .task-plan-list {
            border: 1px solid #dcdee5;
            border-radius: 2px;
        }

        .plan-box {
            display: flex;
            height: 40px;
            padding: 0 20px;
            font-size: 12px;
            color: #63656e;
            cursor: pointer;
            border-bottom: 1px solid #dcdee5;
            transition: all 0.15s;
            align-items: center;

            &.invalid,
            &.disable {
                color: #c4c6cc;
            }

            &.disable {
                cursor: not-allowed;
            }

            &:last-child {
                border-bottom: none;
            }

            .plan-check {
                position: relative;
                width: 18px;
                height: 18px;
                margin-left: auto;
                border: 1px solid #dcdee5;
                border-radius: 50%;
                transition: all 0.15s;

                &.active {
                    background: #3a84ff;
                    border-color: #3a84ff;

                    &::after {
                        position: absolute;
                        top: 4px;
                        left: 3px;
                        width: 8px;
                        height: 4px;
                        border-bottom: 2px solid #fff;
                        border-left: 2px solid #fff;
                        content: "";
                        transform: rotate(-45deg) scaleY(1);
                        transform-origin: center;
                    }
                }

                &.disable {
                    background-color: #fafbfd;
                    border-color: #dcdee5;
                }
            }
        }
    }
</style>
