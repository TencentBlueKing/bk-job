/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
*/

const fs = require('fs');
const path = require('path');
const webpack = require('webpack');
const { VueLoaderPlugin } = require('vue-loader');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const TerserPlugin = require('terser-webpack-plugin');
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const LodashWebpackPlugin = require('lodash-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ESLintPlugin = require('eslint-webpack-plugin');
const StylelintPlugin = require('stylelint-webpack-plugin');
// const PreloadWebpackPlugin = require('@vue/preload-webpack-plugin');
// const SpeedMeasurePlugin = require('speed-measure-webpack-plugin');
const figlet = require('figlet');
const marked = require('marked');
const renderer = new marked.Renderer();

const resolve = dir => path.join(__dirname, dir);
const genAssetPath = dir => path.posix.join('static', `${dir}/[name].[hash:7].[ext]`);

// const smp = new SpeedMeasurePlugin();

module.exports = function (env) {
    const isDevelopment = env.development;

    const appendThreadLoader = function () {
        if (isDevelopment) {
            return {
                loader: 'thread-loader',
                options: {
                    poolTimeout: Infinity,
                },
            };
        }
    };
    
    // webpack.config是函数在构建过程会重复执行，
    if (isDevelopment) {
        // 读取指定mode的.env文件
        const envModeConfigFile = path.resolve(__dirname, `.env.${env.mode}`);
        if (!fs.existsSync(envModeConfigFile)) {
            console.error(`\n\n\n**** ${env.mode}模式时本地需提供 .env.${env.mode} 配置文件，可查看 README.MD 或联系管理员****\n\n\n`);
            process.exit(1);
        }
        require('dotenv').config({
            path: envModeConfigFile,
        });
        // .env.local 会对指定mode的配置文件进行覆盖
        const localModeConfigFile = path.resolve(__dirname, '.env.local');
        if (fs.existsSync(localModeConfigFile)) {
            require('dotenv').config({
                path: localModeConfigFile,
            });
        }
    }
    return {
        mode: isDevelopment ? 'development' : 'production',
        devtool: isDevelopment ? 'eval-source-map' : 'hidden-source-map',
        cache: isDevelopment
            ? {
                type: 'filesystem',
                memoryCacheUnaffected: true,
            }
            : false,
        entry: {
            main: resolve('/src/main.js'),
        },
        output: {
            pathinfo: false,
            path: resolve('dist'),
            publicPath: '/',
            filename: isDevelopment ? 'js/[name].js' : 'js/[name].[chunkhash].js',
            chunkFilename: isDevelopment ? 'js/[name].js' : 'js/[name].[chunkhash].js',
            clean: true,
        },
        optimization: isDevelopment
            ? {
                moduleIds: 'named',
                removeAvailableModules: false,
                removeEmptyChunks: false,
                splitChunks: false,
            }
            : {
                minimizer: [
                    new TerserPlugin({}),
                    new CssMinimizerPlugin(),
                ],
                moduleIds: 'deterministic',
                runtimeChunk: {
                    name: 'runtime',
                },
                splitChunks: {
                    chunks: 'all',
                    minSize: 30000,
                    minChunks: 1,
                    maxAsyncRequests: 5,
                    maxInitialRequests: 3,
                    automaticNameDelimiter: '~',
                    name: false,
                    cacheGroups: {
                        bkMagic: {
                            chunks: 'all',
                            name: 'bk-magic-vue',
                            priority: 10,
                            reuseExistingChunk: true,
                            test: module => /bk-magic-vue/.test(module.context),
                        },
                        vendors: {
                            test: /[\\/]node_modules[\\/]/,
                            name: 'vendors',
                            priority: 9,
                        },
                        commom: {
                            chunks: 'all',
                            name: 'common',
                            priority: 8,
                            reuseExistingChunk: true,
                            test: module => /src\/components/.test(module.context),
                        },
                        twice: {
                            chunks: 'all',
                            name: 'twice',
                            priority: 7,
                            minChunks: 2,
                        },
                        default: {
                            chunks: 'async',
                            minChunks: 1,
                            priority: -20,
                            reuseExistingChunk: true,
                        },
                    },
                },
            },
        module: {
            noParse: [
                /\/node_modules\/jquery\/dist\/jquery\.min\.js$/,
                /\/node_modules\/echarts\/dist\/echarts\.min\.js$/,
            ],
            rules: [
                {
                    test: /\.vue$/,
                    use: [
                        appendThreadLoader(),
                        {
                            loader: 'vue-loader',
                            options: {
                                transformAssetUrls: {
                                    video: 'src',
                                    source: 'src',
                                    img: 'src',
                                    image: 'xlink:href',
                                },
                            },
                        },
                    ].filter(_ => _),
                },
                {
                    test: /\.js$/,
                    use: [
                        appendThreadLoader(),
                        {
                            loader: 'babel-loader',
                            options: {
                                include: [resolve('src')],
                                // 确保 JS 的转译应用到 node_modules 的 Vue 单文件组件
                                exclude: file => /node_modules/.test(file) && !/\.vue\.js/.test(file),
                            },
                        },
                    ].filter(_ => _),
                },
                {
                    test: /\.md$/,
                    use: [
                        {
                            loader: 'html-loader',
                        },
                        {
                            loader: 'markdown-loader',
                            options: {
                                renderer,
                                highlight (code, lang) {
                                    const hljs = require('highlight.js');
                                    const language = hljs.getLanguage(lang) ? lang : 'plaintext';
                                    return hljs.highlight(code, { language }).value;
                                },
                                headerIds: false,
                            },
                        },
                    ],
                },
                {
                    test: /\.(css|scss|postcss)$/,
                    use: [
                        'vue-style-loader',
                        isDevelopment
                            ? ''
                            : {
                                loader: MiniCssExtractPlugin.loader,
                                options: {
                                    esModule: false,
                                },
                            },
                        {
                            loader: 'css-loader',
                            options: {
                                importLoaders: 1,
                            },
                        },
                        {
                            loader: 'postcss-loader',
                        },
                    ].filter(_ => _),
                },
                {
                    test: /\.(png|jpe?g|gif|svg)(\?.*)?$/,
                    type: 'asset',
                    generator: {
                        filename: genAssetPath('images'),
                    },
                },
                {
                    test: /\.(mp4|webm|ogg|mp3|wav|flac|aac)(\?.*)?$/,
                    type: 'asset',
                    generator: {
                        filename: genAssetPath('media'),
                    },
                },
                {
                    test: /\.(woff2?|eot|ttf|otf)(\?.*)?$/,
                    type: 'asset',
                    generator: {
                        filename: genAssetPath('fonts'),
                    },
                },
            ],
        },
        resolve: {
            modules: [resolve('src'), resolve('node_modules')],
            extensions: ['.js', '.vue', '.json'],
            symlinks: false,
            fallback: {
                path: false,
            },
            alias: {
                vue$: 'vue/dist/vue.esm.js',
                '@': resolve('src'),
                ace: 'ace-builds/src-noconflict',
                lib: resolve('lib'),
                '@common': resolve('src/common'),
                '@components': resolve('src/components'),
                '@domain': resolve('src/domain'),
                '@router': resolve('src/router'),
                '@model': resolve('src/domain/model'),
                '@service': resolve('src/domain/service'),
                '@store': resolve('src/store'),
                '@utils': resolve('src/utils'),
                '@views': resolve('src/views'),
            },
        },
        plugins: [
            new webpack.DefinePlugin(isDevelopment
                ? {
                    'process.env': {
                        JOB_WELCOME: JSON.stringify(figlet.textSync('Welcome To Job\nlatest', {
                            horizontalLayout: 'full',
                        })),
                        JOB_VERSION: JSON.stringify('latest'),
                    },
                }
                : {
                    'process.env': {
                        JOB_WELCOME: JSON.stringify(figlet.textSync(`Welcome To Job\n${process.env.JOB_VERSION}`, {
                            horizontalLayout: 'full',
                        })),
                        JOB_VERSION: JSON.stringify(process.env.JOB_VERSION),
                    },
                }),
            new HtmlWebpackPlugin(isDevelopment
                ? {
                    filename: 'index.html',
                    template: 'index-dev.html',
                    inject: true,
                    templateParameters: {
                        AJAX_URL_PREFIX: process.env.AJAX_URL_PREFIX,
                    },
                }
                : {
                    filename: 'index.html',
                    template: 'index.html',
                    inject: true,
                    minify: {
                        removeComments: true,
                        collapseWhitespace: true,
                        removeAttributeQuotes: true,
                    },
                }),
            !isDevelopment && new MiniCssExtractPlugin({
                filename: 'static/css/[name].[contenthash].css',
                ignoreOrder: true,
            }),
            isDevelopment && new ESLintPlugin({
                extensions: ['js', 'vue'],
                lintDirtyModulesOnly: true,
                failOnError: false,
                threads: 2,
            }),
            isDevelopment && new StylelintPlugin({
                files: ['./**/*.vue', './**/*.css'],
                extensions: ['css', 'scss', 'sass', 'postcss'],
                lintDirtyModulesOnly: true,
                emitWarning: true,
            }),
            new webpack.ProgressPlugin(),
            new VueLoaderPlugin(),
            new LodashWebpackPlugin(),
            // moment 优化，只提取本地包
            new webpack.ContextReplacementPlugin(/moment\/locale$/, /zh-cn/),
            new CopyWebpackPlugin({
                patterns: [
                    {
                        from: resolve('static/images'),
                        to: resolve('dist/static/images'),
                        toType: 'dir',
                    },
                    {
                        from: resolve('static/login_success.html'),
                        to: resolve('dist/static/login_success.html'),
                    },
                ],
            }),
            
        ].filter(_ => _),
        devServer: {
            host: '0.0.0.0',
            port: 8081,
            allowedHosts: 'all',
            liveReload: false,
            client: {
                logging: 'warn',
                overlay: false,
            },
            historyApiFallback: true,
        },
    };
};
