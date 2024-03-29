package org.cd2h.incommon;

import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Properties;
import javax.xml.parsers.*;

import edu.uiowa.util.PropertyLoader;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.*;

public class Loader {
	static Logger logger = Logger.getLogger(Loader.class);
	static DecimalFormat formatter = new DecimalFormat("0000");
	static Properties prop_file = null;

	static Connection conn = null;

	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure(args[0]);
		conn = getConnection();
		
		conn.prepareStatement("truncate incommon.organization").execute();

	    URL theURL = new URL("https://mdq.incommon.org/entities/idps/all");
		InputStream in = theURL.openConnection().getInputStream();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(in);
		Element root = doc.getDocumentElement();
		root.normalize();
		logger.info("document root: " + doc.getDocumentElement().getNodeName());
		NodeList nList = doc.getElementsByTagName("EntityDescriptor");
		for (int temp = 0; temp < nList.getLength(); temp++) {
            Element node = (Element) nList.item(temp);
            logger.trace("entry: " +  asString(node));
            String entityID = node.getAttribute("entityID");
            logger.info("entityID: " + entityID);
            Node org = node.getElementsByTagName("Organization").item(0);
            logger.debug("org: "  + asString((Element)org));
            String name = getElementString((Element)org, "OrganizationName");
            logger.info("\tname: " + name);
            String displayname = getElementString((Element)org, "OrganizationDisplayName");
            logger.info("\tdisplayname: " + displayname);
            String url = getElementString((Element)org, "OrganizationURL");
            logger.info("\turl: " + url);
            
            PreparedStatement stmt = conn.prepareStatement("insert into incommon.organization values(?,?,?,?)");
            stmt.setString(1, entityID);
            stmt.setString(2, name);
            stmt.setString(3, displayname);
            stmt.setString(4, url);
            stmt.execute();
            stmt.close();
		}
		in.close();
	}
	
	static String asString(Element element) {
		return ((org.w3c.dom.ls.DOMImplementationLS)element.getOwnerDocument().getImplementation()).createLSSerializer().writeToString(element);
	}
	
	static String getElementString(Element node, String element)  {
		NodeList nList = node.getElementsByTagName(element);
		for (int temp = 0; temp < nList.getLength(); temp++) {
            Element child = (Element) nList.item(temp);
            logger.debug("lang: " +  child.getAttribute("xml:lang"));
            if (child.getAttribute("xml:lang").equals("en"))  {
                logger.debug("\tnode: " +  child.toString());
            	return child.getTextContent();
            }
		}
		return  null;
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
		conn.setAutoCommit(true);

		return conn;
	}
}