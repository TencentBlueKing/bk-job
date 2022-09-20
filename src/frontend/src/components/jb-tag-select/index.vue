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
        class="job-tag-select">
        <bk-select
            ref="select"
            :clearable="false"
            display-tag
            multiple
            searchable
            :value="realValue"
            @change="handleChange">
            <bk-option
                v-for="tagItem in list"
                :id="tagItem.id"
                :key="tagItem.id"
                :name="tagItem.name" />
            <template
                v-if="!publicScript"
                slot="extension">
                <auth-component auth="tag/create">
                    <div @click="handleCreate">
                        <i class="bk-icon icon-plus-circle mr10" />{{ $t('新建标签') }}
                    </div>
                    <div slot="forbid">
                        <i class="bk-icon icon-plus-circle mr10" />{{ $t('新建标签') }}
                    </div>
                </auth-component>
            </template>
        </bk-select>
        <lower-component
            :custom="isShowCreate"
            level="custom">
            <operation-tag
                v-model="isShowCreate"
                @on-change="handleTagNew" />
        </lower-component>
    </div>
</template>
<script>
    import PubliceTagManageService from '@service/public-tag-manage';
    import TagManageService from '@service/tag-manage';

    import { checkPublicScript } from '@utils/assist';

    import OperationTag from '@components/operation-tag';

    export default {
        name: 'JbTagSelect',
        components: {
            OperationTag,
        },
        props: {
            value: {
                type: Array,
                default: () => [],
            },
        },
        data () {
            return {
                // tag 列表loading
                isLoading: true,
                // 新建 tag 弹框
                isShowCreate: false,
                realValue: [],
                publicScript: true,
                list: [],
            };
        },
        watch: {
            value: {
                handler (value) {
                    this.realValue = value.map(_ => _.id);
                },
                immediate: true,
            },
        },
        created () {
            this.publicScript = checkPublicScript(this.$route);
            
            this.fetchData();
        },
        methods: {
            /**
             * @desc 获取 tag 列表
             */
            fetchData () {
                this.isLoading = true;
                const requestHandler = this.publicScript ? PubliceTagManageService.fetchTagList : TagManageService.fetchWholeList;
                return requestHandler()
                    .then((data) => {
                        this.list = Object.freeze(data);
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 外部调用显示tag选择面板
             */
            show () {
                this.$refs.select.show();
            },
            
            /**
             * @desc 更新选中的tag
             */
            handleChange (value) {
                const valueMap = value.reduce((result, item) => {
                    result[item] = true;
                    return result;
                }, {});
                const result = [];
                this.list.forEach((item) => {
                    if (valueMap[item.id]) {
                        result.push({ ...item });
                    }
                });
                this.$emit('on-change', result);
                this.$emit('change', result);
                this.$emit('input', result);
            },
            /**
             * @desc 显示新建tag弹框
             */
            handleCreate () {
                this.$refs.select.close();
                this.isShowCreate = true;
            },
            /**
             * @desc 新建标签
             * @param { Object } tag
             */
            handleTagNew (tag) {
                this.fetchData()
                    .then(() => {
                        this.realValue.push(tag.id);
                        this.isShowCreate = false;
                        this.$refs.select.show();
                    });
            },
        },
    };
</script>
<style lang='postcss'>
    .job-tag-select {
        .bk-select {
            &.is-focus {
                z-index: 9;
            }
        }

        .bk-select-dropdown {
            background: #fff;
        }
    }
</style>
