package org.cd2h.rppr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Base64.Decoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.cd2h.drive.util.GoogleAPI;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;

import edu.uiowa.slis.GitHubTagLib.util.LocalProperties;
import edu.uiowa.slis.GitHubTagLib.util.PropertyLoader;

@SuppressWarnings("serial")
public class Generator extends BodyTagSupport {
    static Logger logger = Logger.getLogger(Generator.class);
    protected static LocalProperties google_prop_file = null;
    protected static LocalProperties prop_file = null;
    static Connection conn = null;
    static String client_id = "";
    static String client_secret = "";
    static String APPLICATION_NAME = "CD2H Onboarding Sync";
    static List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
    static JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    static Decoder decoder = Base64.getMimeDecoder();
    static DecimalFormat formatter = new DecimalFormat("###,###,###"); 
    
    static enum Modes {
	BODY, MEMBERS, BUDGET, PROJECT
    };

    static Modes mode = Modes.BODY;
    static boolean showBudget = false;

    static Pattern projectPattern = Pattern.compile("^\\* \\[([^\\]]+)\\]\\(([^ \\)]+)\\)( +\\(\\[([^\\]]+)\\]\\(([^ \\)]+)\\)\\))?$");
    static List<List<Object>> sheet = null;

    public static void main(String[] args) throws IOException, GeneralSecurityException, ParseException, SQLException, ClassNotFoundException {
	PropertyConfigurator.configure(args[0]);
	initialize();
	showBudget = true; // presumably running on command line to generate docx
	
	if (showBudget)
	    loadBudgetSheet();
	System.out.print(generate("admin"));
	System.out.print(generate("informatics-maturity"));
	System.out.print(generate("next-gen-data-sharing"));
	System.out.print(generate("resource-discovery"));
	System.out.print(generate("software-cloud-infrastructure"));
    }
    
    static void initialize() throws ClassNotFoundException, SQLException {
	google_prop_file = PropertyLoader.loadProperties("google");
	prop_file = PropertyLoader.loadProperties("github");
	client_id = prop_file.getProperty("client_id");
	client_secret = prop_file.getProperty("client_secret");
	
	conn = getConnection();
    }

    String coreName = null;

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        Properties props = new Properties();
        props.setProperty("user", google_prop_file.getProperty("jdbc.user"));
        props.setProperty("password", google_prop_file.getProperty("jdbc.password"));
        // if (use_ssl.equals("true")) {
        // props.setProperty("sslfactory",
        // "org.postgresql.ssl.NonValidatingFactory");
        // props.setProperty("ssl", "true");
        // }
        Connection conn = DriverManager.getConnection(google_prop_file.getProperty("jdbc.url"), props);
        // conn.setAutoCommit(false);
        return conn;
    }

    public int doStartTag() {
	try {
	    initialize();
	    if (showBudget)
		loadBudgetSheet();
	    pageContext.getOut().print(pandocBridge(generate(coreName)));
	} catch (Exception e) {
	    logger.error("Exception raised: ", e);
	}
	return SKIP_BODY;
    }

    public String getCoreName() {
	return coreName;
    }

    public void setCoreName(String coreName) {
	this.coreName = coreName;
    }
    
    static void loadBudgetSheet() throws GeneralSecurityException, IOException, ParseException {
	final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
	final String spreadsheetId = google_prop_file.getProperty("budget.spreadsheetId");
	final String range = "fromKen!A1:M34";
	Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, GoogleAPI.getCredentials(HTTP_TRANSPORT, SCOPES, google_prop_file.getProperty("sheets.credentials"), google_prop_file.getProperty("sheets.tokens")))
		.setApplicationName(APPLICATION_NAME)
		.build();
	
	sheet = service.spreadsheets().values().get(spreadsheetId, range).execute().getValues();
	logger.debug("response: " + sheet);
	logger.debug("value: " + sheet.get(2).get(11));
	logger.debug("value: " + projectBudget("engagement"));
	logger.debug("value: " + projectBudget("operational-architecture"));
	logger.debug("value: " + projectBudget("idea-challenge"));
	logger.debug("value: " + coreBudget("Admin"));

    }
    
    static int projectBudget(String label) throws ParseException {
	String stringValue = null;
	switch (label) {
	case "engagement":
	    stringValue = (String) sheet.get(2).get(11);
	    break;
	case "operational-architecture":
	    stringValue = (String) sheet.get(3).get(11);
	    break;
	case "idea-challenge":
	    stringValue = (String) sheet.get(4).get(11);
	    break;
	case "governance-pathways":
	    stringValue = (String) sheet.get(5).get(11);
	    break;
	case "reusable-data":
	    stringValue = (String) sheet.get(6).get(11);
	    break;
	case "maturity-model":
	    stringValue = (String) sheet.get(7).get(11);
	    break;
	case "data-harmonization":
	    stringValue = (String) sheet.get(8).get(11);
	    break;
	case "loinc2hpo":
	    stringValue = (String) sheet.get(9).get(11);
	    break;
	case "hot-fhir":
	    stringValue = (String) sheet.get(10).get(11);
	    break;
	case "architecting_attribution":
	    stringValue = (String) sheet.get(11).get(11);
	    break;
	case "edu-harmonization":
	    stringValue = (String) sheet.get(12).get(11);
	    break;
	case "information-architecture":
	    stringValue = (String) sheet.get(13).get(11);
	    break;
	case "menRva":
	    stringValue = (String) sheet.get(14).get(11);
	    break;
	case "CTS-Personas":
	    stringValue = (String) sheet.get(15).get(11);
	    break;
	case "scits-platform":
	    stringValue = (String) sheet.get(16).get(11);
	    break;
	case "cloud-tool-architecture":
	    stringValue = (String) sheet.get(17).get(11);
	    break;
	case "leaf-edw":
	    stringValue = (String) sheet.get(18).get(11);
	    break;
	case "mortality-prediction":
	    stringValue = (String) sheet.get(19).get(11);
	    break;
	case "peer-review":
	    stringValue = (String) sheet.get(20).get(11);
	    break;
	default:
	    stringValue =  "-1";
	}
	return NumberFormat.getNumberInstance(java.util.Locale.US).parse(stringValue).intValue();
    }

    static int coreBudget(String label) throws ParseException {
	switch (label) {
	case "admin":
	    return projectBudget("engagement")+projectBudget("operational-architecture")+projectBudget("idea-challenge");
	case "informatics-maturity":
	    return projectBudget("governance-pathways")+projectBudget("reusable-data")+projectBudget("maturity-model");
	case "next-gen-data-sharing":
	    return projectBudget("data-harmonization")+projectBudget("loinc2hpo")+projectBudget("hot-fhir");
	case "resource-discovery":
	    return projectBudget("architecting-attribution")+projectBudget("educational-harmonization")+projectBudget("information-architecture")
	    		+ projectBudget("menRva")+projectBudget("personas")+projectBudget("science-of-translational-science-platform");
	case "software-cloud-infrastructure":
	    return projectBudget("cloud-tool-architecture")+projectBudget("leaf-edw")+projectBudget("mortality-prediction")+projectBudget("peer-review");
	default:
	    return -1;
	}
    }

    public static String generate(String repoName) throws IOException, ParseException, SQLException {
	StringBuffer buffer = new StringBuffer();
	String readme = fetch(repoName, "README.md");
	BufferedReader reader = new BufferedReader(new StringReader(readme));
	String current = null;
	reader.mark(200);
	while ((current = reader.readLine()) != null) {
	    switch (mode) {
	    case BODY:
		logger.debug("current: " + current);
		buffer.append(current + "\n");
		switch (current) {
		case "# Other Core Members":
		    mode = Modes.MEMBERS;
		    generateMembers(repoName, buffer);
		    break;
		case "# Year 3 Budget":
		    mode = Modes.BUDGET;
		    generateCoreBudget(repoName, buffer);
		    break;
		case "### Current Projects":
		    mode = Modes.PROJECT;
		    break;
		}
		break;
	    case MEMBERS:
		if (current.startsWith("#")) {
		    // "push" this line back into the reader
		    reader.reset();
		    mode = Modes.BODY;
		} else
		    logger.debug("\tskipping: " + current);
		break;
	    case BUDGET:
		if (current.startsWith("#")) {
		    // "push" this line back into the reader
		    reader.reset();
		    mode = Modes.BODY;
		} else
		    logger.debug("\tskipping: " + current);
		break;
	    case PROJECT:
		if (current.startsWith("#")) {
		    // "push" this line back into the reader
		    reader.reset();
		    mode = Modes.BODY;
		} else if (current.startsWith("*")) {
		    generateProject(current, buffer);
		} else {
		    logger.debug("current: " + current);
		    buffer.append(current + "\n");
		}
		break;
	    }
	    // mark the input in case we end up in a non-BODY mode
	    reader.mark(200);
	}
	return buffer.toString();
    }

    static void generateMembers(String repoName, StringBuffer buffer) throws SQLException {
	String previousInstitution = "";
	int core_id = 0;
	switch(repoName) {
	case "admin":
	    core_id = 1;
	    break;
	case "informatics-maturity":
	    core_id = 2;
	    break;
	case "next-gen-data-sharing":
	    core_id = 3;
	    break;
	case "resource-discovery":
	    core_id = 4;
	    break;
	case "software-cloud-infrastructure":
	    core_id = 5;
	    break;
	}
	PreparedStatement stmt = conn.prepareStatement("select distinct person.* from google.project,google.role,google.person where core_id=? and project.project_id=role.project_id and role.person_id=person.person_id and (role='Contributor' or role='Lead') order by institution,last_name");
	stmt.setInt(1, core_id);
	ResultSet rs = stmt.executeQuery();
	while (rs.next()) {
	    String institution = rs.getString(6);
	    String firstName = rs.getString(3);
	    String lastName = rs.getString(4);
	    if (!previousInstitution.equals(institution)) {
		buffer.append("\n" + institution + "\n");
		previousInstitution = institution;
	    }
	    buffer.append("* " + lastName + ", " + firstName + "\n");
	}
    }

    static void generateCoreBudget(String core, StringBuffer buffer) throws ParseException {
	if (showBudget)
	    buffer.append("$"+formatter.format(coreBudget(core))+"\n");
	else
	    buffer.append("suppressed until final release\n");
    }

    static void generateProjectBudget(String project, StringBuffer buffer) throws ParseException {
	if (showBudget)
	    buffer.append("$"+formatter.format(projectBudget(project))+"\n");
	else
	    buffer.append("suppressed until final release\n");
    }

    static void generateProject(String reference, StringBuffer buffer) throws IOException, ParseException {
	Matcher matcher = projectPattern.matcher(reference);
	if (matcher.matches()) {
	    String title = matcher.group(1);
	    String url = matcher.group(2);
	    String rppr_title = matcher.group(4);
	    String rppr_url = matcher.group(5);
	    logger.debug("project: " + title + " : " + url);
	    logger.debug("\tRPPR: " + rppr_title + " : " + rppr_url);
	    buffer.append("\n\n --- \n #### " + title + "\n --- \n\n");
	    if (rppr_title == null) {
		buffer.append("No RPPR link for project.\n");
	    } else {
		String rppr = fetch(rppr_url.replaceFirst("github.com/", "api.github.com/repos/").replaceFirst("blob/master", "contents"));
		logger.debug("RPPR: " + rppr);
		BufferedReader reader = new BufferedReader(new StringReader(rppr));
		String current = null;
		reader.mark(200);
		Modes projectMode = Modes.BODY;
		while ((current = reader.readLine()) != null) {
		    switch (projectMode) {
		    case BODY:
			logger.debug("current: " + current);
			buffer.append((current.length() > 0 && current.charAt(0) == '#' ? "####" : "") + current + "\n");
			switch (current) {
			case "# Budget":
			    projectMode = Modes.BUDGET;
			    generateProjectBudget(url.substring(url.lastIndexOf("/")+1),buffer);
			    break;
			}
			break;
		    case BUDGET:
			if (current.startsWith("#")) {
			    // "push" this line back into the reader
			    reader.reset();
			    projectMode = Modes.BODY;
			} else
			    logger.debug("\tskipping: " + current);
			break;
		    default:
			break;
		    }
		    // mark the input in case we end up in a non-BODY mode
		    reader.mark(200);
		}
	    }
	}
    }

    public static String fetch(String repoName, String fileName) {
	return fetch("https://api.github.com/repos/data2health/" + repoName + "/contents/" + fileName);
    }

    public static String fetch(String prefix) {
	String content = null;
	JSONObject container;
	try {
	    URL theURL = new URL(prefix + "?client_id=" + client_id
		    + "&client_secret=" + client_secret);
	    BufferedReader reader = new BufferedReader(new InputStreamReader(theURL.openConnection().getInputStream()));

	    container = new JSONObject(new JSONTokener(reader));
	    content = decode(container.getString("content"));
	} catch (Exception e) {
	    logger.error("exception:", e);
	}
	if (content != null)
	    content = content.replace('\u0000', ' ');
	logger.debug("readme: " + content);
	return content;
    }

    static String decode(String base64string) {
	return new String(decoder.decode(base64string));
    }

    static String pandocBridge(String input) {
	StringBuffer result = new StringBuffer();

	try {
	    logger.debug("spawning pandoc process...");
	    // spawn child process
	    Process lgProcess = Runtime.getRuntime().exec("/usr/local/bin/pandoc -f gfm -t html");

	    // initialize input and output streams
	    PrintWriter to = new PrintWriter(lgProcess.getOutputStream());
	    BufferedReader from = new BufferedReader(new InputStreamReader(lgProcess.getInputStream()));
	    to.print(input);
	    to.close();
	    String buffer = null;
	    while ((buffer = from.readLine()) != null) {
		result.append(buffer + "\n");
		logger.debug(buffer);
	    }
	    from.close();
	} catch (Exception e) {
	    logger.warn("Error spawning pandoc child process: " + e);
	}
	return result.toString();
    }
}
