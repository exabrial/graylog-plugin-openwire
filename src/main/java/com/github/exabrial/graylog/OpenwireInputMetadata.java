package com.github.exabrial.graylog;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

public class OpenwireInputMetadata implements PluginMetaData {
	private static final String PLUGIN_PROPERTIES = "com.github.exabrial.graylog-plugin-openwire/graylog-plugin.properties";

	@Override
	public String getUniqueId() {
		return OpenwireGELFInput.class.getCanonicalName();
	}

	@Override
	public String getName() {
		return "ActiveMQ Openwire Input Plugin";
	}

	@Override
	public String getAuthor() {
		return "Jonathan S Fisher";
	}

	@Override
	public URI getURL() {
		return URI.create("https://github.com/exabrial/graylog-plugin-openwire");
	}

	@Override
	public Version getVersion() {
		return Version.fromPluginProperties(getClass(), PLUGIN_PROPERTIES, "version", Version.from(1, 0, 0));
	}

	@Override
	public String getDescription() {
		return "Process messages from one or multiple queues of an ActiveMQ Openwire broker";
	}

	@Override
	public Version getRequiredVersion() {
		return Version.fromPluginProperties(getClass(), PLUGIN_PROPERTIES, "graylog.version", Version.from(2, 4, 0));
	}

	@Override
	public Set<ServerStatus.Capability> getRequiredCapabilities() {
		return Collections.emptySet();
	}
}
