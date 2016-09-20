var redis = require("./redis");
var app = require("./app");

module.exports = app;

var sloganHandler = function(request, response) {
    redis.get("slogan", function(err, msg) {
        if(err) {
            response.writeHead(501, {"Content-Type": "text/plain"});
            response.end(JSON.stringify(err));
        } else {
            response.writeHead(200, {"Content-Type": "application/json"});
            response.end(JSON.stringify(msg));
        }
    });
};

app.get('/slogan', sloganHandler);


