var count = 10000,
    connections = 100;
password = "foobared",
         redis = require("redis"),
         client = redis.createClient({
             host: '192.168.0.78',
             port: 6379,
             //password: password
         });

client.on("error", function(err) {
    console.log("Error: " + err);
});

for(var i = 0; i < connections; ++i) {
    var conn = client.duplicate();
    conn.on("connect", function() {
        //conn.auth(password);
        //console.log("Authenticated with password: " + password);
        publish();
    });

    var publish = function() {
        conn.publish("chat", "Hello, World! - " + count--, function(err, msg) {
            //console.log("error = " + err + ", message = " + msg);
            publish();
        });
    }
}
