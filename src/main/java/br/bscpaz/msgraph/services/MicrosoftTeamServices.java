package br.bscpaz.msgraph.services;

import br.bscpaz.legacy.ProcessoAudiencia;
import br.bscpaz.legacy.ProcessoAudienciaRetorno;

public interface MicrosoftTeamServices {

	public ProcessoAudienciaRetorno criarAgendamento(ProcessoAudiencia audiencia);
	
	public ProcessoAudienciaRetorno alterarAgendamento(ProcessoAudiencia audiencia);

	public ProcessoAudienciaRetorno cancelarAgendamento(ProcessoAudiencia audiencia);
	
}
