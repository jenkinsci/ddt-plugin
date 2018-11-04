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
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import jenkins.tasks.SimpleBuildStep;

import java.io.IOException;
import javax.servlet.ServletException;
import org.jenkinsci.Symbol;

import java.util.*;

import hudson.model.Descriptor.FormException;
import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


public class QADDT extends Builder implements SimpleBuildStep {
	
	private final String name;
	private String tags;
	
	@DataBoundConstructor
	@SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "I want to override it every time and the Descriptor is final")
	public QADDT(String name, String tags) {
		this.name = name;
		this.tags = tags;
	}
	
	public String getName() {
		return name;
	}
	
	public String getTags() {
		// if (tags == null || tags.length() == 0) {
		// 	QADDTest tmp_test = QADDTConfig.getTest(name);
			
		// 	if (tmp_test != null) {
		// 		tags = tmp_test.getTags();
		// 	}
		// }
		
		return tags;
	}
	
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		listener.getLogger().println("Hello 22, " + name + " :: " + tags + "!");
		
		// QADDTAPI.test(name, tags);
	}
	
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		
		public ListBoxModel doFillNameItems(@QueryParameter("name") final String outer_name) {
			List<Option> options = new ArrayList();
			Map<String,String> tmp_tests = QADDTConfig.getTestsMap();
			
			if (outer_name.length() > 0 && QADDTConfig.getTest(outer_name) == null) {
				tmp_tests.put(outer_name, "Deprecated Test: " + outer_name);
			}
			
			for(Map.Entry<String,String> entry : tmp_tests.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				boolean is_selected = false;
				
				if (outer_name != null && key != null && outer_name.equals(key)) {
					is_selected = true;
				}
				
				options.add(new Option(value, key, is_selected));
			}
			
			return new ListBoxModel(options);
		}
		
		public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
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
