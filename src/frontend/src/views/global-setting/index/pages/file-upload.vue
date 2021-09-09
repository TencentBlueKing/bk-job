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
    <div class="file-upload-manage-page" v-bkloading="{ isLoading }">
        <smart-action offset-target="input-wraper">
            <div class="wraper">
                <div class="block-title">{{ $t('setting.本地文件上传大小限制') }}:</div>
                <jb-form>
                    <jb-form-item>
                        <div class="input-wraper">
                            <bk-input style="width: 560px;" v-model="info.amount" />
                            <bk-select class="unit-item" v-model="info.unit" :clearable="false">
                                <bk-option id="GB" name="GB" />
                                <bk-option id="MB" name="MB" />
                            </bk-select>
                        </div>
                    </jb-form-item>
                </jb-form>
            </div>
            <template #action>
                <bk-button
                    class="w120"
                    theme="primary"
                    :loading="isSubmiting"
                    @click="handleSubmit">
                    {{ $t('setting.保存') }}
                </bk-button>
            </template>
        </smart-action>
    </div>
</template>
<script>
    import GlobalSettingService from '@service/global-setting';
    import I18n from '@/i18n';

    export default {
        data () {
            return {
                isLoading: true,
                isSubmiting: false,
                info: {},
            };
        },
        created () {
            this.fetchJobConfig();
        },
        methods: {
            fetchJobConfig () {
                this.isLoading = true;
                GlobalSettingService.fetchFileUpload()
                    .then((data) => {
                        this.info = data;
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            handleSubmit () {
                this.isSubmiting = true;
                GlobalSettingService.saveFileUpload(this.info)
                    .then(() => {
                        this.messageSuccess(I18n.t('setting.保存成功'));
                    })
                    .finally(() => {
                        this.isSubmiting = false;
                    });
            },
        },
    };
</script>
<style lang='postcss'>
    .file-upload-manage-page {
        display: flex;
        justify-content: center;
        padding: 40px 0;

        .wraper {
            margin-bottom: 10px;
        }

        .input-wraper {
            display: flex;
        }

        .unit-item {
            width: 80px;
            margin-left: -2px;
            background: #f5f7fa;
            border-bottom-left-radius: 0;
            border-top-left-radius: 0;
        }
    }
</style>
