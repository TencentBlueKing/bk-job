<template>
    <div class="script-manage-new-version">
        <jb-form
            ref="form"
            form-type="vertical"
            :model="formData"
            :rules="rules">
            <jb-form-item
                :label="$t('script.选择载入版本')"
                required
                property="scriptVersionId"
                style="margin-bottom: 0;">
                <bk-select v-model="formData.scriptVersionId">
                    <bk-option
                        v-for="item in renderList"
                        :id="item.scriptVersionId"
                        :name="item.version"
                        :key="item.scriptVersionId">
                        <span>{{ item.version }}</span>
                        <span v-html="item.statusHtml" />
                    </bk-option>
                </bk-select>
            </jb-form-item>
        </jb-form>
    </div>
</template>
<script>
    import _ from 'lodash';
    import I18n from '@/i18n';

    export default {
        name: '',
        props: {
            versionList: {
                type: Array,
                require: true,
            },
        },
        data () {
            return {
                renderList: [],
                formData: {
                    scriptVersionId: '',
                },
            };
        },
        watch: {
            versionList: {
                handler (versionList) {
                    this.renderList = Object.freeze(versionList);
                    this.formData.scriptVersionId = '';
                },
                immediate: true,
            },
        },
        created () {
            this.rules = {
                scriptVersionId: [
                    {
                        required: true,
                        message: I18n.t('script.脚本版本必填'),
                        trigger: 'blur',
                    },
                ],
            };
        },
        methods: {
            /**
             * @desc 选中版本开始复制新建
             */
            submit () {
                return this.$refs.form.validate()
                    .then(() => {
                        const scriptVersion = _.find(this.renderList, ({ scriptVersionId }) => scriptVersionId === this.formData.scriptVersionId);
                        this.$emit('on-create', scriptVersion);
                    });
            },
            reset () {
                this.formData.scriptVersionId = '';
                this.$refs.form.clearError();
            },
        },
    };
</script>
