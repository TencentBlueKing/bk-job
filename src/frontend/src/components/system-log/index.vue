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
    <jb-dialog
        class="system-log-dialog"
        close-icon
        esc-close
        mask-close
        :show-footer="false"
        :value="value"
        :width="1105"
        @cancel="handleClose">
        <div
            ref="log"
            v-bkloading="{ isLoading }"
            class="system-log-layout">
            <div class="layout-left">
                <scroll-faker class="version-wraper">
                    <div
                        v-for="(log, index) in list"
                        :key="log.version"
                        class="log-tab"
                        :class="{ active: index === activeIndex }"
                        @click="handleTabChange(index)">
                        <div class="title">
                            {{ log.version }}
                        </div>
                        <div class="date">
                            {{ log.time }}
                        </div>
                        <div
                            v-if="index === 0"
                            class="new-flag">
                            {{ $t('当前版本') }}
                        </div>
                    </div>
                </scroll-faker>
            </div>
            <div class="layout-right">
                <scroll-faker class="content-wraper">
                    <div
                        class="markdowm-container"
                        v-html="logContent" />
                </scroll-faker>
            </div>
        </div>
    </jb-dialog>
</template>
<script>
    import Tippy from 'bk-magic-vue/lib/utils/tippy';
    import Cookie from 'js-cookie';
    import marked from 'marked';

    import WebGlobalService from '@service/web-global';

    import ScrollFaker from '@components/scroll-faker';

    export default {
        name: 'SystemVersionLog',
        components: {
            ScrollFaker,
        },
        props: {
            value: {
                type: Boolean,
                default: false,
            },
        },
        data () {
            return {
                isLoading: true,
                activeIndex: 0,
                list: [],
            };
        },
        computed: {
            logContent () {
                if (this.list.length < 1) {
                    return '';
                }
                return marked.parse(this.list[this.activeIndex].content);
            },
        },

        created () {
            this.fetchData();
            // 对比版本号，每次版本更新自动显示版本日志
            this.isDefaultShow = false;
            const currentVersion = process.env.JOB_VERSION;
            this.isDefaultShow = !Cookie.get('job_supermen') || Cookie.get('job_supermen') !== currentVersion;
            if (this.isDefaultShow) {
                this.$emit('input', true);
                this.$emit('change', true);
            }
        },
        methods: {
            /**
             * @desc 或版本日志数据
             */
            fetchData () {
                this.isLoading = true;
                const requestHandler = this.$i18n.locale === 'en-US'
                    ? WebGlobalService.fetchVersionENLog
                    : WebGlobalService.fetchVersionLog;
                requestHandler()
                    .then((data) => {
                        this.list = data;
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 日志收起时显示 tips
             */
            showTips () {
                if (!this.popperInstance) {
                    this.popperInstance = Tippy(document.querySelector('#siteHelp'), {
                        arrow: true,
                        allowHTML: true,
                        placement: 'bottom-end',
                        trigger: 'manual',
                        theme: 'light',
                        hideOnClick: false,
                        animateFill: false,
                        animation: 'slide-toggle',
                        lazy: false,
                        ignoreAttributes: true,
                        boundary: 'window',
                        distance: 30,
                        zIndex: window.__bk_zIndex_manager.nextZIndex(), // eslint-disable-line no-underscore-dangle
                    });
                }
                this.popperInstance.setContent(`
                    <div style="width: 220px; font-size: 12px; line-height: 20px; color: #63656E;">
                        <div style="color: #979BA5">JOB 小贴士：</div>
                        <div> 想要再次查阅「版本日志」也可以从此处进入喔～</div>
                    </div>
                `);
                this.popperInstance.show();
            },
            /**
             * @desc 关闭日志收起 tips
             */
            hideTips () {
                this.popperInstance && this.popperInstance.hide();
            },
            /**
             * @desc 切换版本日志内容
             * @param { Number } index 日志索引
             */
            handleTabChange (index) {
                this.activeIndex = index;
            },
            /**
             * @desc 关闭日志弹框
             *
             * 写入cookie版本号标记，每次发版自动弹出
             * 关闭时有收起动画，显示tips，动画持续事件400ms
             */
            handleClose () {
                this.$emit('input', false);
                this.$emit('change', false);
                Cookie.set('job_supermen', process.env.JOB_VERSION, { expires: 3600 });

                const animateTimes = 400;

                const $sourceEle = this.$refs.log.cloneNode(true);
                const {
                    top: sourceTop,
                    left: sourceLeft,
                    width: sourceWidth,
                    height: sourceHeight,
                } = this.$refs.log.getBoundingClientRect();
                $sourceEle.classList.add('hide');
                const styles = $sourceEle.style;
                styles.position = 'fixed';
                styles.top = `${sourceTop}px`;
                styles.left = `${sourceLeft}px`;
                styles.width = `${sourceWidth}px`;
                styles.height = `${sourceHeight}px`;
                styles.zIndex = window.__bk_zIndex_manager.nextZIndex(); // eslint-disable-line no-underscore-dangle
                document.body.appendChild($sourceEle);
                setTimeout(() => {
                    const $targetEle = document.querySelector('#siteHelp');
                    const {
                        top: targetTop,
                        left: targetLeft,
                        width: targetWidth,
                        height: targetHeight,
                    } = $targetEle.getBoundingClientRect();
                    const translateX = targetLeft + targetWidth / 2 - (sourceLeft + sourceWidth / 2);
                    const translateY = -(sourceTop + sourceHeight / 2 - (targetTop + targetHeight / 2));
                    styles.transform = `translate(${translateX}px, ${translateY}px) scale(0)`;
                    setTimeout(() => {
                        document.body.removeChild($sourceEle);
                        if (this.isDefaultShow) {
                            this.showTips();
                            this.isDefaultShow = false;
                            setTimeout(() => {
                                this.hideTips();
                            }, 3000);
                        }
                    }, animateTimes);
                });
            },
        },
    };
</script>
<style lang='postcss'>
    .system-log-dialog {
        .bk-dialog-tool,
        .bk-dialog-header {
            display: none;
        }

        .bk-dialog-body {
            padding: 0;
        }
    }

    .system-log-layout {
        position: relative;
        display: flex;
        height: 600px;
        background: #fff;

        &.hide {
            overflow: hidden;
            box-shadow: 0 1px 2px 0 rgb(99 101 110 / 100%);
            transition: 0.4s cubic-bezier(0.74, 0.01, 0.2, 1);
            transform-origin: center;
        }

        .layout-left {
            flex: 0 0 180px;
            position: relative;
            padding: 40px 0;
            background: #fafbfd;

            &::after {
                position: absolute;
                top: 0;
                right: 0;
                width: 1px;
                height: 100%;
                background: #dcdee5;
                content: "";
            }
        }

        .layout-right {
            flex: 1;
            padding: 45px;
        }

        .version-wraper {
            max-height: 520px;
        }

        .content-wraper {
            max-height: 510px;
        }

        .log-tab {
            position: relative;
            display: flex;
            height: 54px;
            padding-left: 30px;
            cursor: pointer;
            border-bottom: 1px solid #dcdee5;
            flex-direction: column;
            justify-content: center;

            &.active {
                background: #fff;

                &::before {
                    background: #3a84ff;
                }

                .title {
                    color: #313238;
                }
            }

            &:first-child {
                border-top: 1px solid #dcdee5;
            }

            &::before {
                position: absolute;
                top: -1px;
                left: 0;
                width: 4px;
                height: 100%;
                border: 1px solid transparent;
                content: "";
            }

            .title {
                font-size: 16px;
                font-weight: bold;
                line-height: 22px;
                color: #63656e;
            }

            .date {
                font-size: 12px;
                line-height: 17px;
                color: #63656e;
            }

            .new-flag {
                position: absolute;
                top: 10px;
                right: 20px;
                display: flex;
                width: 58px;
                height: 20px;
                font-size: 12px;
                color: #fff;
                background: #699df4;
                border-radius: 2px;
                align-items: center;
                justify-content: center;
            }
        }

        .markdowm-container {
            font-size: 14px;
            color: #313238;

            h1,
            h2,
            h3,
            h4,
            h5 {
                height: auto;
                margin: 10px 0;
                font:
                    normal 14px/1.5
                    "Helvetica Neue",
                    Helvetica,
                    Arial,
                    "Lantinghei SC",
                    "Hiragino Sans GB",
                    "Microsoft Yahei",
                    sans-serif;
                font-weight: bold;
                color: #34383e;
            }

            h1 {
                font-size: 30px;
            }

            h2 {
                font-size: 24px;
            }

            h3 {
                font-size: 18px;
            }

            h4 {
                font-size: 16px;
            }

            h5 {
                font-size: 14px;
            }

            em {
                font-style: italic;
            }

            div,
            p,
            font,
            span,
            li {
                line-height: 1.3;
            }

            p {
                margin: 0 0 1em;
            }

            table,
            table p {
                margin: 0;
            }

            ul,
            ol {
                padding: 0;
                margin: 0 0 1em 2em;
                text-indent: 0;
            }

            ul {
                padding: 0;
                margin: 10px 0 10px 15px;
                list-style-type: none;
            }

            ol {
                padding: 0;
                margin: 10px 0 10px 25px;
            }

            ol > li {
                line-height: 1.8;
                white-space: normal;
            }

            ul > li {
                padding-left: 15px !important;
                line-height: 1.8;
                white-space: normal;

                &::before {
                    display: inline-block;
                    width: 6px;
                    height: 6px;
                    margin-right: 9px;
                    margin-left: -15px;
                    background: #000;
                    border-radius: 50%;
                    content: "";
                }
            }

            li > ul {
                margin-bottom: 10px;
            }

            li ol {
                padding-left: 20px !important;
            }

            ul ul,
            ul ol,
            ol ol,
            ol ul {
                margin-bottom: 0;
                margin-left: 20px;
            }

            ul.list-type-1 > li {
                padding-left: 0 !important;
                margin-left: 15px !important;
                list-style: circle !important;
                background: none !important;
            }

            ul.list-type-2 > li {
                padding-left: 0 !important;
                margin-left: 15px !important;
                list-style: square !important;
                background: none !important;
            }

            ol.list-type-1 > li {
                list-style: lower-greek !important;
            }

            ol.list-type-2 > li {
                list-style: upper-roman !important;
            }

            ol.list-type-3 > li {
                list-style: cjk-ideographic !important;
            }

            pre,
            code {
                width: 95%;
                padding: 0 3px 2px;
                font-family: Monaco, Menlo, Consolas, "Courier New", monospace;
                font-size: 14px;
                color: #333;
                border-radius: 3px;
            }

            code {
                padding: 2px 4px;
                font-family: Consolas, monospace, tahoma, Arial;
                color: #d14;
                border: 1px solid #e1e1e8;
            }

            pre {
                display: block;
                padding: 9.5px;
                margin: 0 0 10px;
                font-family: Consolas, monospace, tahoma, Arial;
                font-size: 13px;
                word-break: break-all;
                word-wrap: break-word;
                white-space: pre-wrap;
                background-color: #f6f6f6;
                border: 1px solid #ddd;
                border: 1px solid rgb(0 0 0 / 15%);
                border-radius: 2px;
            }

            pre code {
                padding: 0;
                white-space: pre-wrap;
                border: 0;
            }

            blockquote {
                padding: 0 0 0 14px;
                margin: 0 0 20px;
                border-left: 5px solid #dfdfdf;
            }

            blockquote p {
                margin-bottom: 0;
                font-size: 14px;
                font-weight: 300;
                line-height: 25px;
            }

            blockquote small {
                display: block;
                line-height: 20px;
                color: #999;
            }

            blockquote small::before {
                content: "\2014 \00A0";
            }

            blockquote::before,
            blockquote::after {
                content: "";
            }
        }

        .log-close {
            position: absolute;
            top: 4px;
            right: 4px;
            width: 26px;
            height: 26px;
            font-size: 22px;
            line-height: 26px;
            color: #979ba5;
            text-align: center;
            cursor: pointer;
            border-radius: 50%;
            transition: all 0.15s;

            &:hover {
                background-color: #f0f1f5;
            }
        }
    }
</style>
