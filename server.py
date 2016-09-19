#!/usr/bin/env python
#  -*- coding: utf-8 -*-
import sys
import time
from flask import Flask,redirect,Response,request,g

app = Flask( __name__, static_url_path = '')

reload( sys )
sys.setdefaultencoding( 'utf-8' )

@app.errorhandler(Exception)
def all_exception_handler(error):
    print type(error)
    print type(LookupError())
    if type(error)==type(LookupError()):
        print "exception is 401"
        return Response(status=401)
    print "exception: %s" %error
    return u'dashboard 暂时无法访问，请联系管理员', 500

@app.teardown_request
def teardown_request(exception):
    if exception:
        print "teardown_request",exception
    else:
   	    print "teardown_request"

@app.teardown_request
def teardown_request1(exception):
    if exception:
    	print "teardown_request1",exception
    else:
   	    print "teardown_request1"
   	
@app.before_request
def chart_before():
    now = int(time.time())
    g.legend = request.args.get("legend") or "off"
    print "before_request"
    #raise LookupError

@app.before_request
def chart_before1():
	print "before_request1"
	
@app.route( '/', methods = ['GET'] )
def index():
    print locals(),globals()
    #raise Exception,"3333"
    return "xxxxxxxxxxxx"


if __name__ == '__main__':
    app.run( host = '0.0.0.0', port = 4000 )
