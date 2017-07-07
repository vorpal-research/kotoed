import * as webpack from 'webpack';
import * as path from 'path';

declare const __dirname;

const src_main = path.resolve(__dirname, "src/main/");
// Maybe we should put it to webroot/static in kotoed-server's pom.xml
const dst_path = path.resolve(__dirname, "target/js/webroot/static/");

const config: webpack.Configuration = {
    context: src_main,
    entry: {
        hello: "./ts/hello.ts",
        code: "./ts/code.ts"

    },
    output: {
        path: dst_path,
        filename: 'js/[name].bundle.js'
    },
    resolve: {
        extensions: [".webpack.js", ".web.js", ".ts", ".tsx", ".js", ".css", ".less"],
        alias: {
            css: path.resolve(src_main, "css"),
            ts: path.resolve(src_main, "ts"),
            js: path.resolve(src_main, "js"),
            less: path.resolve(src_main, "less"),

        }
    },
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                loader: "awesome-typescript-loader"
            },
            {
                test: /\.css$/,
                use: [
                    {
                        loader: 'style-loader',
                    },
                    {
                        loader: 'css-loader'
                    },
                ]
            },
            {
                test: /\.less$/,
                use: [
                    {
                        loader: 'style-loader',
                    },
                    {
                        loader: 'css-loader',
                    },
                    {
                        loader: 'less-loader'
                    },
                ]
            },

            {
                test: /\.(woff2?|ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/,
                loader: "file-loader?name=fonts/[name].[ext]"
            }

        ]
    },
    plugins: [
        new webpack.ProvidePlugin({  // TODO this is shit
            jQuery: 'jquery',
            $: 'jquery',
            jquery: 'jquery'
        })
    ],
    devtool: 'inline-source-map',
};

export default config;