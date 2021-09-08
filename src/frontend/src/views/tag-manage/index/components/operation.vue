<template>
    <div class="tag-manage-operation">
        <jb-form
            :model="formData"
            ref="form"
            form-type="vertical"
            :rules="rules">
            <jb-form-item
                label="标签名"
                required
                property="name">
                <bk-input v-model="formData.name" />
            </jb-form-item>
            <jb-form-item label="描述">
                <bk-input
                    v-model="formData.description"
                    type="textarea"
                    :maxlength="100" />
            </jb-form-item>
        </jb-form>
    </div>
</template>
<script>
    import TagManageService from '@service/tag-manage';
    import { tagNameRule } from '@utils/validator';

    const genDefaultData = () => ({
        name: '',
        description: '',
    });

    export default {
        name: '',
        props: {
            data: {
                type: Object,
                default: () => genDefaultData(),
            },
        },
        data () {
            return {
                isEdit: false,
                formData: genDefaultData(),
            };
        },
        watch: {
            /**
             * @desc 同步 tag 数据进行编辑
             */
            data: {
                handler (data) {
                    const {
                        id,
                        name,
                        description,
                    } = data;
                    this.formData = {
                        name,
                        description,
                    };
                    this.isEdit = id > 0;
                },
                immediate: true,
            },
        },
        created () {
            this.rules = {
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
        },
        methods: {
            submit () {
                const requestHandler = this.isEdit ? TagManageService.updateTag : TagManageService.createTag;
                return this.$refs.form.validate()
                    .then(() => requestHandler(this.formData)
                        .then(() => {
                            this.$messageSuccess('新建标签成功');
                            this.$emit('on-change');
                        }));
            },
        },
    };
</script>
<style lang="postcss">
    .tag-manage-operation {
        display: block;
    }
</style>
