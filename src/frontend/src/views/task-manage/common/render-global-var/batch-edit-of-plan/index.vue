<template>
    <global-variable-layout style="margin-bottom: -20px;">
        <global-variable
            v-for="variable in variableList"
            ref="variable"
            :type="variable.type"
            :key="variable.id"
            :data="variable" />
    </global-variable-layout>
</template>
<script>
    import GlobalVariableLayout from '@components/global-variable/layout';
    import GlobalVariable from '@components/global-variable/edit';

    export default {
        name: '',
        components: {
            GlobalVariableLayout,
            GlobalVariable,
        },
        props: {
            variableList: Array,
        },
        methods: {
            submit () {
                return Promise.all(this.$refs.variable.map(item => item.validate()))
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
