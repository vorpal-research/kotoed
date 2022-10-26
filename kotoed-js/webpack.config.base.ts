import * as webpack from 'webpack';
import * as path from 'path';
import * as ExtractTextPlugin from "extract-text-webpack-plugin"

declare const __dirname: string;

const src = path.resolve(__dirname, "src");
const srcMain = path.resolve(src, "main");
// Maybe we should put it to webroot/static in kotoed-server's pom.xml
const dstPath = path.resolve(__dirname, "target/js/webroot/static/");

const alwaysPutInVendorBundle: Array<RegExp> = [
    /.*kotoed-bootstrap.*/
];

const neverPutInVendorBundle: Array<RegExp> = [
    /.*@blueprintjs.*/ // Sets global styles without classes
];

function kotoedEntry(root: string, notifications: boolean = true): string[] {
    function* gen() {
        yield "./less/global.less";
        yield "babel-polyfill";
        if (notifications) {
            yield "./ts/notifications/notificationMenu.tsx";
            yield "./ts/notifications/popupNotifications.tsx";
        }

        yield root;
    }
    return [...gen()]
}

const config: webpack.Configuration = {
    context: srcMain,

    entry: {
        profile: kotoedEntry("./ts/profile/index.tsx"),
        profileEdit: kotoedEntry("./ts/profile/edit.tsx"),
        login: kotoedEntry("./ts/login/index.tsx", false),
        resetPassword: kotoedEntry("./ts/login/resetPasswordForm.tsx", false),
        restorePassword: kotoedEntry("./ts/login/restorePasswordForm.tsx", false),
        code: kotoedEntry("./ts/code/index.tsx"),
        submissionResults: kotoedEntry("./ts/views/submissionResults.tsx"),
        submissionDetails: kotoedEntry("./ts/submissionDetails/index.tsx"),
        commentSearch: kotoedEntry("./ts/views/commentSearch.tsx"),
        commentTemplateSearch: kotoedEntry("./ts/views/commentTemplateSearch.tsx"),
        buildTemplateEdit: kotoedEntry("./ts/buildTemplates/edit.tsx"),
        buildSystemSummary: kotoedEntry("./ts/buildSystem/summary.tsx"),
        buildSystemStatus: kotoedEntry("./ts/buildSystem/status.tsx"),
        courseEdit: kotoedEntry("./ts/courses/edit.tsx"),
        denizenSearch: kotoedEntry("./ts/denizen/list.tsx"),
        courseList: kotoedEntry("./ts/courses/list.tsx"),
        projectList: kotoedEntry("./ts/projects/list.tsx"),
        submissionList: kotoedEntry("./ts/submissions/list.tsx"),
        courseReport: kotoedEntry("./ts/views/courseReport.tsx"),
        images: "./ts/images.ts",
        serviceWorker: "./ts/notifications/serviceWorker.js"
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
            res: path.resolve(src, "resources")

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
                        }
                    }
                ]
            },
            {
                test: /\.jsx?$/,
                exclude: path.resolve(__dirname, "node_modules/"),
                use: [
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
                test: /serviceWorker\.js$/,
                exclude: path.resolve(__dirname, "node_modules/"),
                loader: "file-loader",
                options: {
                    name: "static/[name].[ext]",
                    publicPath: '/static/'
                }
            },
            {
                test: /\.(woff2?|ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/,
                issuer: /(\.less|\.css)$/,
                loader: "file-loader",
                options: {
                    name: "fonts/[name].[ext]",
                    publicPath: '/static/'  // CSS are put into css/ folder by ExtractTextPlugin
                }
            },
            {
                test: /\.(png|jpg)(\?v=\d+\.\d+\.\d+)?$/,
                loader: "file-loader",
                options: {
                    name: "img/[name].[ext]",
                    publicPath: '/static/'
                }
            },


        ]
    },
    plugins: [
        new webpack.ProvidePlugin({  // TODO this is shit but Bootstrap JS does not work without it
            jQuery: 'jquery',
            $: 'jquery',
            jquery: 'jquery',
            diff_match_patch: 'diff-match-patch',
            DIFF_DELETE: ['diff-match-patch', 'DIFF_DELETE'],
            DIFF_INSERT: ['diff-match-patch', 'DIFF_INSERT'],
            DIFF_EQUAL: ['diff-match-patch', 'DIFF_EQUAL']
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
