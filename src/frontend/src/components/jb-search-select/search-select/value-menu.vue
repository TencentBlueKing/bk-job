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

<script>
    import _ from 'lodash';

    import { encodeRegexp } from './helper';
    import locale from './locale';
    import Mixin from './mixin';

    export default {
        name: 'BKSearchValue',
        mixins: [
            Mixin,
        ],
        data () {
            return {
                activeIndex: -1, // 上下键选中项索引，多选时不支持上下键位操作
                list: [],
                search: '', // 输入框的键入的筛选值
                currentItem: {}, // 当前key
                menu: {},
                error: '',
                loading: false,
                checkeMap: {}, // 多选已选中的项
            };
        },
        computed: {
            // 是否弹出面板
            needRender () {
                const { currentItem } = this;
                // 未选中key
                if (!currentItem.id) {
                    return false;
                }
                // 已选中key
                // 有配置 conditions，并且没有选中 conditions
                if (currentItem.conditions
                    && currentItem.conditions.length
                    && !this.menu.condition[this.searchSelect.primaryKey]) {
                    return true;
                }
                // 已选中key
                // 但没有配置children && remoteMethod
                if (!(currentItem.children && currentItem.children.length > 0)
                    && typeof currentItem.remoteMethod !== 'function') {
                    return false;
                }
                return true;
            },
            // 是否多选
            isMultiable () {
                const { currentItem } = this;
                // 1，如果是条件筛选则只支持单选
                if (currentItem.conditions && currentItem.conditions.length) {
                    return false;
                }
                // 2，非条件筛选根据用户配置
                return currentItem.multiable;
            },
            // 是否展示conditions
            isCondition () {
                // 1，有配置conditions
                // 2，未选择conditions
                const { currentItem } = this;
                return currentItem.conditions && currentItem.conditions.length > 0;
            },
        },
        watch: {
            // 处理默认选中、筛选选中状态
            list (list) {
                this.activeIndex = -1;
                if (this.isMultiable) {
                    // 多选
                        
                    // 没有过滤项默认不选中——不做选中处理
                    if (!this.search) {
                        return;
                    }
                    const {
                        primaryKey,
                        displayKey,
                    } = this.searchSelect;
                    const checked = {};
                    const searchKeys = this.search.split(/[｜|]/);
                    searchKeys.forEach((currentKey) => {
                        // 过滤空值
                        if (!currentKey.replace(/[ {2}\n]/, '')) {
                            return;
                        }
                        const realSearch = currentKey.trim();
                        // 忽律大小写精确匹配
                        const regx = new RegExp(`^${encodeRegexp(realSearch)}$`, 'i');
                        list.forEach((currentValue) => {
                            if (regx.test(currentValue[displayKey])) {
                                checked[currentValue[primaryKey]] = currentValue;
                            }
                        });
                    });
                    this.checkeMap = Object.freeze(checked);
                } else {
                    // 单选

                    // 没有过滤项——默认选中第一个
                    if (!this.search) {
                        this.activeIndex = 0;
                        return;
                    }
                    // 默认选中模糊匹配的第一个
                    const regx = new RegExp(encodeRegexp(this.search), 'i');
                    // eslint-disable-next-line no-plusplus
                    for (let i = 0; i < list.length; i++) {
                        if (regx.test(list[i][this.searchSelect.displayKey])) {
                            this.activeIndex = i;
                            return;
                        }
                    }
                }
            },
        },
        created () {
            setTimeout(() => {
                const checkMap = this.menu.checked.reduce((result, item) => {
                    result[item.id] = item;
                    return result;
                }, {});
                this.checkeMap = Object.freeze(checkMap);
            });
            // eslint-disable-next-line no-underscore-dangle
            this.generatorList = _.debounce(this._generatorList, 200);
        },
        mounted () {
            document.body.addEventListener('keydown', this.handleKeydown);
        },
        beforeDestroy () {
            document.body.removeEventListener('keydown', this.handleKeydown);
        },
        methods: {
            /**
             * @desc 生成 value 面板的列表
             */
            async _generatorList () {
                const { currentItem } = this;
                this.error = false;

                if (!this.needRender) {
                    return;
                }

                // 本地配置condition
                // 没有选择condition 优先选择condition
                if (this.isCondition && !this.menu.condition[this.searchSelect.primaryKey]) {
                    this.loading = false;
                    this.list = Object.freeze([
                        ...currentItem.conditions,
                    ]);
                    return;
                }

                // 本地配置children
                if (currentItem.children && currentItem.children.length > 0) {
                    this.loading = false;
                    this.list = Object.freeze([
                        ...currentItem.children,
                    ]);
                    return;
                }

                // 远程获取value列表
                let remoteMethod = '';
                if (typeof currentItem.remoteMethod === 'function') {
                    const {
                        _remoteKeyImmediateChildrenMap,
                        primaryKey,
                    } = this.searchSelect;
                    // remoteMethod 是立即执行的——从缓存中取值
                    const children = _remoteKeyImmediateChildrenMap[currentItem[primaryKey]];
                    if (children) {
                        this.list = Object.freeze([
                            ...children,
                        ]);
                        return;
                    }
                    /* eslint-disable prefer-destructuring */
                    remoteMethod = currentItem.remoteMethod;
                }
                if (remoteMethod) {
                    this.loading = true;
                    try {
                        const list = await remoteMethod(this.search, currentItem, 0);
                        this.list = Object.freeze([
                            ...list,
                        ]);
                    } catch {
                        this.error = true;
                    } finally {
                        this.loading = false;
                    }
                }
            },
            
            handleClick (item) {
                // 禁用
                if (item.disabled) {
                    return false;
                }
                // 条件筛选
                if (this.isCondition && Object.keys(this.menu.condition).length < 1) {
                    this.$emit('select-condition', item);
                    return;
                }
                const { primaryKey } = this.searchSelect;
                
                // 多选
                if (this.isMultiable) {
                    const checkeMap = { ...this.checkeMap };
                    const key = item[primaryKey];
                    if (checkeMap[key]) {
                        delete checkeMap[key];
                    } else {
                        checkeMap[key] = item;
                    }
                    this.checkeMap = Object.freeze(checkeMap);
                    this.$emit('select-check', Object.values(this.checkeMap));
                } else {
                    // 单选
                    const checkeMap = {
                        [item[primaryKey]]: item,
                    };
                    this.checkeMap = Object.freeze(checkeMap);
                    this.handleSubmit();
                }
            },
            
            handleKeydown (e) {
                if (!this.needRender) {
                    return;
                }
                // 多选不支持上下移动选中
                if (this.isMultiable || this.list.length < 1) {
                    return;
                }
                // enter键直接触发选中
                if (event.keyCode === 13) {
                    if (this.activeIndex < 0) {
                        return;
                    }
                    this.handleClick(this.list[this.activeIndex], this.activeIndex);
                    return;
                }
                this.scrollActiveToView(event);
            },
            
            handleSubmit (e) {
                this.$emit('select-check', Object.values(this.checkeMap));
                
                this.$emit('change');
            },
            
            handleCancel () {
                this.$emit('cancel');
            },

            renderContent (h) {
                // 显示错误
                if (this.error) {
                    return (
                    <div class="search-error">{ this.error }</div>
                    );
                }
                // 显示loading
                if (this.loading) {
                    return (
                    <div class="search-loading">{ this.searchSelect.remoteLoadingText }</div>
                    );
                }
                // 列表为空
                if (this.needRender && !this.list.length) {
                    return (
                    <div class="search-loading">{ this.searchSelect.remoteEmptyText }</div>
                    );
                }

                const renderList = (h) => {
                    const { displayKey } = this.searchSelect;
                    const { primaryKey } = this.searchSelect;

                    return (
                    <ul ref="list" class="search-menu">
                        { this.list.map((item, index) => {
                            const id = item[primaryKey];
                                
                            return (
                                <li class={{
                                    'search-menu-item': true,
                                    'is-group': !!item.isGroup,
                                    'is-disabled': item.disabled,
                                    active: this.activeIndex === index,
                                }}>
                                    <div
                                        class="item-name"
                                        onClick={e => this.handleClick(item, index, id)}>
                                        { item[displayKey] }
                                    </div>
                                    { this.isMultiable && !!this.checkeMap[item.id]
                                        ? <i class="bk-icon icon-check-1 item-icon" />
                                        : '' }
                                </li>
                            );
                        }) }
                    </ul>
                    );
                };

                const renderFooter = (h) => {
                    // 多选的时候显示底部操作按钮
                    if (!this.isMultiable) {
                        return '';
                    }

                    const submitBtnClasses = {
                        'footer-btn': true,
                        disabled: Object.keys(this.checkeMap).length < 1,
                    };
                    return (
                    <div class="search-list-footer">
                        <div class={submitBtnClasses} onClick={this.handleSubmit}>
                            {locale.t('bk.searchSelect.ok')}
                        </div>
                        <div class="footer-btn" onClick={this.handleCancel}>
                            {locale.t('bk.searchSelect.cancel')}
                        </div>
                    </div>
                    );
                };
                return (
                <div>
                    { renderList(h) }
                    { renderFooter(h) }
                </div>
                );
            },
        },
        
        render (h) {
            if (!this.needRender) {
                return null;
            }
            return (
            <div class="jb-bk-search-list" tabIndex="-1" role="search-value">
                { this.renderContent(h) }
            </div>
            );
        },
    };
</script>
