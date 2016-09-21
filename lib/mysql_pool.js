var mysql = require("mysql");

module.exports = mysql.createPool({
    connectionLimit: 16,
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'wangsx',
    database: 'config'
});

