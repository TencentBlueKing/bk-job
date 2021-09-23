<template>
    <jb-dialog
        :value="value"
        :title="dialogInfo.title"
        header-position="left"
        :mask-close="false"
        class="job-tag-create-dialog"
        :width="480">
        <jb-form
            v-if="value"
            :model="formData"
            ref="formRef"
            form-type="vertical"
            style="margin-top: 5px;"
            :rules="rules">
            <jb-form-item
                :label="$t('标签名称')"
                required
                property="name">
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
                    type="textarea"
                    :maxlength="100" />
            </jb-form-item>
        </jb-form>
        <template slot="footer">
            <bk-button
                theme="primary"
                class="mr10"
                :loading="isSubmiting"
                @click="handleSubmit">
                {{ dialogInfo.okText }}
            </bk-button>
            <bk-button @click="handleCancel">{{ $t('取消') }}</bk-button>
        </template>
    </jb-dialog>
</template>
<script>
    import {
        reactive,
        ref,
        toRefs,
        watch,
        computed,
        getCurrentInstance,
    } from '@vue/composition-api';
    import I18n from '@/i18n';
    import TagManageService from '@service/tag-manage';
    import TagModel from '@model/tag';
    import { leaveConfirm } from '@utils/assist';
    import { tagNameRule } from '@utils/validator';

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
            // props.data 有更新时同步最新的值
            watch(() => props.data, (data) => {
                const {
                    name,
                    description,
                } = data;
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
                                window.changeAlert = false;
                                ctx.emit('on-change');
                                proxy.messageSuccess(I18n.t('编辑标签成功'));
                                closeDialog();
                            });
                        }
                        return TagManageService.createTag(state.formData)
                            .then((data) => {
                                window.changeAlert = false;
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
