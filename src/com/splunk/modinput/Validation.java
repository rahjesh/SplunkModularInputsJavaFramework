package com.splunk.modinput;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "server_host", "server_uri", "session_key",
		"checkpoint_dir", "items" }, name = "items")
@XmlRootElement(name = "items")
public class Validation {

	private String server_host;
	private String server_uri;
	private String session_key;
	private String checkpoint_dir;

	private List<Item> items = new ArrayList<Item>();

	public String getServer_host() {
		return server_host;
	}

	public void setServer_host(String server_host) {
		this.server_host = server_host;
	}

	public String getServer_uri() {
		return server_uri;
	}

	public void setServer_uri(String server_uri) {
		this.server_uri = server_uri;
	}

	public String getSession_key() {
		return session_key;
	}

	public void setSession_key(String session_key) {
		this.session_key = session_key;
	}

	public String getCheckpoint_dir() {
		return checkpoint_dir;
	}

	public void setCheckpoint_dir(String checkpoint_dir) {
		this.checkpoint_dir = checkpoint_dir;
	}

	@XmlElement(name = "item")
	public List<Item> getItems() {
		return items;
	}

	public void addItem(Item item) {
		this.items.add(item);
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}

}
