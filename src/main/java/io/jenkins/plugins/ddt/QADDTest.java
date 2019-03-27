package io.jenkins.plugins.ddt;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;

/**
 * This class represents a "test" (record) for the QADDTConfig.
 * @author Evgeny Kolyakov
 */
@ExportedBean(defaultVisibility = 999)
public class QADDTest extends AbstractDescribableImpl<QADDTest> implements Serializable {
	
	private final String uuid;
	private final String name;
	
	private String tags;
	
	/**
	 * This is the serializable constructor.
	 * @param uuid {String} The test's UUID (copied from https://qa-app.doorzz.com).
	 * @param name {String} The local title for the test.
	 * @param tags {String} A list of tags separated by comas (,)
	 */
	@DataBoundConstructor
	public QADDTest(String uuid, String name, String tags) {
		this.uuid = uuid;
		this.name = name;
		this.tags = tags;
	}
	
	/**
	 * Getter for the name field.
	 * @return {String} Returns the "test" title.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Getter for the uuid field.
	 * @return {String} Returns the "test" UUID.
	 */
	@Exported
	public String getUuid() {
		return uuid;
	}
	
	/**
	 * Getter for the tags field.
	 * @return {String} Returns the "test" tags.
	 */
	@Exported
	public String getTags() {
		return tags;
	}

	/**
	 * Setter for the tags field.
	 * @param tags {String} The list of tags to set.
	 */
	@DataBoundSetter
	public void setTags(String tags) {
		this.tags = tags;
	}
	
	/**
	 * Local implementation of toString() fot the "test" object.
	 * @return {String} Returns a packed version of the "test".
	 */
	@Override
	public String toString() {
		return name + ":" + uuid + "::" + tags;
	}
	
	/**
	 * Local implementation of hashCode() fot the "test" object.
	 * @return {String} Returns the hashCode() according to the (unique) UUID.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}
	
	/**
	 * Local implementation of equals() fot the "test" object.
	 * @param obj {Object} The object to check.
	 * @return {boolean} Reurns true if the objects are equals - same UUID, otherwise, false.
	 */
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
			if (other.getUuid() != null) {
				return false;
			}
		} else if (!uuid.equals(other.getUuid())) {
			return false;
		}
		return true;
	}
	
	/**
	 * This is a helper class for Jenkins.
	 */
	@Extension
	public static class DescriptorImpl extends Descriptor<QADDTest> {
		
		// public DescriptorImpl() {
		// 	load();
		// }

		/**
		 * This is for Jenkins to display the "Test" title in the header of each test in Jenkins - Configuration.
		 * @return {String} Returns the constant string "Test".
		 */
		@Override
		public String getDisplayName() {
			return "Test";
		}
	}
	
	private static final long serialVersionUID = 1L;
}