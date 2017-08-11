var connections = 100,
    password = "foobared",
    redis = require("redis"),
    client = redis.createClient({
        host: '192.168.0.78',
        port: 6379,
    });

client.on("error", function(err) {
    console.log("Error: " + err);
});

var publish = function(conn) {
    conn.publish("chat", `Hello, World!`, function(err, msg) {
        publish(conn);
    });
};

for(var i = 0; i < connections; ++i) {
    var conn = client.duplicate();

    publish(conn);
}