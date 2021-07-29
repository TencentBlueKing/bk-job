<template>
    <tbody>
        <tr>
            <td>
                {{ data.typeText }}
            </td>
            <td>
                <jb-edit-input
                    mode="block"
                    :value="data.name"
                    field="name"
                    :rules="rules.name"
                    @on-change="handleChange" />
            </td>
            <td>
                <jb-edit-host
                    v-if="data.isHost"
                    :value="data.defaultTargetValue"
                    field="defaultTargetValue"
                    @on-change="handleChange" />
                <jb-edit-input
                    v-else
                    mode="block"
                    :value="data.defaultValue"
                    field="defaultValue"
                    @on-change="handleChange" />
            </td>
            <td>
                <jb-edit-textarea
                    mode="block"
                    :rows="1"
                    :value="data.description"
                    field="description"
                    @on-change="handleChange" />
            </td>
            <td>
                <bk-checkbox
                    v-if="withChangable"
                    :value="data.changeable"
                    :true-value="1"
                    :false-value="0" />
                <span v-else>--</span>
            </td>
            <td>
                <bk-checkbox
                    :value="data.required"
                    :true-value="1"
                    :false-value="0" />
            </td>
            <td class="action-row">
                <Icon type="add-fill" class="action-btn" @click="handleAppend" />
                <Icon type="reduce-fill" class="action-btn" @click="handleDelete" />
            </td>
        </tr>
    </tbody>
</template>
<script>
    import GlobalVariableModel from '@model/task/global-variable';
    import I18n from '@/i18n';
    import { globalVariableNameRule } from '@utils/validator';
    import JbEditInput from '@components/jb-edit/input';
    import JbEditTextarea from '@components/jb-edit/textarea';
    import JbEditHost from '@components/jb-edit/host';

    export default {
        name: '',
        components: {
            JbEditInput,
            JbEditTextarea,
            JbEditHost,
        },
        props: {
            data: {
                type: Object,
                required: true,
            },
            variableNameList: {
                type: Array,
            },
        },
        computed: {
            /**
             * @desc 是否有赋值可变选项
             * @returns { Boolean }
             */
            withChangable () {
                return [
                    GlobalVariableModel.TYPE_STRING,
                    GlobalVariableModel.TYPE_RELATE_ARRAY,
                    GlobalVariableModel.TYPE_INDEX_ARRAY,
                ].includes(this.data.type);
            },
        },
        created () {
            this.rules = {
                name: [
                    {
                        required: true,
                        message: I18n.t('template.变量名称必填'),
                        trigger: 'blur',
                    },
                    {
                        validator: globalVariableNameRule.validator,
                        message: globalVariableNameRule.message,
                        trigger: 'blur',
                    },
                    {
                        validator: val => !this.variableNameList.includes(val),
                        message: I18n.t('template.变量名称已存在，请重新输入'),
                        trigger: 'blur',
                    },
                ],
            };
        },
        methods: {
            /**
             * @desc 验证变量名
             * @returns {Promise}
             */
            validate () {
                this.errorNameText = '';
                if (!this.data.name) {
                    this.errorNameText = I18n.t('template.变量名称必填');
                } else if (!globalVariableNameRule.validator(this.data.name)) {
                    this.errorNameText = globalVariableNameRule.message;
                } else if (this.variableNameList.includes(this.data.name)) {
                    this.errorNameText = I18n.t('template.变量名称已存在，请重新输入');
                }
                if (this.errorNameText) {
                    return Promise.reject(new Error(this.errorNameText));
                }
                return Promise.resolve();
            },
            /**
             * @desc 更新变量
             * @param {Object} payload 更新字段数据
             */
            handleChange (payload) {
                this.$emit('on-change', Object.assign({}, this.data, payload));
            },
            /**
             * @desc 删除自己
             */
            handleDelete () {
                this.$emit('on-delete');
            },
            /**
             * @desc 添加新变量
             */
            handleAppend () {
                this.$emit('on-append');
            },
        },
    };
</script>
