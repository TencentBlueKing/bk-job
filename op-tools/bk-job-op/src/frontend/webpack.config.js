const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const { VueLoaderPlugin } = require('vue-loader');

// 构建产物直接落到 Spring Boot 的静态资源目录，由 OP 自身托管
const OUTPUT_DIR = path.resolve(__dirname, '../backend/src/main/resources/static');

// 后端自用的静态提示页，由 Java 代码 forward，不是前端构建产物，清理时必须保留
const KEEP_FILES = /^(403\.html|503-login-unconfigured\.html)$/;

// dev 模式下接口转发到本地 OP
const DEV_PROXY_TARGET = process.env.OP_BACKEND || 'http://127.0.0.1:8080';

module.exports = (env, argv) => {
  const isProd = argv.mode === 'production';
  const cssLoaders = [isProd ? MiniCssExtractPlugin.loader : 'style-loader', 'css-loader'];

  return {
    mode: isProd ? 'production' : 'development',
    devtool: isProd ? false : 'eval-cheap-module-source-map',
    entry: {
      main: path.resolve(__dirname, 'src/main.js'),
    },
    output: {
      path: OUTPUT_DIR,
      publicPath: '/',
      filename: isProd ? 'js/[name].[contenthash:8].js' : 'js/[name].js',
      chunkFilename: isProd ? 'js/[name].[contenthash:8].js' : 'js/[name].js',
      assetModuleFilename: 'img/[name].[contenthash:8][ext]',
      clean: {
        keep: KEEP_FILES,
      },
    },
    resolve: {
      extensions: ['.js', '.vue', '.json'],
      alias: {
        '@': path.resolve(__dirname, 'src'),
      },
      // bk-magic-vue 的 dist 包引用了 node 的 path 模块，webpack5 不再自动 polyfill
      fallback: {
        path: require.resolve('path-browserify'),
      },
    },
    module: {
      rules: [
        {
          test: /\.vue$/,
          loader: 'vue-loader',
        },
        {
          test: /\.js$/,
          exclude: /node_modules/,
          loader: 'babel-loader',
        },
        {
          test: /\.css$/,
          use: cssLoaders,
        },
        {
          test: /\.(png|jpe?g|gif|svg)$/i,
          type: 'asset/resource',
        },
        {
          // 字体单独落到 fonts 目录，与后端拦截器的静态资源放行路径保持一致
          test: /\.(woff2?|eot|ttf)$/i,
          type: 'asset/resource',
          generator: {
            filename: 'fonts/[name].[contenthash:8][ext]',
          },
        },
      ],
    },
    plugins: [
      new VueLoaderPlugin(),
      new HtmlWebpackPlugin({
        template: path.resolve(__dirname, 'index.html'),
        filename: 'index.html',
        inject: 'body',
      }),
      ...(isProd
        ? [new MiniCssExtractPlugin({ filename: 'css/[name].[contenthash:8].css' })]
        : []),
    ],
    optimization: {
      splitChunks: {
        cacheGroups: {
          vendors: {
            test: /[\\/]node_modules[\\/]/,
            name: 'vendors',
            chunks: 'all',
          },
        },
      },
    },
    performance: {
      hints: false,
    },
    devServer: {
      port: 8081,
      historyApiFallback: true,
      proxy: [
        {
          context: ['/issueStatus', '/web'],
          target: DEV_PROXY_TARGET,
          changeOrigin: true,
        },
      ],
    },
  };
};
