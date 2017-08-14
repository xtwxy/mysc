package com.wincom.dcim.driver;

public interface DriverCodec {
	void received(Sender s, Command m);
	void datalink(Sender s);
	void initialized();
	void stop();
}
