<template>
    <div class="ip-selector-custom-input">
        <div
            ref="inputRef"
            class="custom-input">
            <bk-input
                v-model="customInputText"
                type="textarea"
                :native-attributes="{ spellcheck: false }"
                :style="customInputStyles" />
            <div class="custom-input-parse-error">
                <span v-if="errorInputStack.length > 0">
                    {{ errorInputStack.length }}处格式有误
                    <span
                        v-bk-tooltips="$t('标识错误')"
                        class="parse-error-btn"
                        @click="handleHighlightError">
                        <i class="bk-ipselector-icon bk-ipselector-ip-audit" />
                    </span>
                </span>
                <span v-if="invalidInputStack.length > 0">
                    <span v-if="errorInputStack.length > 0"> ;</span>
                    {{ invalidInputStack.length }}处 IP 不存在
                    <span
                        v-bk-tooltips="$t('标识错误')"
                        class="parse-error-btn"
                        @click="handleHighlightInvalid">
                        <i class="bk-ipselector-icon bk-ipselector-ip-audit" />
                    </span>
                </span>
            </div>
            <div class="custom-input-action">
                <bk-button
                    size="small"
                    theme="primary"
                    :loading="isLoading"
                    outline
                    class="parse-btn"
                    @click="handleParseCustomInput">
                    点击解析
                </bk-button>
                <bk-button
                    size="small"
                    class="clear-btn"
                    @click="handleClearCustomInput">
                    清空
                </bk-button>
            </div>
        </div>
        <div
            class="host-table"
            v-bkloading="{ isLoading }">
            <bk-input
                v-model="serachKey"
                placeholder="请输入 IP/IPv6/主机名称 或 选择条件搜索"
                style="margin-bottom: 12px;" />
            <render-host-table
                :data="renderData"
                :height="renderTableHeight"
                @row-click="handleRowClick">
                <template #header-selection>
                    <page-check
                        :value="pageCheckValue"
                        @change="handlePageCheck" />
                </template>
                <template #selection="{ row }">
                    <bk-checkbox :value="Boolean(hostCheckedMap[row.hostId])" />
                </template>
                <template
                    v-if="hostTableData.length < 1"
                    #empty>
                    <i class="bk-ipselector-icon bk-ipselector-info-circle" />
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
    </div>
</template>
<script setup>
    import {
        ref,
        shallowRef,
        computed,
        watch,
    } from 'vue';
    import _ from 'lodash';
    import AppManageService from '@service/app-manage';
    import RenderHostTable from '../../../common/render-table/host.vue';
    import useDialogSize from '../../../hooks/use-dialog-size';
    import useDebounceRef from '../../../hooks/use-debounced-ref';
    import useLocalPagination from '../../../hooks/use-local-pagination';
    import {
        getPaginationDefault,
        encodeRegexp,
        makeMap,
    } from '../../../utils';
    import PageCheck from '../table-page-check.vue';

    const props = defineProps({
        lastHostList: {
            type: Array,
            required: true,
        },
    });

    const emits = defineEmits(['change']);

    const isLoading = ref(false);
    const inputRef = ref();
    const customInputText = ref('');
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

    const customInputStyles = computed(() => ({
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

    watch(() => props.lastHostList, (lastHostList) => {
        hostCheckedMap.value = lastHostList.reduce((result, hostItem) => {
            result[hostItem.hostId] = hostItem;
            return result;
        }, {});
    }, {
        immediate: true,
    });

    // 判断 page-check 的状态
    const syncPageCheckValue = () => {
        if (hostTableData.value.length > 0) {
            pageCheckValue.value = 'page';
            hostTableData.value.forEach((hostItem) => {
                if (!hostCheckedMap.value[hostItem.hostId]) {
                    pageCheckValue.value = '';
                }
            });
        } else {
            pageCheckValue.value = '';
        }
    };

    const triggerChange = () => {
        emits('change', 'customInputHostList', Object.values(hostCheckedMap.value));
    };

    // 解析输入
    const handleParseCustomInput = () => {
        const inputText = _.trim(customInputText.value);
        if (!inputText) {
            return;
        }
        const itemList = inputText.split(/[;,；，\n|]+/).filter(_ => !!_);

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
        const keyRegex = /^[1-9a-z][0-9a-z_-]*$/;

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
            const keyMatch = itemText.match(keyRegex);
            if (keyMatch) {
                const [key] = keyMatch;
                keyList.push(key);
                return;
            }
            // 输入内容无法匹配任何格式
            errorInput.push(itemText);
        });

        isLoading.value = true;
        AppManageService.fetchInputParseHostList({
            ipList,
            ipv6List,
            keyList,
        })
        .then((data) => {
            hostTableData.value = data;
            syncPageCheckValue();
            const ipMap = makeMap(ipList);
            const ipv6Map = makeMap(ipv6List);
            const keyMap = makeMap(keyList);
            
            data.forEach((hostData) => {
                if (ipMap[hostData.ip]) {
                    delete ipMap[hostData.ip];
                }
                if (ipMap[`${hostData.cloudArea.id}:${hostData.ip}`]) {
                    delete ipMap[`${hostData.cloudArea.id}:${hostData.ip}`];
                }
                if (ipv6Map[hostData.ipv6]) {
                    delete ipv6Map[hostData.ipv6];
                }
                if (keyMap[hostData.hostId]) {
                    delete keyMap[hostData.hostId];
                }
                if (keyMap[hostData.hostName]) {
                    delete keyMap[hostData.hostName];
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
            customInputText.value = [
                errorInputStack.value.join('\n'),
                invalidInputStack.value.join('\n'),
            ].join('\n');
        })
        .finally(() => {
            isLoading.value = false;
        });
    };

    // 清空输入框
    const handleClearCustomInput = () => {
        invalidInputStack.value = [];
        errorInputStack.value = [];
        customInputText.value = '';
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
            renderData.value.forEach((hostItem) => {
                checkedMap[hostItem.hostId] = hostItem;
            });
        } else if (checkValue === 'pageCancle') {
            renderData.value.forEach((hostItem) => {
                delete checkedMap[hostItem.hostId];
            });
        } else if (checkValue === 'allCancle') {
            searchWholeData.value.forEach((hostItem) => {
                delete checkedMap[hostItem.hostId];
            });
        } else if (checkValue === 'all') {
            hostTableData.value.forEach((hostItem) => {
                checkedMap[hostItem.hostId] = hostItem;
            });
        }
        pageCheckValue.value = checkValue;
        hostCheckedMap.value = checkedMap;
        triggerChange();
    };

    // 选中指定主机
    const handleRowClick = (data) => {
        const checkedMap = { ...hostCheckedMap.value };
        if (checkedMap[data.hostId]) {
            delete checkedMap[data.hostId];
        } else {
            checkedMap[data.hostId] = data;
        }
        hostCheckedMap.value = checkedMap;
        triggerChange();
        syncPageCheckValue();
    };
    
</script>
<style lang="postcss">
    .ip-selector-custom-input {
        display: flex;

        .custom-input {
            flex: 0 0 383px;

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
            padding: 0 24px 0 16px;
        }
    }
</style>
