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
    <div class="jb-apply-permission">
        <div class="no-permission-tips">
            <img
                class="lock"
                src="/static/images/no-permission.svg">
            <p class="tips-text">
                {{ titleText }}
            </p>
        </div>
        <div
            v-bkloading="{ isLoading: loading }"
            class="apply-permission-content"
            :style="listStyle">
            <bk-table
                v-if="!loading"
                class="apply-permission-table"
                :data="permissionList">
                <bk-table-column
                    :label="actionText"
                    :width="300">
                    <template slot-scope="{ row }">
                        {{ row.actionName }}
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="resourceText">
                    <template slot-scope="{ row }">
                        <div class="resource-content">
                            <template v-if="row.relatedResources.length > 0">
                                <p
                                    v-for="(resource, index) in row.relatedResources"
                                    :key="index">
                                    <span>{{ resource.resourceTypeName }}</span>：
                                    <span>{{ resource.resourceName }}</span>
                                </p>
                            </template>
                            <span v-else>--</span>
                        </div>
                    </template>
                </bk-table-column>
                <div slot="empty">
                    <span v-if="!loading">{{ errorTips }}</span>
                </div>
            </bk-table>
        </div>
    </div>
</template>
<script>
    import I18n from '@/i18n';

    export default {
        props: {
            maxHeight: {
                type: Number,
                default: 0,
            },
            permissionList: {
                type: Array,
                default: () => ([]),
            },
            loading: {
                type: Boolean,
            },
        },
        computed: {
            listStyle () {
                const styles = {};
                if (this.loading) {
                    styles['min-height'] = '80px';
                }
                if (this.maxHeight) {
                    styles.maxHeight = `${this.maxHeight}px`;
                    styles.overflow = 'auto';
                }
                return styles;
            },
        },
        created () {
            this.titleText = I18n.t('该操作需要以下权限');
            this.actionText = I18n.t('需申请的权限');
            this.resourceText = I18n.t('关联的资源实例');
            this.errorTips = I18n.t('你已拥有权限，请刷新页面');
        },
    };
</script>
<style lang="postcss" scoped>
    .jb-apply-permission {
        .no-permission-tips {
            text-align: center;
        }

        .lock {
            width: 120px;
            height: 100px;
        }

        .tips-text {
            margin: 8px 0 22px;
            font-size: 20px;
            color: #63656e;
        }

        .apply-permission-table {
            border: none;
        }

        .bk-table-outer-border::after {
            display: none;
        }

        .resource-content {
            padding: 10px 0;

            p {
                line-height: 24px;
            }
        }
    }
</style>
