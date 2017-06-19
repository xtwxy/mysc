var password = "foobared",
    redis = require("redis"),
    client = redis.createClient({
        host: 'localhost',
        port: 6379,
        password: password
    }),
    clientBlocking = client.duplicate();

client.on("error", function(err) {
    console.log("Error: " + err);
});

client.on("connect", function() {
    client.auth(password);
    console.log("Authenticated with password: " + password);
});

var blpop = function() {
    clientBlocking.blpop("queue", 1, function(err, data) {
        if((data !== undefined) && (data !== null)) {
            console.log("blpop: (" + data + ").");
            blpop();
        } else {
            client.quit();
            clientBlocking.quit();
        }
    });
};

blpop();
