package com.github.exabrial.graylog;

import javax.inject.Inject;

import org.graylog2.inputs.codecs.GelfCodec;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class OpenwireGELFInput extends MessageInput {
	private static final String NAME = "Openwire (GELF)";

	@AssistedInject
	public OpenwireGELFInput(final MetricRegistry metricRegistry, @Assisted Configuration configuration,
			OpenwireTransport.Factory openwireTransportFactory, GelfCodec.Factory gelfCodecFactory, LocalMetricRegistry localRegistry,
			Config config, Descriptor descriptor, ServerStatus serverStatus) {
		super(metricRegistry, configuration, openwireTransportFactory.create(configuration), localRegistry,
				gelfCodecFactory.create(configuration), config, descriptor, serverStatus);
	}

	public interface Factory extends MessageInput.Factory<OpenwireGELFInput> {
		@Override
		OpenwireGELFInput create(Configuration configuration);

		@Override
		Config getConfig();

		@Override
		Descriptor getDescriptor();
	}

	public static class Descriptor extends MessageInput.Descriptor {
		@Inject
		public Descriptor() {
			super(NAME, false, "https://github.com/exabrial/graylog-plugin-openwire");
		}
	}

	public static class Config extends MessageInput.Config {
		@Inject
		public Config(OpenwireTransport.Factory transport, GelfCodec.Factory codec) {
			super(transport.getConfig(), codec.getConfig());
		}
	}
}
