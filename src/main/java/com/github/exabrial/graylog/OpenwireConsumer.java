package com.github.exabrial.graylog;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenwireConsumer {
	private final String brokerUrl;
	private final String queue;
	private final MessageInput sourceInput;
	private final OpenwireTransport openwireTransport;

	private AtomicLong totalBytesRead = new AtomicLong(0);
	private AtomicLong lastSecBytesRead = new AtomicLong(0);
	private AtomicLong lastSecBytesReadTmp = new AtomicLong(0);
	private Connection connection;

	OpenwireConsumer(String brokerUrl, String queue, MessageInput sourceInput, ScheduledExecutorService scheduler,
			OpenwireTransport openwireTransport) {
		this.brokerUrl = brokerUrl;
		this.queue = queue;
		this.sourceInput = sourceInput;
		this.openwireTransport = openwireTransport;

		scheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				lastSecBytesRead.set(lastSecBytesReadTmp.getAndSet(0));
			}
		}, 1, 1, TimeUnit.SECONDS);
	}

	public void run() throws JMSException {
		if (!isConnected()) {
			connect();
		}
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination destination = session.createQueue(queue);
		MessageConsumer consumer = session.createConsumer(destination);
		consumer.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(Message message) {
				try {
					TextMessage textMessage = (TextMessage) message;
					String body = textMessage.getText();
					totalBytesRead.addAndGet(body.length());
					lastSecBytesReadTmp.addAndGet(body.length());
					final RawMessage rawMessage = new RawMessage(body.getBytes(StandardCharsets.UTF_8));
					if (openwireTransport.isThrottled()) {
						openwireTransport.blockUntilUnthrottled();
					}
					sourceInput.processRawMessage(rawMessage);
				} catch (JMSException exception) {
					Logger log = LoggerFactory.getLogger(getClass());
					log.error("onMessage() error", exception);
					throw new RuntimeException(exception);
				}
			}
		});
	}

	public void connect() throws JMSException {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
		connection = connectionFactory.createConnection();
		connection.start();
		connection.setExceptionListener(new ExceptionListener() {
			@Override
			public void onException(JMSException exception) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("onException() error", exception);
			}
		});
	}

	public void stop() throws JMSException {
		if (connection != null) {
			try {
				connection.close();
			} finally {
				connection = null;
			}
		}
	}

	public boolean isConnected() {
		return connection != null;
	}

	public AtomicLong getLastSecBytesRead() {
		return lastSecBytesRead;
	}

	public AtomicLong getTotalBytesRead() {
		return totalBytesRead;
	}
}
