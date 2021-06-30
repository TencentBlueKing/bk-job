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
    <tbody class="create-server-file" :key="reset">
        <tr v-if="hasSaved">
            <td colspan="4">
                <bk-button text @click="handleAddNew">
                    <Icon type="plus" />
                    {{ $t('添加一行') }}
                </bk-button>
            </td>
            <td />
        </tr>
        <tr v-else>
            <td>
                <edit-file-path
                    :value="serverFile.fileLocation"
                    mode="input"
                    @on-change="handleFileChange" />
            </td>
            <template v-if="serverFile.isHostEmpty">
                <td colspan="2">
                    <div class="server-add-btn">
                        <bk-select
                            ref="serverTypeSelect"
                            class="server-type-select"
                            :value="sourceFileType"
                            :popover-width="$i18n.locale === 'en-US' ? 130 : 85"
                            ext-popover-cls="server-file-popover-class"
                            :clearable="false"
                            style="width: 78px;"
                            @change="handleSourceFileTypeChange">
                            <bk-option id="globalVar" :name="$t('全局变量')" />
                            <bk-option id="manualAddition" :name="$t('手动添加')" />
                        </bk-select>
                        <div class="line" />
                        <template v-if="sourceFileType === 'globalVar'">
                            <bk-select
                                :placeholder="$t('请选择主机列表变量')"
                                class="server-add-variable"
                                :value="serverFile.host.variable"
                                :clearable="false"
                                @change="handleVariableChange"
                                searchable>
                                <bk-option v-for="(option, index) in variable"
                                    :key="index"
                                    :id="option.name"
                                    :name="option.name" />
                            </bk-select>
                        </template>
                        <template v-else>
                            <div class="server-add-host" @click="handleShowChooseIp">
                                <Icon type="plus" class="add-flag" />
                                {{ $t('添加服务器') }}
                            </div>
                        </template>
                    </div>
                </td>
            </template>
            <template v-else>
                <td>
                    <div class="file-edit-server" @click="handleShowChooseIp">
                        <p v-html="serverFile.serverDesc" />
                    </div>
                </td>
                <td>--</td>
            </template>
            <td>
                <account-select
                    class="account-add-btn"
                    :value="serverFile.account"
                    type="system"
                    @change="handleAccountChange" />
            </td>
            <td>
                <bk-button
                    text
                    @click="handlerSave"
                    :disabled="serverFile.isDisableSave">
                    {{ $t('保存') }}
                </bk-button>
                <bk-button text @click="handlerCancel">{{ $t('取消') }}</bk-button>
            </td>
        </tr>
        <choose-ip ref="chooseIp" v-model="isShowChooseIp" @on-change="handleHostChange" />
    </tbody>
</template>
<script>
    import _ from 'lodash';
    import {
        mapMutations,
    } from 'vuex';
    import SourceFileVO from '@domain/variable-object/source-file';
    import {
        findParent,
    } from '@utils/vdom';
    import ChooseIp from '@components/choose-ip';
    import AccountSelect from '@components/account-select';
    import EditFilePath from '../../components/edit-file-path';

    const generatorDefault = () => new SourceFileVO({
        fileLocation: [],
        fileType: SourceFileVO.typeServer,
        account: '',
    });

    export default {
        name: '',
        components: {
            ChooseIp,
            AccountSelect,
            EditFilePath,
        },
        props: {
            data: {
                type: Array,
                required: true,
            },
            account: {
                type: Array,
                required: true,
            },
            variable: {
                type: Array,
                required: true,
            },
        },
        data () {
            return {
                isShowChooseIp: false,
                hasSaved: this.data.length > 0,
                sourceFileType: 'globalVar',
                serverFile: new SourceFileVO(generatorDefault()),
                reset: 0,
            };
        },
        methods: {
            ...mapMutations('distroFile', [
                'editNewSourceFile',
            ]),
            /**
             * @desc 文件路径更新
             * @param {Array} fileLocation 文件路径
             */
            handleFileChange (fileLocation) {
                this.serverFile.fileLocation = fileLocation;
                window.changeAlert = true;
                this.editNewSourceFile(true);
            },
            /**
             * @desc 服务器类型更新
             * @param {String} type 服务器类型
             */
            handleSourceFileTypeChange (type) {
                this.sourceFileType = type;
                const formItem = findParent(this, 'jb-form-item');
                if (formItem) {
                    setTimeout(() => {
                        formItem.clearValidator();
                    });
                }
            },
            /**
             * @desc 服务器类型为全局变量时更新选择的全局变量
             * @param {String} variable 全局变量名
             */
            handleVariableChange (variable) {
                if (!variable) {
                    return;
                }
                this.serverFile.host.variable = variable;
                window.changeAlert = true;
                this.editNewSourceFile(true);
                const formItem = findParent(this, 'jb-form-item');
                if (formItem) {
                    setTimeout(() => {
                        formItem.clearValidator();
                    });
                }
            },
            /**
             * @desc 服务器类型为主机时更新显示ip选择器弹层
             */
            handleShowChooseIp () {
                this.isShowChooseIp = true;
            },
            /**
             * @desc 服务器类型为主机时主机值更新
             * @param {Object} hostNodeInfo 主机值
             */
            handleHostChange (hostNodeInfo) {
                window.changeAlert = true;
                this.serverFile.host.hostNodeInfo = hostNodeInfo;
                this.editNewSourceFile(true);
            },
            /**
             * @desc 服务器账号更新
             * @param {Number} accountId 主机值
             */
            handleAccountChange (accountId) {
                if (accountId === '') {
                    return;
                }
                const { id } = _.find(this.account, item => item.id === accountId);
                this.serverFile.account = id;
                const formItem = findParent(this, 'jb-form-item');
                if (formItem) {
                    setTimeout(() => {
                        formItem.clearValidator();
                    });
                }
            },
            /**
             * @desc 添加一个服务器文件
             */
            handleAddNew () {
                this.hasSaved = false;
            },
            /**
             * @desc 保存添加的服务器文件
             */
            handlerSave () {
                this.$emit('on-change', this.serverFile);
                this.handlerCancel();
            },
            /**
             * @desc 取消添加的服务器文件
             */
            handlerCancel () {
                this.$emit('on-cancel');
                this.serverFile = generatorDefault();
                this.sourceFileType = 'globalVar';
                this.$refs.chooseIp.reset();
                this.reset += 1;
                this.hasSaved = true;
                setTimeout(() => {
                    this.editNewSourceFile(false);
                });
            },
        },
    };
</script>
