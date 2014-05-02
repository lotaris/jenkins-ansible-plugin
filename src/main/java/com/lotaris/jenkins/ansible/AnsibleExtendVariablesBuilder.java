package com.lotaris.jenkins.ansible;

import com.lotaris.jenkins.ansible.model.Ansible;
import com.lotaris.jenkins.ansible.yaml.AnsibleYamlLoader;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Extension;
import hudson.Util;
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
 * Build step to extend the current variables with variable values from an Ansible
 * configuration file.
 * 
 * To work properly, the step need to know where to find the Ansible configuration file
 * and also which properties that should be extracted from the configuration file
 * 
 * @author Laurent Prevost <laurent.prevost@lotaris.com>
 */
public class AnsibleExtendVariablesBuilder extends Builder {
	/**
	 * Ansible Configuration file
	 */
	private final String configurationFile;
	
	/**
	 * The coma separated list of properties
	 */
	private final String properties;

	@DataBoundConstructor
	public AnsibleExtendVariablesBuilder(String configurationFile, String properties) {
		this.properties = properties;
		this.configurationFile = configurationFile;
	}

	public String getProperties() {
		return properties;
	}

	public String getConfigurationFile() {
		return configurationFile;
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
		try {
			// Get the build variables
			EnvVars env = build.getEnvironment(listener);
			
			// Expand the configuraiton file path
			String configurationFileExpanded = env.expand(configurationFile);
			configurationFileExpanded = Util.replaceMacro(configurationFileExpanded, build.getBuildVariableResolver());

			Ansible ans = AnsibleYamlLoader.load(configurationFileExpanded);
			
			Map<String, String> extendedParameters = new HashMap<String, String>();
			for (String propertyName : properties.split(",")) {
				if (!build.getBuildVariables().containsKey(propertyName)) {
					listener.getLogger().println("Parameter found for [" + propertyName + "] with value [" + ans.getProperties().get(propertyName) + "]");
					extendedParameters.put(propertyName, ans.getProperties().get(propertyName));
				}
			}
			
			if (extendedParameters.size() > 0) {
				build.addAction(new AnsibleExtendVariablesAction(extendedParameters));
			}
			
			return true;
		}
		catch (IOException e) {
			listener.error("Unable to read the Ansible configuration file.", e);
			return false;
		}
		catch (InterruptedException e) {
			listener.error("Unable to get the build parameters.", e);
			return false;
		}
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
		 * Performs on-the-fly validation of the form field 'configuration file'.
		 *
		 * @param value This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the browser.
		 * <p>
		 * Note that returning {@link FormValidation#error(String)} does not prevent the form from being saved. It just means that a message will be displayed to
		 * the user.
		 */
		public FormValidation doCheckConfigurationFile(@QueryParameter String value)
			throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error("Please set a configuration file");
			}
			if (value.length() < 4) {
				return FormValidation.warning("Isn't the configuration file	 too short?");
			}
			return FormValidation.ok();
		}

		/**
		 * Performs on-the-fly validation of the form field 'properties'.
		 *
		 * @param value This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the browser.
		 * <p>
		 * Note that returning {@link FormValidation#error(String)} does not prevent the form from being saved. It just means that a message will be displayed to
		 * the user.
		 */
		public FormValidation doCheckProperties(@QueryParameter String value)
			throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error("Please set at least one property name");
			}
			if (value.length() < 4) {
				return FormValidation.warning("Isn't the property list too short?");
			}
			return FormValidation.ok();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		public String getDisplayName() {
			return "Extand build parameters from properties from Ansible configuration file.";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}
	}
}
