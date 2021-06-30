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
    <div class="step-distro-file">
        <jb-form ref="form" :model="formData" fixed :label-width="formMarginLeftWidth">
            <card-layout :title="$t('template.基本信息')" class="block">
                <item-factory
                    name="stepName"
                    field="name"
                    :placeholder="$t('template.推荐按步骤实际处理的场景行为来取名...')"
                    :form-data="formData"
                    @on-change="handleChange" />
                <item-factory
                    name="timeout"
                    field="timeout"
                    :form-data="formData"
                    @on-change="handleChange" />
                <item-factory
                    name="errorHandle"
                    field="ignoreError"
                    :form-data="formData"
                    @on-change="handleChange" />
                <item-factory
                    name="speedLimit"
                    field="uploadSpeedLimit"
                    :label="$t('template.上传限速')"
                    :form-data="formData"
                    @on-change="handleChange" />
                <item-factory
                    name="speedLimit"
                    field="downloadSpeedLimit"
                    :label="$t('template.下载限速')"
                    :form-data="formData"
                    @on-change="handleChange" />
            </card-layout>
            <card-layout :title="$t('template.文件来源')" class="block">
                <item-factory
                    name="sourceFileOfTemplate"
                    field="fileSourceList"
                    :variable="variable"
                    :form-data="formData"
                    @on-change="handleChange" />
            </card-layout>
            <card-layout :title="$t('template.传输目标')" class="block">
                <item-factory
                    ref="targetPath"
                    name="targetPath"
                    field="path"
                    tips-placement="top"
                    :form-data="formData"
                    @on-change="handleChange" />
                <item-factory
                    name="transferMode"
                    field="transferMode"
                    :form-data="formData"
                    @on-change="handleChange" />
                <item-factory
                    name="executeAccount"
                    field="account"
                    :form-data="formData"
                    @on-change="handleChange" />
                <item-factory
                    name="targetServerOfTemplate"
                    field="server"
                    :variable="variable"
                    :form-data="formData"
                    @on-change="handleChange" />
            </card-layout>
        </jb-form>
    </div>
</template>
<script>
    import _ from 'lodash';
    import I18n from '@/i18n';
    import {
        mapState,
    } from 'vuex';
    import TaskHostNodeModel from '@model/task-host-node';
    import CardLayout from '@components/task-step/file/card-layout';
    import ItemFactory from '@components/task-step/file/item-factory';
    import {
        getScriptName,
    } from '@utils/assist';

    const getDefaultData = () => ({
        id: 0,
        // 步骤名称
        name: getScriptName(I18n.t('template.步骤分发文件')),
        // 源文件列表
        fileSourceList: [],
        // 超时
        timeout: 7200,
        // 上传文件限速
        uploadSpeedLimit: 0,
        // 传输模式： 1 - 严谨模式； 2 - 强制模式；3 - 安全模式
        transferMode: 2,
        // 忽略错误 0 - 不忽略 1 - 忽略
        ignoreError: 0,
        // 下载文件限速
        downloadSpeedLimit: 0,

        // 目标路径，通过三个输入框（account、path、server）赋值
        // 最终组合成一个 fileDestination
        // fileDestination: {
        //     account: '', // 执行账号
        //     path: '', // 目标路径
        //     server: {} // 执行目标
        // }
        account: '',
        path: '',
        server: new TaskHostNodeModel({}),
    });

    export default {
        name: '',
        components: {
            CardLayout,
            ItemFactory,
        },
        inheritAttrs: false,
        props: {
            variable: {
                type: Array,
                default: () => [],
            },
            data: {
                type: Object,
                default: () => [],
            },
        },
        data () {
            return {
                formData: getDefaultData(),
            };
        },
        computed: {
            ...mapState('distroFile', {
                isEditNewSourceFile: state => state.isEditNewSourceFile,
                isLocalFileUploading: state => state.isLocalFileUploading,
                isLocalFileUploadFailed: state => state.isLocalFileUploadFailed,
            }),
            formMarginLeftWidth () {
                return this.$i18n.locale === 'en-US' ? 140 : 110;
            },
        },
        watch: {
            data: {
                handler (newData) {
                    if (_.isEmpty(newData)) {
                        return;
                    }
                    const {
                        account,
                        path,
                        server,
                    } = newData.fileDestination;

                    const originData = { ...newData };
                    delete originData.fileDestination;

                    this.formData = {
                        ...this.formData,
                        ...originData,
                        account,
                        path,
                        server,
                    };
                    setTimeout(() => {
                        this.$refs.form.validate();
                    });
                },
                immediate: true,
            },
        },
        methods: {
            /**
             * @desc 表单字段更新
             * @param {String} field 字段名
             * @param {Any} value 字段值
             */
            handleChange (field, value) {
                this.formData[field] = value;
            },
            /**
             * @desc 提交表单
             *
             * 1，首先检测是否有没保存的源文件
             * 2，表单验证
             *   - 表单验证失败检测是否有本地文件上传未完成或者本地文件上传失败
             */
            submit () {
                return Promise.resolve()
                    // 检测没有保存的源文件
                    .then(() => new Promise((resolve, reject) => {
                        if (!this.isEditNewSourceFile) {
                            return resolve();
                        }
                        this.$bkInfo({
                            title: I18n.t('template.您有未保存的源文件'),
                            type: 'warning',
                            okText: I18n.t('template.继续提交'),
                            cancelText: I18n.t('template.去保存'),
                            confirmFn: () => {
                                setTimeout(() => {
                                    resolve();
                                }, 300);
                            },
                            cancelFn: () => {
                                setTimeout(() => {
                                    reject(new Error('save'));
                                }, 300);
                            },
                        });
                    }))
                    // 检测源文件的同名文件和目录
                    .then(() => new Promise((resolve, reject) => {
                        const fileLocationMap = {};
                        const pathReg = /([^/]+\/?)\*?$/;
                        let isDouble = false;
                        // 路径中以 * 结尾表示分发所有文件，可能和分发具体文件冲突
                        let hasDirAllFile = false;
                        let hasFile = false;
                        // eslint-disable-next-line no-plusplus
                        for (let i = 0; i < this.formData.fileSourceList.length; i++) {
                            const currentFileSource = this.formData.fileSourceList[i];
                            // eslint-disable-next-line no-plusplus
                            for (let j = 0; j < currentFileSource.fileLocation.length; j++) {
                                const currentFileLocation = currentFileSource.fileLocation[j];
                                // 分发所有文件
                                if (/\*$/.test(currentFileLocation)) {
                                    hasDirAllFile = true;
                                    if (hasFile) {
                                        isDouble = true;
                                        break;
                                    }
                                    continue;
                                }
                                // 分发具体的文件
                                if (!/(\/|(\/\*))$/.test(currentFileLocation)) {
                                    hasFile = true;
                                    if (hasDirAllFile) {
                                        isDouble = true;
                                        break;
                                    }
                                }
                                const pathMatch = currentFileLocation.match(pathReg);
                                if (pathMatch) {
                                    if (fileLocationMap[pathMatch[1]]) {
                                        isDouble = true;
                                        break;
                                    } else {
                                        fileLocationMap[pathMatch[1]] = 1;
                                    }
                                }
                            }
                        }
                            
                        if (!isDouble) {
                            resolve();
                            return;
                        }
                        // 有重名目录和文件
                        this.$bkInfo({
                            title: I18n.t('template.源文件可能出现同名'),
                            subTitle: I18n.t('template.多文件源传输场景下容易出现同名文件覆盖的问题，你可以在目标路径中使用 [源服务器IP] 的变量来尽可能规避风险。'),
                            okText: I18n.t('template.好的，我调整一下'),
                            cancelText: I18n.t('template.已知悉，确定执行'),
                            closeIcon: false,
                            width: 500,
                            confirmFn: () => {
                                setTimeout(() => {
                                    // 聚焦到目标路径输入框
                                    this.$refs.targetPath.$el.scrollIntoView();
                                    this.$refs.targetPath.$el.querySelector('.bk-form-input').focus();
                                    reject(new Error('transferMode change'));
                                }, 300);
                            },
                            cancelFn: () => {
                                setTimeout(() => {
                                    resolve();
                                }, 300);
                            },
                        });
                    }))
                    .then(() => {
                        const {
                            id,
                            name,
                            timeout,
                            uploadSpeedLimit,
                            downloadSpeedLimit,
                            transferMode,
                            ignoreError,
                            fileSourceList,
                            account,
                            path,
                            server,
                        } = this.formData;

                        const result = {
                            id,
                            name,
                            delete: 0,
                            type: 2,
                            fileStepInfo: {
                                timeout,
                                uploadSpeedLimit,
                                downloadSpeedLimit,
                                transferMode,
                                ignoreError,
                                fileSourceList,
                                fileDestination: {
                                    account,
                                    path,
                                    server,
                                },
                                
                            },
                        };
                        return this.$refs.form.validate()
                            // 表单验证通过直接提交
                            .then(() => {
                                this.$emit('on-change', result, true);
                            })
                            // 表单验证失败时，检测本地文件上传状态
                            .catch(() => new Promise((resolve, reject) => {
                                let confirmInfo = null;
                                const handleClose = () => {
                                    confirmInfo.close();
                                    reject(new Error('not save'));
                                };
                                const subHeader = () => (
                                <div>
                                    <div style="text-align: center">
                                        <bk-button
                                            onClick={handleClose}
                                            style="width: 96px"
                                            theme="primary">
                                            { I18n.t('template.去处理') }
                                        </bk-button>
                                    </div>
                                </div>
                                );
                                if (this.isLocalFileUploading) {
                                    confirmInfo = this.$bkInfo({
                                        type: 'error',
                                        title: I18n.t('template.本地源文件上传未完成'),
                                        subHeader: subHeader(),
                                        showFooter: false,
                                    });
                                } else if (this.isLocalFileUploadFailed) {
                                    confirmInfo = this.$bkInfo({
                                        type: 'error',
                                        title: I18n.t('template.本地源文件上传失败'),
                                        subHeader: subHeader(),
                                        showFooter: false,
                                    });
                                } else {
                                    this.$emit('on-change', result, false);
                                    resolve();
                                }
                            }));
                    });
            },
        },
    };
</script>
<style lang='postcss'>
    .step-distro-file {
        .card-box {
            padding-left: 0;
            margin-bottom: 6px;

            .card-title {
                padding-left: 30px;
            }
        }
    }
</style>
