<template>
    <div>
        <jb-search-select
            @on-change="handleSearch"
            :data="searchSelect"
            :popover-zindex="99999"
            :placeholder="$t('script.搜索名称，版本号')"
            style="width: 100%; margin-bottom: 20px;" />
        <div v-bkloading="{ isLoading }">
            <bk-table :data="renderList">
                <!-- 执行方案引用 -->
                <bk-table-column
                    :label="$t('script.执行方案')"
                    align="left"
                    show-overflow-tooltip>
                    <template slot-scope="{ row }">
                        <bk-button
                            text
                            @click="handleGoPlanDetail(row)">
                            {{ row.taskPlanName }}
                        </bk-button>
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('script.引用的版本号')"
                    prop="scriptVersion"
                    align="left" />
                <bk-table-column
                    :label="$t('script.状态')"
                    prop="scriptStatusDesc"
                    align="left"
                    width="120">
                    <template slot-scope="{ row }">
                        <span v-html="row.statusHtml" />
                    </template>
                </bk-table-column>
            </bk-table>
        </div>
    </div>
</template>
<script>
    import ScriptService from '@service/script-manage';
    import PublicScriptService from '@service/public-script-manage';
    import I18n from '@/i18n';
    import {
        checkPublicScript,
        encodeRegexp,
    } from '@utils/assist';
    import JbSearchSelect from '@components/jb-search-select';

    export default {
        name: '',
        components: {
            JbSearchSelect,
        },
        props: {
            params: {
                type: Object,
                required: true,
            },
        },
        data () {
            return {
                isLoading: false,
                wholeList: [],
                renderList: [],
            };
        },
        created () {
            this.publicScript = checkPublicScript(this.$route);
            this.serviceHandler = this.publicScript ? PublicScriptService : ScriptService;

            this.fetchData();

            this.searchSelect = [
                {
                    name: I18n.t('script.名称'),
                    id: 'name',
                    default: true,
                },
                {
                    name: I18n.t('script.版本号.colHead'),
                    id: 'scriptVersion',
                },
            ];
        },
        
        methods: {
            /**
             * @desc 获取关联脚本列表
             */
            fetchData () {
                this.isLoading = true;
                this.serviceHandler.citeInfo(this.params)
                    .then(({ citedTaskPlanList }) => {
                        this.wholeList = Object.freeze(citedTaskPlanList);
                        this.renderList = this.wholeList;
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 本地搜索
             * @param {Object} payload 搜索条件
             */
            handleSearch (payload) {
                let list = this.wholeList;
                Object.keys(payload).forEach((key) => {
                    const reg = new RegExp(encodeRegexp(payload[key]), 'i');
                    let realKey = key;
                    if (key === 'name') {
                        realKey = 'taskPlanName';
                    }
                    list = list.filter(item => reg.test(item[realKey]));
                });
                this.renderList = Object.freeze(list);
            },
            /**
             * @desc 查看引用脚本的执行方案详情
             * @param {Object} payload 应用字段数据
             *
             * 需要解析资源的 scopeType、scopeId
             */
            handleGoPlanDetail (payload) {
                const { href } = this.$router.resolve({
                    name: 'viewPlan',
                    params: {
                        templateId: payload.taskTemplateId,
                    },
                    query: {
                        viewPlanId: payload.taskPlanId,
                    },
                });
                window.open(href.replace(/^\/[^/]+\/\d+/, `/${payload.scopeType}/${payload.scopeId}`));
            },
        },
    };
</script>
