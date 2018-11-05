package io.jenkins.plugins.ddt;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Project;

import hudson.model.Run;
import jenkins.model.RunAction2;

import jenkins.model.TransientActionFactory;


public class QADDTAction implements RunAction2 {

	private transient Run run;

	public QADDTAction(Run run) {
		this.run = run;
	}

	@Override
	public String getIconFileName() {
		return "search.png";
	}

	@Override
	public String getDisplayName() {
		return "QADDT Results";
	}

	@Override
	public String getUrlName() {
		return "ddt";
	}
	
	@Override
	public void onAttached(Run<?, ?> run) {
		this.run = run; 
	}

	@Override
	public void onLoad(Run<?, ?> run) {
		this.run = run; 
	}

	public Run getRun() { 
		return run;
	}
	
	@Extension
	public static class QADDTActionFactory extends TransientActionFactory<Run> {

		@Override
		public Class<Run> type() {
			return Run.class; 
		}

		@Nonnull
		@Override
		public Collection<? extends Action> createFor(@Nonnull Run run) {
			return Collections.singleton(new QADDTAction(run)); 
		}
	}
}