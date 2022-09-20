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
    <jb-dialog
        class="choose-source-file"
        :esc-close="false"
        header-position="left"
        :mask-close="false"
        :title="$t('选择文件源文件')"
        :value="value"
        :width="1200"
        @input="handleCancle">
        <div v-bkloading="{ isLoading }">
            <component
                :is="panelCom"
                v-if="value"
                class="dialog-content"
                :file-location="fileLocation"
                :file-source-id="fileSourceId"
                @on-file-change="handelFileChange"
                @on-source-change="handleSourceChange" />
        </div>
        <template #footer>
            <bk-button
                class="mr10"
                :disabled="isSelectedEmpty"
                theme="primary"
                @click="handleSubmit">
                <span>{{ $t('添加') }}</span>
                <span
                    v-if="!isSelectedEmpty"
                    class="result-nums">{{ fileLocation.length }}</span>
            </bk-button>
            <bk-button @click="handleCancle">
                {{ $t('取消') }}
            </bk-button>
        </template>
    </jb-dialog>
</template>
<script>
    import SourceFileVO from '@domain/variable-object/source-file';

    import SelectFile from './select-file';
    import SelectFileSource from './select-file-source';

    const SELECT_FILE_SOURCE = 'selectFileSource';
    const SELECT_FILE = 'selectFile';
    
    export default {
        components: {
            SelectFileSource,
            SelectFile,
        },
        props: {
            value: {
                type: Boolean,
                defaule: false,
            },
            sourceFile: {
                type: Object,
                defaule: () => ({}),
            },
        },
        data () {
            return {
                isLoading: false,
                selectStep: SELECT_FILE_SOURCE,
                fileSourceId: '',
                fileLocation: [],
            };
        },
        computed: {
            panelCom () {
                if (!this.fileSourceId) {
                    return SelectFileSource;
                }
                const comMap = {
                    [SELECT_FILE_SOURCE]: SelectFileSource,
                    [SELECT_FILE]: SelectFile,
                };
                return comMap[this.selectStep];
            },
            isSelectedEmpty () {
                return this.fileLocation.length < 1;
            },
        },
        watch: {
            value: {
                /**
                 * @desc 显示源文件选择框
                 *
                 * 1，有指定fileSourceId进入编辑状态
                 * 解析fileSourceId、bucketName，定位到已选的文件夹
                 * 2，没有指定fileSourceId进行新建状态
                 *  先选择文件源
                 *
                 */
                handler (value) {
                    if (!value) {
                        return;
                    }
                    if (this.sourceFile.fileSourceId) {
                        const {
                            fileSourceId,
                            fileLocation,
                        } = this.sourceFile;
                        this.fileSourceId = fileSourceId;
                        this.fileLocation = fileLocation;
                        this.selectStep = SELECT_FILE;
                    } else {
                        this.fileSourceId = '';
                        this.fileLocation = [];
                        this.selectStep = SELECT_FILE_SOURCE;
                    }
                },
                immediate: true,
            },
        },
        methods: {
            /**
             * @desc 选中文件源
             * @param {Object} fileSource 选中的文件源
             *
             * 选中fileSource时列表需要切换成bucket列表
             *
             */
            handleSourceChange (fileSource) {
                this.fileSourceId = fileSource.id;
                this.selectStep = SELECT_FILE;
                this.fileLocation = [];
            },
            /**
             * @desc 选中文件
             * @param {Array} fileLocation 选中的文件源
             */
            handelFileChange (fileLocation) {
                this.fileLocation = Object.freeze([...fileLocation]);
            },

            /**
             * @desc 确认按钮事件
             *
             * 选中文件后,过滤重新选择的文件与已选中文件
             * 数据传递到父组件,关闭对话框
             */
            handleSubmit () {
                const fileSourceObj = new SourceFileVO({
                    fileSourceId: this.fileSourceId,
                    fileType: 3,
                    fileLocation: this.fileLocation,
                });
                this.$emit('on-change', fileSourceObj);
                this.handleCancle();
            },

            /**
             * @desc 取消按钮事件
             */
            handleCancle () {
                this.$emit('input', false);
            },

        },
    };
</script>
<style lang="postcss">
    .choose-source-file {
        .result-nums {
            display: inline-block;
            min-width: 18px;
            min-height: 18px;
            padding: 0 5px;
            font-size: 12px;
            line-height: 18px;
            color: #3a84ff;
            text-align: center;
            background-color: #fff;
            border-radius: 10px;
        }

        .dialog-content {
            height: 570px;
        }
    }
</style>
