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
    client.subscribe("chat");
    console.log("Authenticated with password: " + password);
});

client.on("message", function(channel, message) {
    //console.log("MSG: " + message);
});
