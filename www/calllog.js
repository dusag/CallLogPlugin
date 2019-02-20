var exec = require('cordova/exec');

exports.onLogCall = function(success, error) {
    exec(success, error, "CallLogPlugin", "onLogCall", []);
};

exports.enableLogging = function(success, error) {
    exec(success, error, "CallLogPlugin", "enableLogging", []);
};

exports.disableLogging = function(success, error) {
    exec(success, error, "CallLogPlugin", "disableLogging", []);
};