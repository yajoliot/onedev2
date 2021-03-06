package io.onedev.server.web.resource;

import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

public class AgentLibResourceReference extends ResourceReference {

	private static final long serialVersionUID = 1L;

	public AgentLibResourceReference() {
		super("agent-lib");
	}

	@Override
	public IResource getResource() {
		return new AgentLibResource();
	}

}
