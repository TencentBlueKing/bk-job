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
    <div class="job-tag-select" v-bkloading="{ isLoading }">
        <bk-select
            ref="select"
            searchable
            multiple
            display-tag
            :clearable="false"
            :value="realValue"
            @change="handleChange">
            <bk-option
                v-for="tagItem in list"
                :key="tagItem.id"
                :id="tagItem.id"
                :name="tagItem.name" />
            <template v-if="!publicScript" slot="extension">
                <auth-component auth="tag/create">
                    <div @click="handleCreate">
                        <i class="bk-icon icon-plus-circle mr10" />{{ $t('新增.action') }}
                    </div>
                    <div slot="forbid">
                        <i class="bk-icon icon-plus-circle mr10" />{{ $t('新增.action') }}
                    </div>
                </auth-component>
            </template>
        </bk-select>
        <jb-dialog
            v-model="isShowCreate"
            class="job-tag-create-dialog"
            :title="$t('新建标签')"
            header-position="left"
            :width="480"
            @click.stop="">
            <jb-form form-type="vertical" :model="formData" :rules="rules" ref="tagCreateForm">
                <jb-form-item :label="$t('标签名称')" required property="tagName" style="margin-bottom: 0;">
                    <bk-input
                        v-model="formData.tagName"
                        :native-attributes="{ autofocus: 'autofocus' }"
                        @keydown="handleEnter"
                        :placeholder="$t('只允许包含：汉字 A-Z a-z 0-9 _ - ! # @ $ & % ^ ~ = + .')" />
                </jb-form-item>
            </jb-form>
            <div slot="footer">
                <bk-button
                    class="mr10"
                    theme="primary"
                    :loading="isSubmiting"
                    @click="handleCreateSubmit">
                    {{ $t('确定') }}
                </bk-button>
                <bk-button @click="handleCreateCancel">{{ $t('取消') }}</bk-button>
            </div>
        </jb-dialog>
    </div>
</template>
<script>
    import PubliceTagManageService from '@service/public-tag-manage';
    import TagManageService from '@service/tag-manage';
    import I18n from '@/i18n';
    import {
        isPublicScript,
        leaveConfirm,
    } from '@utils/assist';
    import {
        tagNameRule,
    } from '@utils/validator';
    import JbDialog from '@components/jb-dialog';

    export default {
        name: 'JbTagSelect',
        components: {
            JbDialog,
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
                // 新建tag loading
                isSubmiting: false,
                // 新建 tag 弹框
                isShowCreate: false,
                realValue: [],
                publicScript: true,
                list: [],
                formData: {
                    tagName: '',
                },
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
            this.publicScript = isPublicScript(this.$route);
            
            this.fetchData();

            this.rules = {
                tagName: [
                    {
                        validator: tags => tagNameRule.validator(tags),
                        message: tagNameRule.message,
                        trigger: 'blur',
                    },
                ],
            };
        },
        methods: {
            /**
             * @desc 获取 tag 列表
             */
            fetchData () {
                this.isLoading = true;
                const requestHandler = this.publicScript ? PubliceTagManageService.fetchTagList : TagManageService.fetchWholeList;
                requestHandler()
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
             * @desc enter 建触发提交
             */
            handleEnter (value, event) {
                if (event.isComposing) {
                    // 跳过输入发复合时间
                    return;
                }
                if (event.keyCode !== 13) {
                    // 非enter键
                    return;
                }
                this.handleCreateSubmit();
            },
            /**
             * @desc 提交新 tag
             */
            handleCreateSubmit () {
                this.isSubmiting = true;
                this.$refs.tagCreateForm.validate()
                    .then(() => TagManageService.createTag(this.formData)
                        .then((newTagId) => {
                            window.changeAlert = false;
                            this.fetchData()
                                .then(() => {
                                    this.realValue.push(newTagId);
                                });
                            this.messageSuccess(I18n.t('操作成功'));
                            this.handleCreateCancel();
                        }))
                    .finally(() => {
                        this.isSubmiting = false;
                    });
            },
            /**
             * @desc 取消新建tag
             */
            handleCreateCancel () {
                leaveConfirm()
                    .then(() => {
                        this.$refs.tagCreateForm.clearError();
                        this.$refs.select.show();
                        this.isShowCreate = false;
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
