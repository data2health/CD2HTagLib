package org.cd2h.ohdsi;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.cd2h.JSONTagLib.util.LocalProperties;
import org.cd2h.JSONTagLib.util.PropertyLoader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Parser {
	static Connection theConnection = null;
	static LocalProperties prop_file = null;
	protected static final Log logger = LogFactory.getLog(Parser.class);

	@SuppressWarnings("resource")
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		PropertyConfigurator.configure(args[0]);
		prop_file = PropertyLoader.loadProperties("ohdsi.properties");
		theConnection = getConnection();
		
		simpleStmt("truncate ohdsi.domain_changes");
		simpleStmt("truncate ohdsi.new_concepts_grouped");
		simpleStmt("truncate ohdsi.standard_concept_changes");
		simpleStmt("truncate ohdsi.new_concepts");
		simpleStmt("truncate ohdsi.change_of_concept_mapping_grouped");
		simpleStmt("truncate ohdsi.new_vocabulary");
		
		PreparedStatement stmt = theConnection.prepareStatement("select raw->>'name', raw->>'body' from ohdsi.raw");
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			String release = rs.getString(1);
			String body = rs.getString(2);
			logger.info("parsing release " + release);
			logger.debug("parsing release " + body);
			
			Document document = Jsoup.parse(body);
			for (Element table : document.getElementsByTag("table")) {
				logger.info("table: " + table);
				boolean first = true;
				boolean domain_id = false;
				boolean old_domain_id = false;
				boolean old_standard_concept = false;
				boolean new_standard_concept = false;
				boolean new_vocab = false;
				for (Element row : table.getElementsByTag("tr")) {
					logger.debug("row: " + row);
					if (first) {
						if (row.getElementsByTag("th").size() == 1)
							new_vocab = true;
						for (Element header : row.getElementsByTag("th")) {
							switch (header.text()) {
							case "domain_id":
								domain_id = true;
								break;
							case "old_domain_id":
								old_domain_id = true;
								break;
							case "old_standard_concept":
								old_standard_concept = true;
								break;
							case "new_standard_concept":
								new_standard_concept = true;
								break;
							default:
							}
						}
					} else {
						logger.debug("domain_id: " + domain_id);
						logger.debug("old_domain_id: " + old_domain_id);
						logger.debug("old_standard_concept: " + old_standard_concept);
						logger.debug("new_standard_concept: " + new_standard_concept);
						logger.debug("new_vocab: " + new_vocab);
						
						if (old_domain_id) {
							logger.info("domain change table");
							PreparedStatement insStmt = theConnection.prepareStatement("insert into ohdsi.domain_changes values(?,?,?,?,?)");
							insStmt.setString(1, release);
							insStmt.setString(2, row.child(0).text());
							insStmt.setString(3, row.child(1).text());
							insStmt.setString(4, row.child(2).text());
							insStmt.setInt(5, new Scanner(row.child(3).text()).useDelimiter("[^\\d]+").nextInt());
							insStmt.execute();
							insStmt.close();
						} else if (old_standard_concept) {
							logger.info("standard concept change table");
							PreparedStatement insStmt = theConnection.prepareStatement("insert into ohdsi.standard_concept_changes values(?,?,?,?,?)");
							insStmt.setString(1, release);
							insStmt.setString(2, row.child(0).text());
							insStmt.setString(3, row.child(1).text());
							insStmt.setString(4, row.child(2).text());
							insStmt.setInt(5, new Scanner(row.child(3).text()).useDelimiter("[^\\d]+").nextInt());
							insStmt.execute();
							insStmt.close();
						} else if (domain_id) {
							logger.info("new concept table");
							PreparedStatement insStmt = theConnection.prepareStatement("insert into ohdsi.new_concepts_grouped values(?,?,?,?)");
							insStmt.setString(1, release);
							insStmt.setString(2, row.child(0).text());
							insStmt.setString(3, row.child(1).text());
							insStmt.setInt(4, new Scanner(row.child(2).text()).useDelimiter("[^\\d]+").nextInt());
							insStmt.execute();
							insStmt.close();
						} else if (new_standard_concept) {
							logger.info("new standard concept table");
							PreparedStatement insStmt = theConnection.prepareStatement("insert into ohdsi.new_concepts values(?,?,?,?)");
							insStmt.setString(1, release);
							insStmt.setString(2, row.child(0).text());
							insStmt.setString(3, row.child(1).text());
							insStmt.setInt(4, new Scanner(row.child(2).text()).useDelimiter("[^\\d]+").nextInt());
							insStmt.execute();
							insStmt.close();
						} else if (new_vocab) {
							logger.info("new vocab table");							
							PreparedStatement insStmt = theConnection.prepareStatement("insert into ohdsi.new_vocabulary values(?,?)");
							insStmt.setString(1, release);
							insStmt.setString(2, row.child(0).text());
							insStmt.execute();
							insStmt.close();
						} else {
							logger.info("concept mapping status table");							
							PreparedStatement insStmt = theConnection.prepareStatement("insert into ohdsi.change_of_concept_mapping_grouped values(?,?,?,?,?)");
							insStmt.setString(1, release);
							insStmt.setString(2, row.child(0).text());
							insStmt.setString(3, row.child(1).text());
							insStmt.setString(4, row.child(2).text());
							insStmt.setInt(5, new Scanner(row.child(3).text()).useDelimiter("[^\\d]+").nextInt());
							insStmt.execute();
							insStmt.close();
						}
					}
					
					first = false;
				}
			}
		}
		stmt.close();

	}

	public static Connection getConnection() throws SQLException, ClassNotFoundException {
		Class.forName("org.postgresql.Driver");
		Properties props = new Properties();
		props.setProperty("user", prop_file.getProperty("jdbc.user"));
		props.setProperty("password", prop_file.getProperty("jdbc.password"));
		Connection conn = DriverManager.getConnection(prop_file.getProperty("jdbc.url"), props);
		return conn;
	}

	public static void simpleStmt(String queryString) {
		try {
			logger.debug("executing " + queryString + "...");
			PreparedStatement beginStmt = theConnection.prepareStatement(queryString);
			beginStmt.executeUpdate();
			beginStmt.close();
		} catch (Exception e) {
			logger.error("Error in database initialization: " + e);
			e.printStackTrace();
		}
	}
}
