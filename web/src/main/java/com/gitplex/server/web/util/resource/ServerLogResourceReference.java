package com.gitplex.server.web.util.resource;

import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

public class ServerLogResourceReference extends ResourceReference {

	private static final long serialVersionUID = 1L;

	public ServerLogResourceReference() {
		super("server-log");
	}

	@Override
	public IResource getResource() {
		return new ServerLogResource();
	}

}
