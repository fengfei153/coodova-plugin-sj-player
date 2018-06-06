var exec = require('cordova/exec');
function SJPlayerPlugin(){};

SJPlayerPlugin.prototype.playVideo = function(succCall, errorCall, args){
	exec(succCall, errorCall, 'SJPlayerPlugin', 'playVideo', [args]);
}
SJPlayerPlugin.prototype.destroy = function(succCall, errorCall){
	exec(succCall, errorCall, 'SJPlayerPlugin', 'destroy', []);
}

if(!window.plugins) {
	window.plugins = {};
}
module.exports = new SJPlayerPlugin();
