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
    <div>
        <div
            class="row"
            :class="diff.approvalUser">
            <div class="label">
                {{ $t('template.确认人：') }}
            </div>
            <div class="value">
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
                        v-for="user in data.approvalUser.userList"
                        :key="user"
                        class="item">
                        <Icon
                            class="approval-flag"
                            type="user" />
                        {{ user }}
                    </div>
                </div>
            </div>
        </div>
        <div
            class="row"
            :class="diff.notifyChannel">
            <div class="label">
                {{ $t('template.通知方式：') }}
            </div>
            <div class="value">
                {{ renderChannel }}
            </div>
        </div>
        <div
            class="row"
            :class="diff.approvalMessage">
            <div class="label">
                {{ $t('template.确认描述：') }}
            </div>
            <div class="value">
                {{ data.approvalMessage || '-' }}
            </div>
        </div>
    </div>
</template>
<script>
    import NotifyService from '@service/notify';
    import QueryGlobalSettingService from '@service/query-global-setting';

    export default {
        name: '',
        inheritAttrs: false,
        props: {
            data: {
                type: Object,
                required: true,
            },
            diff: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                renderRoleList: [],
                renderChannel: '',
            };
        },
        created () {
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
                        this.renderRoleList = this.data.approvalUser.roleList.map(_ => roleMap[_]);
                    });
            },
            fetchAllChannel () {
                QueryGlobalSettingService.fetchActiveNotifyChannel()
                    .then((data) => {
                        const channelMap = {};
                        data.forEach((channel) => {
                            channelMap[channel.code] = channel.name;
                        });
                        this.renderChannel = this.data.notifyChannel.map(_ => channelMap[_]).join('，');
                    });
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .approval-wraper {
        display: inline-flex;
        align-items: center;
        flex-wrap: wrap;
        margin-top: -10px;

        .item {
            display: flex;
            height: 20px;
            padding: 0 6px;
            margin-top: 10px;
            margin-right: 10px;
            font-size: 12px;
            color: #63656e;
            white-space: nowrap;
            background: #f0f1f5;
            border-radius: 2px;
            align-items: center;
        }

        .approval-flag {
            margin-right: 4px;
        }
    }
</style>
