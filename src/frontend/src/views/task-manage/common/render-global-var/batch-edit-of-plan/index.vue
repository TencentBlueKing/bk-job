<template>
    <global-variable-layout type="vertical">
        <global-variable
            v-for="variable in usedList"
            ref="used"
            :type="variable.type"
            :key="variable.id"
            :data="variable" />
        <toggle-display
            v-if="unusedList.length > 0"
            :count="unusedList.length"
            style="margin-top: 20px;">
            <div style="margin-top: 20px;">
                <global-variable
                    v-for="variable in unusedList"
                    ref="unused"
                    :type="variable.type"
                    :key="variable.id"
                    :data="variable" />
            </div>
        </toggle-display>
    </global-variable-layout>
</template>
<script>
    import GlobalVariableLayout from '@components/global-variable/layout';
    import GlobalVariable from '@components/global-variable/edit';
    import ToggleDisplay from '@components/global-variable/toggle-display';

    export default {
        name: '',
        components: {
            ToggleDisplay,
            GlobalVariableLayout,
            GlobalVariable,
        },
        props: {
            variableList: Array,
            selectedList: {
                type: Array,
                default: () => [],
            },
        },
        created () {
            const selectedNameMap = this.selectedList.reduce((result, name) => {
                result[name] = true;
                return result;
            }, {});
            // 执行方案中的步骤使用了得变量
            this.usedList = [];
            // 未被使用的变量
            this.unusedList = [];
            this.variableList.forEach((variable) => {
                if (selectedNameMap[variable.name]) {
                    this.usedList.push(variable);
                } else {
                    this.unusedList.push(variable);
                }
            });
        },
        methods: {
            submit () {
                const validateQueue = [];
                if (this.$refs.used) {
                    this.$refs.used.forEach((validate) => {
                        validateQueue.push(validate());
                    });
                }
                if (this.$refs.unused) {
                    this.$refs.unused.forEach((validate) => {
                        validateQueue.push(validate());
                    });
                }
                return Promise.all(validateQueue)
                    .then((editVariableList) => {
                        // 全局变量的最新值
                        const editVariableMap = editVariableList.reduce((result, variable) => {
                            const { value, targetValue } = variable;
                            result[variable.id] = {
                                defaultValue: value,
                                defaultTargetValue: targetValue,
                            };
                            return result;
                        }, {});
                        // 更新执行方案中全局变量的值
                        const variableList = this.variableList.reduce((result, variable) => {
                            if (editVariableMap[variable.id]) {
                                result.push(Object.assign({}, variable, editVariableMap[variable.id]));
                            }
                            return result;
                        }, []);
                        this.$emit('on-change', variableList);
                    });
            },
        },
    };
</script>
