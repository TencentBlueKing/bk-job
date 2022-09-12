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
            <thead>
                <th style="width: 40%;">
                    {{ $t('文件名称') }}
                </th>
                <th style="width: auto;">
                    {{ $t('文件源.text') }}
                </th>
                <th>{{ $t('操作') }}</th>
            </thead>
            <tbody>
                <tr
                    v-for="(sourceFile, index) in list"
                    :key="index">
                    <td>
                        <div
                            class="path-text-style"
                            @click="handleEditSourceFile(index)">
                            <render-file-name :data="sourceFile.fileLocation" />
                        </div>
                    </td>
                    <td>
                        <render-source-name :file-source-id="sourceFile.fileSourceId" />
                    </td>
                    <td>
                        <div class="action-box">
                            <bk-button
                                text
                                @click="handlerRemove(index)">
                                {{ $t('移除') }}
                            </bk-button>
                        </div>
                    </td>
                </tr>
            </tbody>
        </table>
        <lower-component
            :custom="isShowSourceFile"
            level="custom">
            <choose-source-file
                v-model="isShowSourceFile"
                :source-file="data[editSourceFileIndex] || {}"
                @on-change="handleSourceFileChange" />
        </lower-component>
    </div>
</template>
<script>
    import ChooseSourceFile from './components/choose-source-file/';
    import RenderFileName from './components/render-file-name';
    import RenderSourceName from './components/render-source-name';

    export default {
        components: {
            ChooseSourceFile,
            RenderFileName,
            RenderSourceName,
            
        },
        props: {
            data: {
                type: Array,
                default: () => [],
            },
        },
        data () {
            return {
                isShowSourceFile: false,
                editSourceFileIndex: -1,
                list: [],
            };
        },
        watch: {
            data: {
                handler (newData) {
                    if (this.innerChange) {
                        this.innerChange = false;
                        return;
                    }
                    this.list = Object.freeze(newData);
                },
                immediate: true,
            },
        },
        methods: {
            /**
             * @desc 触发文件源 更新
             */
            triggerChange () {
                this.innerChange = true;
                this.$emit('on-change', [...this.list]);
            },
            /**
             * @desc 更新文件源文件
             * @param {Object} payload 文件源文件数据
             */
            handleSourceFileChange (payload) {
                const newData = [...this.list];
                if (this.editSourceFileIndex > -1) {
                    newData.splice(this.editSourceFileIndex, 1, payload);
                } else {
                    newData.push(payload);
                }

                this.list = Object.freeze(newData);
                
                this.editSourceFileIndex = -1;
                this.triggerChange();
            },
            /**
             * @desc 组件外部调用，显示选择文件源弹层
             */
            handleShowSourceDialog () {
                this.isShowSourceFile = true;
                this.editSourceFileIndex = -1;
            },
            /**
             * @desc 显示选择文件源对话框模板
             */
            handleEditSourceFile (index) {
                this.isShowSourceFile = true;
                this.editSourceFileIndex = index;
            },

            /**
             * @desc 移除选中的文件源数据
             * @param {Number} index 已选中的文件源数据数组对应下标
             */
            handlerRemove (index) {
                const newData = [...this.list];
                newData.splice(this.editSourceFileIndex, 1);
                this.list = Object.freeze(newData);
                this.triggerChange();
            },
        },
    };
</script>
