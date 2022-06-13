const path = require('path')
const webpack = require('webpack')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const TerserPlugin = require('terser-webpack-plugin')
const { GitRevisionPlugin } = require('git-revision-webpack-plugin')
//const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin
const gitRevisionPlugin = new GitRevisionPlugin()
const CompressionPlugin = require('compression-webpack-plugin')

const d = new Date(gitRevisionPlugin.lastcommitdatetime())
const dateInfo =
  d.getFullYear() +
  '-' +
  ('0' + (d.getMonth() + 1)).slice(-2) +
  '-' +
  ('0' + d.getDate()).slice(-2)
const versionInfo = gitRevisionPlugin.version()

module.exports = {
  entry: './src/index.tsx',
  output: {
    path: path.join(__dirname, 'build'),
    filename: 'index.bundle.[chunkhash:4].js',
    publicPath: '/',
    clean: true
  },
  devtool: 'source-map',
  optimization: {
    removeAvailableModules: true,
    minimize: true,
    minimizer: [
      new TerserPlugin({
        terserOptions: {
          format: {
            comments: false
          }
        },
        extractComments: false,
        parallel: true
      })
    ],
    splitChunks: {
      chunks: 'all'
    }
  },
  mode: process.env.NODE_ENV || 'production',
  resolve: {
    extensions: ['.tsx', '.ts', '.js']
  },
  devServer: {
    static: {
      directory: path.join(__dirname, 'src')
    },
    historyApiFallback: true
  },
  module: {
    rules: [
      {
        test: /\.(js|jsx)$/,
        exclude: /node_modules/,
        use: ['babel-loader']
      },
      {
        test: /\.m?js/,
        resolve: {
          fullySpecified: false
        }
      },
      {
        test: /\.(ts|tsx)$/,
        exclude: /node_modules/,
        use: ['ts-loader']
      },
      {
        test: /\.s[ac]ss$/i,
        use: ['style-loader', 'css-loader', 'sass-loader']
      },
      {
        test: /\.css$/i,
        use: ['style-loader', 'css-loader']
      },
      {
        test: /\.(woff|woff2|ttf|otf|eot)$/,
        type: 'asset/resource',
        generator: {
          filename: 'fonts/[name][ext]'
        }
      },
      {
        test: /\.(jpe?g|png|gif|svg|ico)$/,
        type: 'asset/resource',
        generator: {
          filename: 'images/[name][ext]'
        }
      }
    ]
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: path.join(__dirname, 'src', 'index.html'),
      favicon: path.join(__dirname, 'src', 'images/favicon.ico')
    }),
    new webpack.DefinePlugin({
      'process.env.REACT_APP': JSON.stringify(process.env.REACT_APP),
      'process.env.NODE_DEBUG': JSON.stringify(process.env.NODE_DEBUG)
    }),
    new webpack.ProvidePlugin({
      process: 'process/browser'
    }),
    new webpack.EnvironmentPlugin({
      BACKEND_API_URL:
        JSON.stringify(process.env.BACKEND_API_URL) ||
        'https://api.test.saedi.io',
      BUILD_INFO: `build: ${dateInfo}#${versionInfo}`
    }),
    //new BundleAnalyzerPlugin(),
    new CompressionPlugin({
      algorithm: 'gzip'
    })
  ]
}
