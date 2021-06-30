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
    <div class="job-tag-wrapper" v-bkloading="{ isLoading }">
        <tab-item
            :name="$t('template.全部作业')"
            :id="1"
            :value="classesId"
            icon="business-manage"
            :count="totalCount"
            :loading="isNumberLoading"
            @on-change="handleClassesChange" />
        <tab-item
            :name="$t('template.未分类')"
            :id="2"
            :value="classesId"
            icon="unclassified"
            :count="unclassifiedCount"
            :loading="isNumberLoading"
            @on-change="handleClassesChange" />
        <tab-item
            :name="$t('template.待更新')"
            :id="3"
            :value="classesId"
            icon="update"
            :count="needUpdateCount"
            :loading="isNumberLoading"
            @on-change="handleClassesChange" />
        <div class="line" />
        <template v-for="item in renderTagList">
            <tab-item
                v-if="!item.isLoading && item.count > 0"
                :key="item.id"
                :id="item.id"
                :count="item.count"
                :name="item.name"
                :icon="'tag'"
                :value="tagId"
                :can-edit="true"
                :loading="item.isLoading"
                :tag-list="list"
                @on-change="handleTagChange"
                @on-edit="handleEdit" />
        </template>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import TagService from '@service/tag-manage';
    import TabItem from './tab-item';

    export default {
        name: 'RenderTagTabItem',
        components: {
            TabItem,
        },
        props: {
            active: {
                type: [
                    String,
                    Number,
                ],
                default: '',
            },
        },
        data () {
            return {
                isLoading: false,
                isNumberLoading: true,
                classesId: 1,
                tagId: 0,
                list: [],
                countMap: {},
            };
        },
        computed: {
            totalCount () {
                return this.countMap.total || 0;
            },
            unclassifiedCount () {
                return this.countMap.unclassified || 0;
            },
            needUpdateCount () {
                return this.countMap.needUpdate || 0;
            },
            renderTagList () {
                if (this.isNumberLoading) {
                    return this.list;
                }
                const tagCountMap = this.countMap.tagCount;
                return this.list.map((tag) => {
                    tag.count = tagCountMap[tag.id] || 0;
                    return tag;
                });
            },
        },
        created () {
            this.init();
        },
        mounted () {
            this.parseDefaultValueFromURL();
        },
        methods: {
            /**
             * @desc 获取tag列表
             */
            fetchTagList () {
                this.$request(TagService.fetchTagList(), () => {
                    this.isLoading = true;
                }).then((data) => {
                    this.list = data;
                    this.$emit('on-init', data);
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 获取tag的使用数量
             */
            fetchTagTemplateNum () {
                TagService.fetchTagTemplateNum()
                    .then((data) => {
                        this.countMap = data;
                    })
                    .finally(() => {
                        this.isNumberLoading = false;
                    });
            },
            /**
             * @desc 初始化逻辑
             */
            init () {
                this.fetchTagList();
                this.fetchTagTemplateNum();
            },
            /**
             * @desc 解析url中的默认tag
             */
            parseDefaultValueFromURL () {
                let classesId = 1;
                if (this.$route.query.type) {
                    classesId = ~~this.$route.query.type || 1;
                    this.handleClassesChange(classesId);
                    return;
                }
                
                if (this.$route.query.panelTag) {
                    const currentTagId = parseInt(this.$route.query.panelTag, 10);
                    if (currentTagId > 0) {
                        this.classesId = 0;
                        this.handleTagChange(currentTagId);
                    }
                }
            },
            /**
             * @desc 分类切换
             * @param {Number} id 分类id
             */
            handleClassesChange (id) {
                if (this.classesId === id) {
                    return;
                }
                this.classesId = id;
                this.tagId = 0;
                this.$emit('on-change', {
                    type: this.classesId,
                    panelTag: '',
                });
            },
            /**
             * @desc tag切换
             * @param {Number} id 分类id
             */
            handleTagChange (id) {
                if (id === this.tagId) return;
                this.tagId = id;
                this.classesId = 0;
                this.$emit('on-change', {
                    type: '',
                    panelTag: this.tagId,
                });
            },
            /**
             * @desc 编辑tag
             * @param {Object} payload 标签数据
             *
             * 编辑成功需要刷新标签数据
             */
            handleEdit (payload) {
                TagService.updateTag(payload)
                    .then(() => {
                        this.messageSuccess(I18n.t('template.标签名更新成功'));
                        this.init();
                    });
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .job-tag-wrapper {
        display: flex;
        flex-direction: column;
        min-height: 50%;
        padding: 24px 0;

        .line {
            height: 1px;
            margin: 10px 0;
            background: #f0f1f5;
        }
    }
</style>
