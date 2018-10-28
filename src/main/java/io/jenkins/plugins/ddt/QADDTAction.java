package io.jenkins.plugins.ddt;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;

import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.Project;

import jenkins.model.TransientActionFactory;


public class QADDTAction implements RootAction {

	// private Project project;

	public QADDTAction(/*Project project*/) {
		// this.project = project;
	}

	@Override
	public String getIconFileName() {
		return "gear2.png";
	}

	@Override
	public String getDisplayName() {
		return "QA Data Driven Tests";
	}

	@Override
	public String getUrlName() {
		return "ddt";
	}
	
	@Extension
	public static class QADDTActionFactory extends TransientActionFactory<Project> {

		@Override
		public Class<Project> type() {
			return Project.class; 
		}

		@Nonnull
		@Override
		public Collection<? extends RootAction> createFor(@Nonnull Project project) {
			return Collections.singleton(new QADDTAction()); 
		}
	}
}