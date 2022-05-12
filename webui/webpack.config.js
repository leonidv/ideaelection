const path = require("path");
const webpack = require("webpack");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const { GitRevisionPlugin } = require("git-revision-webpack-plugin");
//const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const gitRevisionPlugin = new GitRevisionPlugin();

const d = new Date(gitRevisionPlugin.lastcommitdatetime());
const dateInfo =
  d.getFullYear() +
  "-" +
  ("0" + (d.getMonth() + 1)).slice(-2) +
  "-" +
  ("0" + d.getDate()).slice(-2);
const versionInfo = gitRevisionPlugin.version();

module.exports = {
  entry: "./src/index.tsx",
  output: {
    path: path.join(__dirname, "build"),
    filename: "index.bundle.[chunkhash:4].js",
    publicPath: "/",
  },
  devtool: "source-map",
  mode: process.env.NODE_ENV || "production",
  resolve: {
    extensions: [".tsx", ".ts", ".js"],
  },
  devServer: {
    contentBase: path.join(__dirname, "src"),
    historyApiFallback: true,
  },
  module: {
    rules: [
      {
        test: /\.(js|jsx)$/,
        exclude: /node_modules/,
        use: ["babel-loader"],
      },
      {
        test: /\.m?js/,
        resolve: {
          fullySpecified: false,
        },
      },
      {
        test: /\.(ts|tsx)$/,
        exclude: /node_modules/,
        use: ["ts-loader"],
      },
      {
        test: /\.s[ac]ss$/i,
        use: ["style-loader", "css-loader", "sass-loader"],
      },
      {
        test: /\.css$/i,
        use: ["style-loader", "css-loader"],
      },
      {
        test: /\.(jpg|jpeg|png|gif|mp3|svg)$/,
        use: ["file-loader"],
      },
      {
        test: /\.(woff(2)?|ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/,
        use: [
          {
            loader: "file-loader",
            options: {
              name: "[name].[ext]",
              outputPath: "fonts/",
            },
          },
        ],
      },
    ],
  },
  externals: {
    config: JSON.stringify({
      BACKEND_API_URL: "https://app.saedi.io/api",
    }),
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: path.join(__dirname, "src", "index.html"),
    }),
    new webpack.DefinePlugin({
      "process.env.REACT_APP": JSON.stringify(process.env.REACT_APP),
      "process.env.NODE_DEBUG": JSON.stringify(process.env.NODE_DEBUG),
    }),
    new webpack.ProvidePlugin({
      process: "process/browser",
    }),
    new webpack.EnvironmentPlugin({
      BACKEND_API_URL: "https://api.test.saedi.io",
      BUILD_INFO: `build: ${dateInfo}#${versionInfo}`,
    }),
    // ,new BundleAnalyzerPlugin()
  ],
};
