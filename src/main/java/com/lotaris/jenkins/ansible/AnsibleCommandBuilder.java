package com.lotaris.jenkins.ansible;

import com.lotaris.jenkins.ansible.model.Ansible;
import com.lotaris.jenkins.ansible.model.Playbook;
import com.lotaris.jenkins.ansible.yaml.AnsibleYamlLoader;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ArgumentListBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import javax.servlet.ServletException;

/**
 * Command runner for Ansible based on an Ansible configuration file given.
 * 
 * @author Laurent Prevost <laurent.prevost@lotaris.com>
 */
public class AnsibleCommandBuilder extends Builder {
	/**
	 * The configuration file to read
	 */
	private final String configurationFile;
	
	/**
	 * The workspace where the command should be run
	 */
	private final String workspace;
	
	/**
	 * VM type to build
	 */
	private final String playbookName;
	
	/**
	 * Coma separated parameters
	 */
	private final String parameters;

	@DataBoundConstructor
	public AnsibleCommandBuilder(String configurationFile, String workspace, String playbookName, String parameters) {
		this.configurationFile = configurationFile;
		this.workspace = workspace;
		this.playbookName = playbookName;
		this.parameters = parameters;
	}

	public String getConfigurationFile() {
		return configurationFile;
	}

	public String getParameters() {
		return parameters;
	}

	public String getPlaybookName() {
		return playbookName;
	}

	public String getWorkspace() {
		return workspace;
	}
	
	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
		// Create a process
		Launcher.ProcStarter ps = launcher.launch();

		try {
			// Retrieve the build parameters
			EnvVars env = build.getEnvironment(listener);
			
			// Expend the file configuration path
			String configurationFileExpanded = env.expand(configurationFile);
//			configurationFileExpanded = Util.replaceMacro(configurationFileExpanded, build.getBuildVariableResolver());

			ArgumentListBuilder cmdArguments = new ArgumentListBuilder("ansible-playbook");
			
			Ansible ans = AnsibleYamlLoader.load(configurationFileExpanded);
			
//			listener.getLogger().println("Ansible configuration: " + ans);
			
			manageArguments(cmdArguments, ans, env);

			if (workspace != null && !workspace.isEmpty()) {
				ps = ps.pwd(env.expand(workspace));
			}
			else {
				ps =ps.pwd(build.getWorkspace());
			}
			
			ps = ps
				.cmds(cmdArguments)
				.stdout(listener.getLogger())
				.stderr(listener.getLogger());

			int rc = launcher.launch(ps).join();

			if (rc == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		catch (IOException e) {
			listener.error("Unable to run the command.", e);
			return false;
		}
		catch (InterruptedException e) {
			listener.error("Unable to run the command.", e);
			return false;
		}
	}

	/**
	
	/**
	 * Build the command arguments
	 * 
	 * @param argListBld The argument list builder
	 * @param ans The Ansible configuration
	 * @param env The build parameters
	 */
	private void manageArguments(ArgumentListBuilder argListBld, Ansible ans, EnvVars env) {
		// Check there is at least one playbook
		if (ans.getPlaybooks() == null || ans.getPlaybooks().isEmpty()) {
			throw new RuntimeException("No playbooks specified in the Ansible configuration file. It must be at least one defined.");
		}
		
		// Retrieve the playbook to run
		String expandedPlaybookName = env.expand(playbookName);
		Playbook playbook = null;
		for (Playbook pb : ans.getPlaybooks()) {
			// Be sure the configuration file is correct
			if (pb.getName() == null || pb.getName().isEmpty()) {
				throw new RuntimeException("One playbook has no name. Playbook name is mandatory.");
			}
			
			if (pb.getName().equals(expandedPlaybookName)) {
				playbook = pb;
				break;
			}
		}

		// No playbook found
		if (playbook == null) {
			throw new RuntimeException("Unable to find the playbook [" + expandedPlaybookName + "] in the Ansible configuration file.");
		}

		// Manage verbose level
		manageVerboseLevel(argListBld, ans, playbook);

		// Manage the inventory file
		manageInventory(argListBld, ans, playbook);

		// Manage the playbook file
		managePlaybook(argListBld, playbook);
		
		// Manage the extra variables
		manageExtraParameters(argListBld, ans, playbook, env);
	}

	/**
	 * Manage the verbose level argument
	 * 
	 * @param argListBld The argument list
	 * @param ans The Ansible configuration
	 * @param playbook The playbook
	 */
	private void manageVerboseLevel(ArgumentListBuilder argListBld, Ansible ans, Playbook playbook) {
		// Manage the inventory file
		String verbose = null;
		
		// Retrieve inventory from playbook config or from global config
		if (playbook.getVerbose()!= null && !playbook.getVerbose().isEmpty()) {
			verbose = playbook.getVerbose();
		}
		else if (ans.getVerbose() != null && !ans.getVerbose().isEmpty()) {
			verbose = ans.getVerbose();
		}
		
		// Add inventory to command line if necessary
		if (verbose != null) {
			argListBld.add("-" + verbose);
		}
	}

	/**
	 * Manage the inventory file argument
	 * 
	 * @param argListBld The argument list
	 * @param ans The Ansible configuration
	 * @param playbook The playbook
	 */
	private void manageInventory(ArgumentListBuilder argListBld, Ansible ans, Playbook playbook) {
		// Manage the inventory file
		String inventory = null;
		
		// Retrieve inventory from playbook config or from global config
		if (playbook.getInventory() != null && !playbook.getInventory().isEmpty()) {
			inventory = playbook.getInventory();
		}
		else if (ans.getInventory() != null && !ans.getInventory().isEmpty()) {
			inventory = ans.getInventory();
		}
		
		// Add inventory to command line if necessary
		if (inventory != null) {
			argListBld.add("-i").add(inventory);
		}
	}
	
	/**
	 * Manage the playbook argument
	 * 
	 * @param argListBld The argument list
	 * @param playbook The playbook
	 */
	private void managePlaybook(ArgumentListBuilder argListBld, Playbook playbook) {
		if (playbook.getFile() == null || playbook.getFile().isEmpty()) {
			throw new RuntimeException("There is no playbook file specified for playbook [" + playbook.getName() + "]. The file is mandatory.");
		}
		
		argListBld.add(playbook.getFile());
	}
	
	/**
	 * Manage the extra variables
	 * 
	 * @param argListBld The argument list
	 * @param ans The Ansible configuration
	 * @param playbook The playbook
	 * @param env The build parameters
	 */
	private void manageExtraParameters(ArgumentListBuilder argListBld, Ansible ans, Playbook playbook, EnvVars env) {
		// Manage a map of extra parameters
		Map<String, String> extraVars = new HashMap<String, String>();

		// Expand specific variables
		if (playbook.getVars() != null) {
			for (Map.Entry<String, String> var : playbook.getVars().entrySet()) {
				if (var.getKey() != null && var.getValue() != null) {
					extraVars.put(var.getKey(), handleVariable(ans, playbook, var.getValue(), env));
				}
			}
		}
	
		// Add extra vars
		if (!extraVars.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			
			// Build the extra vars string
			for (Map.Entry<String, String> var : extraVars.entrySet()) {
				sb
					.append(var.getKey())
					.append("=")
					.append(var.getValue())
					.append(" ");
			}
			
			argListBld
				.add("--extra-vars")
				.add(sb.substring(0, sb.length() - 1));
		}
	}
	
	/**
	 * Take care to replace the parameters value correctly trough the Jenkins filtering or
	 * the Ansible plugin filtering.
	 * 
	 * @param ans Ansible configuration
	 * @param playbook The playbook
	 * @param value The value to filter
	 * @param env The build parameters to help the filtering
	 * @return The value filtered
	 */
	private String handleVariable(Ansible ans, Playbook playbook, String value, EnvVars env) {
		String resultValue = env.expand(value);

		// Replace all parameters
		if (parameters != null && !parameters.isEmpty()) {
			String expParameters = env.expand(parameters);
			for (String param : expParameters.split(",")) {
				if (param.contains("=")) {
					String[] var = param.split("=");
					
					// Specific behavior for host parameter which is an index of host present in the hosts list
					if (var[0].equals("host") && playbook.getProperties() != null && playbook.getProperties().get("hosts") != null && !playbook.getProperties().get("hosts").isEmpty()) {
						try {
							int hostIndex = Integer.parseInt(var[1]) - 1;
							String[] hosts = playbook.getProperties().get("hosts").split(",");

							// Host index validation
							if (hostIndex < 0 || hostIndex > hosts.length) {
								throw new RuntimeException("The [host] parameter value should be a valid index (Should be >=1 and the number of hosts present in the properties.");
							}

							resultValue = resultValue.replaceAll("\\{\\{( *)host( *)\\}\\}", hosts[hostIndex]);
						}
						catch (NumberFormatException nfe) {
							throw new RuntimeException("Unable to parse the [host] parameter which is an index of host.", nfe);
						}
					}
					
					// Standard filtering
					else {
						resultValue = resultValue.replaceAll("\\{\\{( *)" + var[0] + " ( *)\\}\\}", var[1]);
					}
				}
			}
		}
		
		return resultValue;
	}
	
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	/**
	 * Descriptor for {@link HelloWorldBuilder}. Used as a singleton. The class is marked as public so that it can be accessed from views.
	 *
	 * <p>
	 * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
	 * for the actual HTML fragment for the configuration screen.
	 */
	@Extension // This indicates to Jenkins that this is an implementation of an extension point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		/**
		 * In order to load the persisted global configuration, you have to call load() in the constructor.
		 */
		public DescriptorImpl() {
			load();
		}
		
		/**
		 * Performs on-the-fly validation of the form field 'configurationFile'.
		 *
		 * @param value This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the browser.
		 * <p>
		 * Note that returning {@link FormValidation#error(String)} does not prevent the form from being saved. It just means that a message will be displayed to
		 * the user.
		 */
		public FormValidation doCheckConfigurationFile(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error("Please set a configuration file");
			}
			if (value.length() < 4) {
				return FormValidation.warning("Isn't the configuration file	 too short?");
			}
			return FormValidation.ok();
		}

		/**
		 * Performs on-the-fly validation of the form field 'workspace'.
		 *
		 * @param value This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the browser.
		 * <p>
		 * Note that returning {@link FormValidation#error(String)} does not prevent the form from being saved. It just means that a message will be displayed to
		 * the user.
		 */
		public FormValidation doCheckWorkspace(@QueryParameter String value) throws IOException, ServletException {
			if (!value.isEmpty() && value.length() < 4) {
				return FormValidation.warning("Isn't the workspace too short?");
			}
			return FormValidation.ok();
		}

		/**
		 * Performs on-the-fly validation of the form field 'playbookName'.
		 *
		 * @param value This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the browser.
		 * <p>
		 * Note that returning {@link FormValidation#error(String)} does not prevent the form from being saved. It just means that a message will be displayed to
		 * the user.
		 */
		public FormValidation doCheckPlaybookName(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error("Please set a playbook name");
			}
			if (value.length() < 2) {
				return FormValidation.warning("Isn't the playbook name too short?");
			}
			return FormValidation.ok();
		}

		/**
		 * Performs on-the-fly validation of the form field 'parameters'.
		 *
		 * @param value This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the browser.
		 * <p>
		 * Note that returning {@link FormValidation#error(String)} does not prevent the form from being saved. It just means that a message will be displayed to
		 * the user.
		 */
		public FormValidation doCheckParameters(@QueryParameter String value) throws IOException, ServletException {
			if (!value.isEmpty() && value.length() < 5) {
				return FormValidation.warning("Isn't the parameters too short?");
			}
			return FormValidation.ok();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		public String getDisplayName() {
			return "Ansible command runner";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}
	}
}
