package org.cd2h.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Loader {
    static Logger logger = Logger.getLogger(Loader.class);
    static Properties prop_file = PropertyLoader.loadProperties("cd2h");
    static Connection conn = null;
    static enum MODE {PREFIX, BLANK, BODY};
    static MODE mode = MODE.PREFIX;
    static Pattern linePattern = Pattern.compile("^( *\\* )?(.*)$");
    
    static String hub = null;
    static String[] slot = new String[5];
    static int[] indent = new int[5];

    public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
	PropertyConfigurator.configure(args[0]);
	conn = getConnection();

	File root = new File("/Users/eichmann/ctsa_services");
	for (File hub : root.listFiles()) {
	    processHub(hub);
	}
    }
    
    static void processHub(File hubFile) throws IOException, SQLException {
	hub =  hubFile.getName();
	logger.info("hub: " + hub);

	PreparedStatement stmt = conn.prepareStatement("delete from ctsa_services.facet_base where hub=?");
	stmt.setString(1, hub);
	stmt.execute();
	stmt.close();

	BufferedReader theReader = new BufferedReader(new FileReader(hubFile));
	String buffer = null;
	while ((buffer = theReader.readLine()) != null) {
	    logger.debug("buffer: " + buffer);
	    switch (mode) {
	    case PREFIX:
		if (buffer.trim().length() == 0)
		    mode = MODE.BLANK;
		break;
	    case BLANK:
		if (buffer.trim().length() > 0) {
		    theReader.reset();
		    mode = MODE.BODY;
		}
		break;
	    case BODY:
		processLine(buffer);
		break;
	    }
	    theReader.mark(200);
	}
	theReader.close();
    }
    
    static void processLine(String line) throws SQLException {
	logger.info("line: " + line);
	Matcher lineMatcher = linePattern.matcher(line);
	if (lineMatcher.matches()) {
	    String prefix = (lineMatcher.group(1) == null ? "" : lineMatcher.group(1));
	    int currentIndent = prefix.length();
	    String content = lineMatcher.group(2);
	    logger.debug("\tindent: " + currentIndent + "\t" + content);
	    
	    int fence = 1;
	    while (currentIndent > 0 && indent[fence] > 0 && indent[fence] < currentIndent) {
		fence++;
	    }
	    clear(fence);
	    indent[fence] = currentIndent;
	    slot[currentIndent == 0 ? 0 : fence] = content;
	    logger.debug("\tfence: " + fence + "\tindent: " + Arrays.toString(indent));
	    logger.info("\t\tslot: " + Arrays.toString(slot));
	    
	    PreparedStatement stmt = conn.prepareStatement("insert into ctsa_services.facet_base(hub,slot0,slot1,slot2,slot3,slot4) values(?,?,?,?,?,?)");
	    stmt.setString(1, hub);
	    stmt.setString(2, slot[0]);
	    stmt.setString(3, slot[1]);
	    stmt.setString(4, slot[2]);
	    stmt.setString(5, slot[3]);
	    stmt.setString(6, slot[4]);
	    stmt.execute();
	    stmt.close();
	}
    }
    
    static void clear(int index) {
	for (int i = index; i <= 4; i++) {
	    slot[i] = null;
	    indent[i] = 0;
	}
    }

    static Connection getConnection() throws ClassNotFoundException, SQLException {
	Connection local = null;
	Properties props = new Properties();
	props.setProperty("user", prop_file.getProperty("jdbc.user"));
	props.setProperty("password", prop_file.getProperty("jdbc.password"));
	Class.forName("org.postgresql.Driver");
	local = DriverManager.getConnection(prop_file.getProperty("jdbc.url"), props);
	return local;
    }
}
