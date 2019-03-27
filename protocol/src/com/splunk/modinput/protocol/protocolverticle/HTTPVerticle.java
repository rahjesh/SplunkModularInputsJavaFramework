package com.splunk.modinput.protocol.protocolverticle;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import org.vertx.java.platform.Verticle;

import com.splunk.modinput.ModularInput;

/**
 * A HTTP(s) Server
 * 
 * @author ddallimore
 * 
 */
public class HTTPVerticle extends Verticle {

	String address = UUID.randomUUID().toString();
	protected static Logger logger = Logger.getLogger(HTTPVerticle.class);

	public void start() {

		JsonObject config = container.config();

		JsonObject handlerConfig = new JsonObject(config.getString("handler_config"));

		handlerConfig.putString("address", address);

		container.deployWorkerVerticle(config.getString("handler_verticle"), handlerConfig,
				config.getNumber("handler_verticle_instances").intValue(), false, new AsyncResultHandler<String>() {
					public void handle(AsyncResult<String> asyncResult) {
						if (asyncResult.succeeded()) {
							// ok
						} else {
							logger.error("Can't instantiate handler verticle : "
									+ ModularInput.getStackTrace(asyncResult.cause()));

						}
					}
				});

		boolean useSSL = false;

		if (config.containsField("use_ssl"))
			useSSL = Boolean.parseBoolean(config.getNumber("use_ssl").intValue() == 1 ? "true" : "false");

		HttpServer server = vertx.createHttpServer();

		if (config.containsField("accept_backlog"))
			server.setAcceptBacklog(config.getNumber("accept_backlog").intValue());

		if (useSSL) {
			server.setSSL(true);
			if (config.containsField("keystore_path"))
				server.setKeyStorePath(config.getString("keystore_path"));
			if (config.containsField("keystore_pass"))
				server.setKeyStorePassword(config.getString("keystore_pass"));
			if (config.containsField("truststore_path"))
				server.setTrustStorePath(config.getString("truststore_path"));
			if (config.containsField("truststore_pass"))
				server.setTrustStorePassword(config.getString("truststore_pass"));
			if (config.containsField("client_auth_required"))
				server.setClientAuthRequired(Boolean
						.parseBoolean(config.getNumber("client_auth_required").intValue() == 1 ? "true" : "false"));
		}

		if (config.containsField("receive_buffer_size"))
			server.setReceiveBufferSize(config.getNumber("receive_buffer_size").intValue());
		if (config.containsField("tcp_nodelay"))
			server.setTCPNoDelay(
					Boolean.parseBoolean(config.getNumber("tcp_nodelay").intValue() == 1 ? "true" : "false"));
		if (config.containsField("tcp_keepalive"))
			server.setTCPKeepAlive(
					Boolean.parseBoolean(config.getNumber("tcp_keepalive").intValue() == 1 ? "true" : "false"));
		if (config.containsField("so_linger"))
			server.setSoLinger(config.getNumber("so_linger").intValue());

		int port = config.getNumber("port").intValue();

		String bindAddress = "localhost";

		if (config.containsField("bind_address"))
			bindAddress = config.getString("bind_address");

		server.requestHandler(new Handler<HttpServerRequest>() {

			public void handle(HttpServerRequest request) {
				String method = request.method();
				if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT")) {

					request.bodyHandler(new Handler<Buffer>() {
						public void handle(Buffer body) {
							EventBus eb = vertx.eventBus();
							eb.send(address, body.getBytes());

						}
					});
					request.expectMultiPart(true);

					request.uploadHandler(new Handler<HttpServerFileUpload>() {
						public void handle(HttpServerFileUpload upload) {
							upload.dataHandler(new Handler<Buffer>() {
								public void handle(Buffer buffer) {
									EventBus eb = vertx.eventBus();
									eb.send(address, buffer.getBytes());
								}

							});
						}
					});

					request.response().setStatusCode(200).setStatusMessage("OK")
					.putHeader("Access-Control-Allow-Origin", "*")
					.putHeader("Access-Control-Allow-Methods", "GET,PUT,POST").end();

				}

				else if (method.equalsIgnoreCase("OPTIONS")) {
					
					request.bodyHandler(new Handler<Buffer>() {
						public void handle(Buffer body) {
							// do nothing

						}
					});
					
					request.response().setStatusCode(200).setStatusMessage("OK")
							.putHeader("Access-Control-Allow-Origin", "*")
							.putHeader("Access-Control-Allow-Methods", "GET,PUT,POST").end();
				} else {

					request.response().setStatusCode(405).setStatusMessage("Method " + method + " Not Allowed").end();
					logger.error("Unsupported HTTP Method : " + method);
				}
				request.exceptionHandler(new Handler<Throwable>() {
					public void handle(Throwable t) {
						logger.error("Error in the HTTP Server: " + ModularInput.getStackTrace(t));
					}
				});
			}
		}).listen(port, bindAddress);
	}
}
