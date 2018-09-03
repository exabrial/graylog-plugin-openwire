package com.github.exabrial.graylog;

import org.graylog2.plugin.PluginModule;

public class OpenwireInputModule extends PluginModule {
	@Override
	protected void configure() {
		addTransport("openwire-transport", OpenwireTransport.class, OpenwireTransport.Config.class, OpenwireTransport.Factory.class);
		addMessageInput(OpenwireRawInput.class, OpenwireRawInput.Factory.class);
		addMessageInput(OpenwireGELFInput.class, OpenwireGELFInput.Factory.class);
		addMessageInput(OpenwireSyslogInput.class, OpenwireSyslogInput.Factory.class);
	}
}
