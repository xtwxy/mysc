var http = require("http");
var app = require("./lib/app");

/* assemble the request handles here to form the application. */
require("./lib/tree_node");


http.createServer(app).listen(8080);

