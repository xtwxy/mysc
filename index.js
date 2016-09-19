var express = require("express");
var path = require("path");
var http = require("http");
var pool = require("./lib/mysql_pool");

var app = express();

var treeNodesHandler = function(request, response) {
    var parent_id = 0;
    if(request.params.parent_id) {
        parent_id = parseInt(request.params.parent_id, 10);
    }
    pool.query('select * from tree_node where parent_id = ?',
            [parent_id],
            function(error, results, fields) {
                if(error) {
                    response.writeHead(501, {'Content-Type': 'text/plain'});
                    response.end(JSON.stringify(error));
                } else {
                    response.writeHead(200, {'Content-Type': 'application/json'});
                    response.end(JSON.stringify(results));
                }
            });
};

app.get('/tree-nodes/:parent_id', treeNodesHandler);
app.get('/tree-nodes', treeNodesHandler);

http.createServer(app).listen(8080);

