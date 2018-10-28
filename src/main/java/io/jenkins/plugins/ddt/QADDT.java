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
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;


public class QADDT extends Builder implements SimpleBuildStep {
	
	private final String name;
	private final String tags;
	
	private String qaddtUser;
	private String qaddtPass;
	
	private static Map<String, String> saved_tests;
	
	private static String outer_name;
	
	@DataBoundConstructor
	public QADDT(String name, String tags) {
		this.name = name;
		this.outer_name = name;
		this.tags = tags;
		
		// this.saved_tests = new HashMap<String,String>();
		// this.saved_tests.put("9fc88d0f08dbd81c5ad3ee4524f800a18b1f5178", "Test #1");
		// this.saved_tests.put("f6a2a5a3353e62c2ba7563362b179cd2f7efb92f", "Test #2");
		// this.saved_tests.put("42c8f3198db0c9986b263e023cab7cc6556eabfc", "Test #13");
	}
	
	public String getName() {
		return name;
	}
	
	public String getTags() {
		return tags;
	}
	
	public String getQaddtUser() {
		return qaddtUser;
	}

	public String getQaddtPass() {
		return qaddtPass;
	}
	
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		listener.getLogger().println("Hello 22, " + name + " :: " + tags + "!");
	}
	
	@Symbol("greet")
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		
		public ListBoxModel doFillNameItems() {
			List<Option> options = new ArrayList();
		
			saved_tests = new HashMap<String,String>();
			saved_tests.put("9fc88d0f08dbd81c5ad3ee4524f800a18b1f5178", "Test #1");
			saved_tests.put("f6a2a5a3353e62c2ba7563362b179cd2f7efb92f", "Test #2");
			saved_tests.put("42c8f3198db0c9986b263e023cab7cc6556eabfc", "Test #13");
			
			for(Map.Entry<String,String> entry : saved_tests.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				boolean is_selected = false;
				
				if (outer_name == key) {
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
		
		public FormValidation doTestConnection(@QueryParameter("qaddtUser") final String user,
				@QueryParameter("qaddtPass") final String pass) throws IOException, ServletException {
			
			System.out.println(" >> qaddtUser: " + user);
			System.out.println(" >> qaddtPass: " + pass);
			
			try {
				if (user == null || user.length() == 0) {
					return FormValidation.error(Messages.QADDT_DescriptorImpl_warnings_wrongSymbol());
				}
				if (pass == null || pass.length() == 0) {
					return FormValidation.error(Messages.QADDT_DescriptorImpl_warnings_wrongSymbol());
				}
				
				// TODO: Test connection
				
				return FormValidation.ok("Success");
			} catch (Exception e) {
				return FormValidation.error("Validation error : " + e.getMessage());
			}
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
