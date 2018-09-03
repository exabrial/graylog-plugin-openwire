package com.github.exabrial.graylog;

import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Named;
import javax.jms.JMSException;

import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class OpenwireTransport extends ThrottleableTransport {
	public static final String brokerUrl = "ActiveMQ Broker URL";
	public static final String queueName = "Queue Name";
	private static final Logger log = LoggerFactory.getLogger(OpenwireTransport.class);

	private final Configuration configuration;
	private final EventBus eventBus;
	private final MetricRegistry localRegistry;
	private final ScheduledExecutorService scheduler;

	private OpenwireConsumer consumer;

	@AssistedInject
	public OpenwireTransport(@Assisted final Configuration configuration, final EventBus eventBus,
			final LocalMetricRegistry localRegistry, @Named("daemonScheduler") final ScheduledExecutorService scheduler) {
		super(eventBus, configuration);
		this.configuration = configuration;
		this.eventBus = eventBus;
		this.localRegistry = localRegistry;
		this.scheduler = scheduler;

		localRegistry.register("read_bytes_1sec", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return consumer.getLastSecBytesRead().get();
			}
		});
		localRegistry.register("written_bytes_1sec", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return 0L;
			}
		});
		localRegistry.register("read_bytes_total", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return consumer.getTotalBytesRead().get();
			}
		});
		localRegistry.register("written_bytes_total", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return 0L;
			}
		});
	}

	@Subscribe
	public void lifecycleChanged(final Lifecycle lifecycle) {
		try {
			log.debug("Lifecycle changed to {}", lifecycle);
			switch (lifecycle) {
				case PAUSED:
				case FAILED:
				case HALTING:
					try {
						if (consumer != null) {
							consumer.stop();
						}
					} catch (final JMSException e) {
						log.warn("Unable to stop consumer", e);
					}
					break;
				default:
					if (consumer.isConnected()) {
						log.debug("Consumer is already connected, not running it a second time.");
						break;
					}
					try {
						consumer.run();
					} catch (final JMSException e) {
						log.warn("Unable to resume consumer", e);
					}
					break;
			}
		} catch (final Exception e) {
			log.warn("This should not throw any exceptions", e);
		}
	}

	@Override
	public void setMessageAggregator(final CodecAggregator aggregator) {
	}

	@Override
	public void doLaunch(final MessageInput input) throws MisfireException {
		consumer = new OpenwireConsumer(configuration.getString(brokerUrl), configuration.getString(queueName), input, scheduler, this);
		eventBus.register(this);
		try {
			consumer.run();
		} catch (final JMSException e) {
			eventBus.unregister(this);
			throw new MisfireException("Could not launch Openwire consumer", e);
		}
	}

	@Override
	public void doStop() {
		if (consumer != null) {
			try {
				consumer.stop();
			} catch (final JMSException e) {
				log.error("Could not stop Openwire consumer", e);
			}
		}
		eventBus.unregister(this);
	}

	@Override
	public MetricSet getMetricSet() {
		return localRegistry;
	}

	@FactoryClass
	public interface Factory extends Transport.Factory<OpenwireTransport> {
		@Override
		OpenwireTransport create(Configuration configuration);

		@Override
		Config getConfig();
	}

	@ConfigClass
	public static class Config extends ThrottleableTransport.Config {
		@Override
		public ConfigurationRequest getRequestedConfiguration() {
			final ConfigurationRequest cr = super.getRequestedConfiguration();
			cr.addField(new TextField("brokerUrl", brokerUrl, defaultBrokerUrl(),
					"ActiveMQ Broker URL to connect to; Reference the ActiveMQ documentation for help",
					ConfigurationField.Optional.NOT_OPTIONAL));
			cr.addField(new TextField("queueName", queueName, defaultQueueName(), "Name of queue to listen on",
					ConfigurationField.Optional.NOT_OPTIONAL));
			return cr;
		}

		protected String defaultBrokerUrl() {
			return "failover:(ssl://activemq-1.example.com:61616,ssl://activemq-2.example.com:61616)?randomize=false&backup=true";
		}

		protected String defaultQueueName() {
			return "com.example.logback";
		}
	}
}
