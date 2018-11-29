package io.jenkins.plugins.ddt;

import hudson.Extension;
import hudson.BulkChange;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.model.Descriptor.FormException;

import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.IOException;
import javax.servlet.ServletException;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.lang.management.ManagementFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This class represents a "config" of the "ddt" package.
 * @author Evgeny Kolyakov
 */
@Extension
public class QADDTConfig extends GlobalConfiguration {
	
	private String user;
	private Secret pass;
	
	private List<QADDTest> tests;
	
	/**
	 * The constructor initializes the tests field (because it's special) and loads the data.
	 */
	public QADDTConfig() {
		tests = new ArrayList<>();
		pass = Secret.fromString("");
		load();
	}
	
	/**
	 * Getter for the user field.
	 * @return {String} Returns the username for the QADDTAPI.
	 */
	public String getUser() {
		return user;
	}
	
	/**
	 * Getter for the pass field.
	 * @return {String} Returns the (encrypted) password for the QADDTAPI.
	 */
	public String getPass() {
		return pass.getEncryptedValue();
	}
	
	/**
	 * Getter for the tests field.
	 * @return {String} Returns the list of all the saved (parent) "tests".
	 */
	public List<QADDTest> getTests() {
		return tests;
	}
	
	/**
	 * Get the list of all the "tests" as a key-value pairs.
	 * @return {Map} Returns the map of (uuid, name) entities.
	 */
	public static Map<String,String> getTestsMap() {
		Map<String,String> tmp_tests = new HashMap<>();
		tmp_tests.put("", "Choose QA DDT Test");
		
		for (QADDTest tmp_test : QADDTConfig.get().tests) {
			tmp_tests.put(tmp_test.getUuid(), tmp_test.getName());
		}
		
		return tmp_tests;
	}
	
	/**
	 * Get a "test" by UUID.
	 * @param uuid {String} The UUID to search by.
	 * @return {QADDTest} Return the "test" if found, otherwise, null.
	 */
	public static QADDTest getTest(String uuid) {
		for (QADDTest tmp_test : QADDTConfig.get().tests) {
			if (tmp_test.getUuid().equals(uuid)) {
				return tmp_test;
			}
		}
		
		return null;
	}
	
	/**
	 * Handler for the "Test Connection" button/feature in Jenkins - Configuration - QADDT (section)
	 * @param username {String} The username to test.
	 * @param password {String} The password to test.
	 * @return {FormValidation} Returns ok() or error() according to the successful login() into the API.
	 * @throws IOException This comes from inheritance.
	 * @throws ServletException This comes from inheritance.
	 */
	public FormValidation doTestConnection(@QueryParameter("user") final String username,
			@QueryParameter("pass") final String password) throws IOException, ServletException {
		
		try {
			if (username == null || username.length() == 0) {
				return FormValidation.warning(Messages.QADDT_DescriptorImpl_warning_missingUser());
			}
			if (password == null || password.length() == 0) {
				return FormValidation.warning(Messages.QADDT_DescriptorImpl_warning_missingPass());
			}
			
			Secret _password = Secret.fromString(password);
			
			QADDTAPI api = new QADDTAPI();
			if (!api.login(username, _password.getEncryptedValue())) {
				return FormValidation.error(Messages.QADDT_DescriptorImpl_errors_wrongCredentials());
			}
			api.logout();
			
			return FormValidation.ok("Success");
		} catch (Exception e) {
			String msg = e.getMessage();
			if (msg == null) {
				return FormValidation.error("Lost connection to the server! Please check you internet connectivity.");
			}
			return FormValidation.error("Validation error: " + msg);
		}
	}

	/**
	 * Jenkins configuration function responsible for configuring the plugin.
	 * @param req {StaplerRequest} The request we got.
	 * @param formData {JSONObject} The object containing the form data.
	 * @return {boolean} Return true if validation and save() where successful, otherwise, it throws an exception.
	 * @throws FormException This is needed to stop/cancel saving upon failure during validation.
	 */
	@Override
	@SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "I want to override it every time")
	public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
		formData = formData.getJSONObject("credentials");
		String cur_user = formData.getString("user");
		String cur_pass = formData.getString("pass");
		boolean success = false;
		FormValidation check = null;
		
		try {
			check = doTestConnection(cur_user, cur_pass);
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error during form validation", e);
		}
		
		if (check != null && check.renderHtml().contains("Success")) {
			if (!getPass().equals(cur_pass)) {
				Secret passer = Secret.fromString(cur_pass);
			}
			
			user = cur_user;
			pass = Secret.fromString(cur_pass);
			
			List<QADDTest> tmp_tests = new ArrayList<>();
			try {
				tmp_tests = setTest(tmp_tests, formData.getJSONObject("tests"));
			} catch (JSONException e) {
				JSONArray cur_tests = formData.getJSONArray("tests");
				for (int i=0; i < cur_tests.size(); ++i) {
					tmp_tests = setTest(tmp_tests, cur_tests.getJSONObject(i));
				}
			} finally {
				tests = tmp_tests;
			}
			
			save();
			
			success = true;
		}
		
		if (!success) {
			throw new FormException(Messages.QADDT_DescriptorImpl_errors_wrongCredentials(), "credentials.user");
		}
		
		return super.configure(req, formData);
	}
	
	/**
	 * This is a helper function for configure() for validating, sanitizing and setting "tests".
	 * @param tmp_tests {List} The list of "tests" to which to append the new "test" after successful validation.
	 * @param test {JSONObject} The potential data to check.
	 * @return {List} Returns the list of the tests we got, but maybe with a new "test".
	 */
	private static List<QADDTest> setTest(List<QADDTest> tmp_tests, JSONObject test) {
		if (!test.toString().equals("null") && test.getString("uuid").length() > 0) {
			tmp_tests.add(new QADDTest(
				test.getString("uuid").replaceAll("[^a-zA-Z0-9\\-]*", ""),
				test.getString("name").replaceAll("[^a-zA-Z0-9 \\-_=\\(\\)\\{\\}\\[\\]<>@\\+.$/\\\\]*", ""),
				test.getString("tags").replaceAll("[^a-zA-Z0-9,]*", "")
			));
		}
		
		return tmp_tests;
	}

	/**
	 * This is a static method to get the "config" instance.
	 * @return {QADDTConfig} Returns the (one and only) running "config" instance.
	 */
	public static QADDTConfig get() {
		return (QADDTConfig) Jenkins.getInstance().getDescriptorOrDie(QADDTConfig.class);
	}

	/**
	 * This is an override for Jenkins save() for adding custom logs.
	 */
	@Override
	public synchronized void save() {
		if(BulkChange.contains(this)) {
			LOGGER.log(Level.WARNING, "Bulked...");
			return;
		}
		
		try {
			getConfigFile().write(this);
			LOGGER.log(Level.INFO, "Successfully saved " + getConfigFile());
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to save " + getConfigFile(), e);
		}
	}

	/**
	 * This is for Jenkins to display the "QADDT" title in the header of the build step in Jenkins - Job - Configuration.
	 * @return {String} Returns the constant string from the "dictionary" "QADDT".
	 */
	@Override
	public String getDisplayName() {
		return Messages.QADDT_DescriptorImpl_DisplayName();
	}

	private static final Logger LOGGER = Logger.getLogger(QADDTConfig.class.getName());
}