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
    <router-link
        v-if="permission"
        v-bind="$attrs"
        v-on="$listeners">
        <slot />
    </router-link>
    <span
        v-else
        v-cursor
        class="not-permission"
        @click="handleCheckPermission">
        <slot />
    </span>
</template>
<script>
    import {
        permissionDialog,
    } from '@/common/bkmagic';

    export default {
        name: 'AuthRouterLink',
        inheritAttrs: false,
        props: {
            permission: {
                type: Boolean,
                required: true,
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
        methods: {
            /**
             * @desc 无权限时弹框提示资源权限申请
             */
            handleCheckPermission () {
                permissionDialog({
                    operation: this.auth,
                    resourceId: this.resourceId,
                    scopeType: this.scopeType,
                    scopeId: this.scopeId,
                });
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .not-permission {
        color: #c4c6cc;
        cursor: default;
    }
</style>
