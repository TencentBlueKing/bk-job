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
        class="artificial-view">
        <detail-item :label="$t('template.确认人：')">
            <div class="approval-wraper">
                <div
                    v-for="role in renderRoleList"
                    :key="role"
                    class="item">
                    <Icon
                        class="approval-flag"
                        type="user-group-gray" />
                    {{ role }}
                </div>
                <div
                    v-for="user in stepInfo.approvalUser.userList"
                    :key="user"
                    class="item">
                    <Icon
                        class="approval-flag"
                        type="user" />
                    {{ user }}
                </div>
            </div>
        </detail-item>
        <detail-item :label="$t('template.通知方式：')">
            {{ renderChannel }}
        </detail-item>
        <detail-item :label="$t('template.确认描述：')">
            {{ stepInfo.approvalMessage || '--' }}
        </detail-item>
        <slot />
    </div>
</template>
<script>
    import NotifyService from '@service/notify';
    import QueryGlobalSettingService from '@service/query-global-setting';

    import DetailItem from '@components/detail-layout/item';

    export default {
        name: '',
        components: {
            DetailItem,
        },
        inheritAttrs: false,
        props: {
            data: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                isLoading: true,
                stepInfo: {},
                renderRoleList: [],
                renderChannel: '',
            };
        },
        created () {
            this.stepInfo = Object.freeze(this.data.approvalStepInfo);
            Promise.all([
                this.fetchRoleList(),
                this.fetchAllChannel(),
            ]).finally(() => {
                this.isLoading = false;
            });
        },
        methods: {
            fetchRoleList () {
                NotifyService.fetchRoleList()
                    .then((data) => {
                        const roleMap = {};
                        data.forEach((role) => {
                            roleMap[role.code] = role.name;
                        });
                        // 过滤掉已经被删除的角色分组
                        this.renderRoleList = this.stepInfo.approvalUser.roleList.reduce((result, item) => {
                            if (roleMap[item]) {
                                result.push(roleMap[item]);
                            }
                            return result;
                        }, []);
                    });
            },
            fetchAllChannel () {
                if (this.stepInfo.notifyChannel.length < 1) {
                    this.renderChannel = '--';
                    return Promise.resolve();
                }
                QueryGlobalSettingService.fetchActiveNotifyChannel()
                    .then((data) => {
                        const channelMap = {};
                        data.forEach((channel) => {
                            channelMap[channel.code] = channel.name;
                        });
                        const channel = this.stepInfo.notifyChannel.reduce((result, item) => {
                            // 过滤掉已经被删除的通知渠道
                            if (channelMap[item]) {
                                result.push(channelMap[item]);
                            }
                            return result;
                        }, []);
                        if (channel.length < 1) {
                            this.renderChannel = '--';
                        }
                        this.renderChannel = channel.join('，');
                    });
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .artificial-view {
        .detail-item {
            margin-bottom: 0;
        }

        .approval-wraper {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            min-height: 34px;
            margin-top: -5px;

            .item {
                display: flex;
                height: 20px;
                padding: 0 6px;
                margin-top: 10px;
                margin-right: 10px;
                font-size: 12px;
                color: #63656e;
                background: #f0f1f5;
                border-radius: 2px;
                align-items: center;
            }

            .approval-flag {
                margin-right: 4px;
            }
        }
    }
</style>
