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
    <div class="sync-plan-script-content">
        <span class="sript-content-text">{{ $t('template.查看脚本') }}</span>
        <Icon
            class="script-content-detail"
            type="audit"
            @click="handleView" />
        <div
            v-if="isShowContent"
            ref="dialog"
            class="script-diff-dialog"
            :style="dialogStyles">
            <div class="content-title">
                <div class="content-old">
                    {{ $t('template.同步前') }}
                </div>
                <div class="content-new">
                    {{ $t('template.同步后') }}
                </div>
            </div>
            <div
                v-bkloading="{ isLoading }"
                class="content-wraper">
                <scroll-faker>
                    <jb-diff
                        :context="Infinity"
                        :format="'side-by-side'"
                        :new-content="newCode"
                        :old-content="oldCode"
                        theme="dark" />
                </scroll-faker>
            </div>
            <Icon
                class="content-close"
                type="close"
                @click="handleClose" />
        </div>
    </div>
</template>
<script>
    import { Base64 } from 'js-base64';

    import ScriptService from '@service/script-manage';

    import { findParent } from '@utils/vdom';

    import { findStep } from '../../common/utils';

    export default {
        name: '',
        data () {
            return {
                isLoading: true,
                isShowContent: false,
                content: '',
            };
        },
        computed: {
            dialogStyles () {
                return {
                    'z-index': window.__bk_zIndex_manager.nextZIndex(), // eslint-disable-line no-underscore-dangle
                };
            },
        },
        created () {
            const currentStep = findParent(this, 'DiffTaskStep');
            const dataSourceParent = findParent(this, 'SyncPlanStep2');
            const currentPlanStep = findStep(dataSourceParent.planStepList, currentStep.data.realId);
            const currentTemplateStep = findStep(dataSourceParent.templateStepList, currentStep.data.realId);
            
            Promise.all([
                this.fetchContent(currentPlanStep),
                this.fetchContent(currentTemplateStep),
            ]).then(([
                oldCode,
                newCode,
            ]) => {
                this.oldCode = oldCode;
                this.newCode = newCode;
            })
                .finally(() => {
                    this.isLoading = false;
                });
        },
        methods: {
            fetchContent (step) {
                const currentStepData = step.scriptStepInfo;
                if (currentStepData.scriptSource === 1) {
                    return Promise.resolve(Base64.decode(currentStepData.content));
                }
                if (!currentStepData.scriptVersionId) {
                    return Promise.resolve('');
                }
                return ScriptService.versionDetail({
                    id: currentStepData.scriptVersionId,
                }).then(data => Base64.decode(data.content));
            },
            handleView () {
                this.isShowContent = true;
                this.$nextTick(() => {
                    document.body.appendChild(this.$refs.dialog);
                });
            },
            handleClose () {
                if (this.$refs.dialog) {
                    document.body.removeChild(this.$refs.dialog);
                }
                this.isShowContent = false;
            },
        },
    };
</script>
<style lang='postcss'>
    .sync-plan-script-content {
        display: flex;
        align-items: center;
        height: 24px;

        .script-content-detail {
            padding: 4px 5px;
            font-size: 17px;
            color: #3a84ff;
            cursor: pointer;
        }
    }

    .script-diff-dialog {
        position: fixed;
        top: 0;
        right: 0;
        bottom: 0;
        left: 0;
        z-index: 9999;
        padding: 0 40px;
        background: #fff;

        .content-title {
            display: flex;
            align-items: center;
            height: 40px;
            line-height: 40px;

            .content-old,
            .content-new {
                flex: 1;
            }
        }

        .content-wraper {
            height: calc(100vh - 40px);
            line-height: initial;
            background: #1d1d1d;
        }

        .content-close {
            position: absolute;
            top: 0;
            right: 0;
            padding: 5px;
            font-size: 30px;
            color: #1d1d1d;
            cursor: pointer;
            transition: all 0.15s;

            &:hover {
                transform: rotateZ(90deg);
            }
        }
    }
</style>
