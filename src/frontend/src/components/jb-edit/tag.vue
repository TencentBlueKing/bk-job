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
    <div class="jb-edit-tag" :class="{ shortcurt }" @click.stop="">
        <div
            v-if="!isEditing"
            class="render-value-box"
            @click.stop="handleTextClick">
            <div ref="content" class="value-text" v-bk-overflow-tips tag-edit-tag>
                <slot v-bind:value="text">{{ text || '--' }}</slot>
            </div>
            <template v-if="!isLoading">
                <div
                    v-if="shortcurt"
                    class="tag-shortcurt-box"
                    @click.stop="">
                    <div class="shortcurt-action-btn">
                        <Icon type="copy" @click="handleCopy" />
                        <Icon type="paste" class="paste-btn" @click="handlePaste" />
                    </div>
                </div>
                <div v-else class="tag-normal-box">
                    <Icon
                        type="edit-2"
                        class="edit-action"
                        @click.self.stop="handleEdit" />
                </div>
            </template>
            <Icon
                v-if="isLoading"
                type="loading-circle"
                class="tag-edit-loading" />
        </div>
        <div v-else class="edit-value-box">
            <jb-tag-select
                ref="tagSelect"
                :value="localValue"
                @on-change="handleTagValueChange" />
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';
    import I18n from '@/i18n';
    import { execCopy } from '@utils/assist';
    import JbTagSelect from '@components/jb-tag-select';

    let copyMemo = [];

    const isEqual = (pre, next) => {
        if (pre.length !== next.length) {
            return false;
        }
        if (pre.length === 0) {
            return true;
        }
        const preMap = pre.reduce((result, item) => {
            result[item.id] = true;
            return result;
        }, {});
        next.forEach((item) => {
            delete preMap[item.id];
        });
        return Object.keys(preMap).length < 1;
    };

    export default {
        name: 'JbEditTag',
        components: {
            JbTagSelect,
        },
        props: {
            field: {
                type: String,
                required: true,
            },
            value: {
                type: Array,
                default: () => [],
            },
            // 显示复制粘贴按钮
            shortcurt: {
                type: Boolean,
                default: false,
            },
            remoteHander: {
                type: Function,
                default: () => Promise.resolve(),
            },
            rules: {
                type: Array,
                default: () => [],
            },
        },
        data () {
            return {
                isEditing: false,
                isLoading: false,
                localValue: this.value,
            };
        },
        computed: {
            /**
             * @desc 标签显示文本
             * @returns { String }
             */
            text () {
                return this.localValue.map(_ => _.name).join('，');
            },
        },
        watch: {
            value: {
                handler (value) {
                    this.localValue = value;
                    this.memoValue = [...this.value];
                },
                immediate: true,
            },
        },
        mounted () {
            document.body.addEventListener('click', this.hideEdit);
            this.$once('hook:beforeDestroy', () => {
                document.body.removeEventListener('click', this.hideEdit);
            });
        },
        
        methods: {
            /**
             * @desc 触发标签修改操作
             */
            triggerRemote () {
                this.isEditing = false;
                
                if (isEqual(this.memoValue, this.localValue)) {
                    return;
                }
                
                this.isLoading = true;
                
                this.remoteHander({
                    [this.field]: this.localValue.map(({ id }) => ({ id })),
                }).then(() => {
                    this.memoValue = this.localValue;
                    this.messageSuccess(I18n.t('编辑成功'));
                    this.$emit('on-change', {
                        [this.field]: this.localValue,
                    });
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 切换编辑状态
             */
            hideEdit (event) {
                if (!this.isEditing) return;
                if (event.path && event.path.length > 0) {
                    // eslint-disable-next-line no-plusplus
                    for (let i = 0; i < event.path.length; i++) {
                        const target = event.path[i];
                        if (/tippy-popper/.test(target.className)
                            || /job-tag-create-dialog/.test(target.className)) {
                            return;
                        }
                    }
                }
                
                this.triggerRemote();
            },
            /**
             * @desc tag 值更新
             * @param { Array } tagList
             */
            handleTagValueChange (tagList) {
                this.localValue = Object.freeze(tagList);
            },
            /**
             * @desc 编辑 tag
             */
            handleEdit () {
                document.body.click();
                this.$nextTick(() => {
                    this.isEditing = true;
                    setTimeout(() => {
                        this.$refs.tagSelect.show();
                    });
                });
            },
            /**
             * @desc 点击 tag 文本开始编辑状态
             */
            handleTextClick () {
                if (!this.shortcurt) {
                    return;
                }
                this.handleEdit();
            },
            /**
             * @desc 复制 tag
             */
            handleCopy () {
                if (this.localValue.length < 1) {
                    this.$bkMessage({
                        theme: 'warning',
                        message: I18n.t('标签为空'),
                    });
                    return;
                }
                copyMemo = _.cloneDeep(this.localValue);
                execCopy(this.text);
            },
            /**
             * @desc 粘贴 tag
             */
            handlePaste () {
                if (copyMemo.length < 1) {
                    this.$bkMessage({
                        theme: 'warning',
                        message: I18n.t('请先复制标签'),
                    });
                    return;
                }
                this.localValue = [
                    ...new Set([
                        ...this.localValue,
                        ...copyMemo,
                    ]),
                ];
                this.triggerRemote();
            },
        },
    };
</script>
<style lang="postcss">
    .edit-tag-column {
        .cell {
            overflow: initial;
            line-height: 18px;
            text-overflow: initial;
            word-break: break-all;
            -webkit-line-clamp: unset;

            .jb-edit-tag {
                margin-left: -4px;
            }
        }
    }
</style>
<style lang='postcss'>
    @keyframes tag-edit-loading {
        to {
            transform: rotateZ(360deg);
        }
    }

    .jb-edit-tag {
        position: relative;
        height: 30px;
        cursor: pointer;
        border-radius: 2px;

        &.shortcurt:hover {
            background: #e1e2e6;

            .shortcurt-action-btn {
                display: flex;
            }
        }

        &:hover {
            .tag-normal-box,
            .tag-shortcurt-box {
                display: flex;
                opacity: 1;
                transform: scale(1);
            }
        }

        .render-value-box {
            display: flex;
            height: 30px;
            padding-left: 4px;
            line-height: 30px;
            align-items: center;
        }

        .value-text {
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .tag-normal-box,
        .tag-shortcurt-box {
            display: none;
            height: 30px;
            min-width: 24px;
            color: #979ba5;
            opacity: 0;
            align-items: center;
        }

        .tag-shortcurt-box {
            padding-right: 6px;
            margin-left: auto;
            font-size: 16px;
            flex: 0 0 42px;

            .paste-btn {
                margin-left: 4px;
            }

            .shortcurt-action-btn {
                align-items: center;

                i:hover {
                    color: #3a84ff;
                }
            }
        }

        .tag-normal-box {
            font-size: 16px;
            transform: scale(0);
            transition: 0.15s;
            transform-origin: left center;

            .edit-action {
                padding: 6px 15px 6px 2px;
                cursor: pointer;

                &:hover {
                    color: #3a84ff;
                }
            }
        }

        .tag-edit-loading {
            position: absolute;
            top: 9px;
            right: 10px;
            animation: 'tag-edit-loading' 1s linear infinite;
        }

        .edit-value-box {
            width: 100%;
        }
    }
</style>
