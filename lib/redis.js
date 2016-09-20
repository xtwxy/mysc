var redis = require("redis");

var client = redis.createClient({
    host: '192.168.0.88',
    port: 6379
});

client.on("connect", function() {
    client.auth("wincom");
});

module.exports = client;

