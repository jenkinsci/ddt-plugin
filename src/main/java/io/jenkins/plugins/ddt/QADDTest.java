package io.jenkins.plugins.ddt;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.model.Descriptor.FormException;
import org.kohsuke.stapler.StaplerRequest;
import net.sf.json.JSONObject;

import java.io.Serializable;


@ExportedBean(defaultVisibility = 999)
public class QADDTest extends AbstractDescribableImpl<QADDTest> implements Serializable {
	private final String uuid;
	private final String name;
	private String tags;
	
	@DataBoundConstructor
	public QADDTest(String uuid, String name, String tags) {
		this.uuid = uuid;
		this.name = name;
		this.tags = tags;
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

	@DataBoundSetter
	public void setTags(String tags) {
		this.tags = tags;
	}
	
	@Override
	public String toString() {
		return name + ":" + uuid + "::" + tags;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		QADDTest other = (QADDTest) obj;
		if (uuid == null) {
			if (other.uuid != null) {
				return false;
			}
		} else if (!uuid.equals(other.uuid)) {
			return false;
		}
		return true;
	}
	
	@Extension
	public static class DescriptorImpl extends Descriptor<QADDTest> {
		
		// public DescriptorImpl() {
		// 	load();
		// }
		
		@Override
		public String getDisplayName() {
			return "Test";
		}
	}
	
	private static final long serialVersionUID = 1L;
}