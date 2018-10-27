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
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

public class HelloWorldBuilder extends Builder implements SimpleBuildStep {
	
	private final String name;
	private final String tags;
	
	@DataBoundConstructor
	public HelloWorldBuilder(String name, String tags) {
		this.name = name;
		this.tags = tags;
	}
	
	public String getName() {
		return name;
	}
	
	public String getTags() {
		return tags;
	}
	
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		listener.getLogger().println("Hello 22, " + name + " :: " + tags + "!");
	}
	
	@Symbol("greet")
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		
		public ListBoxModel doFillNameItems() {
			return new ListBoxModel(
				new Option("Test #1", "9fc88d0f08dbd81c5ad3ee4524f800a18b1f5178", true),
				new Option("Test #2", "f6a2a5a3353e62c2ba7563362b179cd2f7efb92f", false),
				new Option("Test #13", "42c8f3198db0c9986b263e023cab7cc6556eabfc", false)
			);
		}
		
		public FormValidation doCheckName(@QueryParameter String value, @QueryParameter String tag) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error(Messages.HelloWorldBuilder_DescriptorImpl_errors_missingName());
			if (value.length() < 4)
				return FormValidation.warning(Messages.HelloWorldBuilder_DescriptorImpl_warnings_tooShort());
			if (tag != null && tag.length() > 0 && tag.matches(".*[^0-9a-zA-Z_,;].*")) {
				return FormValidation.warning(Messages.HelloWorldBuilder_DescriptorImpl_warnings_wrongSymbol());
			}
			return FormValidation.ok();
		}
		
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}
		
		@Override
		public String getDisplayName() {
			return Messages.HelloWorldBuilder_DescriptorImpl_DisplayName();
		}

	}

}
