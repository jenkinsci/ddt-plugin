package io.jenkins.plugins.ddt;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Descriptor.FormException;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
// import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
// import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;

// import org.junit.Assert.assertEquals;

import net.sf.json.JSONObject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


public class QADDTStep extends Builder implements SimpleBuildStep {
	
	private final String parent_uuid;
	private String tags;
	private String uuid;
	
	@DataBoundConstructor
	@SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "I want to override it every time and the Descriptor is final")
	public QADDTStep(String parent_uuid, String tags, String uuid) {
		this.parent_uuid = parent_uuid;
		this.tags = tags;
		this.uuid = uuid;
	}
	
	public String getParent_uuid() {
		return parent_uuid;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public String getTags() {
		if (tags == null || tags.length() == 0) {
			QADDTest tmp_test = QADDTConfig.getTest(parent_uuid);
			
			if (tmp_test != null) {
				tags = tmp_test.getTags();
			}
		} else {
			tags = "none";
		}
		
		return tags;
	}
	
	// @JavaScriptMethod
	// public void setUITags(String tags) {
	// 	this.tags = tags;
	// }
	
	// @Test
	// public void testLogic() {
	// 	boolean success = false;
	// 	PrintStream cur_log = new PrintStream();
		
	// 	try {
	// 		parent_uuid = "93a72a541c29aed27b59155266b7f04f1a6bd89df23dc434e471f5eb6c818050";
	// 		tags = "test";
	// 		logic(cur_log);
	// 		success = true;
	// 	} catch (Exception e) {
	// 		cur_log.println("Failed logic!!!");
	// 	}
		
	// 	assertEquals(true, success);
	// }
	
	private void logic(PrintStream cur_log) throws InterruptedException {
		QADDTAPI api = new QADDTAPI();
		
		cur_log.println("Initializing test: " + parent_uuid + "(" + tags + ")");
		
		uuid = api.test(parent_uuid, tags);
		
		if (uuid == null) {
			throw new InterruptedException("Failed initialization!!!");
		}
		
		cur_log.println("Waiting for test '" + uuid + "' to start");
		
		if (!api.poll(uuid, "tasks.txt", "text/plain", 125)) {
			throw new InterruptedException("UI test '" + uuid + "' didn't start properly");
		}
		
		cur_log.println("Testing...");
		
		if (!api.poll(uuid, "report.xml", "text/xml", 17280)) {
			throw new InterruptedException("UI test '" + uuid + "' failed.");
		}
		
		api.logout();
		
		cur_log.println("Done test: '" + uuid + "'");
	}
	
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		logic(listener.getLogger());
	}
	
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		
		public ListBoxModel doFillParent_uuidItems(@QueryParameter("parent_uuid") final String outer_name) {
			List<Option> options = new ArrayList();
			Map<String,String> tmp_tests = QADDTConfig.getTestsMap();
			
			if (outer_name.length() > 0 && QADDTConfig.getTest(outer_name) == null) {
				tmp_tests.put(outer_name, "Deprecated Test: " + outer_name);
			}
			
			for(Map.Entry<String,String> entry : tmp_tests.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				boolean is_selected = false;
				
				if (key != null && outer_name.equals(key)) {
					is_selected = true;
				}
				
				options.add(new Option(value, key, is_selected));
			}
			
			return new ListBoxModel(options);
		}
		
		public FormValidation doCheckParent_uuid(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error(Messages.QADDT_DescriptorImpl_errors_missingName());
			}
			// if (value.length() < 4) {
			// 	return FormValidation.warning(Messages.QADDT_DescriptorImpl_warnings_tooShort());
			// }
			return FormValidation.ok();
		}
		
		public FormValidation doCheckTags(@QueryParameter String value) throws IOException, ServletException {
			if (value != null && value.length() > 0 && value.matches(".*[^0-9a-zA-Z_,;].*")) {
				return FormValidation.error(Messages.QADDT_DescriptorImpl_warnings_wrongSymbol());
			}
			return FormValidation.ok();
		}
		
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}
		
		@Override
		public String getDisplayName() {
			return Messages.QADDT_DescriptorImpl_DisplayName();
		}
	}
}
