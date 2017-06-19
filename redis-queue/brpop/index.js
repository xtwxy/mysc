var password = "foobared",
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
});

var brpop = function() {
    client.brpop("queue", 1, function(err, data) {
        if((data !== undefined) && (data !== null)) {
            console.log("brpop: (" + data + ").");
            brpop();
        } else {
            client.quit();
        }
    });
};

brpop();
