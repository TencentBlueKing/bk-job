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
    <bk-button
        v-if="showRaw"
        v-bind="$attrs"
        v-on="$listeners">
        <slot />
    </bk-button>
    <bk-button
        v-else
        v-cursor
        v-bind="$attrs"
        class="permission-disable"
        :loading="isLoading"
        @click.stop="handleRequestPermission">
        <slot />
    </bk-button>
</template>
<script>
    import PermissionCheckService from '@service/permission-check';

    import {
        permissionDialog,
    } from '@/common/bkmagic';

    export default {
        name: 'AuthButton',
        inheritAttrs: false,
        props: {
            permission: {
                type: [
                    Boolean, String,
                ],
                default: '',
            },
            auth: {
                type: String,
                required: true,
            },
            resourceId: {
                type: [
                    Number, String,
                ],
            },
            scopeType: String,
            scopeId: String,
        },
        data () {
            return {
                isLoading: false,
                hasPermission: false,
            };
        },
        computed: {
            showRaw () {
                if (this.permission) {
                    return true;
                }
                if (this.hasPermission) {
                    return true;
                }
                return false;
            },
        },
        watch: {
            resourceId (resourceId) {
                if (!resourceId) {
                    return;
                }
                this.checkPermission();
            },
        },
        created () {
            this.checkPermission();
            this.authResult = {};
        },
        methods: {
            /**
             * @desc 主动鉴权，指定资源和资源权限
             */
            fetchPermission () {
                this.isLoading = true;
                PermissionCheckService.fetchPermission({
                    operation: this.auth,
                    resourceId: this.resourceId,
                    scopeType: window.PROJECT_CONFIG.SCOPE_TYPE,
                    scopeId: window.PROJECT_CONFIG.SCOPE_ID,
                    returnPermissionDetail: true,
                }).then((data) => {
                    this.hasPermission = data.pass;
                    this.authResult = data;
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 判断预鉴权逻辑
             */
            checkPermission () {
                if (this.permission === '' && this.auth) {
                    this.fetchPermission();
                }
            },
            /**
             * @desc 无权限时弹框提示资源权限申请
             */
            handleRequestPermission () {
                if (this.isLoading) {
                    return;
                }
                permissionDialog({
                    operation: this.auth,
                    resourceId: this.resourceId,
                    scopeType: this.scopeType,
                    scopeId: this.scopeId,
                }, this.authResult);
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .permission-disable {
        color: #fff !important;
        background-color: #dcdee5 !important;
        border-color: #dcdee5 !important;

        &.bk-button-text {
            color: #c4c6cc !important;
            background: none !important;
        }
    }
</style>
