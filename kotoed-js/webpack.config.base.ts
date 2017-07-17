import * as webpack from 'webpack';
import * as path from 'path';
import * as ExtractTextPlugin from "extract-text-webpack-plugin"
declare const __dirname;

const src_main = path.resolve(__dirname, "src/main/");
// Maybe we should put it to webroot/static in kotoed-server's pom.xml
const dst_path = path.resolve(__dirname, "target/js/webroot/static/");

const config: webpack.Configuration = {
    context: src_main,
    entry: {
        hello: "./ts/hello.ts",
        code: "./ts/code/index.tsx"
    },
    output: {
        path: dst_path,
        filename: 'js/[name].bundle.js'
    },
    resolve: {
        extensions: [".webpack.js", ".web.js", ".ts", ".tsx", ".js", ".jsx", ".css", ".less"],
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
                test: /\.jsx?$/,
                exclude: path.resolve(__dirname, "node_modules/"),
                loader: "babel-loader"
            },
            {
                test: /\.css$/,
                use: ExtractTextPlugin.extract({
                    fallback: 'style-loader',
                    use: [
                        {
                            loader: 'css-loader',
                            options: {
                                sourceMap: true
                            }
                        }
                    ],
                    allChunks: true
                })
            },
            {
                test: /\.less$/,
                use: ExtractTextPlugin.extract({
                    //resolve-url-loader may be chained before sass-loader if necessary
                    fallback: 'style-loader',
                    use: [
                        {
                            loader: 'css-loader',
                            options: {
                                // importLoaders: 2,
                                sourceMap: true
                            }
                        },
                        'less-loader?sourceMap'
                    ],
                    allChunks: true
                })
            },

            {
                test: /\.(woff2?|ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/,
                issuer: /(\.less|\.css)$/,
                loader: "file-loader",
                options: {
                    name: "fonts/[name].[ext]",
                    publicPath: '../'  // CSS are put into css/ folder by ExtractTextPlugin
                }
            }

        ]
    },
    plugins: [
        new webpack.ProvidePlugin({  // TODO this is shit
            jQuery: 'jquery',
            $: 'jquery',
            jquery: 'jquery'
        }),

        new webpack.optimize.CommonsChunkPlugin({
            name: "vendor",
            minChunks: function (module) {
                // this assumes your vendor imports exist in the node_modules directory
                return (module.context && module.context.indexOf("node_modules") !== -1) ||
                    // TODO maybe there is a better way of detecting kotoed-bootstrap
                    (module.resource && module.resource.indexOf("kotoed-bootstrap") !== -1);
            }
        }),
        new ExtractTextPlugin({
            filename:'css/[name].css',
            allChunks: true
        }),
    ]
};

export default config;