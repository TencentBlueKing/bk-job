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
    <jb-sideslider
        :is-show="isShow"
        :quick-close="true"
        :show-footer="false"
        :title="$t('script.查看脚本')"
        :width="900"
        @update:isShow="handleCancel">
        <div
            v-if="isShow"
            v-bkloading="{ isLoading }">
            <script-detail
                v-if="!isLoading"
                :offset-bottom="0"
                :script-info="scriptInfo" />
        </div>
    </jb-sideslider>
</template>
<script>
    import PublicScriptService from '@service/public-script-manage';
    import ScriptService from '@service/script-manage';

    import {
        checkPublicScript,
    } from '@utils/assist';

    import ScriptDetail from '../../common/detail';

    export default {
        name: '',
        components: {
            ScriptDetail,
        },
        props: {
            isShow: {
                type: Boolean,
                default: false,
            },
            scriptVersionId: {
                type: Number,
                required: true,
            },
        },
        data () {
            return {
                isLoading: false,
                scriptInfo: {},
            };
        },
        watch: {
            isShow (isShow) {
                if (isShow && this.scriptVersionId > 0) {
                    this.fetchScriptDetail();
                }
            },
        },
        created () {
            this.publicScript = checkPublicScript(this.$route);
            this.serviceHandler = this.publicScript ? PublicScriptService : ScriptService;
        },
        methods: {
            fetchScriptDetail () {
                this.isLoading = true;
                this.serviceHandler.versionDetail({
                    id: this.scriptVersionId,
                }).then((data) => {
                    this.scriptInfo = Object.freeze(data);
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            handleCancel () {
                this.$emit('update:isShow', false);
            },
        },
    };
</script>
<style lang='postcss' scoped>
    %tab-item {
        display: flex;
        align-items: center;
        height: 43px;
        padding-left: 20px;
        font-size: 12px;
    }

    .script-detail {
        position: relative;

        .detail-layout {
            margin-bottom: 20px;

            .script-detail-version {
                font-size: 18px;
                color: #000;
            }

            .script-detail-status {
                width: 46px;
                height: 16px;
                padding: 0 5px;
                margin-right: 10px;
                margin-left: 8px;
                font-size: 12px;
                text-align: center;
            }

            .status-online {
                color: #45c272;
                background-color: #e5f6ea;
            }

            .status-disabled {
                color: #9fa2ac;
                background: #f0f1f5;
            }

            .detail-item {
                margin-top: 10px;
                margin-bottom: 0;
            }
        }

        .render-script-version {
            display: flex;
            margin-bottom: 60px;
        }

        .version-tab {
            display: flex;
            flex-direction: column;
            flex: 0 0 280px;
            border: 1px solid #dcdee5;
            user-select: none;

            .version-tab-title {
                @extend %tab-item;

                color: #313238;
                border-bottom: 1px solid #dcdee5;
            }

            .version-tab-item {
                @extend %tab-item;

                color: #63656e;
                cursor: pointer;
                background: #fff;
                border-bottom: 1px solid #dcdee5;

                &.active {
                    color: #3a84ff;
                    background: #f0f1f5 !important;

                    .version-active {
                        display: block;
                    }
                }

                &:hover {
                    background: #fafbfd;
                }

                .text {
                    max-width: 150px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: normal;
                }

                .online-flag {
                    display: flex;
                    width: 50px;
                    height: 16px;
                    margin-left: 8px;
                    color: #fff;
                    background: #ea3636;
                    border-radius: 8px;
                    align-items: center;
                    justify-content: center;
                }

                .version-active {
                    display: none;
                    margin-right: 12px;
                    margin-left: auto;
                    font-size: 14px;
                    color: #979ba5;
                }
            }
        }

        .version-content {
            display: flex;
            background-color: #f0f1f5;
            border: 1px solid #dcdee5;
            flex-direction: column;
            flex: 1;

            .content-tab {
                display: flex;
                border-bottom: 1px solid #dcdee5;
            }

            .content-tab-item {
                @extend %tab-item;

                padding-left: 0;
                color: #313238;
                cursor: pointer;
                border-right: 1px solid #dcdee5;
                flex: 0 0 120px;
                align-items: center;
                justify-content: center;

                &.active {
                    color: #3a84ff;
                    background: #fff;
                    border-top: 2px solid #3a84ff;
                }
            }

            .content-dispaly {
                min-height: 300px;
                background: #fff;
                flex: 1;
            }
        }

        .render-version-log {
            min-height: 480px;
            padding: 12px 10px;
            font-size: 12px;
            line-height: 20px;
            color: #63656e;
            white-space: pre-line;
        }

        .action-btn {
            width: 100px;
            margin-right: 10px;
        }
    }
</style>
