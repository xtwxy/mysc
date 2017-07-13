var count = 10000,
    connections = 300,
    password = "foobared",
    redis = require("redis"),
    client = redis.createClient({
        host: '192.168.0.78',
        port: 6379,
    });

client.on("error", function(err) {
   console.log("Error: " + err);
});

var subscribe = function(conn) {

    conn.on("connect", function() {
        client.subscribe("chat");
    });

    conn.on("message", function(channel, message) {
    });
};

for(var i = 0; i < connections; ++i) {
    var conn = client.duplicate();
    subscribe(conn);
}
