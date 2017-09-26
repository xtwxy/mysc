package com.wincom.dcim.message.common

trait Command {
	def entityId: String
	def user: Option[String]
}

