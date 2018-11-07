package io.jenkins.plugins.ddt;

import hudson.Plugin;
import hudson.model.Api;

import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * This is the root class for this plugin. 
 * It exposes an Api for the plugin which allows listing all the tests on the following url: /plugin/ddt/api/xml
 * @author Evgeny Kolyakov
 */
@ExportedBean
public class QADDT extends Plugin {

	/**
	 * This is the standard Jenkins way to expose an API. 
	 * @return {Api} Return new Api object.
	 */
	public Api getApi() {
		return new Api(this);
	}
	
	/**
	 * Get the full list of tests.
	 * @return {List} Returns a list of the currently registered tests.
	 */
	@Exported
	public List<QADDTest> getTests() {
		return Collections.unmodifiableList(QADDTConfig.get().getTests());
	}

}