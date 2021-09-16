<template>
    <div class="variable-use-guide">
        <div class="header">
            <div>使用指引</div>
            <div class="tab-container">
                <div
                    class="tab-item"
                    :class="{ active: tab === 'global' }"
                    @click="handleTabToggle('global')">
                    全局变量
                </div>
                <div
                    class="tab-item"
                    :class="{ active: tab === 'magic' }"
                    @click="handleTabToggle('magic')">
                    魔法变量
                </div>
            </div>
        </div>
        <scroll-faker style="height: calc(100% - 88px);">
            <div class="content">
                <div v-show="tab === 'global'" class="type-global-variable">
                    <h1 style="margin-top: 0;">说明</h1>
                    <p>即，在作业中用户自定义的全局通用变量</p>
                    <h1>用法</h1>
                    <p>使用 dollar 符 + 大括号: <code>${变量名}</code></p>
                    <h1>在哪里用？</h1>
                    <ul>
                        <li>
                            字符串
                            <ol>
                                <li>1. 脚本中直接引用，当前仅支持 <code>Shell</code></li>
                                <li>2. “文件分发”步骤的源 <code>文件路径</code> 和 <code>目标路径</code></li>
                                <li>3. “执行脚本”步骤的 <code>脚本参数</code></li>
                            </ol>
                        </li>
                        <li>
                            命名空间
                            <p>脚本中直接引用，当前仅支持 <code>Shell</code></p>
                        </li>
                        <li>
                            主机列表
                            <p>无法直接引用，需要配合 <code>魔法变量</code></p>
                        </li>
                        <li>
                            密文
                            <p>脚本中直接引用，当前仅支持 <code>Shell</code></p>
                        </li>
                        <li>
                            数组
                            <p>脚本中直接引用，当前仅支持 <code>Shell</code></p>
                        </li>
                    </ul>
                </div>
                <div v-show="tab === 'magic'" class="type-magic-variable">
                    <h1 style="margin-top: 0;">说明</h1>
                    <p>JOB 平台执行引擎提供的特有的变量能力</p>
                    <h1>用法</h1>
                    <p>脚本中使用，并且需要事先声明： <code v-text="'job_import {{变量}}'" /></p>
                    <p>声明后，同样是使用 dollar 符 + 大括号： <code>${变量名}</code></p>
                    <h1>在哪里用？</h1>
                    <p>当前仅支持在 <code>Shell</code> 脚本语言中使用</p>
                    <h1>变量列表</h1>
                    <ul>
                        <li>
                            <span>获取 <code>主机列表</code> 类型的全局变量值</span>
                            <pre><code v-text="'#job_import {{主机列表的全局变量名}}\necho ${主机列表的全局变量名}'" /></pre>
                            <p>输出结果（示例）：</p>
                            <pre><code v-text="'0:10.1.1.100, 1:20.2.2.200'" /></pre>
                            <p>输出格式为：<code>云区域 ID + 冒号 + 内网 IP</code></p>
                            <p>多个 IP 地址以 <code>逗号</code> 分隔</p>
                        </li>
                        <li>
                            <span>获取上一个步骤执行的主机列表</span>
                            <pre><code v-text="stepHostText" /></pre>
                        </li>
                        <li>
                            <span>获取其他主机的命名空间变量值</span>
                            <pre><code v-text="otherNamespaceText" /></pre>
                        </li>
                        <li>
                            <span>输出结果（示例）：</span>
                            <pre><code v-text="outputText" /></pre>
                        </li>
                    </ul>
                </div>
            </div>
        </scroll-faker>
        <div class="close-btn" @click="handleClose">
            <Icon type="close" />
        </div>
    </div>
</template>
<script>
    export default {
        name: '',
        data () {
            return {
                tab: 'global',
            };
        },
        created () {
            this.stepHostText = [
                '# job_import {{JOB_LAST_ALL}}',
                '# 获取上一个步骤的所有执行主机 IP 列表',
                '',
                '# job_import {{JOB_LAST_SUCCESS}}',
                '#获取上一个步骤执行成功的主机 IP 列表',
                '',
                '# job_import {{JOB_LAST_FAIL}}',
                '# 获取上一个步骤执行失败的主机 IP 列表',
            ].join('\n');
            this.otherNamespaceText = [
                '# job_import {{JOB_NAMESPACE_ALL}}',
                '# 获取所有命名空间变量的汇聚值',
                'echo ${JOB_NAMESPACE_ALL}',
                '',
                '# job_import {{JOB_NAMESPACE_命名空间变量名}',
                '# 获取某个命名空间变量的汇聚值',
                'echo ${JOB_NAMESPACE_命名空间变量名}',
            ].join('\n');
            this.outputText = [
                '### echo ${JOB_NAMESPACE_ALL} 的输出(假定有 ns_var1 和 ns_var2 两个命名空间类型全局变量)：',
                '{"ns_var1":{"0:10.10.10.1":"xxxx","0:10.10.10.2":"yyyy","0:10.10.10.3":"zzzz"},"ns_var2":{"0:20.20.20.1":"aaaa",',
                '"0:20.20.20.2":"bbbb","0:20.20.20.3":"cccc","0:20.20.20.4":"dddd"}}',
                '',
                '### echo ${JOB_NAMESPACE_命名空间变量名} 的输出：',
                '{"0:10.10.10.1":"xxxx","0:10.10.10.2":"yyyy","0:10.10.10.3":"zzzz"}',
            ].join('\n');
        },
        methods: {
            handleTabToggle (tab) {
                this.tab = tab;
            },
            handleClose () {
                this.$emit('on-close');
            },
        },
    };
</script>
<style lang="postcss">
    .variable-use-guide {
        position: relative;
        height: 100%;
        background: #fff;

        .header {
            padding-top: 16px;
            padding-left: 20px;
            font-size: 16px;
            color: #313238;
            background: #f0f1f5;
            border-bottom: 1px solid #dcdee5;

            .tab-container {
                display: flex;
                margin-top: 15px;

                .tab-item {
                    width: 84px;
                    height: 35px;
                    margin-right: 8px;
                    margin-bottom: -1px;
                    font-size: 13px;
                    line-height: 35px;
                    color: #63656e;
                    text-align: center;
                    cursor: pointer;
                    background: #dcdee5;
                    border: 1px solid #dcdee5;
                    border-bottom: none;
                    border-top-right-radius: 4px;
                    border-top-left-radius: 4px;
                    transition: all 0.1s;

                    &.active {
                        color: #313238;
                        background: #fff;
                    }
                }
            }
        }

        .content {
            padding: 15px 20px;
            overflow: hidden;
            line-height: 18px;
            color: #63656e;

            h1 {
                margin-top: 24px;
                font-size: 12px;
                font-weight: bold;
            }

            p {
                margin-top: 6px;
            }

            code {
                padding: 0 4px;
                font-size: 12px;
                color: #ea3636;
                background: #fffafa;
                border: 1px solid #ffecec;
                border-radius: 2px;
            }

            pre {
                code {
                    display: block;
                    width: 100%;
                    padding: 8px 12px;
                    margin-top: 10px;
                    overflow-x: auto;
                    line-height: 22px;
                    color: #6a9a7b;
                    text-align: left;
                    background: #f5f6fa;
                    border: none;
                }
            }

            ul {
                & > li {
                    position: relative;
                    padding-left: 11px;
                    color: #313238;

                    &::before {
                        position: absolute;
                        top: 6px;
                        left: 0;
                        width: 5px;
                        height: 5px;
                        margin-right: 6px;
                        vertical-align: middle;
                        background: #979ba5;
                        border-radius: 50%;
                        content: '';
                    }

                    &:nth-child(n+2) {
                        margin-top: 20px;
                    }
                }
            }

            ol {
                color: #63656e;
            }

            li {
                margin-top: 6px;
            }
        }

        .close-btn {
            position: absolute;
            top: 10px;
            right: 10px;
            width: 26px;
            height: 26px;
            font-size: 18px;
            line-height: 26px;
            color: #979ba5;
            text-align: center;
            cursor: pointer;
            border-radius: 50%;

            &:hover {
                background-color: #dcdee5;
            }
        }
    }
</style>
