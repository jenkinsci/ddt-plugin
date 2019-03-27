package io.jenkins.plugins.ddt;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import jenkins.tasks.SimpleBuildStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;

import javax.servlet.ServletException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This class represents a build step's configuration and more importantly is the "run" for Jenkins.
 * @author Evgeny Kolyakov
 */
public class QADDTStepRaw extends Builder implements SimpleBuildStep {
	
	private String yaml;
	private String tags;
	
	/**
	 * This is the serializable constructor.
	 * @param yaml {String} The yaml data of a "test" (see, https://github.com/freaker2k7/ui-data-driven-tests).
	 * @param tags {String} The list of tags separated by comas (,)
	 */
	@DataBoundConstructor
	public QADDTStepRaw(String yaml, String tags) {
		this.yaml = yaml;
		this.tags = tags;
	}
	
	/**
	 * Getter for the yaml field.
	 * @return {String} Returns the parent UUID of the "run".
	 */
	public String getYaml() {
		return yaml;
	}
	
	/**
	 * Getter for the tags field.
	 * @return {String} Returns the "run" tags.
	 */
	public String getTags() {
		return tags;
	}
	
	/**
	 * This is the main function that actually "re-runs" the given "test" as a new "test".
	 * @param cur_log {PrintStream} Jenkins job console's logger.
	 * @param os {OutputStream} Stream to the Jenkins job /workspace/. 
	 * @throws IOException In case of an exception the "run"/"job" must be stopped!
	 */
	private void _perform(PrintStream cur_log, OutputStream os) throws IOException {
		QADDTAPI api = new QADDTAPI();
		
		cur_log.println("Initializing test [tags: '" + tags + "']");
		
		String uuid = api.test_raw(yaml, tags);
		
		if (uuid == null) {
			throw new IOException("Failed initialization!!!");
		}
		
		cur_log.println("Waiting for test '" + uuid + "' to start");
		
		if (!api.poll(uuid, "tasks.txt", "text/plain", 125)) { // (~5*)(120 + 5) ~ 625 sec.
			throw new IOException("UI test '" + uuid + "' didn't start properly");
		}
		
		cur_log.println("Testing...");
		
		if (!api.poll(uuid, "report.xml", "text/xml", 17280)) { // (~5*)17280 ~ 86400 sec. /// TODO: Differ by PRO.
			throw new IOException("UI test '" + uuid + "' failed.");
		}
		
		byte[] report = api.fetch(uuid, "report.xml", "text/xml").getBytes("UTF-8");
		os.write(report);
		os.close();
		
		api.logout();
		
		cur_log.println("Done test: '" + uuid + "'");
	}
	
	/**
	 * Jenkins SimpleBuildStep's function responsible for performing the step.
	 * @param run {Run} The run itself.
	 * @param workspace {FilePath} Jenkins job workspace.
	 * @param launcher {Launcher} Jenkins job launcher.
	 * @param listener {TaskListener} Jenkins job console (log) stream..
	 * @throws InterruptedException This comes from inheritance.
	 * @throws IOException This is the needed exception in case the "run" must be stopped!
	 */
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		EnvVars environment = run.getEnvironment(listener);
		yaml = environment.expand(yaml);
		_perform(listener.getLogger(), workspace.child("report_" + Math.random() + ".xml").write());
	}
	
	/**
	 * This is a helper class for Jenkins.
	 */
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		
		/**
		 * Validate the yaml (query) parameter while choosing.
		 * @param value {String} The yaml data.
		 * @return {FormValidation} Return ok() or error() according to whether the input was valid.
		 * @throws IOException This comes from inheritance.
		 * @throws ServletException This comes from inheritance.
		 */
		public FormValidation doCheckYaml(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() < 100) {
				return FormValidation.error(Messages.QADDT_DescriptorImpl_errors_missingYaml());
			}
			// if (value.length() < 4) {
			// 	return FormValidation.warning(Messages.QADDT_DescriptorImpl_warnings_tooShort());
			// }
			return FormValidation.ok();
		}
		
		/**
		 * Validate the tags (query) parameter while typing.
		 * @param value {String} The tags query parameter value.
		 * @return {FormValidation} Return ok() or error() according to whether the input was valid.
		 * @throws IOException This comes from inheritance.
		 * @throws ServletException This comes from inheritance.
		 */
		public FormValidation doCheckTags(@QueryParameter String value) throws IOException, ServletException {
			if (value != null && value.length() > 0 && value.matches(".*[^0-9a-zA-Z_,;].*")) {
				return FormValidation.error(Messages.QADDT_DescriptorImpl_warnings_wrongSymbol());
			}
			return FormValidation.ok();
		}
		
		/**
		 * This is a Jenkins function that needs to be overriden for the step to become active.
		 * @param aClass {Class} A class
		 * @return {boolean} Constant true.
		 */
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}
		
		/**
		 * This is for Jenkins to display the "QADDT" title in the header of the build step in Jenkins - Job - Configuration.
		 * @return {String} Returns the constant string from the "dictionary" "QADDT".
		 */
		@Override
		public String getDisplayName() {
			return Messages.QADDTRaw_DescriptorImpl_DisplayName();
		}
	}
}
