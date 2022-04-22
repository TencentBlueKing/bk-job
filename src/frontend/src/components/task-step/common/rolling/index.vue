<template>
    <div class="task-step-rolling">
        <jb-form-item label="滚动执行">
            <bk-switcher
                :value="formData[enabledField]"
                theme="primary"
                @change="handleRollingEnableChange" />
        </jb-form-item>
        <div v-if="formData[enabledField]">
            <jb-form-item
                ref="expr"
                label="滚动策略"
                required
                :property="exprField"
                :rules="rollingExprRule"
                class="rolling-expr-field">
                <div class="form-item-content">
                    <bk-input
                        :value="formData[exprField]"
                        @change="handleRollingExprChange" />
                    <div v-if="tips" class="strategy-tips">{{ tips }}</div>
                    <div v-if="errorMessage" class="strategy-error">{{ errorMessage }}</div>
                </div>
            </jb-form-item>
            <jb-form-item
                ref="rollingMode"
                label="滚动机制"
                required>
                <bk-select
                    :value="formData[modeField]"
                    :clearable="false"
                    class="form-item-content"
                    @change="handleRollingModeChange">
                    <bk-option :id="1" name="默认（执行失败则暂停）" />
                    <bk-option :id="2" name="忽略失败，自动滚动下一批" />
                    <bk-option :id="3" name="不自动，每批次都人工确认" />
                </bk-select>
            </jb-form-item>
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';
    import rollingExprParse from '@utils/rolling-expr-parse';

    export default {
        name: '',
        props: {
            enabledField: {
                type: String,
                required: true,
            },
            exprField: {
                type: String,
                required: true,
            },
            modeField: {
                type: String,
                required: true,
            },
            formData: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                tips: '',
                errorMessage: '',
            };
        },
        computed: {
            /**
             * @desc 滚动策略验证规则，不需要滚动执行时不进行验证
             * @returns { Array }
             */
            rollingExprRule () {
                if (!this.formData[this.enabledField]) {
                    return [];
                }
                return [
                    {
                        required: true,
                        message: '滚动策略必填',
                        trigger: 'blur',
                    },
                    {
                        validator: (value) => {
                            try {
                                rollingExprParse(value);
                                return true;
                            } catch {
                                return false;
                            }
                        },
                        message: '滚动策略格式不正确',
                        trigger: 'blur',
                    },
                ];
            },
        },
        watch: {
            formData: {
                handler (formData) {
                    this.validatorExpr(formData[this.exprField]);
                },
                immediate: true,
                deep: true,
            },
        },
        methods: {
            /**
             * @desc 验证滚动规则
             * @param { String } expr
             */
            validatorExpr (expr) {
                try {
                    this.errorMessage = '';
                    this.tips = rollingExprParse(expr);
                } catch (error) {
                    this.tips = '';
                    this.errorMessage = error.message;
                }
            },
            /**
             * @desc 是否启用滚动
             * @param { Boolean } enabled
             */
            handleRollingEnableChange (enabled) {
                this.$emit('on-change', this.enabledField, enabled);
                if (!enabled) {
                    this.tips = '';
                    this.errorMessage = '';
                    this.$emit('on-reset', {
                        [this.exprField]: '',
                        [this.modeField]: 1,
                    });
                }
                // 滚动策略默认 10%
                this.$emit('on-reset', {
                    [this.exprField]: '10%',
                    [this.modeField]: 1,
                });
                this.$nextTick(() => {
                    if (this.formData[this.enabledField]) {
                        this.$refs.rollingMode.$el.scrollIntoView();
                    }
                });
            },
            /**
             * @desc 滚动策略更新
             * @param { String } expr 滚动表达式
             */
            handleRollingExprChange: _.debounce(function (expr) {
                this.$refs.expr && this.$refs.expr.clearValidator();
                this.validatorExpr(expr);
                this.$emit('on-change', this.exprField, expr);
            }, 20),
            /**
             * @desc 滚动机制更新
             * @param { Number } rollingMode
             */
            handleRollingModeChange (rollingMode) {
                this.$emit('on-change', this.modeField, rollingMode);
            },
        },
    };
</script>
<style lang="postcss">
    .task-step-rolling {
        display: block;

        .rolling-expr-field {
            .form-error-tip {
                display: none;
            }
        }

        .strategy-tips {
            margin-top: 4px;
            font-size: 12px;
            line-height: 16px;
            color: #979ba5;
        }

        .strategy-error {
            margin-top: 4px;
            font-size: 12px;
            line-height: 18px;
            color: #ea3636;
        }
    }
</style>
