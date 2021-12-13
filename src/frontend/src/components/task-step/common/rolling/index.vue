<template>
    <div class="task-step-rolling">
        <jb-form-item label="滚动执行">
            <bk-switcher
                :value="isRolling"
                theme="primary"
                @change="handleRollingEnableChange" />
        </jb-form-item>
        <div v-show="isRolling">
            <jb-form-item
                label="滚动策略"
                required
                :property="rollingExprField"
                :rules="rollingExprRule">
                <div class="form-item-content">
                    <bk-input
                        :value="formData[rollingExprField]"
                        @change="handleRollingExprChange" />
                    <div v-if="tips" class="strategy-tips">{{ tips }}</div>
                    <div v-if="errorMessage" class="strategy-error">{{ errorMessage }}</div>
                </div>
            </jb-form-item>
            <jb-form-item ref="rollingMode" label="滚动机制" required>
                <bk-select
                    :value="formData[rollingModeField]"
                    :clearable="false"
                    class="form-item-content">
                    <bk-option :id="1" name="默认（执行失败则暂停）" />
                    <bk-option :id="11" name="执行成功，自动滚动下一批" />
                    <bk-option :id="1111" name="忽略失败，自动滚动下一批" />
                    <bk-option :id="111" name="不自动，每批次都人工确认" />
                </bk-select>
            </jb-form-item>
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';
    import rollingExecute from '@utils/rolling-execute';

    export default {
        name: '',
        props: {
            rollingExprField: {
                type: String,
                required: true,
            },
            rollingModeField: {
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
                isRolling: false,
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
                if (!this.isRolling) {
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
                                return !!rollingExecute(value);
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
        methods: {
            /**
             * @desc 是否启用滚动
             * @param { Boolean } value
             */
            handleRollingEnableChange (value) {
                this.isRolling = value;
                if (!this.isRolling) {
                    this.tips = '';
                    this.errorMessage = '';
                }
                
                this.$nextTick(() => {
                    if (this.isRolling) {
                        this.$refs.rollingMode.$el.scrollIntoView();
                    }
                });
            },
            /**
             * @desc 滚动策略更新
             * @param { String } expr 滚动表达式
             */
            handleRollingExprChange: _.debounce(function (expr) {
                try {
                    this.errorMessage = '';
                    this.tips = rollingExecute(expr);
                } catch (error) {
                    this.tips = '';
                    this.errorMessage = error.message;
                }
            }, 20),
        },
    };
</script>
<style lang="postcss">
    .task-step-rolling {
        display: block;

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
