var express = require("express");
var path = require("path");
var bodyParser = require('body-parser');

var app = express();

var htdocs = path.resolve(__dirname, "./htdocs");
app.use(express.static(htdocs));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

module.exports = app;

