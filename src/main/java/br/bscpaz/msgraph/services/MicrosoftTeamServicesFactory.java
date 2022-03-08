package br.bscpaz.msgraph.services;

import br.bscpaz.msgraph.services.impl.MicrosoftTeamsServicesImpl;

public final class MicrosoftTeamServicesFactory {

	private static MicrosoftTeamServicesFactory instance = null;
	
	private MicrosoftTeamServicesFactory() {
	}

	public static synchronized MicrosoftTeamServicesFactory getInstance() {
		if (instance == null) {
			instance = new MicrosoftTeamServicesFactory();
		}
		return instance;
	}

	@SuppressWarnings("static-method")
	public MicrosoftTeamServices getServices() throws Exception {
		return new MicrosoftTeamsServicesImpl();
	}	
}
