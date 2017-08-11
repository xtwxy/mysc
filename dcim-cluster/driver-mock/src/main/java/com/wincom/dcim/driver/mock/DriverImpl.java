package com.wincom.dcim.driver.mock;

import java.util.Map;

import com.wincom.dcim.driver.Command;
import com.wincom.dcim.driver.Driver;
import com.wincom.dcim.driver.Sender;

public class DriverImpl implements Driver {

	public DriverImpl(Map<String, String> params) {

	}

	@Override
	public void received(Sender s, Command m) {

	}

	@Override
	public void datalink(Sender s) {

	}

	@Override
	public void initialized() {

	}
}
