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
    <div
        v-bkloading="{ isLoading }"
        class="jb-user-selector"
        :class="{ disabled: isLoading }">
        <user-selector
            v-if="!isLoading"
            v-bind="$attrs"
            :default-alternate="formatDefaultAlternate"
            :disabled-users="disabledUsers"
            :fuzzy-search-method="handleFuzzySearch"
            :history-key="historyKey"
            :list-scroll-height="300"
            :render-list="renterMerberItem"
            :render-tag="renderTag"
            :value="defaultValue"
            @change="handleChange" />
    </div>
</template>
<script>
    import _ from 'lodash';

    import NotifyService from '@service/notify';

    import { encodeRegexp } from '@utils/assist';

    import UserSelector from '@blueking/user-selector';

    import I18n from '@/i18n';

    const CACHE_KEY = 'job-user-selector-cache';

    export default {
        name: 'JobUserSelector',
        components: {
            UserSelector,
        },
        props: {
            // 已选 user
            user: {
                type: Array,
                default: () => [],
            },
            // 已选 role
            role: {
                type: Array,
                default: () => [],
            },
            // 筛选时每页条数
            limit: {
                type: Number,
                default: 10,
            },
            // 支持选择角色
            showRole: {
                type: Boolean,
                default: true,
            },
            // 在下拉面板中显示禁用用户
            showDisableUser: {
                type: Boolean,
                default: true,
            },
            // 不在下拉列表中显示设置的 user
            filterList: {
                type: Array,
                default: () => [],
            },
        },
        data () {
            return {
                isLoading: false,
                checkedUserNums: 0,
                roleList: [],
                defaultValue: [],
                disabledUsers: [],
            };
        },
        computed: {
            realLimit () {
                return this.limit + this.checkedUserNums;
            },
        },
        watch: {
            user: {
                handler () {
                    let valueList = [...this.user];
                    if (this.showRole) {
                        valueList = [...this.role].concat(valueList);
                    }
                    this.defaultValue = valueList.filter(name => !this.filterList.includes(name));
                },
                immediate: true,
            },
            role: {
                handler () {
                    const valueList = [
                        ...this.role,
                        ...this.user,
                    ];
                    this.defaultValue = valueList.filter(name => !this.filterList.includes(name));
                },
                immediate: true,
            },
        },
        created () {
            this.historyKey = CACHE_KEY;
            this.roleCacheMap = {};
            this.fetchRoleList();
        },
        methods: {
            /**
             * @desc 获取角色列表
             *
             * 如果不需要显示角色则不用请求接口
             */
            fetchRoleList () {
                if (!this.showRole) {
                    return;
                }
                this.isLoading = true;
                NotifyService.fetchRoleList()
                    .then((data) => {
                        const roleList = [];
                        const roleCacheMap = {};
                        data.forEach((role) => {
                            if (this.filterList.includes(role.code)) {
                                return;
                            }
                            roleList.push({
                                display_name: role.name,
                                username: role.code,
                                type: 'role',
                            });
                            roleCacheMap[role.code] = role.name;
                        });
                        // 角色列表
                        this.roleList = Object.freeze(roleList);
                        this.roleCacheMap = roleCacheMap;
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 检测用户是否被过滤掉
             * @param { String } username
             * @returns { Boolean }
             */
            checkUsernameFilter (username) {
                return this.filterList.includes(username);
            },
            /**
             * @desc 输入框获得焦点时面板数据
             *
             * 显示角色列表，显示用户最近输入缓存
             */
            formatDefaultAlternate () {
                const stack = [];
                
                if (this.roleList.length > 0) {
                    stack.push({
                        display_name: I18n.t('角色'),
                        username: 'role',
                        children: _.cloneDeep(this.roleList),
                    });
                }

                // 最近选择的数据
                if (localStorage.getItem(this.historyKey)) {
                    const historyList = JSON.parse(localStorage.getItem(this.historyKey)) || [];
                    // 只显示最近输入的用户
                    const children = historyList.reduce((result, item) => {
                        if (item.type === 'user'
                            && !this.checkUsernameFilter(item.username)) {
                            result.push(item);
                        }
                        return result;
                    }, []);
                    stack.push({
                        display_name: I18n.t('最近输入'),
                        username: 'history',
                        children,
                    });
                }
                return stack;
            },
            /**
             * @desc 模糊搜索
             * @param { String } keyword 用户搜索关键字
             */
            handleFuzzySearch (keyword) {
                const params = {
                    limit: this.realLimit,
                    prefixStr: keyword,
                };
                const formatData = (target) => {
                    const stack = [];
                    // 匹配角色
                    if (this.showRole) {
                        const filterReg = new RegExp(encodeRegexp(keyword));
                        const filterRoleList = this.roleList.filter(role => filterReg.test(role.display_name)
                            || filterReg.test(role.username));
                        if (filterRoleList.length > 0) {
                            stack.push({
                                display_name: I18n.t('角色'),
                                username: 'role',
                                children: filterRoleList,
                            });
                        }
                    }
                    // 用户接口数据为空
                    if (target.length < 1) {
                        return stack;
                    }
                    
                    this.disabledUsers = [];
                    const userList = [];
                
                    target.forEach((curUser) => {
                        // 被过滤掉的用户
                        if (this.filterList.includes(curUser.englishName)) {
                            return;
                        }
                        // 黑名单用户
                        if (!this.showDisableUser && !curUser.enable) {
                            return;
                        }
                        if (!curUser.enable) {
                            this.disabledUsers.push(curUser.englishName);
                        }
                        userList.push({
                            display_name: curUser.englishName,
                            username: curUser.englishName,
                            type: 'user',
                        });
                    });
                
                    stack.push({
                        display_name: I18n.t('用户'),
                        username: 'user',
                        children: Object.freeze(userList),
                    });
                    return stack;
                };
                return NotifyService.fetchAllUsers(params)
                    .then(data => ({
                        next: false,
                        results: formatData(data, keyword),
                    }));
            },
            renderTag (h, node) {
                let { username } = node;
                let iconType = 'user';
                // 角色
                if (this.roleCacheMap[node.username]) {
                    username = this.roleCacheMap[node.username];
                    iconType = 'user-group-gray';
                }
                
                return (
                    <div class='jb-user-seletor-member-tag' key={node.index} title={username}>
                        <icon type={iconType} class='tag-icon' />
                        <span class='text'>{username}</span>
                    </div>
                );
            },
            renterMerberItem (h, payload) {
                const curData = payload.user;
                
                const renderLogoHtml = () => {
                    if (curData.logo) {
                        return (
                            <img class='img' src={curData.logo} />
                        );
                    }
                    return (
                        <icon type={curData.type === 'role' ? 'user-group-gray' : 'user'} class='item-icon' />
                    );
                };

                const isDisabled = payload.disabled;
                const isEnLang = this.$i18n.locale === 'en-US';

                const renderForbidHtml = () => {
                    if (isDisabled) {
                        return (
                            <icon type={isEnLang ? 'forbid-en' : 'forbid'} class='forbid-icon' svg />
                        );
                    }
                    return '';
                };
                
                return (
                    <div
                        class={{
                            'jb-user-selector-member-item': true,
                            'is-disabled': isDisabled,
                        }}
                        title={curData.display_name}>
                        {renderLogoHtml()}
                        <span class='text'>{curData.display_name}</span>
                        {renderForbidHtml()}
                    </div>
                );
            },
            handleChange (payload) {
                const role = [];
                const user = [];
                payload.forEach((item) => {
                    if (this.roleCacheMap[item]) {
                        role.push(item);
                    } else {
                        user.push(item);
                    }
                });
                this.checkedUserNums = user.length;
                this.$emit('on-change', user, role);
            },
        },
    };
</script>
<style lang="postcss">
    .jb-user-selector {
        width: 100%;

        &.disabled {
            height: 32px;
        }

        .user-selector {
            width: 100%;
        }
    }

    .jb-user-seletor-member-tag {
        padding-right: 2px;

        .tag-icon {
            font-size: 16px;
            color: #979ba5;
        }

        .text {
            display: inline-block;
            max-width: 150px;
            margin-left: 4px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            vertical-align: top;
        }
    }

    .jb-user-selector-member-item {
        position: relative;
        display: flex;
        padding-left: 10px;
        align-items: center;

        &.is-disabled {
            cursor: not-allowed;
            user-select: none;

            .text,
            .item-icon {
                color: #c4c6cc;
            }
        }

        .img {
            width: 20px;
            border-radius: 50%;
        }

        .item-icon {
            font-size: 20px;
            color: #979ba5;
        }

        .text {
            display: inline-block;
            max-width: 150px;
            margin-left: 5px;
            overflow: hidden;
            color: #63656e;
            text-overflow: ellipsis;
            white-space: nowrap;
            vertical-align: top;
        }

        .forbid-icon {
            position: absolute;
            right: 0;
            margin-left: 4px;
            font-size: 30px;
        }
    }
</style>
