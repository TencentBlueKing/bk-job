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
        <jb-form-item
            ref="item"
            :label="$t('源文件')"
            :property="field"
            required
            :rules="rules">
            <div>
                <bk-button
                    class="mr10"
                    @click="handleAddServerFile">
                    <Icon type="plus" />
                    {{ $t('添加服务器文件') }}
                </bk-button>
                <bk-button
                    v-if="ENABLE_FEATURE_FILE_MANAGE"
                    class="mr10"
                    @click="handleAddSourceFile">
                    <Icon type="plus" />
                    {{ $t('添加文件源文件') }}
                </bk-button>
                <bk-button
                    class="mr10"
                    :loading="isConfigLoading"
                    @click="handlerUploadLocalFile">
                    <Icon type="plus" />
                    {{ $t('添加本地文件') }}
                </bk-button>
                <span class="source-file-tips">
                    {{ $t('添加本地文件会有同名文件覆盖风险') }}
                    <Icon
                        v-bk-tooltips="uploadFileTipsConfig"
                        class="tips-flag"
                        type="info" />
                </span>
            </div>
            <view-file
                v-show="showFileView"
                ref="serverFileView"
                v-model="isAddServerFile"
                class="source-file-view"
                :data="sourceFileList"
                v-bind="$attrs"
                @on-change="handleSourceFileChange" />
        </jb-form-item>
        <div
            id="uploadFileTips"
            class="upload-file-tips">
            <div class="row">
                {{ $t('支持中文文件名，本地上传文件大小不能超过') }} {{ fileMaxUploadSize }}
            </div>
            <div class="row">
                {{ $t('文件名支持使用通配符，如： /tmp/jobsvr_2020*.log') }}
            </div>
            <div class="row">
                {{ $t('文件名支持正则表达式写法以匹配多个文件，文件名前 需加 REGEX: 前缀，如：/tmp/REGEX:myfile-[A-Za-z]{0,10}.tar.gz') }}
            </div>
            <div class="row">
                {{ $t('如需分发文件目录，文件名请以/结束') }}
            </div>
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';
    import { mapMutations } from 'vuex';

    import QuertGlobalSettingService from '@service/query-global-setting';

    import ViewFile from './view';

    import I18n from '@/i18n';

    export default {
        name: 'SourceFileBase',
        components: {
            ViewFile,
        },
        inheritAttrs: false,
        props: {
            field: {
                type: String,
                default: '',
            },
            data: {
                type: Array,
                default: () => [],
            },
        },
        data () {
            return {
                isConfigLoading: true,
                isAddServerFile: false,
                sourceFileList: [],
                showFileView: false,
                fileMaxUploadSize: '2GB',
                ENABLE_FEATURE_FILE_MANAGE: false,
            };
        },
        watch: {
            data: {
                handler (newSourceFileList) {
                    if (this.isSelfChange) {
                        this.isSelfChange = false;
                        return;
                    }
                    if (newSourceFileList.length < 1 && this.sourceFileList.length > 0) {
                        this.sourceFileList = [];
                        return;
                    }
                    if (this.sourceFileList.length < 1) {
                        this.sourceFileList = _.cloneDeep(newSourceFileList);
                        this.isAddServerFile = this.sourceFileList.some(item => item.isServerFile);
                    }
                },
                immediate: true,
            },
            sourceFileList (sourceFileList) {
                if (sourceFileList.length > 0) {
                    this.$refs.item.clearValidator();
                }
            },
        },
        created () {
            this.rules = [
                {
                    validator: () => {
                        if (this.sourceFileList.length < 1) {
                            this.updateFileUploadFailed(false);
                            this.updateFileUploading(false);
                        }
                        return this.sourceFileList.length;
                    },
                    message: I18n.t('源文件必填'),
                    trigger: 'blur',
                },
                {
                    validator: () => {
                        // eslint-disable-next-line no-plusplus
                        for (let i = 0; i < this.sourceFileList.length; i++) {
                            const currentFile = this.sourceFileList[i];
                            if (currentFile.isLocalFile
                                && currentFile.uploadStatus === 'danger') {
                                // 标记有本地文件上传失败
                                this.updateFileUploadFailed(true);
                                return false;
                            }
                        }
                        this.updateFileUploadFailed(false);
                        return true;
                    },
                    message: I18n.t('本地源文件上传失败'),
                    trigger: 'blur',
                },
                {
                    validator: () => {
                        // eslint-disable-next-line no-plusplus
                        for (let i = 0; i < this.sourceFileList.length; i++) {
                            const currentFile = this.sourceFileList[i];
                            if (currentFile.isLocalFile
                                && currentFile.uploadStatus === 'primary') {
                                // 标记有本地文件正在上传
                                this.updateFileUploading(true);
                                return false;
                            }
                        }
                        this.updateFileUploading(false);
                        return true;
                    },
                    message: I18n.t('本地源文件上传未完成'),
                    trigger: 'change',
                },
                {
                    validator: () => {
                        // eslint-disable-next-line no-plusplus
                        for (let i = 0; i < this.sourceFileList.length; i++) {
                            const currentFile = this.sourceFileList[i];
                            if (!currentFile.isServerFile) {
                                continue;
                            }
                            // 服务器文件路径不能为空
                            if (currentFile.fileLocation.length < 1) {
                                return false;
                            }
                        }
                        return true;
                    },
                    message: I18n.t('服务器源文件路径必填'),
                    trigger: 'change',
                },
                {
                    validator: () => {
                        // eslint-disable-next-line no-plusplus
                        for (let i = 0; i < this.sourceFileList.length; i++) {
                            const currentFile = this.sourceFileList[i];
                            if (!currentFile.isServerFile) {
                                continue;
                            }
                            // 如果服务器列表使用的是主机变量
                            if (currentFile.isVar && currentFile.host.isEmpty) {
                                return false;
                            }
                        }
                        return true;
                    },
                    message: I18n.t('服务器源文件中服务器列表不能为空'),
                    trigger: 'change',
                },
            ];
            this.uploadFileTipsConfig = {
                allowHtml: true,
                width: '335px',
                theme: 'light',
                trigger: 'mouseenter',
                content: '#uploadFileTips',
                placement: 'right-start',
            };
            this.fetchJobConfig();
        },
        mounted () {
            const unWatch = this.$watch(() => this.$refs.serverFileView.isShow, (value) => {
                this.showFileView = value;
            }, {
                immediate: true,
            });
            this.$once('hook:beforeDestroy', () => {
                unWatch();
            });
        },
        methods: {
            ...mapMutations('distroFile', [
                'updateFileUploading',
                'updateFileUploadFailed',
            ]),
            /**
             * @desc 获取系统配置
             *
             * 本地文件上传大小限制
             */
            fetchJobConfig () {
                this.isConfigLoading = true;
                QuertGlobalSettingService.fetchJobConfig()
                    .then((data) => {
                        const { amount, unit } = data.FILE_UPLOAD_SETTING;
                        this.fileMaxUploadSize = `${amount}${unit}`;
                        this.ENABLE_FEATURE_FILE_MANAGE = data.ENABLE_FEATURE_FILE_MANAGE;
                    })
                    .finally(() => {
                        this.isConfigLoading = false;
                    });
            },
            /**
             * @desc 触发change事件
             */
            triggerChange () {
                this.isSelfChange = true;
                this.$emit('on-change', this.sourceFileList);
            },
            /**
             * @desc 显示添加服务器文件输入框
             */
            handleAddServerFile () {
                this.isAddServerFile = true;
            },
            /**
             * @desc 显示添加文件源文件弹框
             */
            handleAddSourceFile () {
                this.$refs.serverFileView.handleAddSourceFile();
            },
            /**
             * @desc 选择本地文件开始上传
             */
            handlerUploadLocalFile () {
                this.isAddLocalFile = true;
                this.$refs.serverFileView.startUploadLocalFile();
            },
            /**
             * @desc 更新文件来源的值
             * @param {Array} souceFileList 文件源文件
             */
            handleSourceFileChange (souceFileList) {
                this.sourceFileList = Object.freeze(souceFileList);
                this.triggerChange();
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .source-file-view {
        margin-top: 16px;
    }

    .upload-file-tips {
        font-size: 12px;
        line-height: 16px;
        color: #63656e;

        .row {
            position: relative;
            padding-left: 12px;

            &::before {
                position: absolute;
                top: 6px;
                left: 0;
                width: 4px;
                height: 4px;
                background: currentcolor;
                border-radius: 50%;
                content: "";
            }
        }
    }

    .source-file-tips {
        font-size: 12px;
        color: #979ba5;

        .tips-flag {
            margin-left: 8px;
            font-size: 14px;
            color: #c4c6cc;
            cursor: pointer;
        }
    }

</style>
