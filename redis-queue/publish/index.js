var count = 10000,
    password = "foobared",
    redis = require("redis"),
    client = redis.createClient({
        host: 'localhost',
        port: 6379,
        password: password
    });

client.on("error", function(err) {
    console.log("Error: " + err);
});

client.on("connect", function() {
    client.auth(password);
    console.log("Authenticated with password: " + password);
    publish();
});

var publish = function() {
    client.publish("chat", "Hello, World! - " + count--, function(err, msg) {
        //console.log("error = " + err + ", message = " + msg);
        publish();
    });
}

