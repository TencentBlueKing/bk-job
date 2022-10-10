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
    <div class="script-manage-new-version">
        <jb-form
            ref="form"
            form-type="vertical"
            :model="formData"
            :rules="rules">
            <jb-form-item
                :label="$t('script.选择载入版本')"
                property="scriptVersionId"
                required
                style="margin-bottom: 0;">
                <bk-select v-model="formData.scriptVersionId">
                    <bk-option
                        v-for="item in renderList"
                        :id="item.scriptVersionId"
                        :key="item.scriptVersionId"
                        :name="item.version">
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
