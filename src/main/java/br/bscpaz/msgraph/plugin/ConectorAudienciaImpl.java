package br.bscpaz.msgraph.plugin;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;

import br.bscpaz.msgraph.services.MicrosoftTeamServices;
import br.bscpaz.msgraph.services.MicrosoftTeamServicesFactory;
import br.bscpaz.msgraph.services.dtos.TipoOperacao;
import br.bscpaz.legacy.PontoExtensaoException;
import br.bscpaz.legacy.ProcessoAudiencia;
import br.bscpaz.legacy.ProcessoAudienciaRetorno;

@Name("conectorAudiencia")
@Scope(ScopeType.APPLICATION)
@Install(precedence = Install.APPLICATION)
@Startup
public class ConectorAudienciaImpl implements br.bscpaz.legacy.ConectorAudiencia {

	private static final Logger logger = Logger.getLogger(ConectorAudienciaImpl.class);
	
	@Override
	public ProcessoAudienciaRetorno criarAgendamento(ProcessoAudiencia processoAudiencia) throws PontoExtensaoException {
		return processarAgendamento(processoAudiencia, TipoOperacao.NOVO);
	}

	@Override
	public ProcessoAudienciaRetorno alterarAgendamento(ProcessoAudiencia processoAudiencia) throws PontoExtensaoException {
		return processarAgendamento(processoAudiencia, TipoOperacao.ALTERACAO);
	}

	@Override
	public ProcessoAudienciaRetorno cancelarAgendamento(ProcessoAudiencia processoAudiencia) throws PontoExtensaoException {
		return processarAgendamento(processoAudiencia, TipoOperacao.CANCELAMENTO);	
	}	
	
	private ProcessoAudienciaRetorno processarAgendamento(ProcessoAudiencia processoAudiencia, TipoOperacao operacao) throws PontoExtensaoException {
		ProcessoAudienciaRetorno retorno = new ProcessoAudienciaRetorno();
		long timeMillisEntrada = 0;

		if (logger.isDebugEnabled()) {
			timeMillisEntrada = System.currentTimeMillis();
		}

		if (processoAudiencia.isAtivo()) {
			if (isParametrosValidos(processoAudiencia, operacao)) {
				try {
					MicrosoftTeamServices servico = MicrosoftTeamServicesFactory.getInstance().getServices();
					switch (operacao) {
					case NOVO:
						retorno = servico.criarAgendamento(getAudienciaConfomeModoExecucao(processoAudiencia));
						break;
					case ALTERACAO:
						retorno = servico.alterarAgendamento(getAudienciaConfomeModoExecucao(processoAudiencia));
						break;
					case CANCELAMENTO:
						retorno = servico.cancelarAgendamento(getAudienciaConfomeModoExecucao(processoAudiencia));
						break;
					}
				} catch (Throwable e) {
					retorno.setSucesso(false);
					retorno.setMsgErro(e.getMessage());
					logger.error(e);
				}
			} else {
				String msgErro = "Parâmetros invalidados pelo método isParametrosValidos() do conector de audiência.";
				retorno.setSucesso(false);
				retorno.setMsgErro(msgErro);
				logger.error(msgErro);
			}
		} else {
			String msgErro = "Integração com o Microsoft Teams está desabilitada. Favor verificar parâmetro 'flagMarcacaoAudienciaAtivo'."; 
			retorno.setSucesso(false);
			retorno.setMsgErro(msgErro);
			logger.info(msgErro);
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("Integração com o Microsoft Teams executada em " + (System.currentTimeMillis() - timeMillisEntrada) + "ms");
		}
		return retorno;		
	}	
	
	private boolean isParametrosValidos(ProcessoAudiencia processoAudiencia, TipoOperacao operacao) {
		if (processoAudiencia == null) {
			return false;
		}
		
		if (StringUtils.isBlank(processoAudiencia.getNumeroProcesso())) {
			return false;
		}
		
		if (StringUtils.isBlank(processoAudiencia.getNomeOrgaoOrigem())) {
			return false;
		}
		
		if (StringUtils.isBlank(processoAudiencia.getEmailConciliador())) {
			return false;
		}
		
		if (StringUtils.isBlank(processoAudiencia.getNomeOrgaoConciliador())) {
			return false;
		}
		
		if (processoAudiencia.isModoTesteAtivo() && StringUtils.isBlank(processoAudiencia.getEmailTesteConciliador())) {
			return false;
		}
		
		if (StringUtils.isBlank(processoAudiencia.getLocalAudiencia())) {
			return false;
		}		
		
		if (operacao == TipoOperacao.ALTERACAO || operacao == TipoOperacao.CANCELAMENTO) {
			if (StringUtils.isBlank(processoAudiencia.getChaveCalendarioApoio()) || 
					processoAudiencia.getChaveCalendarioApoio().trim().equals("0")) {
				return false;
			}
		}
		return true;
	}
	
	private ProcessoAudiencia getAudienciaConfomeModoExecucao(ProcessoAudiencia processoAudiencia) {
		if (processoAudiencia.isModoTesteAtivo()) {
			processoAudiencia.setEmailConciliador(processoAudiencia.getEmailTesteConciliador());
			
			logger.info("Executando integração com o Microsoft Teams em modo TESTE enviando mensagens para " + 
					processoAudiencia.getEmailTesteConciliador() + "."	+ 
					"\nFavor consultar parâmetros 'flagMarcacaoAudienciaTesteAtivo' e 'emailTesteAudiencia'.");
		}
		return processoAudiencia;
	}
}
