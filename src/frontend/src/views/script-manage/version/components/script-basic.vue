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
    <div ref="wraper" class="script-detail-wraper" v-bkloading="{ isLoading }">
        <div v-if="data.id" class="detail-row">
            <div class="detail-col">
                <div class="item-label">{{ $t('script.脚本名称：') }}</div>
                <div class="item-value">
                    <auth-component
                        auth="script/edit"
                        :resource-id="data.id">
                        <jb-edit-input
                            style="width: 100%;"
                            field="scriptName"
                            :value="data.name"
                            :remote-hander="val => handleUpdateScript('scriptName', val)" />
                        <div slot="forbid">{{ data.name }}</div>
                    </auth-component>
                </div>
            </div>
            <div class="detail-col">
                <div class="item-label">{{ $t('script.场景标签：') }}</div>
                <div class="item-value">
                    <auth-component
                        auth="script/edit"
                        :resource-id="data.id">
                        <jb-edit-tag
                            class="input"
                            field="scriptTags"
                            :rows="1"
                            :value="data.tags"
                            :remote-hander="val => handleUpdateScript('scriptTags', val)" />
                        <div slot="forbid">{{ data.tagText }}</div>
                    </auth-component>
                </div>
            </div>
            <div class="detail-col col2">
                <div class="item-label">{{ $t('script.描述：') }}</div>
                <div class="item-value">
                    <jb-edit-textarea
                        field="scriptDesc"
                        single-row-render
                        :placeholder="$t('script.在此处标注该脚本的备注和使用说明')"
                        :maxlength="500"
                        :rows="1"
                        :value="data.description"
                        :remote-hander="val => handleUpdateScript('scriptDesc', val)" />
                </div>
            </div>
        </div>
        <div class="detail-row">
            <div class="detail-col">
                <div class="item-label">{{ $t('script.更新人：') }}</div>
                <div class="item-value">
                    <div class="text-box" v-bk-overflow-tips>{{ data.lastModifyUser }}</div>
                </div>
            </div>
            <div class="detail-col">
                <div class="item-label">{{ $t('script.更新时间：') }}</div>
                <div class="item-value">
                    <div class="text-box" v-bk-overflow-tips>{{ data.lastModifyTime }}</div>
                </div>
            </div>
            <div class="detail-col">
                <div class="item-label">{{ $t('script.创建人：') }}</div>
                <div class="item-value">
                    <div class="text-box" v-bk-overflow-tips>{{ data.creator }}</div>
                </div>
            </div>
            <div class="detail-col">
                <div class="item-label">{{ $t('script.创建时间：') }}</div>
                <div class="item-value" v-bk-overflow-tips>
                    <div class="text-box">{{ data.createTime }}</div>
                </div>
            </div>
        </div>
    </div>
</template>
<script>
    import ScriptService from '@service/script-manage';
    import PublicScriptService from '@service/public-script-manage';
    import {
        checkPublicScript,
    } from '@utils/assist';
    import JbEditInput from '@components/jb-edit/input';
    import JbEditTag from '@components/jb-edit/tag';
    import JbEditTextarea from '@components/jb-edit/textarea';

    export default {
        name: '',
        components: {
            JbEditInput,
            JbEditTag,
            JbEditTextarea,
        },
        data () {
            return {
                isLoading: true,
                data: {},
            };
        },
        created () {
            this.publicScript = checkPublicScript(this.$route);
            this.serviceHandler = this.publicScript ? PublicScriptService : ScriptService;
            this.scriptId = this.$route.params.id;
            this.fetchScriptBasic();
        },
        methods: {
            /**
             * @desc 获取脚本基本信息
             */
            fetchScriptBasic () {
                this.isLoading = true;
                this.serviceHandler.fetchBasicInfo({
                    id: this.scriptId,
                }).then((data) => {
                    this.data = Object.freeze(data);
                })
                    .finally(() => {
                        this.isLoading = false;
                        this.$nextTick(() => {
                            this.calcLableWidth();
                        });
                    });
            },
            /**
             * @desc 计算脚本lable的宽度
             */
            calcLableWidth () {
                const $lableEles = this.$refs.wraper.querySelectorAll('.item-label');
                let maxWidth = 0;
                $lableEles.forEach((ele) => {
                    const { width } = ele.getBoundingClientRect();
                    maxWidth = Math.max(maxWidth, width);
                });
                $lableEles.forEach((ele) => {
                    ele.style.width = `${maxWidth + 2}px`;
                });
            },
            /**
             * @desc 更新脚本基本信息
             * @param {String} field 指定更新的字段名
             * @param {Object} payload 更新的字段key和value
             */
            handleUpdateScript (field, payload) {
                return this.serviceHandler.scriptUpdateMeta({
                    id: this.scriptId,
                    ...payload,
                    updateField: field,
                });
            },
        },
    };
</script>
<style lang='postcss'>
    .script-detail-wraper {
        height: 80px;
        padding: 8px 24px;
        background: #fff;
        box-shadow: 0 1px 1px 0 rgba(0, 0, 0, 0.1);

        .detail-row {
            display: flex;
        }

        .detail-col {
            display: flex;
            flex: 0 0 25%;
            width: 0;
            padding-right: 20px;
            font-size: 12px;
            line-height: 32px;

            &.col2 {
                flex-basis: 50%;
            }

            .item-label {
                flex: 0 0 auto;
                color: #b2b5bd;
            }

            .item-value {
                flex: 1;
                width: 0;
                padding-right: 20px;
                color: #63656e;

                .text-box {
                    height: 32px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }
            }
        }
    }
</style>
