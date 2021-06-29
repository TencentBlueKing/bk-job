<template>
    <tr class="global-variable-create-row">
        <td>
            <bk-select
                class="offset-left"
                :clearable="false"
                :value="formData.type"
                @change="value => handleChange('type', value)">
                <bk-option
                    v-for="item in typeList"
                    :key="item.id"
                    :id="item.id"
                    :name="item.name" />
            </bk-select>
        </td>
        <td>
            <div class="variable-name-box offset-left" :class="{ 'edit-error': isNameError }">
                <bk-input
                    :value="formData.name"
                    @blur="handleShowNameError"
                    @change="value => handleChange('name', value)" />
                <Icon
                    v-if="isNameError"
                    v-bk-tooltips="errorNameText"
                    type="info"
                    class="input-error" />
            </div>
        </td>
        <td>
            <template v-if="isHostVarialbe">
                <bk-button
                    v-if="formData.defaultTargetValue.isEmpty"
                    class="add-host-btn offset-left"
                    @click="handleShowChooseIp">
                    <Icon type="plus" />
                    {{ $t('添加服务器') }}
                </bk-button>
                <jb-edit-host
                    v-else
                    class="offset-left"
                    field="defaultTargetValue"
                    :value="formData.defaultTargetValue" />
            </template>
            <bk-input
                v-else
                class="offset-left"
                :value="formData.defaultValue"
                @change="value => handleChange('defaultValue', value)" />
        </td>
        <td>
            <jb-textarea
                class="offset-left"
                :value="formData.description"
                @change="value => handleChange('description', value)" />
        </td>
        <td>
            <bk-checkbox
                v-if="withChangable"
                class="offset-left"
                :value="formData.changeable"
                @change="value => handleChange('changeable', value)"
                :true-value="1"
                :false-value="0" />
            <span v-else>--</span>
        </td>
        <td>
            <bk-checkbox
                class="offset-left"
                :value="formData.required"
                @change="value => handleChange('required', value)"
                :true-value="1"
                :false-value="0" />
        </td>
        <td class="action-row">
            <Icon type="add-fill" @click="handleCreate" class="action-btn" />
            <Icon type="reduce-fill" @click="handleDelete" class="action-btn" />
        </td>
        <choose-ip
            v-model="isShowChooseIp"
            :host-node-info="formData.defaultTargetValue.hostNodeInfo"
            @on-change="handleHostChange" />
    </tr>
</template>
<script>
    import _ from 'lodash';
    import I18n from '@/i18n';
    import { globalVariableNameRule } from '@utils/validator';
    import GlobalVariableModel from '@model/task/global-variable';
    import ChooseIp from '@components/choose-ip';
    import JbEditHost from '@components/jb-edit/host';
    import { createVariable } from '../util';

    export default {
        name: '',
        components: {
            ChooseIp,
            JbEditHost,
        },
        props: {
            list: {
                type: Array,
            },
            data: {
                type: Object,
                require: true,
            },
        },
        data () {
            return {
                formData: _.cloneDeep(this.data),
                isShowChooseIp: false,
                isShowNameError: false,
                errorNameText: '',
            };
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
                ].includes(this.formData.type);
            },
            /**
             * @desc 主机变量
             * @returns { Boolean }
             */
            isHostVarialbe () {
                return [
                    GlobalVariableModel.TYPE_HOST,
                ].includes(this.formData.type);
            },
            /**
             * @desc 变量名验证失败
             * @returns { Boolean }
             */
            isNameError () {
                return this.isShowNameError && this.errorNameText;
            },
        },
        created () {
            this.typeList = [
                {
                    id: GlobalVariableModel.TYPE_STRING,
                    name: I18n.t('template.字符串'),
                },
                {
                    id: GlobalVariableModel.TYPE_NAMESPACE,
                    name: I18n.t('template.命名空间'),
                },
                {
                    id: GlobalVariableModel.TYPE_HOST,
                    name: I18n.t('template.主机列表'),
                },
                {
                    id: GlobalVariableModel.TYPE_PASSWORD,
                    name: I18n.t('template.密文'),
                },
                {
                    id: GlobalVariableModel.TYPE_RELATE_ARRAY,
                    name: I18n.t('template.关联数组'),
                },
                {
                    id: GlobalVariableModel.TYPE_INDEX_ARRAY,
                    name: I18n.t('template.索引数组'),
                },
            ];
        },
        methods: {
            /**
             * @desc 验证变量名
             */
            validate () {
                this.isShowNameError = true;
                this.errorNameText = '';
                if (!this.formData.name) {
                    this.errorNameText = I18n.t('template.变量名称必填');
                }
                if (!globalVariableNameRule.validator(this.formData.name)) {
                    this.errorNameText = globalVariableNameRule.message;
                }
                if (this.list.some(item => item.name && item.name === this.formData.name)) {
                    this.errorNameText = I18n.t('template.变量名称已存在，请重新输入');
                }
                if (this.errorNameText) {
                    return Promise.reject(new Error(this.errorNameText));
                }
                return Promise.resolve();
            },
            /**
             * @desc 触发 change 事件
             */
            triggerChange () {
                this.$emit('on-change', this.formData);
            },
            /**
             * @desc name 编辑框失去焦点时进行验证
             */
            handleShowNameError () {
                this.validate();
            },
            /**
             * @desc 设置主机变量的值
             */
            handleShowChooseIp () {
                this.isShowChooseIp = true;
            },
            /**
             * @desc 更新主机变量
             * @param { Object } hostNodeInfo 主机信息
             */
            handleHostChange (hostNodeInfo) {
                this.formData.defaultTargetValue.hostNodeInfo = hostNodeInfo;
                this.triggerChange();
            },
            /**
             * @desc 字段值更新
             * @param { String } field 字段名
             * @param { Any } value 字段值
             */
            handleChange (field, value) {
                if (field === 'type') {
                    this.formData = createVariable(this.formData.id);
                }
                this.formData[field] = value;
                this.isShowNameError = false;
                this.triggerChange();
            },
            /**
             * @desc 添加新变量
             */
            handleCreate () {
                this.$emit('on-append');
            },
            /**
             * @desc 删除自己
             */
            handleDelete () {
                this.$emit('on-delete');
            },
            
        },
    };
</script>
<style lang="postcss">
    .global-variable-create-row {
        .offset-left {
            margin-left: -10px;
        }

        .bk-select {
            height: 30px;
            line-height: 30px;
            background: #f7f8fa;
            border: 1px solid transparent;

            &:hover {
                background: #f0f1f5;
            }

            .bk-select-name {
                height: 30px;
                padding-right: 16px;
                padding-left: 10px;
                line-height: 30px;
            }
        }

        .bk-input-text {
            .bk-form-input {
                height: 30px;
                line-height: 30px;
                background: #f7f8fa;
                border: 1px solid transparent;

                &:hover {
                    background: #f0f1f5;
                }
            }
        }

        .job-textarea {
            background: #f7f8fa;

            &:hover {
                background: #f0f1f5;
            }

            .job-textarea-edit {
                border: 1px solid transparent;
            }
        }

        .add-host-btn {
            height: 30px;
        }

        .edit-error {
            .bk-input-text {
                .bk-form-input {
                    border-color: #ea3636;
                }
            }
        }

        .variable-name-box {
            position: relative;

            .input-error {
                position: absolute;
                top: 0;
                right: 0;
                bottom: 0;
                z-index: 1;
                display: flex;
                align-items: center;
                padding: 0 10px;
                font-size: 16px;
                color: #ea3636;
                cursor: pointer;
            }
        }
    }
</style>
