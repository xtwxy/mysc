package com.wincom.dcim.message

trait Command {
	def id: String
	def user: Option[String]
}

