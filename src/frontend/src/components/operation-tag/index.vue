<template>
    <jb-dialog
        :value="value"
        @input="handleUpdateDialog"
        :title="dialogInfo.title"
        header-position="left"
        :mask-close="false"
        :width="480">
        <div>
            <jb-form
                :model="formData"
                ref="formRef"
                form-type="vertical"
                :rules="rules">
                <jb-form-item
                    label="标签名"
                    required
                    property="name">
                    <bk-input v-model="formData.name" />
                </jb-form-item>
                <jb-form-item
                    label="描述"
                    style="margin-bottom: 0;">
                    <bk-input
                        v-model="formData.description"
                        type="textarea"
                        :maxlength="100" />
                </jb-form-item>
            </jb-form>
        </div>
        <template slot="footer">
            <bk-button
                theme="primary"
                class="mr10"
                :loading="isSubmiting"
                @click="handleSubmit">
                {{ dialogInfo.okText }}
            </bk-button>
            <bk-button @click="handleCancel">取消</bk-button>
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
    import TagManageService from '@service/tag-manage';
    import TagModel from '@model/tag';
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
                if (isEdit) {
                    return {
                        title: '编辑标签',
                        okText: '保存',
                    };
                }
                return {
                    title: '新建标签',
                    okText: '提交',
                };
            });
            const { proxy } = getCurrentInstance();
            const rules = {
                name: [
                    {
                        required: true,
                        message: '标签名不能为空',
                        trigger: 'blur',
                    },
                    {
                        validator: tagNameRule.validator,
                        message: tagNameRule.message,
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
            /**
             * @desc 更新弹窗显示状态
             * @param { Boolean } value
             */
            const handleUpdateDialog = (value) => {
                ctx.emit('change', value);
                ctx.emit('input', value);
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
                                ctx.emit('on-change');
                                proxy.messageSuccess('编辑标签成功');
                                handleUpdateDialog(false);
                            });
                        }
                        return TagManageService.createTag(state.formData)
                            .then((data) => {
                                ctx.emit('on-change', new TagModel({
                                    id: data,
                                    ...state.formData,
                                }));
                                proxy.messageSuccess('新建标签成功');
                                handleUpdateDialog(false);
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
                handleUpdateDialog(false);
            };
            
            return {
                ...toRefs(state),
                formRef,
                dialogInfo,
                rules,
                handleUpdateDialog,
                handleSubmit,
                handleCancel,
            };
        },
    };
</script>
