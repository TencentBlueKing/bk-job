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
        class="job-tag-create-dialog"
        header-position="left"
        :mask-close="false"
        :title="dialogInfo.title"
        :value="value"
        :width="480">
        <jb-form
            v-if="value"
            ref="formRef"
            form-type="vertical"
            :model="formData"
            :rules="rules"
            style="margin-top: 5px;">
            <jb-form-item
                :label="$t('标签名称')"
                property="name"
                required>
                <jb-input
                    v-model="formData.name"
                    :maxlength="20"
                    :native-attributes="{ autofocus: true }" />
            </jb-form-item>
            <jb-form-item
                :label="$t('描述')"
                style="margin-bottom: 0;">
                <bk-input
                    v-model="formData.description"
                    :maxlength="100"
                    type="textarea" />
            </jb-form-item>
        </jb-form>
        <template slot="footer">
            <bk-button
                class="mr10"
                :loading="isSubmiting"
                theme="primary"
                @click="handleSubmit">
                {{ dialogInfo.okText }}
            </bk-button>
            <bk-button @click="handleCancel">
                {{ $t('取消') }}
            </bk-button>
        </template>
    </jb-dialog>
</template>
<script>
    import {
        computed,
        getCurrentInstance,
        reactive,
        ref,
        toRefs,
        watch,
    } from 'vue';

    import TagManageService from '@service/tag-manage';

    import TagModel from '@model/tag';

    import { leaveConfirm } from '@utils/assist';
    import { tagNameRule } from '@utils/validator';

    import I18n from '@/i18n';

    const genDefaultData = () => ({
        name: '',
        description: '',
    });

    export default {
        name: '',
        props: {
            value: {
                type: Boolean,
                default: false,
            },
            data: {
                type: Object,
                default: () => genDefaultData(),
            },
        },
        setup (props, ctx) {
            const state = reactive({
                isSubmiting: false,
                formData: genDefaultData(),
            });
            const formRef = ref(null);
            const isEdit = computed(() => props.data.id > 0);
            const dialogInfo = computed(() => {
                if (isEdit.value) {
                    return {
                        title: I18n.t('编辑标签'),
                        okText: I18n.t('保存'),
                    };
                }
                return {
                    title: I18n.t('新建标签'),
                    okText: I18n.t('提交'),
                };
            });
            const { proxy } = getCurrentInstance();
            // tag 验证规则
            const rules = {
                name: [
                    {
                        required: true,
                        message: I18n.t('标签名不能为空'),
                        trigger: 'blur',
                    },
                    {
                        validator: tagNameRule.validator,
                        message: tagNameRule.message,
                        trigger: 'blur',
                    },
                    {
                        validator: name => TagManageService.checkName({
                            id: props.data.id || 0,
                            name,
                        }),
                        message: I18n.t('标签名已存在，请重新输入'),
                        trigger: 'blur',
                    },
                ],
            };
            // value 有更新时显示最新的值
            watch(() => props.value, (value) => {
                if (!value) {
                    return;
                }
                const {
                    name,
                    description,
                } = props.data;
                state.formData = {
                    name,
                    description,
                };
            }, {
                immediate: true,
            });
            const closeDialog = () => {
                ctx.emit('change', false);
                ctx.emit('input', false);
                state.formData = genDefaultData();
            };

            /**
             * @desc 提交操作结果
             */
            const handleSubmit = () => {
                state.isSubmiting = true;
                
                return formRef.value.validate()
                    .then(() => {
                        if (isEdit.value) {
                            return TagManageService.updateTag({
                                id: props.data.id,
                                ...state.formData,
                            }).then(() => {
                                window.changeConfirm = false;
                                ctx.emit('on-change');
                                proxy.messageSuccess(I18n.t('编辑标签成功'));
                                closeDialog();
                            });
                        }
                        return TagManageService.createTag(state.formData)
                            .then((data) => {
                                window.changeConfirm = false;
                                ctx.emit('on-change', new TagModel(data));
                                proxy.messageSuccess(I18n.t('新建标签成功'));
                                closeDialog();
                            });
                    })
                    .finally(() => {
                        state.isSubmiting = false;
                    });
            };
            /**
             * @desc 取消编辑
             */
            const handleCancel = () => {
                leaveConfirm()
                    .then(() => {
                        closeDialog();
                    });
            };
            
            return {
                ...toRefs(state),
                formRef,
                dialogInfo,
                rules,
                handleSubmit,
                handleCancel,
            };
        },
    };
</script>
