var http = require("http");
var pool = require("./lib/mysql_pool");
var app = require("./lib/app");
require("./lib/tree_node");


http.createServer(app).listen(8080);

