package io.jenkins.plugins.ddt;

import hudson.Extension;
import hudson.BulkChange;
import hudson.util.FormValidation;
import hudson.model.Descriptor.FormException;

import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

// import hudson.model.AbstractDescribableImpl;
// import hudson.model.Descriptor;
// import org.kohsuke.stapler.export.Exported;
// import org.kohsuke.stapler.export.ExportedBean;

// import java.io.Serializable;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;

import java.io.IOException;
import javax.servlet.ServletException;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


@Extension
public class QADDTConfig extends GlobalConfiguration {
	
	private String user;
	private String pass;
	
	private List<QADDTest> tests;
	
	public QADDTConfig() {
		// if (QADDTAPI.getUsername() != null) {
		// 	user = QADDTAPI.getUsername();
		// }
		// if (QADDTAPI.getPassword() != null) {
		// 	pass = QADDTAPI.getPassword();
		// }
		tests = new ArrayList<QADDTest>();
		load();
	}
	
	public String getUser() {
		return user;
	}
	
	public String getPass() {
		return pass;
	}
	
	public List<QADDTest> getTests() {
		return tests;
	}
	
	public static Map<String,String> getTestsMap() {
		Map<String,String> tmp_tests = new HashMap<String,String>();
		tmp_tests.put("", "Choose QA DDT Test");
		
		for (QADDTest tmp_test : QADDTConfig.get().tests) {
			tmp_tests.put(tmp_test.getUuid(), tmp_test.getName());
		}
		
		return tmp_tests;
	}
	
	public static QADDTest getTest(String uuid) {
		for (QADDTest tmp_test : QADDTConfig.get().tests) {
			if (tmp_test.getUuid().equals(uuid)) {
				return tmp_test;
			}
		}
		
		return null;
	}
	
	public FormValidation doTestConnection(@QueryParameter("user") final String user,
			@QueryParameter("pass") final String pass) throws IOException, ServletException {
		
		try {
			if (user == null || user.length() == 0) {
				return FormValidation.warning(Messages.QADDT_DescriptorImpl_warning_missingUser());
			}
			if (pass == null || pass.length() == 0) {
				return FormValidation.warning(Messages.QADDT_DescriptorImpl_warning_missingPass());
			}
			
			QADDTAPI api = new QADDTAPI();
			if (!api.login(user, pass)) {
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

	@Override
	@SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "I want to override it every time")
	public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
		req.bindParameters(this);
		
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
		
		if (check != null && check.renderHtml().indexOf("Success") != -1) {
			user = cur_user;
			pass = cur_pass;
			
			
			// try {
			// 	tests = req.bindJSONToList(QADDTest.class, formData.get("tests"));
			// 	success = true;
			// } catch (JSONException e) {
			// 	LOGGER.log(Level.SEVERE, "Error during form validation", e);
			// }
			
			List<QADDTest> tmp_tests = new ArrayList<QADDTest>();
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

	@Override
	public String getDisplayName() {
		return Messages.QADDT_DescriptorImpl_DisplayName();
	}
	
	private static List<QADDTest> setTest(List<QADDTest>tmp_tests, JSONObject test) {
		if (!test.toString().equals("null") && test.getString("uuid").length() > 0) {
			tmp_tests.add(new QADDTest(
				test.getString("uuid").replaceAll("[^a-zA-Z0-9\\-]*", ""),
				test.getString("name").replaceAll("[^a-zA-Z0-9 \\-_=\\(\\)\\{\\}\\[\\]<>@\\+.$/\\\\]*", ""),
				test.getString("tags").replaceAll("[^a-zA-Z0-9,]*", "")
			));
		}
		
		return tmp_tests;
	}

	public static QADDTConfig get() {
		return (QADDTConfig) Jenkins.getInstance().getDescriptorOrDie(QADDTConfig.class);
	}

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

	private static final Logger LOGGER = Logger.getLogger(QADDTConfig.class.getName());
}