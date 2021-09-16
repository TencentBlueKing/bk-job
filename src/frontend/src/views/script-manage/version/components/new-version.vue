<template>
    <div class="script-manage-new-version">
        <bk-alert v-if="draftNum > 0" style="margin-bottom: 10px;">
            <div slot="title">
                <span>{{ $t('script.当前已有 [未上线] 版本，') }}</span>
                <a v-if="draftNum > 1" @click="handleGoList">
                    {{ $t('script.返回列表') }}
                </a>
                <a v-else @click="handleGoEdit">
                    {{ $t('script.前往编辑') }}
                </a>
            </div>
        </bk-alert>
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
                draftNum: 0,
                formData: {
                    scriptVersionId: '',
                },
            };
        },
        watch: {
            versionList: {
                handler () {
                    const renderList = [];
                    let draftNum = 0;
                    this.versionList.forEach((item) => {
                        if (!item.isDraft) {
                            renderList.push(item);
                        } else {
                            draftNum += 1;
                        }
                    });
                    this.renderList = Object.freeze(renderList);
                    this.draftNum = draftNum;
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
             * @desc 只有一个未上线版本，开始编辑未上线版本脚本
             */
            handleGoEdit () {
                const draftScriptVersion = _.find(this.versionList, scriptVersion => scriptVersion.isDraft);
                this.$emit('on-edit', draftScriptVersion);
            },
            /**
             * @desc 有多个未上线脚本，返回版本列表
             */
            handleGoList () {
                this.$emit('on-close');
            },
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
