var mysql = require("mysql");

module.exports = mysql.createPool({
    connectionLimit: 16,
    host: '192.168.0.88',
    port: 3306,
    user: 'wangxy',
    password: 'wincom',
    database: 'scdb'
});

