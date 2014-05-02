package com.lotaris.jenkins.ansible;

import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import javax.servlet.ServletException;

/**
 * Build step to expand new variables and make them available from the remaining build steps.
 * 
 * @author Laurent Prevost <laurent.prevost@lotaris.com>
 */
public class ExpandNewVariablesBuilder extends Builder {
	/**
	 * The list of variables
	 */
	private final String variables;

	@DataBoundConstructor
	public ExpandNewVariablesBuilder(String variables) {
		this.variables = variables;
	}

	public String getVariables() {
		return variables;
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
		if (variables != null && !variables.isEmpty()) {
			Map<String, String> expVariables = new HashMap<String, String>();
			
			for (String variable : variables.split("\n")) {
				if (variable.contains("=")) {
					String[] var = variable.split("=");
					expVariables.put(var[0], var[1]);
				}
			}
			
			build.addAction(new AnsibleExtendVariablesAction(expVariables));
		}
		
		return true;
	}		

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		public DescriptorImpl() {
			load();
		}

		/**
		 * Performs on-the-fly validation of the form field 'variables'.
		 *
		 * @param value This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the browser.
		 * <p>
		 * Note that returning {@link FormValidation#error(String)} does not prevent the form from being saved. It just means that a message will be displayed to
		 * the user.
		 */
		public FormValidation doCheckVariables(@QueryParameter String value) throws IOException, ServletException {
			if (!value.isEmpty() && value.length() < 5) {
				return FormValidation.warning("Isn't the variables too short?");
			}
			return FormValidation.ok();
		}		

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		public String getDisplayName() {
			return "Expand build parameters with additional variables.";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}
	}
}
