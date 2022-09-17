<template>
    <div class="ip-selector-manual-input">
        <resize-layout
            :default-width="385"
            flex-direction="left">
            <div
                ref="inputRef"
                class="custom-input">
                <bk-input
                    v-model="manualInputText"
                    :native-attributes="{ spellcheck: false }"
                    placeholder="请输入 IP / IPv6 或主机名称，如（192.168.1.112 或 ebd4:3e1::e13），带云区域请使用冒号分隔，如：0:192.168.9.10
多个可使用换行，空格或；，｜ ”分隔"
                    :style="manualInputStyles"
                    type="textarea" />
                <div class="custom-input-parse-error">
                    <span v-if="errorInputStack.length > 0">
                        <span style="padding-right: 2px; font-weight: bold;">
                            {{ errorInputStack.length }}
                        </span>
                        处格式有误
                        <span
                            v-bk-tooltips="'标识错误'"
                            class="parse-error-btn"
                            @click="handleHighlightError">
                            <ip-selector-icon type="ip-audit" />
                        </span>
                    </span>
                    <span v-if="invalidInputStack.length > 0">
                        <span v-if="errorInputStack.length > 0"> ;</span>
                        <span style="padding-right: 2px; font-weight: bold;">
                            {{ invalidInputStack.length }}
                        </span>
                        处 IP 不存在
                        <span
                            v-bk-tooltips="'标识错误'"
                            class="parse-error-btn"
                            @click="handleHighlightInvalid">
                            <ip-selector-icon type="ip-audit" />
                        </span>
                    </span>
                </div>
                <div class="custom-input-action">
                    <bk-button
                        class="parse-btn"
                        :loading="isLoading"
                        outline
                        size="small"
                        theme="primary"
                        @click="handleParseManualInput">
                        点击解析
                    </bk-button>
                    <bk-button
                        class="clear-btn"
                        size="small"
                        @click="handleClearManualInput">
                        清空
                    </bk-button>
                </div>
            </div>
            <template #right>
                <div
                    v-bkloading="{ isLoading }"
                    class="host-table">
                    <bk-input
                        v-model="serachKey"
                        placeholder="请输入 IP/IPv6/主机名称 或 选择条件搜索"
                        style="margin-bottom: 12px;" />
                    <render-host-table
                        :data="renderData"
                        :height="renderTableHeight"
                        @row-click="handleRowClick">
                        <template #header-selection>
                            <table-page-check
                                :disabled="renderData.length < 1"
                                :value="pageCheckValue"
                                @change="handlePageCheck" />
                        </template>
                        <template #selection="{ row }">
                            <bk-checkbox :value="Boolean(hostCheckedMap[row.host_id])" />
                        </template>
                        <template
                            v-if="hostTableData.length < 1"
                            #empty>
                            <ip-selector-icon type="info-circle" />
                            请先从右侧输入主机并解析
                        </template>
                    </render-host-table>
                    <div
                        v-if="isShowPagination"
                        style="padding: 0 10px 8px 0;">
                        <bk-pagination
                            style="margin-top: 8px;"
                            v-bind="pagination"
                            @change="handlePaginationCurrentChange"
                            @limit-change="handlePaginationLimitChange" />
                    </div>
                </div>
            </template>
        </resize-layout>
    </div>
</template>
<script>
    export default {
        inheritAttrs: false,
    };
</script>
<script setup>
    import _ from 'lodash';
    import {
        computed,
        ref,
        shallowRef,
        watch,
    } from 'vue';

    import IpSelectorIcon from '../../../common/ip-selector-icon';
    import RenderHostTable from '../../../common/render-table/host/index.vue';
    import useDebounceRef from '../../../hooks/use-debounced-ref';
    import useDialogSize from '../../../hooks/use-dialog-size';
    import useLocalPagination from '../../../hooks/use-local-pagination';
    import Manager from '../../../manager';
    import {
        encodeRegexp,
        getPaginationDefault,
        makeMap,
    } from '../../../utils';
    import ResizeLayout from '../resize-layout.vue';
    import TablePageCheck from '../table-page-check.vue';

    const props = defineProps({
        lastHostList: {
            type: Array,
            required: true,
        },
    });

    const emits = defineEmits(['change']);

    const isLoading = ref(false);
    const inputRef = ref();
    const manualInputText = ref('');
    const hostTableData = shallowRef([]);
    const hostCheckedMap = shallowRef({});
    const errorInputStack = shallowRef([]);
    const invalidInputStack = shallowRef([]);

    const tableOffetTop = 60;
    const {
        contentHeight: dialogContentHeight,
    } = useDialogSize();
    const renderTableHeight = dialogContentHeight.value - tableOffetTop;

    const serachKey = useDebounceRef('');
    const pageCheckValue = ref('');

    const manualInputStyles = computed(() => ({
        height: `${dialogContentHeight.value - 94}px`,
    }));

    const {
        isShowPagination,
        pagination,
        data: renderData,
        serachList: searchWholeData,
        handlePaginationCurrentChange,
        handlePaginationLimitChange,
    } = useLocalPagination(hostTableData, getPaginationDefault(renderTableHeight));

    // 判断 page-check 的状态
    const syncTablePageCheckValue = () => {
        if (hostTableData.value.length > 0) {
            pageCheckValue.value = 'page';
            hostTableData.value.forEach((hostDataItem) => {
                if (!hostCheckedMap.value[hostDataItem.host_id]) {
                    pageCheckValue.value = '';
                }
            });
        } else {
            pageCheckValue.value = '';
        }
    };

    let isInnerChange = false;

    watch(() => props.lastHostList, (lastHostList) => {
        if (isInnerChange) {
            isInnerChange = false;
            return;
        }
        hostCheckedMap.value = lastHostList.reduce((result, hostDataItem) => {
            result[hostDataItem.host_id] = hostDataItem;
            return result;
        }, {});
        syncTablePageCheckValue();
    }, {
        immediate: true,
    });

    const triggerChange = () => {
        isInnerChange = true;
        emits('change', 'hostList', Object.values(hostCheckedMap.value));
    };

    // 解析输入
    const handleParseManualInput = () => {
        const inputText = _.trim(manualInputText.value);
        if (!inputText) {
            return;
        }
        const itemList = inputText.split(/[;,；，\n |]+/).filter(_ => !!_);

        const ipRegex = /(((\d+:)?)(?:(?:25[0-5]|2[0-4]\d|((1\d{2})|([1-9]?\d)))\.){3}(?:25[0-5]|2[0-4]\d|((1\d{2})|([1-9]?\d))))/;
        const ipv6RegexList = [
            '(([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))',
            '(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}',
            '((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))',
            '(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})',
            ':((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))',
            '(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})',
            '((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))',
            '(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})',
            '((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))',
            '(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})',
            '((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))',
            '(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})',
            '((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))',
            '(:(((:[0-9A-Fa-f]{1,4}){1,7})',
            '((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))',
        ];
        const ipv6Regex = new RegExp(`^\\s*(${ipv6RegexList.join('|')})(%.+)?\\s*$`);

        const errorInput = [];
        const ipList = [];
        const ipv6List = [];
        const keyList = [];

        itemList.forEach((itemText) => {
            const ipMatch = itemText.match(ipRegex);
            if (ipMatch) {
                const [ipStr] = ipMatch;
                const errorIPRule = new RegExp(`([\\d:.]${encodeRegexp(ipStr)})|(${encodeRegexp(ipStr)}[\\d.])`);
                if (errorIPRule.test(itemText)) {
                    // 无法完全正确的解析 IP（eq: 1.1.1.12.2.2.2，少了分隔造成的）
                    errorInput.push(itemText);
                } else {
                    // 提取识别成功的 IP
                    ipList.push(ipStr);
                    // 将剩下的内容作为错误 IP 处理
                    const errorText = itemText.replace(new RegExp(`(${encodeRegexp(ipStr)})|(\\s)`, 'g'), '');
                    if (errorText) {
                        errorInput.push(errorText);
                    }
                }
                return;
            }
            const ipv6Match = itemText.match(ipv6Regex);
            if (ipv6Match) {
                const [ipv6Str] = ipv6Match;
                ipv6List.push(ipv6Str);
                return;
            }
            if (itemText.length <= 256) {
                keyList.push(itemText);
                return;
            }
            // 输入内容无法匹配任何格式
            errorInput.push(itemText);
        });

        isLoading.value = true;
        Manager.service.fetchHostCheck({
            [Manager.nameStyle('ipList')]: ipList,
            [Manager.nameStyle('ipv6List')]: ipv6List,
            [Manager.nameStyle('keyList')]: keyList,
        })
        .then((data) => {
            hostTableData.value = data;
            syncTablePageCheckValue();
            const ipMap = makeMap(ipList);
            const ipv6Map = makeMap(ipv6List);
            const keyMap = makeMap(keyList);
            
            data.forEach((hostData) => {
                if (ipMap[hostData.ip]) {
                    delete ipMap[hostData.ip];
                }
                if (ipMap[`${hostData.cloud_area.id}:${hostData.ip}`]) {
                    delete ipMap[`${hostData.cloud_area.id}:${hostData.ip}`];
                }
                if (ipv6Map[hostData.ipv6]) {
                    delete ipv6Map[hostData.ipv6];
                }
                if (keyMap[hostData.host_id]) {
                    delete keyMap[hostData.host_id];
                }
                if (keyMap[hostData.host_name]) {
                    delete keyMap[hostData.host_name];
                }
            });
            errorInputStack.value = [
                ...errorInput,
                ...Object.keys(keyMap),
            ];
            invalidInputStack.value = [
                ...Object.keys(ipMap),
                ...Object.keys(ipv6Map),
            ];
            manualInputText.value = _.filter([
                errorInputStack.value.join('\n'),
                invalidInputStack.value.join('\n'),
            ], _ => _).join('\n');
        })
        .finally(() => {
            isLoading.value = false;
        });
    };

    // 清空输入框
    const handleClearManualInput = () => {
        invalidInputStack.value = [];
        errorInputStack.value = [];
        manualInputText.value = '';
    };

    // 高亮错误的输入
    const handleHighlightError = () => {
        const $inputEl = inputRef.value.querySelector('textarea');
        const errorText = errorInputStack.value.join('\n');
        $inputEl.focus();
        $inputEl.selectionStart = 0;
        $inputEl.selectionEnd = errorText.length;
    };
    // 高亮无效的输入（eq：IP 格式正确，但是不在指定的业务下面）
    const handleHighlightInvalid = () => {
        const $inputEl = inputRef.value.querySelector('textarea');
        $inputEl.focus();
        const errorText = errorInputStack.value.join('\n');
        const invalidText = invalidInputStack.value.join('\n');
        
        const startIndex = errorText.length > 0 ? errorText.length + 1 : 0;
        $inputEl.selectionStart = startIndex;
        $inputEl.selectionEnd = startIndex + invalidText.length;
    };
    
    // 本页全选、跨页全选
    const handlePageCheck = (checkValue) => {
        const checkedMap = { ...hostCheckedMap.value };

        if (checkValue === 'page') {
            renderData.value.forEach((hostDataItem) => {
                checkedMap[hostDataItem.host_id] = hostDataItem;
            });
        } else if (checkValue === 'pageCancle') {
            renderData.value.forEach((hostDataItem) => {
                delete checkedMap[hostDataItem.host_id];
            });
        } else if (checkValue === 'allCancle') {
            searchWholeData.value.forEach((hostDataItem) => {
                delete checkedMap[hostDataItem.host_id];
            });
        } else if (checkValue === 'all') {
            hostTableData.value.forEach((hostDataItem) => {
                checkedMap[hostDataItem.host_id] = hostDataItem;
            });
        }
        pageCheckValue.value = checkValue;
        hostCheckedMap.value = checkedMap;
        triggerChange();
    };

    // 选中指定主机
    const handleRowClick = (data) => {
        const checkedMap = { ...hostCheckedMap.value };
        if (checkedMap[data.host_id]) {
            delete checkedMap[data.host_id];
        } else {
            checkedMap[data.host_id] = data;
        }
        hostCheckedMap.value = checkedMap;
        triggerChange();
        syncTablePageCheckValue();
    };
    
</script>
<style lang="postcss">
    .ip-selector-manual-input {
        .custom-input {
            padding-left: 16px;

            .custom-input-parse-error {
                display: flex;
                margin-top: 7px;
                font-size: 12px;
                color: #ea3636;

                .parse-error-btn {
                    color: #979ba5;
                    cursor: pointer;
                }
            }

            .custom-input-action {
                display: flex;
                margin-top: 13px;
            }

            .bk-textarea-wrapper,
            .bk-form-textarea {
                height: 100%;
            }

            textarea {
                &::selection {
                    color: #63656e;
                    background: #fdd;
                }
            }

            .parse-btn {
                flex: 1;
            }

            .clear-btn {
                width: 88px;
                margin-left: 8px;
            }
        }

        .host-table {
            flex: 1;
            padding-left: 16px;
        }
    }
</style>
