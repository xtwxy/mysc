var count = 10000,
    password = "foobared",
    redis = require("redis"),
    client = redis.createClient({
        host: 'redis-server',
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

var lpush = function() {
    client.lpush("queue", "Hello, World! " + count, function(err, data) {
        if((data !== undefined) && (data !== null)) {
            count--;
        } 
                
        if (count > 0) {
            lpush();
        } else {
            client.quit();
        }
    });
};

lpush();
