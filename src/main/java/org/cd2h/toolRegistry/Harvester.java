package org.cd2h.toolRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.cd2h.rppr.Generator;

public class Harvester {
    static Logger logger = Logger.getLogger(Harvester.class);
    static Connection conn = null;
    static enum Modes {
	BODY, NAME, SHORT, LONG
    };

    static Modes mode = Modes.BODY;

    public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
	PropertyConfigurator.configure(args[0]);
	Generator.initialize();
	conn = Generator.getConnection();
	
	PreparedStatement stmt = conn.prepareStatement("select id,github_owner,github_repo from cd2h_tool_registry.tool order by 1,2");
	ResultSet rs = stmt.executeQuery();
	while (rs.next()) {
	    int id = rs.getInt(1);
	    String owner = rs.getString(2);
	    String repo = rs.getString(3);
	    logger.info("id: " + id + "\towner: " + owner + "\trepo: " + repo);
		
	    process(id, owner, repo);

	}
	stmt.close();
    }
    
    static void process(int id, String owner, String repo) throws IOException, SQLException {
	String name = "";
	String short_description = "";
	String long_description = "";
	
	String content = Generator.fetch(owner, repo, "CD2H-tool-registry.md");
	logger.info("content: " + content);
	BufferedReader reader = new BufferedReader(new StringReader(content));
	String current = null;
	reader.mark(200);
	while ((current = reader.readLine()) != null) {
	    current = current.trim();
	    switch (mode) {
	    case BODY:
		logger.debug("current: " + current);
		switch (current) {
		case "# name":
		    mode = Modes.NAME;
		    break;
		case "# short description":
		    mode = Modes.SHORT;
		    break;
		case "# long description":
		    mode = Modes.LONG;
		    break;
		}
		break;
	    case NAME:
		if (current.startsWith("#")) {
		    name = name.trim();
		    // "push" this line back into the reader
		    reader.reset();
		    mode = Modes.BODY;
		} else {
		    logger.debug("\tname line: " + current);
		    name += " " + current;
		}
		break;
	    case SHORT:
		if (current.startsWith("#")) {
		    short_description = short_description.trim();
		    // "push" this line back into the reader
		    reader.reset();
		    mode = Modes.BODY;
		} else {
		    logger.debug("\tshort line: " + current);
		    short_description += (current.length() == 0 ? "\n" : " ") + current;
		}
		break;
	    case LONG:
		if (current.startsWith("#")) {
		    long_description = long_description.trim();
		    // "push" this line back into the reader
		    reader.reset();
		    mode = Modes.BODY;
		} else {
		    logger.debug("\tlong line: " + current);
		    long_description += (current.length() == 0 ? "\n" : " ") + current;
		}
		break;
	    }
	    // mark the input in case we end up in a non-BODY mode
	    reader.mark(200);
	}
	
	logger.info("name: " + name);
	logger.info("short_description: " + short_description);
	logger.info("long_description: " + long_description);
	
	PreparedStatement timeStmt = conn.prepareStatement("update cd2h_tool_registry.tool set last_visited = now(), name = ?, short_description = ?, long_description = ? where id = ?");
	timeStmt.setString(1, name);
	timeStmt.setString(2, short_description);
	timeStmt.setString(3, long_description);
	timeStmt.setInt(4, id);
	timeStmt.execute();
	timeStmt.close();
    }

}
