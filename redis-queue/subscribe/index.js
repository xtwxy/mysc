var count = 10000,
    connections = 1,
    password = "foobared",
    redis = require("redis"),
    client = redis.createClient({
        host: '192.168.0.184',
        port: 6379,
    });

client.on("error", function(err) {
   console.log("Error: " + err);
});

var subscribe = function(conn) {

    conn.on("error", function(err) {
        console.log("Error: " + err);
    });

    conn.on("connect", function() {
        conn.subscribe("chat");
    });

    conn.on("message", function(channel, message) {
    });
};

for(var i = 0; i < connections; ++i) {
    var conn = client.duplicate();
    subscribe(conn);
}
