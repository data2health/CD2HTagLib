package org.cd2h.incommon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import edu.uiowa.util.PropertyLoader;

public class Loader {
	static Logger logger = Logger.getLogger(Loader.class);
	static DecimalFormat formatter = new DecimalFormat("0000");
	static Properties prop_file = null;

	static Connection conn = null;

	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure(args[0]);
		conn = getConnection();

	    URL theURL = new URL("https://mdq.incommon.org/entities");
		InputStream in = theURL.openConnection().getInputStream();
		SAXReader reader = new SAXReader(false);

		Document document = reader.read(in);
		Element root = document.getRootElement();
		logger.debug("document root: " + root.getName());
		in.close();
	}

	static Connection getConnection() throws ClassNotFoundException, SQLException {
		Connection conn = null;
		Properties prop_file = PropertyLoader.loadProperties("loader");

		String use_ssl = prop_file.getProperty("nihdb.use.ssl", "false");
		logger.debug("Database SSL: " + use_ssl);

		String databaseHost = prop_file.getProperty("db.host");
		logger.debug("Database Host: " + databaseHost);

		String databaseName = prop_file.getProperty("db.name");
		logger.debug("Database Name: " + databaseName);

		String db_url = prop_file.getProperty("db.url");
		logger.debug("Database URL: " + db_url);

		Class.forName("org.postgresql.Driver");
		Properties props = new Properties();
		props.setProperty("user", prop_file.getProperty("db.user.name"));
		props.setProperty("password", prop_file.getProperty("db.user.password"));
		if (use_ssl.equals("true")) {
			props.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
			props.setProperty("ssl", "true");
		}
		conn = DriverManager.getConnection(db_url, props);
		conn.setAutoCommit(false);

		return conn;
	}
}