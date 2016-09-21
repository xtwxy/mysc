var pool = require("./mysql_pool");
var app = require("./app");
var async = require("async");

module.exports = app;

function response(res,status,body){
	res.status(status).send(body);
}
var createNodeHandler = function(req, res) {
	pool.getConnection(function(err, connection) {
		if (err) {
			console.error('mysql 链接失败');
			response(res,501,err);
			return;
		}
		// 开始事务
		connection.beginTransaction(function(err) {
			if (err) {
				throw err;
			}
			var queryFuncAyyay = [];
			queryFuncAyyay.push(function(callback) {
				connection.query('INSERT INTO config.LOGIC_OBJECT(ID,LOGIC_TYPE,NAME,SERIAL_ID)values(11,?,?,?)', [
						req.body.logicType, req.body.name, req.body.serialId ], callback);
			});
			async.parallel(queryFuncAyyay, function(err, result) {
				if (err) {
					connection.rollback(function() {
						response(res,501,err);
					});
				} else {
					connection.commit(function(err) {
						if (err) {
							connection.rollback(function() {
								response(501,err);
							});
						}
						response(res,200,"");
					});
				}
			});
		});
	});
};

app.post('/nodes', createNodeHandler);