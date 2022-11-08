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
        class="task-manage-batch-edit-tag">
        <div style="margin-bottom: 8px;">
            {{ $t('template.范围') }}：{{ $t('template.共') }}<span class="strong number">{{ templateNums }}</span>{{ $t('template.个作业') }}
        </div>
        <jb-form form-type="vertical">
            <jb-form-item
                :label="$t('template.标签')"
                required
                style="margin-bottom: 0;">
                <div class="tag-panel">
                    <bk-input
                        class="tag-search"
                        left-icon="bk-icon icon-search"
                        :spellcheck="false"
                        :value="search"
                        @change="handleSearch" />
                    <div
                        class="wrapper"
                        style="height: 210px;">
                        <scroll-faker
                            v-if="renderList.length > 0"
                            ref="scrollRef">
                            <bk-checkbox-group
                                v-model="operationList"
                                class="tag-list">
                                <bk-checkbox
                                    v-for="tagItem in renderList"
                                    :key="tagItem.id"
                                    class="tag-item"
                                    :value="tagItem.id"
                                    v-bind="tagCheckInfoMap[tagItem.id]"
                                    @change="value => handleTagCheckChange(value, tagItem.id)">
                                    {{ tagItem.name }}
                                    <template v-if="tagCheckInfoMap[tagItem.id]">
                                        <span
                                            v-if="tagCheckInfoMap[tagItem.id].checked"
                                            v-bk-tooltips.right="$t('template.勾选范围里，全部作业使用')"
                                            class="relate-all">
                                            All
                                        </span>
                                        <span
                                            v-if="tagCheckInfoMap[tagItem.id].indeterminate"
                                            v-bk-tooltips.right="`${$t('template.勾选范围里，有')} ${tagRelateNumMap[tagItem.id]} ${$t('template.个作业使用')}`"
                                            class="relate-nums">
                                            {{ tagRelateNumMap[tagItem.id] }}/{{ templateNums }}
                                        </span>
                                    </template>
                                    <span
                                        v-if="tagItem.isNew"
                                        class="new-tag-flag">
                                        new
                                    </span>
                                </bk-checkbox>
                            </bk-checkbox-group>
                        </scroll-faker>
                        <Empty
                            v-else-if="search"
                            style="margin-top: 20px;"
                            type="search">
                            <span>{{ $t('template.搜索结果为空') }}，</span>
                            <bk-button
                                text
                                @click="handleClearSearch">
                                {{ $t('template.清空搜索') }}
                            </bk-button>
                        </Empty>
                    </div>
                    <auth-component auth="tag/create">
                        <div
                            class="tag-create"
                            @click="handleNew">
                            <bk-icon
                                style=" margin-right: 8px; font-size: 16px;"
                                type="plus-circle" />
                            <span>{{ $t('template.新建标签') }}</span>
                        </div>
                        <div
                            slot="forbid"
                            class="tag-create">
                            <bk-icon
                                style=" margin-right: 8px; font-size: 16px;"
                                type="plus-circle" />
                            <span>{{ $t('template.新建标签') }}</span>
                        </div>
                    </auth-component>
                </div>
            </jb-form-item>
        </jb-form>
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
    import _ from 'lodash';
    import {
        computed,
        getCurrentInstance,
        onBeforeMount,
        reactive,
        ref,
        toRefs,
    } from 'vue';

    import TagManageService from '@service/tag-manage';
    import TaskManageService from '@service/task-manage';

    import { encodeRegexp } from '@utils/assist';

    import OperationTag from '@components/operation-tag';

    import I18n from '@/i18n';

    export default {
        components: {
            OperationTag,
        },
        props: {
            templateList: {
                type: Array,
                required: true,
            },
        },
        emit: ['on-change'],
        setup (props, ctx) {
            const state = reactive({
                isLoading: true,
                isShowCreate: false,
                search: '',
                newTagList: [],
                wholeTagList: [],
                renderList: [],
                templateNums: props.templateList.length,
                operationList: [],
                tagRelateNumMap: {},
                tagCheckInfoMap: {},
            });
            const scrollRef = ref(null);
            // 初始统计 tag 被模板使用的数量
            const tagRelateNumMap = {};
            // 缓存全选
            const memoCheckedMap = {};
            // 缓存半选
            const memoIndeterminateMap = {};
            // 所选作业关联 tag 的默认选中状态
            const tagCheckInfoMap = {};

            const { proxy } = getCurrentInstance();

            // 展示的 tag 列表
            const renderList = computed(() => {
                const allTagList = [...state.newTagList, ...state.wholeTagList];
                
                if (!state.search) {
                    return allTagList;
                }
                const searchReg = new RegExp(encodeRegexp(state.search), 'i');
                const result = allTagList.filter(item => searchReg.test(item.name));
                return Object.freeze(result);
            });
            /**
             * @desc 获取作业模板的数据
             */
            const fetchTaskBasicsData = () => TaskManageService.fetchBasic({
                ids: props.templateList.map(({ id }) => id).join(','),
            }).then((data) => {
                data.forEach(({ tags }) => {
                    tags.forEach(({ id }) => {
                        // 计算每个被使用到的 tag 数量
                        if (tagRelateNumMap[id]) {
                            tagRelateNumMap[id] += 1;
                        } else {
                            tagRelateNumMap[id] = 1;
                        }
                        // tag 的 checkbox 选中状态
                        if (!tagCheckInfoMap[id]) {
                            tagCheckInfoMap[id] = {
                                indeterminate: true,
                            };
                            memoIndeterminateMap[id] = true;
                        }
                        // 如果所有作业都使用了该 tag 则默认被选中
                        if (tagRelateNumMap[id] === state.templateNums) {
                            delete memoIndeterminateMap[id];
                            memoCheckedMap[id] = id;
                            tagCheckInfoMap[id] = {
                                checked: true,
                            };
                        }
                    });
                });
                    
                state.tagRelateNumMap = Object.freeze(tagRelateNumMap);
                state.operationList = Object.values(memoCheckedMap);
                state.tagCheckInfoMap = Object.freeze(tagCheckInfoMap);
            });
            /**
             * @desc 获取 tag 列表数据
             */
            const fetchWholeTagList = () => TagManageService.fetchWholeList()
                .then((data) => {
                    // 排序
                    // 已经被使用的 tag 在前面
                    state.wholeTagList = Object.freeze(data.reduce((result, item) => {
                        if (tagRelateNumMap[item.id]) {
                            result.unshift(item);
                        } else {
                            result.push(item);
                        }
                        return result;
                    }, []));
                });
            /**
             * @desc 过滤 tag
             * @param { String } search
             */
            const handleSearch = (search) => {
                state.search = _.trim(search);
            };
            /**
             * @desc 清空搜索内容
             */
            const handleClearSearch = () => {
                state.search = '';
            };
            /**
             * @desc 新建 tag
             */
            const handleNew = () => {
                state.isShowCreate = true;
            };

            /**
             * @desc 切换 tag 的选中状态
             * @param { Boolean } value
             * @param { Number } tagId
             */
            const handleTagCheckChange = (value, tagId) => {
                const tagCheckInfoMap = Object.assign({}, state.tagCheckInfoMap);
                if (!tagCheckInfoMap[tagId]) {
                    // 选中添加新 tag
                    tagCheckInfoMap[tagId] = {
                        checked: true,
                    };
                } else if (!tagCheckInfoMap[tagId].checked) {
                    // 未被选中 -> 选中
                    tagCheckInfoMap[tagId] = {
                        checked: true,
                    };
                } else {
                    // 选中 -> 未被选中
                    tagCheckInfoMap[tagId] = {
                        checked: false,
                    };
                }
                window.changeConfirm = true;
                state.tagCheckInfoMap = Object.freeze(tagCheckInfoMap);
            };
            /**
             * @desc 新增 tag
             * @param { Object } tag
             */
            const handleTagNew = (tag) => {
                if (scrollRef.value) {
                    scrollRef.value.scrollTo(0, 0);
                }
                tag.isNew = true;
                state.newTagList.unshift(tag);
                window.changeConfirm = true;
            };
            /**
             * @desc 提交批量编辑
             * @returns { Promise }
             */
            const submit = () => {
                const currentCheckedMap = state.operationList.reduce((result, item) => {
                    result[item] = true;
                    return result;
                }, {});
                // add tag
                const addTagIdList = [...state.operationList];
                // - 移除默认选中的
                _.remove(addTagIdList, id => memoCheckedMap[id]);
                // delete tag
                const deleteTagIdList = [];
                //  - 默认选中，在最新的数据未选中
                Object.keys(memoCheckedMap).forEach((tagId) => {
                    if (!currentCheckedMap[tagId]) {
                        deleteTagIdList.push(Number(tagId));
                    }
                });
                // - 默认半选，在最新的数据中没有任何状态
                Object.keys(memoIndeterminateMap).forEach((tagId) => {
                    if (!state.tagCheckInfoMap[tagId].indeterminate
                        && !state.tagCheckInfoMap[tagId].checked) {
                        deleteTagIdList.push(Number(tagId));
                    }
                });
                
                return TaskManageService.batchUpdateTag({
                    addTagIdList,
                    deleteTagIdList,
                    idList: props.templateList.map(({ id }) => id),
                }).then(() => {
                    proxy.messageSuccess(I18n.t('template.编辑标签成功'));
                    ctx.emit('on-change');
                });
            };

            onBeforeMount(() => {
                state.isLoading = true;
                Promise.all([
                    fetchWholeTagList(),
                    fetchTaskBasicsData(),
                ]).then(() => {
                    state.isLoading = false;
                });
            });

            return {
                ...toRefs(state),
                scrollRef,
                renderList,
                handleSearch,
                handleNew,
                handleTagCheckChange,
                handleTagNew,
                handleClearSearch,
                submit,
            };
        },
    };
</script>
<style lang="postcss">
    .task-manage-batch-edit-tag {
        padding-top: 5px;

        .tag-panel {
            display: flex;
            flex-direction: column;
            border: 1px solid #dcdee5;
            border-radius: 2px;
        }

        .tag-search {
            margin: 0 10px;

            &.bk-form-control {
                &.with-left-icon {
                    width: auto;

                    .left-icon {
                        left: 0;
                    }

                    .bk-form-input {
                        padding-left: 24px;
                    }
                }
            }

            .bk-form-input {
                border-color: transparent !important;
                border-bottom: 1px solid #c4c6cc !important;
            }
        }

        .wrapper {
            padding: 18px 0 18px 12px;

            .tag-list {
                display: flex;
                flex-direction: column;

                .tag-item {
                    margin-bottom: 16px;

                    &:last-child {
                        margin-bottom: 0;
                    }
                }
            }

            .relate-all,
            .relate-nums {
                padding: 0 5px;
                font-size: 12px;
                font-weight: 500;
                line-height: 16px;
                background: #e1ecff;
                border-radius: 2px;
            }

            .relate-all {
                color: #3a84ff;
            }

            .relate-nums {
                color: #979ba5;
                background: #f0f1f5;
            }

            .new-tag-flag {
                padding: 0 4px;
                margin-left: 5px;
                font-size: 12px;
                line-height: 16px;
                color: #ff9c01;
                vertical-align: middle;
                background: #ffe8c3;
                border-radius: 2px;
            }
        }

        .tag-create {
            height: 38px;
            padding-left: 10px;
            font-size: 12px;
            line-height: 38px;
            color: #63656e;
            cursor: pointer;
            background: #fafbfd;
            border-top: 1px solid #dcdee5;
        }
    }
</style>
