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
    <div
        v-bkloading="{ isLoading }"
        class="render-file-source-name">
        <div @click="handleGoFileSource(fileSourceId)">
            {{ fileSourceAlias || '--' }}
            <Icon
                class="link-flag"
                svg
                type="edit" />
        </div>
    </div>
</template>
<script>
    import FileManageService from '@service/file-source-manage';

    export default {
        props: {
            fileSourceId: {
                type: Number,
                required: true,
            },
        },
        data () {
            return {
                isLoading: false,
                fileSourceAlias: '',
            };
        },
        created () {
            this.fetchSourceFileInfo();
        },
        methods: {
            /**
             * @desc 通过文件源id获取文件详细信息
             */
            fetchSourceFileInfo () {
                FileManageService.getSourceInfo({
                    id: this.fileSourceId,
                }).then((data) => {
                    this.fileSourceAlias = data.alias;
                });
            },
            /**
             * @desc 新窗口打开文件源列表页面
             */
            handleGoFileSource () {
                const { href } = this.$router.resolve({
                    name: 'fileList',
                    query: {
                        fileSourceId: this.fileSourceId,
                    },
                });
                window.open(href, '_blank');
            },
        },
    };
</script>
<style lang="postcss">
    .render-file-source-name {
        cursor: pointer;

        .link-flag {
            opacity: 0%;
        }

        &:hover {
            color: #3a84ff !important;

            .link-flag {
                opacity: 100%;
            }
        }
    }
</style>
