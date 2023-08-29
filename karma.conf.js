module.exports = (config) => {
    config.set({
        browsers: ["ChromeHeadless"],
        basePath: "resources/public/",
        files: ["out/test.js"],
        customContextFile: "karma_context.html",
        frameworks: ["cljs-test"],
        plugins: ["karma-cljs-test", "karma-chrome-launcher"],
        colors: true,
        logLevel: config.LOG_INFO,
        client: {
            args: ["shadow.test.karma.init"],
            singleRun: true
        }
    })
}
