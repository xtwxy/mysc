var redis = require("redis");

var client = redis.createClient({
    host: 'localhost',
    port: 6379
});

client.on("connect", function() {
    client.auth("foobared");
});

client.on("error", function(err) {
    console.log("Error: " + err);
});

module.exports = client;

