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
    <smart-action offset-target="bk-form-content">
        <div class="notify-message-page">
            <bk-collapse
                v-if="!isLoading"
                v-model="activeResult">
                <notify-collapse-item
                    v-for="item in triggerTypeList"
                    :key="item.code"
                    :active="activeResult"
                    :name="item.code">
                    <span class="trigger-title">{{ item.name }}</span>
                    <trigger-setting
                        ref="setting"
                        slot="content"
                        :data="formData[item.code]"
                        :template-data="templateData"
                        :type="item.code" />
                </notify-collapse-item>
            </bk-collapse>
        </div>
        <template #action>
            <div class="action-wrapper">
                <bk-button
                    class="w120 mr10"
                    :disabled="isLoading"
                    :loading="isSubmiting"
                    theme="primary"
                    @click="handleSave">
                    {{ $t('notify.保存') }}
                </bk-button>
                <bk-button
                    :disabled="isLoading"
                    @click="handleCancel">
                    {{ $t('notify.重置') }}
                </bk-button>
            </div>
        </template>
    </smart-action>
</template>
<script>
    import NotifyService from '@service/notify';

    import NotifyCollapseItem from './components/notify-collapse-item';
    import TriggerSetting from './components/trigger-setting';

    import I18n from '@/i18n';

    export default {
        components: {
            NotifyCollapseItem,
            TriggerSetting,
        },
        data () {
            return {
                isLoading: true,
                isSubmiting: false,
                activeResult: [],
                templateData: {},
                formData: {},
            };
        },
        computed: {
            /**
             * @desc 页面输入骨架片 loading
             * @returns { Boolean }
             */
            isSkeletonLoading () {
                return this.isLoading;
            },
        },
        created () {
            this.isLoading = true;
            Promise.all([
                this.fetchPageTemplate(),
                this.fetchPoliciesList(),
            ]).finally(() => {
                this.isLoading = false;
            });
        },
        methods: {
            /**
             * @desc 获取页面数据
             */
            fetchPageTemplate () {
                return NotifyService.fetchPageTemplate()
                    .then((data) => {
                        const {
                            triggerTypeList,
                            availableNotifyChannelList,
                            executeStatusList,
                            resourceTypeList,
                            roleList,
                        } = data;
                        this.triggerTypeList = Object.freeze(triggerTypeList);
                        this.templateData = Object.freeze({
                            availableNotifyChannelList,
                            executeStatusList,
                            resourceTypeList,
                            roleList,
                        });
                        this.activeResult = triggerTypeList.map(({ code }) => code);
                    });
            },
            /**
             * @desc 获取消息通知配置的值
             */
            fetchPoliciesList () {
                return NotifyService.fetchPoliciesList()
                    .then((data) => {
                        const triggerPoliciesData = data.reduce((result, item) => {
                            const {
                                extraObserverList,
                                resourceStatusChannelMap,
                                resourceTypeList,
                                roleList,
                                triggerType,
                            } = item;
                            result[triggerType] = {
                                extraObserverList,
                                resourceStatusChannelMap,
                                resourceTypeList,
                                roleList,
                            };
                            return result;
                        }, {});
                        this.formData = Object.freeze(triggerPoliciesData);
                    });
            },
            /**
             * @desc 保存
             */
            handleSave () {
                this.isSubmiting = true;
                const triggerPoliciesList = this.$refs.setting.map(settingItem => settingItem.getValue());
                NotifyService.defaultPoliciesUpdate({
                    triggerPoliciesList,
                }).then(() => {
                    this.messageSuccess(I18n.t('notify.保存成功'));
                    return this.fetchPoliciesList();
                })
                    .finally(() => {
                        this.isSubmiting = false;
                    });
            },
            /**
             * @desc 重置
             */
            handleCancel () {
                this.$refs.setting.forEach(item => item.reset());
            },
        },
    };
</script>
<style lang="postcss" scoped>
    .notify-message-page {
        margin-bottom: 24px;

        .bk-collapse {
            border-bottom: none;
        }
    }
</style>
