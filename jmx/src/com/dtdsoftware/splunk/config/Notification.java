package com.dtdsoftware.splunk.config;

/**
 * POJO for an MBean notification
 * 
 * @author Damien Dallimore damien@baboonbones.com
 * 
 */
public class Notification {

	public String filterImplementationClass;

	public Notification() {
	}

	public String getFilterImplementationClass() {
		return filterImplementationClass;
	}

	public void setFilterImplementationClass(String filterImplementationClass) {
		this.filterImplementationClass = filterImplementationClass;
	}

}
