package com.lotaris.jenkins.ansible.model;

import java.util.List;
import java.util.Map;

/**
 * This is the Ansible configuration to run the proper command
 * 
 * @author Laurent Prevost <laurent.prevost@lotaris.com>
 */
public class Ansible {
	/**
	 * A list of custom properties to help working with the Ansible command
	 */
	private Map<String, String> properties;
	
	/**
	 * The inventory file to modify the behavior of Ansible command
	 */
	private String inventory;
	
	/**
	 * Define the verbosity of the Ansible command
	 */
	private String verbose;

	/**
	 * List of playbooks available in the configuration file
	 */
	private List<Playbook> playbooks;
	
	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public String getInventory() {
		return inventory;
	}

	public void setInventory(String inventory) {
		this.inventory = inventory;
	}

	public String getVerbose() {
		return verbose;
	}

	public void setVerbose(String verbose) {
		this.verbose = verbose;
	}

	public List<Playbook> getPlaybooks() {
		return playbooks;
	}

	public void setPlaybooks(List<Playbook> playbooks) {
		this.playbooks = playbooks;
	}
	
	@Override
	public String toString() {
		return 
			"Properties: " + properties + ", " +
			"Inventory: " + inventory + ", " + 
			"Verbose: " + verbose + ", " +  
			"Playbooks: " + playbooks;
	}
}
