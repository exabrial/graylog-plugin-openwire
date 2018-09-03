package com.github.exabrial.graylog;

import javax.inject.Inject;

import org.graylog2.inputs.codecs.RawCodec;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class OpenwireRawInput extends MessageInput {
	private static final String NAME = "Openwire (Raw/Plaintext)";

	@AssistedInject
	public OpenwireRawInput(final MetricRegistry metricRegistry, @Assisted Configuration configuration,
			OpenwireTransport.Factory openwireTransportFactory, RawCodec.Factory rawCodecFactory, LocalMetricRegistry localRegistry,
			Config config, Descriptor descriptor, ServerStatus serverStatus) {
		super(metricRegistry, configuration, openwireTransportFactory.create(configuration), localRegistry,
				rawCodecFactory.create(configuration), config, descriptor, serverStatus);
	}

	public interface Factory extends MessageInput.Factory<OpenwireRawInput> {
		@Override
		OpenwireRawInput create(Configuration configuration);

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
		public Config(OpenwireTransport.Factory transport, RawCodec.Factory codec) {
			super(transport.getConfig(), codec.getConfig());
		}
	}
}
