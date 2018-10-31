package io.jenkins.plugins.ddt;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;


@ExportedBean(defaultVisibility = 999)
class QADDTest extends AbstractDescribableImpl<QADDTest> implements Serializable {
	private final String uuid;
	private final String name;
	private final String tags;
	
	@DataBoundConstructor
	public QADDTest(String uuid, String name, String tags) {
		this.uuid = uuid;
		this.name = name;
		this.tags = tags;
		
		
		System.out.println(">>>>???>>>>");
		System.out.println("name: " + name);
		System.out.println("uuid: " + uuid);
		System.out.println("<<<<???<<<<");
		
	}
	
	@Exported
	public String getName() {
		return name;
	}
	
	@Exported
	public String getUuid() {
		return uuid;
	}
	
	@Exported
	public String getTags() {
		return tags;
	}
	
	@Override
	public String toString() {
		return name + ":" + uuid + "::" + tags;
	}
	
	@Extension
	public static class DescriptorImpl extends Descriptor<QADDTest> {
		
		@Override
		public String getDisplayName() {
			return "Test";
		}
	}
	
	private static final long serialVersionUID = 1L;
}