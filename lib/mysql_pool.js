var mysql = require("mysql");

module.exports = mysql.createPool({
    connectionLimit: 16,
    host: 'localhost',
    port: 2016,
    user: 'wangxy',
    password: 'wincom',
    database: 'scdb'
});

