var express = require("express");
var path = require("path");
var app = express();

var htdocs = path.resolve(__dirname, "./htdocs");
app.use(express.static(htdocs));

module.exports = app;

