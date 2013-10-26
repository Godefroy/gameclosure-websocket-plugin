exports.init = function(common) {
    var exec = require("child_process").exec;
    exec("git submodule init && git submodule update", function(err, stdout, stderr) {
        if (err) {
            console.log(stderr);
        }
    });
};

exports.load = function(common) {};

exports.testapp = function(common, opts, next) {};