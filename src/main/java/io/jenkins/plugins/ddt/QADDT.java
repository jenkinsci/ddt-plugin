package io.jenkins.plugins.ddt;

import hudson.Plugin;
import hudson.model.Api;

// import java.util.Collections;
// import java.util.List;

// import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class QADDT extends Plugin {

	public Api getApi() {
		return new Api(this);
	}

	/*@Exported
	public List<QADDTest> getTests() {
		return Collections.unmodifiableList(QADDTConfig.getTests());
	}*/

}