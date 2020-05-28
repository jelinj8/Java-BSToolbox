package cz.bliksoft.javautils.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.cli.Options;

import cz.bliksoft.javautils.CryptUtils;
import cz.bliksoft.javautils.PropertiesUtils;

public class OracleDbConnection {
	static Logger log = Logger.getLogger(OracleDbConnection.class.toString());

	private static OracleDbConnection _instance = null;

	public static OracleDbConnection getInstance() throws Exception {
		if (_instance == null) {
			_instance = new OracleDbConnection();
		}
		return _instance;
	}

	private OracleDbConnection() throws Exception {
		processOptions();
		log.info("Loading OJDBC driver.");

		try {
			String driverName = "oracle.jdbc.driver.OracleDriver";// $NON-NLS-1$ //$NON-NLS-1$
			Class.forName(driverName);
			log.info("OJDBC driver loaded.");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			log.severe("Loading of OJDBC driver failed.");
			// System.exit(-1);
			throw new Exception("Loading of OJDBC driver failed.");
		}
	}

	/* private static OracleDbConnection _instance = null; */
	public Connection getConnection() throws Exception {
		return getConnection(null);
	}
	
	public Connection getConnection(String reason) throws Exception {
		/*
		 * if(_instance==null){ _instance = new OracleDbConnection(); }
		 */
//		if (CalloutReceiverMain.debug){
//			if(reason == null){
//				StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
//				if("OracleDbConnection.java".equals(ste.getFileName())){
//					ste = Thread.currentThread().getStackTrace()[3];
//				}
//				reason = MessageFormat.format("{0}:{1} {2}", ste.getFileName(), ste.getLineNumber(), ste.getMethodName());
//			}
//			log.info(MessageFormat.format("Connecting to {0} as {1} ({2})", getOraServerString(), oraUserName, reason));
//		}
		Connection connection = DriverManager.getConnection(getOraServerString(), oraUserName, oraPassword);
		return connection;
	}

	private String getOraServerString() {
		String dbUrl = "jdbc:oracle:thin:@"; //$NON-NLS-1$

		if ((oraAddr == null) || (oraAddr.length() == 0)) {
			dbUrl += oraDatabase;
		} else {
			if ((oraServerPort == null) || (oraServerPort == 0))
				dbUrl += oraAddr + oraDatabase; // $NON-NLS-1$ //$NON-NLS-2$
			else
				dbUrl += oraAddr + ":" + oraServerPort + ":" + oraDatabase; //$NON-NLS-1$ //$NON-NLS-2$
		}
//		if (CalloutReceiverMain.debug) {
//			log.info("DB Connection string: " + dbUrl);
//		}
		return dbUrl;
	}

	private String oraAddr;
	private Integer oraServerPort;
	private String oraUserName;
	private String oraPassword;
	private String oraDatabase;

	private static Options _options;

	// private Option oraServerAddressOpt;
	// private Option oraServerPortOpt;
	// private Option oraUserNameOpt;
	// private Option oraPasswordOpt;
	// private Option oraDatabaseOpt;

	public Options getOptions() {
		if (_options == null) {
			_options = new Options();

			/*
			 * oraServerAddressOpt = new Option("H", "host", true,
			 * "URL or IP of Oracle server");
			 * _options.addOption(oraServerAddressOpt); oraServerPortOpt = new
			 * Option("P", "dbport", true, "port of Oracle server");
			 * _options.addOption(oraServerPortOpt); oraUserNameOpt = new
			 * Option("U", "dbuser", true, "Oracle user name");
			 * _options.addOption(oraUserNameOpt); oraPasswordOpt = new
			 * Option("W", "dbpassword", true, "Oracle password");
			 * _options.addOption(oraPasswordOpt); oraDatabaseOpt = new
			 * Option("b", "database", true, "Oracle database");
			 * _options.addOption(oraDatabaseOpt);
			 */
		}
		return _options;
	}

	private void processOptions() {
		// CommandLineParser parser = new ExtendedPosixParser(true);
		// CommandLine cmd = null;
		// try {
		// Options opts = getOptions();
		// if(CalloutReceiverMain.debug){
		// StringBuilder sb = new StringBuilder("Parsing DB parameters from:
		// \n");
		// for(String s:CalloutReceiverMain.args){
		// sb.append(s);
		// sb.append("\n");
		// }
		// log.info(sb.toString());
		// }
		// cmd = parser.parse(opts, CalloutReceiverMain.args, false);

		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream("database.properties"));

			oraPassword = CryptUtils.getPwdFromProperties(properties, "oraPassword");
			// properties.getProperty("oraPassword");
			oraDatabase = properties.getProperty("oraDatabase");
			oraAddr = properties.getProperty("oraAddr");
			oraServerPort = Integer.parseInt(properties.getProperty("oraServerPort", "1521"));
			oraUserName = properties.getProperty("oraUserName");

			if (CryptUtils.lastPwdModified) {
				try{
				PropertiesUtils.saveProperties(properties, "database.properties", "saved after encoding PWD", true);
				}catch(Exception e){
					log.severe("Faiiled to save properties: " + e.getMessage());
				}
			}
		} catch (IOException e) {

		}
		// if (cmd.hasOption(oraServerAddressOpt.getOpt())) {
		// oraAddr = (cmd.getOptionValue(oraServerAddressOpt.getOpt()));
		// }
		// if (cmd.hasOption(oraServerPortOpt.getOpt())) {
		// oraServerPort = (Integer.parseInt(cmd
		// .getOptionValue(oraServerPortOpt.getOpt())));
		// }
		// if (cmd.hasOption(oraUserNameOpt.getOpt())) {
		// oraUserName = (cmd.getOptionValue(oraUserNameOpt.getOpt()));
		// }
		// if (cmd.hasOption(oraPasswordOpt.getOpt())) {
		// oraPassword = (cmd.getOptionValue(oraPasswordOpt.getOpt()));
		// }
		// if (cmd.hasOption(oraDatabaseOpt.getOpt())) {
		// oraDatabase = (cmd.getOptionValue(oraDatabaseOpt.getOpt()));
		// }
		// } catch (ParseException e) {
		// e.printStackTrace();
		// }
	}
}
