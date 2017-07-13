var count = 1000000,
    connections = 100,
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
    if(count > 0) {
        conn.publish("chat", "Hello, World! - " + count--, function(err, msg) {
            publish(conn);
        });
    } else {
        conn.quit();
        connections--;
        if(connections == 0) {
            console.log("publish complete, quitting...");
            client.quit();
        }
    }
};

for(var i = 0; i < connections; ++i) {
    var conn = client.duplicate();

    publish(conn);
}
