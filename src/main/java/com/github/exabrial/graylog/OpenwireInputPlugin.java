package com.github.exabrial.graylog;

import java.util.Collection;

import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;

@AutoService(Plugin.class)
public class OpenwireInputPlugin implements Plugin {
	@Override
	public PluginMetaData metadata() {
		return new OpenwireInputMetadata();
	}

	@Override
	public Collection<PluginModule> modules() {
		return ImmutableSet.<PluginModule>of(new OpenwireInputModule());
	}
}
