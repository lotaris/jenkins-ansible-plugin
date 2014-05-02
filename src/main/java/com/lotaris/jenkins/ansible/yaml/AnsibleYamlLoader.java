package com.lotaris.jenkins.ansible.yaml;

import com.lotaris.jenkins.ansible.model.Ansible;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Ansible YAML file configuration loader
 *
 * @author Laurent Prevost <laurent.prevost@lotaris.com>
 */
public class AnsibleYamlLoader {
	/**
	 * Load a configuration file
	 * 
	 * @param configFilePath The configuration file to load
	 * @return The Ansible configuration loaded
	 * @throws FileNotFoundException When no file is found
	 */
	public static Ansible load(String configFilePath) throws FileNotFoundException {
		// Read the Ansible configuration
		Representer representer = new Representer();
		representer.getPropertyUtils().setSkipMissingProperties(true);

		Yaml yaml = new Yaml(representer);

		return (Ansible) yaml.loadAs(new FileInputStream(new File(configFilePath)), Ansible.class);
	}
}
