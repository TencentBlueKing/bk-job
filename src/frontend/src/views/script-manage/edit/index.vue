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
    <div style="padding: 6px 0 0 20px;" v-bkloading="{ isLoading }">
        <jb-form :label-width="90" ref="form">
            <jb-form-item :label="$t('script.版本号.label')" :required="true">
                <bk-input class="input" :value="formData.version" readonly />
            </jb-form-item>
            <jb-form-item :label="$t('script.版本日志')">
                <bk-input class="input" v-model="formData.versionDesc" type="textarea" :maxlength="100" />
            </jb-form-item>
            <jb-form-item :label="$t('script.脚本内容')">
                <ace-editor
                    v-model="formData.content"
                    :lang="formData.typeName"
                    :readonly="!isDraft"
                    :options="[formData.typeName]"
                    @on-mode-change="handleTypeChange" />
            </jb-form-item>
            <jb-form-item>
                <div style="margin-top: 10px;">
                    <bk-button theme="primary" @click.stop.prevent="submitData" style="width: 120px; margin-right: 10px;">{{ $t('script.提交') }}</bk-button>
                    <bk-button theme="default" @click="handleCancel">{{ $t('script.取消') }}</bk-button>
                </div>
            </jb-form-item>
        </jb-form>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import ScriptService from '@service/script-manage';
    import PublicScriptService from '@service/public-script-manage';
    import JbForm from '@components/jb-form';
    import JbFormItem from '@components/jb-form/item';
    import AceEditor from '@components/ace-editor';
    import {
        formatScriptTypeValue,
        isPublicScript,
    } from '@utils/assist';

    export default {
        name: '',
        components: {
            JbForm,
            JbFormItem,
            AceEditor,
        },
        data () {
            return {
                isLoading: false,
                isDraft: false,
                formData: {
                    id: '',
                    name: '',
                    typeName: 'Shell',
                    version: '',
                    versionDesc: '',
                    type: 1,
                    content: '',
                },
            };
        },
        created () {
            this.publicScript = isPublicScript(this.$route);
            this.serviceHandler = this.publicScript ? PublicScriptService : ScriptService;
            this.scriptVersionId = this.$route.params.id;
            this.fetchScriptVersion();
        },
        methods: {
            /**
             * @desc 获取指定版本的脚本内容
             */
            fetchScriptVersion () {
                this.$request(this.serviceHandler.versionDetail({
                    id: this.scriptVersionId,
                }, {
                    permission: 'page',
                }), () => {
                    this.isLoading = true;
                }).then((scriptData) => {
                    const { content, id, name, version, versionDesc, type, typeName, isDraft } = scriptData;
                    this.formData = {
                        ...this.formData,
                        id,
                        name,
                        version,
                        versionDesc,
                        type,
                        typeName,
                        content,
                    };
                    this.isDraft = isDraft;
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 脚本语言类型切换
             * @param {String} type 脚本语言
             */
            handleTypeChange (type) {
                this.formData.type = formatScriptTypeValue(type);
            },
            /**
             * @desc 保存脚本
             */
            submitData () {
                const params = {
                    ...this.formData,
                };
                if (!params.id) {
                    delete params.id;
                }
                this.isLoading = true;
                this.serviceHandler.scriptUpdate({
                    ...params,
                    publicScript: false,
                    scriptVersionId: this.scriptVersionId,
                }).then(() => {
                    this.messageSuccess(I18n.t('script.操作成功'), () => {
                        this.handleCancel();
                    });
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 取消编辑
             */
            handleCancel () {
                this.routerBack();
            },
            /**
             * @desc 路由回退
             */
            routerBack () {
                if (this.publicScript) {
                    this.$router.push({
                        name: 'publicScriptVersion',
                        params: {
                            id: this.formData.id,
                        },
                    });
                    return;
                }
                this.$router.push({
                    name: 'scriptVersion',
                    params: {
                        id: this.formData.id,
                    },
                });
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .input {
        width: 510px;
        background: #fff;
    }
</style>
