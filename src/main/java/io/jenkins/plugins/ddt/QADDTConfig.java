package io.jenkins.plugins.ddt;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.Descriptor.FormException;

import jenkins.model.GlobalConfiguration;
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


@Extension
public class QADDTConfig extends GlobalConfiguration {
	
	private static String user;
	private static String pass;
	
	private static List<QADDTest> tests;
	
	public QADDTConfig() {
		if (QADDTAPI.getUsername() != null) {
			user = QADDTAPI.getUsername();
		}
		if (QADDTAPI.getPassword() != null) {
			pass = QADDTAPI.getPassword();
		}
		tests = new ArrayList<QADDTest>();
		load();
	}
	
	@SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "I want to override it every time")
	public String getUser() {
		return user;
	}
	
	@SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "I want to override it every time")
	public String getPass() {
		return pass;
	}
	
	@SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "I want to override it every time")
	public List<QADDTest> getTests() {
		System.out.println(">>>>!!!>>>>");
		System.out.println("tests: " + tests.toString());
		System.out.println("tests type: " + tests.getClass());
		System.out.println("<<<<!!!<<<<");
		
		return tests;
	}
	
	public static Map<String,String> getTestsMap() {
		Map<String,String> tmp_tests = new HashMap<String,String>();
		tmp_tests.put("", "Choose QA DDT Test");
		
		for (QADDTest tmp_test : tests) {
			tmp_tests.put(tmp_test.getUuid(), tmp_test.getName());
		}
		
		return tmp_tests;
	}
	
	public static QADDTest getTest(String uuid) {
		for (QADDTest tmp_test : tests) {
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
			
			if (!QADDTAPI.login(user, pass)) {
				return FormValidation.error(Messages.QADDT_DescriptorImpl_errors_wrongCredentials());
			}
			
			return FormValidation.ok("Success");
		} catch (Exception e) {
			return FormValidation.error("Validation error : " + e.getMessage());
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
			System.out.println("Error during form validation: " + e.getMessage());
		}
		
		if (check != null && check.renderHtml().indexOf("Success") != -1) {
			user = cur_user;
			pass = cur_pass;
			
			List<QADDTest> tmp_tests = new ArrayList<QADDTest>();
			try {
				JSONObject cur_test = formData.getJSONObject("tests");
				if (!cur_test.toString().equals("null")) {
					tmp_tests.add(setTest(cur_test));
				}
			} catch (JSONException e) {
				JSONArray cur_tests = formData.getJSONArray("tests");
				for (int i=0; i < cur_tests.size(); ++i) {
					JSONObject cur_test = cur_tests.getJSONObject(i);
					if (!cur_test.toString().equals("null")) {
						tmp_tests.add(setTest(cur_test));
					}
				}
			} finally {
				
				System.out.println(">>>>>>>>");
				System.out.println("tmp_tests: " + tmp_tests.toString());
				System.out.println("<<<<<<<<");
				
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
	
	private static QADDTest setTest(JSONObject test) {
		return new QADDTest(
			new String(test.getString("name")).replace("[^a-zA-Z0-9 \\-_=\\(\\)\\{\\}\\[\\]<>@\\+.$/\\\\]*", ""),
			new String(test.getString("uuid")).replace("[^a-zA-Z0-9\\-]*", ""),
			new String(test.getString("tags")).replace("[^a-zA-Z0-9,]*", "")
		);
	}
}