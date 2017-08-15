import * as webpack from 'webpack';
import * as path from 'path';
import * as ExtractTextPlugin from "extract-text-webpack-plugin"

declare const __dirname: string;

const srcMain = path.resolve(__dirname, "src/main/");
// Maybe we should put it to webroot/static in kotoed-server's pom.xml
const dstPath = path.resolve(__dirname, "target/js/webroot/static/");

const alwaysPutInVendorBundle: Array<RegExp> = [
    /.*kotoed-bootstrap.*/
];

const neverPutInVendorBundle: Array<RegExp> = [
    /.*@blueprintjs.*/ // Sets global styles without classes
];

const config: webpack.Configuration = {
    context: srcMain,
    entry: {
        hello: ["babel-polyfill", "./ts/hello.ts"],
        login: ["babel-polyfill", "./ts/login/index.tsx"],
        code: ["babel-polyfill", "./ts/code/index.tsx"],
        submissionResults: ["babel-polyfill", "./ts/views/submissionResults.tsx"],
        commentSearch: ["babel-polyfill", "./ts/views/commentSearch.tsx"]
    },
    output: {
        path: dstPath,
        filename: 'js/[name].bundle.js'
    },
    resolve: {
        extensions: [".webpack.js", ".web.js", ".ts", ".tsx", ".js", ".jsx", ".css", ".less"],
        alias: {
            css: path.resolve(srcMain, "css"),
            ts: path.resolve(srcMain, "ts"),
            js: path.resolve(srcMain, "js"),
            less: path.resolve(srcMain, "less"),

        }
    },
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                exclude: path.resolve(__dirname, "node_modules/"),
                use: [
                    "babel-loader",
                    {
                        loader: "awesome-typescript-loader",
                        options: {
                            useBabel: true,
                            useCache: true,
                            cacheDirectory: "target/.cache/atsl"
                        }
                    }
                ]
            },
            {
                test: /\.jsx?$/,
                exclude: path.resolve(__dirname, "node_modules/"),
                use: [
                    {
                        loader: "cache-loader",
                        options: {
                            cacheDirectory: "target/.cache/jsx"
                        }
                    },
                    "babel-loader"
                ]
            },
            {
                test: /\.css$/,
                use:
                    ExtractTextPlugin.extract({
                        fallback: 'style-loader',
                        use: [
                            {
                                loader: "cache-loader",
                                options: {
                                    cacheDirectory: "target/.cache/css"
                                }
                            },
                            {
                                loader: 'css-loader',
                                options: {
                                    sourceMap: true
                                }
                            }
                        ]
                    })
            },
            {
                test: /\.less$/,
                use:
                    ExtractTextPlugin.extract({
                        //resolve-url-loader may be chained before sass-loader if necessary
                        fallback: 'style-loader',
                        use: [
                            {
                                loader: "cache-loader",
                                options: {
                                    cacheDirectory: "target/.cache/less"
                                }
                            },
                            {
                                loader: 'css-loader',
                                options: {
                                    // importLoaders: 2,
                                    sourceMap: true
                                }
                            },
                            'less-loader?sourceMap'
                        ]
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
        new webpack.ProvidePlugin({  // TODO this is shit but Bootstrap JS does not work without it
            jQuery: 'jquery',
            $: 'jquery',
            jquery: 'jquery'
        }),

        new webpack.optimize.CommonsChunkPlugin({
            name: "vendor",
            minChunks: function (module) {

                if (module.resource && alwaysPutInVendorBundle.some(re => re.test(module.resource)))
                    return true;


                if (module.resource && neverPutInVendorBundle.some(re => re.test(module.resource)))
                    return false;


                // this assumes your vendor imports exist in the node_modules directory
                return (module.context && module.context.indexOf("node_modules") !== -1);
            }
        }),
        new ExtractTextPlugin({
            filename: 'css/[name].css',
            allChunks: true
        }),
    ]
};

export default config;