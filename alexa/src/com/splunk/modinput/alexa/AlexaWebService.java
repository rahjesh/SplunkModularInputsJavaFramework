package com.splunk.modinput.alexa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import com.amazon.speech.Sdk;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.servlet.SpeechletServlet;
import com.splunk.PasswordCollection;
import com.splunk.Service;
import com.splunk.modinput.Arg;
import com.splunk.modinput.Endpoint;
import com.splunk.modinput.Input;
import com.splunk.modinput.Item;
import com.splunk.modinput.ModularInput;
import com.splunk.modinput.Param;
import com.splunk.modinput.Scheme;

import com.splunk.modinput.Stanza;

import com.splunk.modinput.Validation;
import com.splunk.modinput.ValidationError;
import com.splunk.modinput.Scheme.StreamingMode;

/**
 * The Modular Input Implementation.Doesn't actually index data into Splunk ,
 * just using the Modular Input mechanism to fire off a standalone subprocess.
 * 
 * 1) loads json config files 2) runs monitor thread to dynamically reload json
 * config files if they have been touched 3) starts the HTTPS web server for
 * receiving Alexa requests and serving MP3 soundbites 4) obtains Service object
 * connection to Splunk REST API for executing searches
 * 
 * @author ddallimore
 *
 */
public class AlexaWebService extends ModularInput {

	private static final String JSON_MAPPING = "mapping.json";
	private static final String JSON_TIMEMAPPING = "timemappings.json";
	private static final String JSON_DYNAMIC_ACTIONS = "dynamicactions.json";

	private static final String DIR_INTENTS = "intents";
	private static final String DIR_DYNAMIC_ACTIONS = "dynamic_actions";

	public static void main(String[] args) {

		AlexaWebService instance = new AlexaWebService();
		instance.init(args);

	}

	boolean validateMode = false;
	String modinputHome = "";

	@Override
	protected void doRun(Input input) throws Exception {

		if (input != null) {

			for (Stanza stanza : input.getStanzas()) {

				String name = stanza.getName();

				if (name != null && name.startsWith("alexa://")) {

					this.modinputHome = System.getProperty("modinputhome");
					setupDefaultSplunkService(input);

					loadMappingJSON();
					loadTimeMappingJSON();
					loadDynamicActionJSON();
					startJSONFileChangeMonitor();
					startWebServer(name, stanza.getParams(), validateMode);

				}

				else {
					logger.error("Invalid stanza name : " + name);
					System.exit(2);
				}

			}
		} else {
			logger.error("Input is null");
			System.exit(2);
		}

	}

	private void startJSONFileChangeMonitor() {
		new FileChangeMonitor().start();

	}

	/**
	 * Thread to monitor changes to config json files
	 * 
	 * @author ddallimore
	 *
	 */
	class FileChangeMonitor extends Thread {

		long lastModifiedMapping;
		long lastModifiedTimeMapping;
		long lastModifiedDynamicActionMapping;

		File mapping;
		File timeMapping;
		File dynamicActionMapping;

		FileChangeMonitor() {

			getFileHandles();
			this.lastModifiedMapping = mapping.lastModified();
			this.lastModifiedTimeMapping = timeMapping.lastModified();
			this.lastModifiedDynamicActionMapping = dynamicActionMapping.lastModified();

		}

		private void getFileHandles() {

			this.mapping = new File(getJSONFilePath(DIR_INTENTS, JSON_MAPPING));
			this.timeMapping = new File(getJSONFilePath(DIR_INTENTS, JSON_TIMEMAPPING));
			this.dynamicActionMapping = new File(getJSONFilePath(DIR_DYNAMIC_ACTIONS, JSON_DYNAMIC_ACTIONS));

		}

		@Override
		public void run() {

			while (true) {

				// a bit hacky , but just shoehorning in a check that the
				// service has not died/timed out etc..
				Service currentService = AlexaSessionManager.getService();
				if (currentService != null) {
					try {
						// simple test call
						currentService.getCapabilities();
					} catch (Exception e) {
						try {
							logger.error("Attempting to re-establish user REST session : "+e.getMessage());
							loadMappingJSON();
						} catch (Exception e2) {
							logger.error("Error reloading mapping json for service checker :" + e.getMessage());
						}
					}
				}

				getFileHandles();
				long thisModifiedMapping = mapping.lastModified();
				long thisModifiedTimeMapping = timeMapping.lastModified();
				long thisModifiedDynamicActionMapping = dynamicActionMapping.lastModified();

				if (thisModifiedMapping > this.lastModifiedMapping) {
					this.lastModifiedMapping = thisModifiedMapping;
					try {
						loadMappingJSON();
					} catch (Exception e) {
						logger.error("Error reloading mapping json :" + e.getMessage());
					}
				}
				if (thisModifiedTimeMapping > this.lastModifiedTimeMapping) {
					this.lastModifiedTimeMapping = thisModifiedTimeMapping;
					try {
						loadTimeMappingJSON();
					} catch (Exception e) {
						logger.error("Error reloading time mapping json :" + e.getMessage());
					}
				}
				if (thisModifiedDynamicActionMapping > this.lastModifiedDynamicActionMapping) {
					this.lastModifiedDynamicActionMapping = thisModifiedDynamicActionMapping;
					try {
						loadDynamicActionJSON();
					} catch (Exception e) {
						logger.error("Error reloading dynamic action json :" + e.getMessage());
					}
				}

				try {
					// poll every 10 seconds
					Thread.sleep(10000);
				} catch (InterruptedException e) {

				}
			}
		}

	}

	/**
	 * Obtain Service object for accessing Splunk REST API
	 * 
	 * @param input
	 */
	private void setupDefaultSplunkService(Input input) {
		// String host = input.getServer_host();
		String uri = input.getServer_uri();
		int port = 8089;
		String token = input.getSession_key();
		try {
			int portOffset = uri.indexOf(":", 8);
			port = Integer.parseInt(uri.substring(portOffset + 1));
		} catch (Exception e) {

		}
		Service service = new Service("localhost", port);
		service.setToken("Splunk " + token);
		service.version = service.getInfo().getVersion();
		AlexaSessionManager.setDefaultService(service);

	}

	private String getJSONFilePath(String dir, String file) {

		return this.modinputHome + File.separator + dir + File.separator + file;

	}

	private JSONObject loadJSON(String dir, String file) throws Exception {
		String jsonMapping = getJSONFilePath(dir, file);
		return new JSONObject(readFile(jsonMapping));
	}

	/**
	 * load mapping json
	 * 
	 * @throws Exception
	 */
	private void loadMappingJSON() throws Exception {

		JSONObject mappingJSON = loadJSON(DIR_INTENTS, JSON_MAPPING);

		String globalUser = "";
		String globalPass = "";
		String globalEarliest = "-60m@m";// default
		String globalLatest = "now";// default
		String globalAppNamespace = "search";// default app context

		if (mappingJSON.has("global_search_earliest"))
			globalEarliest = mappingJSON.getString("global_search_earliest");
		if (mappingJSON.has("global_search_latest"))
			globalLatest = mappingJSON.getString("global_search_latest");
		if (mappingJSON.has("global_search_appnamespace"))
			globalAppNamespace = mappingJSON.getString("global_search_appnamespace");
		if (mappingJSON.has("global_auth_user"))
			globalUser = mappingJSON.getString("global_auth_user");
		if (mappingJSON.has("global_auth_pass"))
			globalPass = mappingJSON.getString("global_auth_pass");
		else
			globalPass = getPlainPasswordFromEncryptedStore(globalUser);

		Service defaultService = AlexaSessionManager.getDefaultService();

		if (globalUser.length() > 0 && globalPass.length() > 0) {

			try {
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("username", globalUser);
				params.put("password", globalPass);
				params.put("app", globalAppNamespace);
				params.put("host", defaultService.getHost());
				params.put("port", defaultService.getPort());
				Service newService = Service.connect(params);
				newService.getInfo().getVersion();
				AlexaSessionManager.setCurrentService(newService);
			} catch (Exception e) {
				logger.error("Error establishing session for user "+ globalUser + " " + e.getMessage());
				AlexaSessionManager.setCurrentService(defaultService);
			}

		} else
			AlexaSessionManager.setCurrentService(defaultService);

		JSONArray mappings = mappingJSON.getJSONArray("mappings");
		Map<String, IntentMapping> intentMappings = new HashMap<String, IntentMapping>();
		for (int i = 0; i < mappings.length(); i++) {
			JSONObject item = mappings.getJSONObject(i);

			IntentMapping im = new IntentMapping();

			try {
				if (item.has("appnamespace"))
					im.setAppNamespace(item.getString("appnamespace"));
				else
					im.setAppNamespace(globalAppNamespace);
			} catch (Exception e) {
			}
			try {
				if (item.has("time_earliest"))
					im.setEarliest(item.getString("time_earliest"));
				else
					im.setEarliest(globalEarliest);
			} catch (Exception e) {
			}
			try {
				if (item.has("time_latest"))
					im.setLatest(item.getString("time_latest"));
				else
					im.setLatest(globalLatest);
			} catch (Exception e) {
			}
			try {
				im.setIntent(item.getString("intent"));
			} catch (Exception e) {
			}
			try {
				im.setResponse(item.getString("response"));
			} catch (Exception e) {
			}
			try {
				im.setSearch(item.getString("search"));
			} catch (Exception e) {
			}
			try {
				im.setSavedSearchName(item.getString("saved_search_name"));
			} catch (Exception e) {
			}
			try {
				im.setSavedSearchArgs(item.getString("saved_search_args"));
			} catch (Exception e) {
			}
			try {
				im.setTimeSlot(item.getString("time_slot"));
			} catch (Exception e) {
			}
			try {
				im.setDynamicAction(item.getString("dynamic_action"));
			} catch (Exception e) {
			}
			try {
				im.setDynamicActionArgs(item.getString("dynamic_action_args"));
			} catch (Exception e) {
			}

			intentMappings.put(im.getIntent(), im);
		}
		AlexaSessionManager.setIntentMappings(intentMappings);

	}

	private String getPlainPasswordFromEncryptedStore(String globalUser) {
		
		String clearPass = "";
		try {
			Service defaultService = AlexaSessionManager.getDefaultService();
			PasswordCollection pc = defaultService.getPasswords();
			clearPass = pc.get(globalUser).getClearPassword();
		} catch (Exception e) {
			logger.error("Error getting password from encrypted store :" + e.getMessage());
		}
		return clearPass;
	}

	/**
	 * load time mapping json
	 * 
	 * @throws Exception
	 */
	private void loadTimeMappingJSON() throws Exception {

		JSONObject mappingJSON = loadJSON(DIR_INTENTS, JSON_TIMEMAPPING);

		JSONArray mappings = mappingJSON.getJSONArray("times");
		Map<String, TimeMapping> timeMappings = new HashMap<String, TimeMapping>();
		for (int i = 0; i < mappings.length(); i++) {
			JSONObject item = mappings.getJSONObject(i);

			TimeMapping tm = new TimeMapping();
			try {
				tm.setUtterance(item.getString("utterance"));
			} catch (Exception e) {
			}
			try {
				tm.setEarliest(item.getString("earliest"));
			} catch (Exception e) {
			}
			try {
				tm.setLatest(item.getString("latest"));
			} catch (Exception e) {
			}

			timeMappings.put(tm.getUtterance(), tm);
		}
		AlexaSessionManager.setTimeMappings(timeMappings);

	}

	/**
	 * load dynamic actions json
	 * 
	 * @throws Exception
	 */
	private void loadDynamicActionJSON() throws Exception {

		JSONObject mappingJSON = loadJSON(DIR_DYNAMIC_ACTIONS, JSON_DYNAMIC_ACTIONS);

		JSONArray mappings = mappingJSON.getJSONArray("actions");
		Map<String, DynamicActionMapping> dynamicActionMappings = new HashMap<String, DynamicActionMapping>();
		for (int i = 0; i < mappings.length(); i++) {
			JSONObject item = mappings.getJSONObject(i);

			DynamicActionMapping dam = new DynamicActionMapping();
			try {
				dam.setName(item.getString("name"));
			} catch (Exception e) {
			}
			try {
				dam.setClassName(item.getString("class"));
			} catch (Exception e) {
			}

			dynamicActionMappings.put(dam.getName(), dam);
		}
		AlexaSessionManager.setDynamicActionMappings(dynamicActionMappings);

	}

	/**
	 * Read a file into a String
	 * 
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	private String readFile(String fileName) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			return sb.toString();
		} finally {
			br.close();
		}
	}

	/**
	 * start the Jetty HTTPS Webserver
	 * 
	 * @param stanzaName
	 * @param params
	 * @param validationMode
	 * @throws Exception
	 */
	private void startWebServer(String stanzaName, List<Param> params, boolean validationMode) throws Exception {

		int httpsPort = 443;
		String httpsScheme = "https";
		String endpoint = "/alexa";
		String keystore = this.modinputHome + File.separator + "crypto" + File.separator + "java-keystore.jks";
		String keystorePass = "";
		String disableRequestSignatureCheck = "false";
		String supportedApplicationIds = "";
		int timestampTolerance = 150;
		String activationKey = "";

		for (Param param : params) {
			String value = param.getValue();
			if (value == null) {
				continue;
			}

			if (param.getName().equals("activation_key")) {
				activationKey = param.getValue();
			} 
			else if (param.getName().equals("https_scheme")) {
				httpsScheme = param.getValue();

			} else if (param.getName().equals("https_port")) {
				try {
					// commenting this out for now as only 443 is supported
					// httpsPort = Integer.parseInt(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine https port value, will revert to default value.");
				}
			} else if (param.getName().equals("endpoint")) {
				endpoint = param.getValue();

			} else if (param.getName().equals("keystore")) {
				keystore = param.getValue();
				System.setProperty("javax.net.ssl.keyStore", keystore);

			} else if (param.getName().equals("keystore_pass")) {
				keystorePass = param.getValue();
				System.setProperty("javax.net.ssl.keyStorePassword", keystore);

			} else if (param.getName().equals("supported_application_ids")) {
				supportedApplicationIds = param.getValue();
				System.setProperty("com.amazon.speech.speechlet.servlet.supportedApplicationIds",
						supportedApplicationIds);

			} else if (param.getName().equals("disable_request_signature_check")) {
				disableRequestSignatureCheck = param.getValue().equals("1") ? "true" : "false";
				System.setProperty("com.amazon.speech.speechlet.servlet.disableRequestSignatureCheck",
						disableRequestSignatureCheck);

			} else if (param.getName().equals("timestamp_tolerance")) {
				try {
					timestampTolerance = Integer.parseInt(param.getValue());
					System.setProperty("com.amazon.speech.speechlet.servlet.timestampTolerance",
							String.valueOf(timestampTolerance));
				} catch (Exception e) {
					logger.error("Can't determine timestampTolerance value, will revert to default value.");
				}
			}
		}
		
		if(!activationKeyCheck(activationKey,"Talk to Splunk with Amazon Alexa")){
			System.exit(2);
		}

		if (!isDisabled(stanzaName)) {

			if (validationMode) {
				// TODO , just a test start then stop of the web server
			} else {

				// Configure server and its associated servlets
				Server server = new Server();
				SslConnectionFactory sslConnectionFactory = new SslConnectionFactory();
				SslContextFactory sslContextFactory = sslConnectionFactory.getSslContextFactory();
				sslContextFactory.setKeyStorePath(keystore);
				sslContextFactory.setKeyStorePassword(keystorePass);
				sslContextFactory.setIncludeCipherSuites(Sdk.SUPPORTED_CIPHER_SUITES);

				HttpConfiguration httpConf = new HttpConfiguration();
				httpConf.setSecurePort(httpsPort);
				httpConf.setSecureScheme(httpsScheme);
				httpConf.addCustomizer(new SecureRequestCustomizer());
				HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConf);

				ServerConnector serverConnector = new ServerConnector(server, sslConnectionFactory,
						httpConnectionFactory);
				serverConnector.setPort(httpsPort);

				Connector[] connectors = new Connector[1];
				connectors[0] = serverConnector;
				server.setConnectors(connectors);

				// alexa web service
				ServletContextHandler alexaContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
				alexaContext.setContextPath("/");
				// server.setHandler(context);

				alexaContext.addServlet(new ServletHolder(createServlet(new SplunkSpeechlet())),
						endpoint.substring(endpoint.indexOf("/")));

				// soundbites
				ResourceHandler resourceHandler = new ResourceHandler();
				resourceHandler.setResourceBase(this.modinputHome + File.separator + "soundbites");
				resourceHandler.setDirectoriesListed(true);
				ContextHandler soundbiteContext = new ContextHandler("/soundbites");
				soundbiteContext.setHandler(resourceHandler);
				// server.setHandler(contextHandler);

				HandlerCollection handlerCollection = new HandlerCollection();
				handlerCollection.setHandlers(new Handler[] { alexaContext, soundbiteContext });

				server.setHandler(handlerCollection);

				server.start();
				server.join();
			}
		}
	}

	private static SpeechletServlet createServlet(final Speechlet speechlet) {
		SpeechletServlet servlet = new SpeechletServlet();
		servlet.setSpeechlet(speechlet);
		return servlet;
	}

	@Override
	protected void doValidate(Validation val) {

		try {

			if (val != null) {
				validateWebServer(val);
				/**
				 * List<Item> items = val.getItems(); for (Item item : items) {
				 * List<Param> params = item.getParams();
				 * 
				 * 
				 * for (Param param : params) { if
				 * (param.getName().equals("some_param")) {
				 * validateSomeParam(param.getValue()); } }
				 * 
				 * }
				 **/
			}
			System.exit(0);
		} catch (Exception e) {
			logger.error(e.getMessage());
			ValidationError error = new ValidationError("Validation Failed : " + e.getMessage());
			sendValidationError(error);
			System.exit(2);
		}

	}

	private void validateWebServer(Validation val) throws Exception {

		try {
			Input input = new Input();

			input.setCheckpoint_dir(val.getCheckpoint_dir());
			input.setServer_host(val.getServer_host());
			input.setServer_uri(val.getServer_uri());
			input.setSession_key(val.getSession_key());

			List<Item> items = val.getItems();
			List<Stanza> stanzas = new ArrayList<Stanza>();
			for (Item item : items) {
				Stanza stanza = new Stanza();
				stanza.setName("alexa://" + item.getName());
				stanza.setParams(item.getParams());
				stanzas.add(stanza);
			}

			input.setStanzas(stanzas);
			this.validateMode = true;
			doRun(input);

		} catch (Throwable t) {
			throw new Exception(
					"An Alexa web server can not be run with the supplied propertys.Reason : " + t.getMessage());
		}

	}

	@Override
	protected Scheme getScheme() {

		Scheme scheme = new Scheme();
		scheme.setTitle("Alexa");
		scheme.setDescription("Alexa");
		scheme.setUse_external_validation(true);
		scheme.setUse_single_instance(true);
		scheme.setStreaming_mode(StreamingMode.XML);

		Endpoint endpoint = new Endpoint();

		Arg arg = new Arg();
		arg.setName("name");
		arg.setTitle("Stanza Name");
		arg.setDescription("");

		endpoint.addArg(arg);
		
		arg = new Arg();
		arg.setName("activation_key");
		arg.setTitle("Activation Key");
		arg.setDescription("Visit http://www.baboonbones.com/#activation  to obtain a non-expiring key");
		arg.setRequired_on_create(true);
		arg.setRequired_on_edit(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("https_port");
		arg.setTitle("HTTPS Port");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("https_scheme");
		arg.setTitle("HTTPS Scheme");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("endpoint");
		arg.setTitle("Endpoint");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("keystore");
		arg.setTitle("Keystore");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("keystore_pass");
		arg.setTitle("Keystore Pass");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("disable_request_signature_check");
		arg.setTitle("Disable Request Signature Check");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("supported_application_ids");
		arg.setTitle("Supported Application IDs");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("timestamp_tolerance");
		arg.setTitle("Timestamp Tolerance");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		scheme.setEndpoint(endpoint);

		return scheme;
	}

}
