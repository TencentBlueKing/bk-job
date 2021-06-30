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
        <table>
            <tbody>
                <tr v-for="(row, index) in fileList" :key="index">
                    <td style="width: 40%;">{{ row.fileLocationText }}</td>
                    <td style="width: auto;">
                        <template v-if="row.fileSize > 0">
                            <p v-if="row.uploadStatus !== 'danger'">{{ $t('本地文件') }}（{{ row.fileSizeText }}）</p>
                            <p v-else style="color: #ff5656;">{{ $t('上传失败') }}</p>
                            <div class="upload-progress">
                                <transition name="fade">
                                    <bk-progress
                                        v-show="row.uploadProgress !== 1"
                                        :theme="row.uploadStatus"
                                        :show-text="false"
                                        :percent="row.uploadProgress" />
                                </transition>
                            </div>
                        </template>
                    </td>
                    <td>
                        <div class="action-box">
                            <bk-button text @click="handlerRemove(index)">{{ $t('移除') }}</bk-button>
                        </div>
                    </td>
                </tr>
            </tbody>
        </table>
        <input
            ref="uploadInput"
            type="file"
            multiple
            style="position: absolute; width: 0; height: 0; opacity: 0;"
            @change="handleStartUpload">
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import TaskExecuteService from '@service/task-execute';
    import QuertGlobalSettingService from '@service/query-global-setting';
    import SourceFileVO from '@domain/variable-object/source-file';

    export default {
        name: '',
        props: {
            data: {
                type: Array,
                required: true,
            },
        },
        data () {
            return {
                fileList: [],
                FILE_UPLOAD_MAX_SIZE: {
                    amount: 2,
                    unit: 'GB',
                },
            };
        },
        computed: {
            fileUploadMaxBytes () {
                const { amount, unit } = this.FILE_UPLOAD_MAX_SIZE;
                const unitMap = {
                    GB: 1073741824,
                    MB: 1048576,
                };
                if (unitMap[unit.toUpperCase()]) {
                    return Number(amount) * unitMap[unit];
                }
                return unit;
            },
            fileMaxUploadSizeText () {
                const { amount, unit } = this.FILE_UPLOAD_MAX_SIZE;
                return `${amount}${unit}`;
            },
        },
        watch: {
            data: {
                handler (newData) {
                    if (this.innerChange) {
                        this.innerChange = false;
                        return;
                    }
                    if (this.fileList.length < 1) {
                        this.fileList = newData;
                    }
                },
                immediate: true,
            },
        },
        created () {
            this.fetchJobConfig();
        },
        methods: {
            /**
             * @desc 获取系统配置
             *
             * 本地文件上传大小限制
             */
            fetchJobConfig () {
                QuertGlobalSettingService.fetchJobConfig()
                    .then((data) => {
                        this.FILE_UPLOAD_MAX_SIZE = data.FILE_UPLOAD_MAX_SIZE;
                    });
            },
            /**
             * @desc 暴露给外部的API，开始选择文件
             */
            startUpload () {
                this.$refs.uploadInput.click();
            },
            /**
             * @desc 触发change事件
             */
            triggerChange () {
                this.innerChange = true;
                this.$emit('on-change', [...this.fileList]);
            },
            /**
             * @desc 上传文件
             * @param {Object} event input事件
             *
             * 1，检测重名
             * 2，检测文件大小是否超过限制
             */
            handleStartUpload (event) {
                const { files } = event.target;
                const uploadFileQueue = [];
                const params = new FormData();
                
                const sameStack = [];
                const largeStack = [];
                
                files.forEach((curFile) => {
                    const { name, size } = curFile;
                    // 重名检测
                    if (this.fileList.some(_ => _.fileLocationText === name)) {
                        sameStack.push(name);
                        return;
                    }
                    if (size > this.fileUploadMaxBytes) {
                        largeStack.push(name);
                        return;
                    }
                    const sourceFile = new SourceFileVO({
                        fileLocation: [name],
                        fileSize: size,
                        fileType: SourceFileVO.typeLocal,
                    });
                    uploadFileQueue.push(sourceFile);
                    params.append('uploadFiles', curFile);
                });
                
                if (sameStack.length > 0) {
                    this.messageError(`${I18n.t('文件')}[${sameStack.join(' / ')}]${I18n.t('已添加')}`);
                }
                if (largeStack.length > 0) {
                    // eslint-disable-next-line max-len
                    this.messageError(`${I18n.t('文件')}[${largeStack.join(' / ')}]${I18n.t('上传失败（本地文件最大仅支持')}${this.fileMaxUploadSizeText}）`);
                }
                if (uploadFileQueue.length < 1) {
                    this.$refs.uploadInput.value = '';
                    return;
                }
                this.fileList.push(...uploadFileQueue);
                
                TaskExecuteService.getUploadFileContent(params, {
                    onUploadProgress: (event) => {
                        uploadFileQueue.forEach((sourceFile) => {
                            const { loaded, total } = event;
                            sourceFile.loaded = loaded;
                            sourceFile.loadTotal = total;
                        });
                    },
                }).then((data) => {
                    uploadFileQueue.forEach((sourceFile, index) => {
                        const { md5, filePath, fileSize } = data[index];
                        sourceFile.fileHash = md5;
                        sourceFile.fileLocation = [filePath];
                        sourceFile.fileSize = fileSize;
                        sourceFile.uploadStatus = 'success';
                    });
                    this.triggerChange();
                })
                    .catch(() => {
                        uploadFileQueue.forEach((sourceFile) => {
                            sourceFile.uploadStatus = 'danger';
                        });
                    });
                this.$refs.uploadInput.value = '';
                this.triggerChange();
            },
            /**
             * @desc 删除上传文件
             * @param {Number} index input事件
             */
            handlerRemove (index) {
                this.fileList.splice(index, 1);
                this.triggerChange();
            },
        },
    };
</script>
