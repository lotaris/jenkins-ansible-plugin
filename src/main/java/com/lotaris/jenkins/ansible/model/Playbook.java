package com.lotaris.jenkins.ansible.model;

import java.util.List;
import java.util.Map;

/**
 * Playbook structure to get the right data to run the right playbook
 * 
 * @author Laurent Prevost <laurent.prevost@lotaris.com>
 */
public class Playbook {
	/**
	 * Playbook name
	 */
	private String name;
	
	/**
	 * The inventory file to modify the behavior of Ansible command and
	 * to override the inventory file from the Ansible global configuration
	 */
	private String inventory;
	
	/**
	 * The playbook file to run
	 */
	private String file;
	
	/**
	 * A list of global variables to avoid repeating variables multiple times
	 */
	private Map<String, String> vars;
	
	/**
	 * A list of custom properties to help working with the Ansible command
	 */
	private Map<String, String> properties;
	
	/**
	 * Define the verbosity of the Ansible command override the Ansible 
	 * global configuration
	 */
	private String verbose;
	
	public Map<String, String> getVars() {
		return vars;
	}

	public void setVars(Map<String, String> vars) {
		this.vars = vars;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getInventory() {
		return inventory;
	}

	public void setInventory(String inventory) {
		this.inventory = inventory;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVerbose() {
		return verbose;
	}

	public void setVerbose(String verbose) {
		this.verbose = verbose;
	}	

	@Override
	public String toString() {
		return 
			"Name: " + name + ", " + 
			"Inventory: " + inventory + ", " + 
			"File: " + file + ", " +
			"Vars: " + vars + ", " + 
			"Properties: " + properties + ", " + 
			"Verbose: " + verbose;
	}
}
