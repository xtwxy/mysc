var http = require("http");
var app = require("./lib/app");

/* assemble the request handles here to form the application. */
require("./lib/tree_node");
require("./lib/slogan");
require("./lib/ctreateNode");


http.createServer(app).listen(9080);

