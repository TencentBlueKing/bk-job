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
    <div class="toggle-display">
        <div v-show="isOpen">
            <slot />
        </div>
        <bk-form-item
            label=" "
            style="margin-top: -10px;">
            <div
                class="action"
                @click="handleToggle">
                <template v-if="isOpen">
                    <Icon
                        class="toggle-arrow"
                        type="angle-double-up" />
                    <span>{{ $t('template.收起更多设置') }}</span>
                </template>
                <template v-else>
                    <Icon
                        class="toggle-arrow"
                        type="angle-double-down" />
                    <span>{{ $t('template.展开更多设置') }}</span>
                </template>
            </div>
        </bk-form-item>
    </div>
</template>
<script>
    export default {
        name: 'ToggleDisplay',
        data () {
            return {
                isOpen: false,
            };
        },
        methods: {
            handleToggle () {
                this.isOpen = !this.isOpen;
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .toggle-display {
        .action {
            display: flex;
            align-items: center;
            font-size: 14px;
            color: #3a84ff;
            cursor: pointer;
        }

        .toggle-arrow {
            margin-right: 3px;
            font-size: 12px;
        }
    }
</style>
