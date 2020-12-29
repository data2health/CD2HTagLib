package org.cd2h.clic;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.cd2h.services.LocalProperties;
import org.cd2h.services.PropertyLoader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HubHarvester {
    static Logger logger = Logger.getLogger(HubHarvester.class);
    static Connection conn = null;
    protected static LocalProperties prop_file = null;

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		PropertyConfigurator.configure(args[0]);
		prop_file = PropertyLoader.loadProperties("clic");
		conn = getConnection();

		Document doc = Jsoup.connect("https://clic-ctsa.org/ctsa-program-hub-directory%3Ffield_hub_fiscal_year_value%3D2020%26title%3D").timeout(0).get();
		logger.trace("doc: " + doc);
		for (Element element : doc.getElementsByClass("hub")) {
			logger.debug("hub: " + element);
		    String name = element.getElementsByClass("views-field-title").text().trim();
		    String href = element.getElementsByClass("views-field-title").first().getElementsByTag("a").first().attr("href").trim();
		    String lat_long = element.getElementsByTag("div").first().attr("data-lat-long").trim();
		    String nid = element.getElementsByTag("div").first().attr("data-nid").trim();
		    logger.info("hub: " + name);
		    logger.info("\tclic url: " + href);
		    logger.info("\tlat-long: " + lat_long);
		    logger.info("\tnid: " + nid);
		    
		    fetchHub(href, lat_long, nid);
		}
	}
	
	static void fetchHub(String url, String lat_long, String nid) throws IOException, SQLException {
		Document doc = Jsoup.connect("https://clic-ctsa.org"+url).timeout(0).get();
		logger.debug("doc: " + doc);
	    String name = doc.getElementsByClass("title").text().trim();
	    logger.info("title: " + name);
	    
	    fetchBlock(doc, "field--name-field-organization");
	    fetchBlock(doc, "field--name-field-hub-mission-statement");
	    fetchBlock(doc, "field--name-field-hub-url");
	    
	    PreparedStatement hubStmt = conn.prepareStatement("insert into clic_directory.hub values(?,?,?,?,?,?)");
	    hubStmt.setString(1, nid);
	    hubStmt.setString(2, name);
	    hubStmt.setString(3, fetchBlock(doc, "field--name-field-organization"));
	    hubStmt.setString(4, fetchBlock(doc, "field--name-field-hub-mission-statement"));
	    hubStmt.setString(5, fetchBlock(doc, "field--name-field-hub-url"));
	    hubStmt.setString(6, lat_long);
	    hubStmt.execute();
	    hubStmt.close();

		if (doc.getElementsByClass("field--name-field-other-organizations").first() != null) {
			for (Element element : doc.getElementsByClass("field--name-field-other-organizations").first().getElementsByClass("field__item")) {
				String title = element.text().trim();
				logger.info("other org: " + title);
			    PreparedStatement orgStmt = conn.prepareStatement("insert into clic_directory.other_organization values(?,?)");
			    orgStmt.setString(1, nid);
			    orgStmt.setString(2, title);
			    orgStmt.execute();
			    orgStmt.close();
			}
		}

		if (doc.getElementsByClass("social-link-field").first() != null) {
			for (Element element : doc.getElementsByClass("social-link-field").first().getElementsByTag("a")) {
				String href = element.attr("href");
				String title = element.attr("title");
				logger.info("social: " + title + " : " + href);
			    PreparedStatement socStmt = conn.prepareStatement("insert into clic_directory.social values(?,?,?)");
			    socStmt.setString(1, nid);
			    socStmt.setString(2, title);
			    socStmt.setString(3, href);
			    socStmt.execute();
			    socStmt.close();
			}
		}
	    
		if (doc.getElementsByClass("field--name-field-degree-and-certificate-pro").first() != null) {
			for (Element element : doc.getElementsByClass("field--name-field-degree-and-certificate-pro").first().getElementsByClass("paragraph--type-degree-and-certificate-programs")) {
				String href = element.getElementsByTag("a").attr("href").trim();
				String tag = element.getElementsByTag("a").text().trim();
				String progname = element.getElementsByClass("field--name-field-program-name").text().trim();
				String progtype = element.getElementsByClass("field--name-field-program-type").text().trim();
				logger.info("degree program: " + progtype + " : " + progname);
				logger.info("\t" + href + " : " + tag);
			    PreparedStatement degStmt = conn.prepareStatement("insert into clic_directory.degree values(?,?,?,?,?)");
			    degStmt.setString(1, nid);
			    degStmt.setString(2, progtype);
			    degStmt.setString(3, progname);
			    degStmt.setString(4, href);
			    degStmt.setString(5, tag);
			    degStmt.execute();
			    degStmt.close();
			}
		}
	    
	    fetchBlocks(doc, nid, "field--name-field-hub-ul1-director");
	    fetchBlocks(doc, nid, "field--name-field-hub-ul1-administrator");
	    fetchBlocks(doc, nid, "field--name-field-hub-kl2-director");
	    fetchBlocks(doc, nid, "field--name-field-hub-kl2-adminstrator");
	    fetchBlocks(doc, nid, "field--name-field-hub-tl1-director");
	    fetchBlocks(doc, nid, "field--name-field-hub-tl1-adminstrator");
	    fetchBlocks(doc, nid, "field--name-field-hub-comm-rep");
	    fetchBlocks(doc, nid, "field--name-field-hub-meth-and-process-dtf");
	    fetchBlocks(doc, nid, "field--name-field-hub-wokforce-dev-dtf");
	    fetchBlocks(doc, nid, "field--name-field-hub-primary-cm-contact");
	    fetchBlocks(doc, nid, "field--name-field-hub-addtl-cm-contacts");
	    fetchBlocks(doc, nid, "field--name-field-hub-collab-engage-dtf");
	    fetchBlocks(doc, nid, "field--name-field-hub-ial-dtf-rep");
	    fetchBlocks(doc, nid, "field--name-field-hub-informatics-dtf");
	}
	
	static String fetchBlock(Document doc, String block) {
	    Element element = doc.getElementsByClass(block).first();
	    if (element == null)
	    	return null;
	    String label = element.getElementsByClass("field__label").first().text().trim();
	    String item = element.getElementsByClass("field__item").first().text().trim();
	    logger.info("label: " + label + "\titem: " + item);
	    return item;
	}

	static void fetchBlocks(Document doc, String nid, String block) throws SQLException {
	    Element element = doc.getElementsByClass(block).first();
	    if (element == null)
	    	return;
	    String label = element.getElementsByClass("field__label").first().text().trim();
	    for (Element item : element.getElementsByClass("user")) {
	    	String first_name = getOptString(item, "field--name-field-first-name");
	    	String last_name = getOptString(item, "field--name-field-last-name");
	    	String title = getOptString(item, "field--name-field-job-title");
	    	String organization = getOptString(item, "field--name-field-organization");
	    	String pictureURL = getOptImage(item, "field--name-user-picture");
	    	logger.info("person: " + label + " : " + first_name + " " + last_name);
	    	logger.info("\t" + title + " : " + organization);
	    	logger.info("\t" + pictureURL);
		    PreparedStatement perStmt = conn.prepareStatement("insert into clic_directory.personnel values(?,?,?,?,?,?,?)");
		    perStmt.setString(1, nid);
		    perStmt.setString(2, label);
		    perStmt.setString(3, first_name);
		    perStmt.setString(4, last_name);
		    perStmt.setString(5, title);
		    perStmt.setString(6, organization);
		    perStmt.setString(7, pictureURL);
		    perStmt.execute();
		    perStmt.close();
	    }
	}
	
	static String getOptString(Element item, String field) {
		Elements elements = item.getElementsByClass(field);
		if (elements == null)
			return null;
		Element element = elements.first();
		if (element == null)
			return null;
		return element.text().trim();
	}

	static String getOptImage(Element item, String field) {
		Elements elements = item.getElementsByClass(field);
		if (elements == null)
			return null;
		Element element = elements.first();
		if (element == null)
			return null;
		return element.getElementsByTag("img").attr("src").trim();
	}

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        Properties props = new Properties();
        props.setProperty("user", prop_file.getProperty("jdbc.user"));
        props.setProperty("password", prop_file.getProperty("jdbc.password"));
        // if (use_ssl.equals("true")) {
        // props.setProperty("sslfactory",
        // "org.postgresql.ssl.NonValidatingFactory");
        // props.setProperty("ssl", "true");
        // }
        Connection conn = DriverManager.getConnection(prop_file.getProperty("jdbc.url"), props);
        // conn.setAutoCommit(false);
        return conn;
    }
}
