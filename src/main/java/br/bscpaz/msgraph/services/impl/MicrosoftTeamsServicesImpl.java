package br.bscpaz.msgraph.services.impl;


import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.microsoft.graph.auth.publicClient.UsernamePasswordProvider;
import com.microsoft.graph.models.extensions.Attendee;
import com.microsoft.graph.models.extensions.DateTimeTimeZone;
import com.microsoft.graph.models.extensions.EmailAddress;
import com.microsoft.graph.models.extensions.Event;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.ItemBody;
import com.microsoft.graph.models.extensions.Location;
import com.microsoft.graph.models.generated.AttendeeType;
import com.microsoft.graph.models.generated.BodyType;
import com.microsoft.graph.models.generated.OnlineMeetingProviderType;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.requests.extensions.GraphServiceClient;

import br.bscpaz.msgraph.services.MicrosoftTeamServices;
import br.bscpaz.msgraph.util.PropertyReader;
import br.bscpaz.legacy.ProcessoAudiencia;
import br.bscpaz.legacy.ProcessoAudienciaRetorno;

public class MicrosoftTeamsServicesImpl implements MicrosoftTeamServices {

	private final static String TIME_ZONE = "E. South America Standard Time";

	private static final String MICROSOFT_TEAMS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	private static final String ENV_MICROSOFT_TEAMS_USERNAME = "ENV_MICROSOFT_TEAMS_USERNAME";
	private static final String ENV_MICROSOFT_TEAMS_PASSWORD = "ENV_MICROSOFT_TEAMS_PASSWORD";
	private static final String ENV_MICROSOFT_TEAMS_CLIENTID = "ENV_MICROSOFT_TEAMS_CLIENTID";
	
	private UsernamePasswordProvider authProvider = null;

	public MicrosoftTeamsServicesImpl() {
		String clientId = System.getenv(ENV_MICROSOFT_TEAMS_CLIENTID);
		String userName = System.getenv(ENV_MICROSOFT_TEAMS_USERNAME);
		String userPassword = System.getenv(ENV_MICROSOFT_TEAMS_PASSWORD);
		
		if (userName == null || userName.isEmpty()) {
			System.out.println("===> ERRO: variavel de ambiente \"ENV_MICROSOFT_TEAMS_USERNAME\" nao definida");
		}

		if (userPassword == null || userPassword.isEmpty()) {
			System.out.println("===> ERRO: variavel de ambiente \"ENV_MICROSOFT_TEAMS_PASSWORD\" nao definida");
		}
		
        List<String> scopes = PropertyReader.getListProperty("TEAMS_SCOPE");
        authProvider = new UsernamePasswordProvider(clientId, scopes, userName, userPassword);
	}	

	@Override
	public ProcessoAudienciaRetorno criarAgendamento(ProcessoAudiencia audiencia) {
		ProcessoAudienciaRetorno retorno = new ProcessoAudienciaRetorno();
		String assunto = "Nova audiência de conciliação agendada.";
		IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
		
		try {
			Event evento = criarAgendamento(audiencia, assunto, graphClient);
			if (evento != null) {
				if (evento.id != null && !evento.id.trim().equals("")) {
					retorno.setSucesso(true);
					retorno.setChaveCalendarioApoio(evento.id);
					retorno.setEnviadoPara(audiencia.getEmailConciliador());
				} else {
					retorno.setSucesso(false);
					retorno.setMsgErro("O Id de retorno do Teams é nulo ou vazio.");
				}
			} else {
				retorno.setSucesso(false);
				retorno.setMsgErro("O retorno de comunicação com o graph é nulo.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			retorno.setSucesso(false);
			retorno.setMsgErro(e.getMessage());
		}			
		return retorno;
	}

	@Override
	public ProcessoAudienciaRetorno alterarAgendamento(ProcessoAudiencia audiencia) {
		ProcessoAudienciaRetorno retorno = new ProcessoAudienciaRetorno();		
		String assunto = "Audiência de conciliação remarcada.";
		
		//Cria novo agendamento
		IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
		Event post = getMicrosoftTeamsEvent(audiencia, assunto);
		
		try {
			//Atualiza a audiência já agendada.
			Event evento = graphClient.me().events(audiencia.getChaveCalendarioApoio()).buildRequest().patch(post);
			
			if (evento != null) {
				if (evento.id != null && !evento.id.trim().equals("")) {
					retorno.setSucesso(true);
					retorno.setChaveCalendarioApoio(evento.id);
					retorno.setEnviadoPara(audiencia.getEmailConciliador());
				} else {
					retorno.setSucesso(false);
					retorno.setMsgErro("O Id de retorno do Teams é nulo ou vazio.");
				}
			} else {
				retorno.setSucesso(false);
				retorno.setMsgErro("O retorno de comunicação com o graph é nulo.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			retorno.setSucesso(false);
			retorno.setMsgErro(e.getMessage());
		}			
		return retorno;
	}
	
	@Override
	public ProcessoAudienciaRetorno cancelarAgendamento(ProcessoAudiencia audiencia) {
		ProcessoAudienciaRetorno retorno = new ProcessoAudienciaRetorno();	
		
		IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
		LinkedList<Option> requestOptions = new LinkedList<Option>();
		
		//Atualização do motivo do cancelamento.
		Event compromissoCancelado = new Event();
		ItemBody body = new ItemBody();
		body.contentType = BodyType.HTML;
		body.content = audiencia.getObservacao();
		compromissoCancelado.body = body;
		
		try {
			//Atualiza o motivo do cancelamento.
			graphClient.me().events(audiencia.getChaveCalendarioApoio()).buildRequest().patch(compromissoCancelado);
        	
			//Após a inclusão da informação de cancelamento, efetiva o cancelamento.
			graphClient.me().events(audiencia.getChaveCalendarioApoio()).buildRequest(requestOptions).delete();
			retorno.setSucesso(true);
        	
		} catch (Exception e) {
			e.printStackTrace();
			retorno.setSucesso(false);
			retorno.setMsgErro(e.getMessage());
		}
        return retorno;
	}
	
	private Event criarAgendamento(ProcessoAudiencia audiencia, String assunto, IGraphServiceClient graphClient) {
		Event resultado = null;
		Event post = getMicrosoftTeamsEvent(audiencia, assunto);
		
		try {
			resultado = graphClient.me().events().buildRequest().post(post);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultado;
	}	

	private Event getMicrosoftTeamsEvent(ProcessoAudiencia audiencia, String assunto) {
		Event event = new Event();
		
		if (audiencia.isAudienciaVirtual()) {
			event.isOnlineMeeting = true;
			event.onlineMeetingProvider = OnlineMeetingProviderType.TEAMS_FOR_BUSINESS;
		}
		
		event.subject = getAssunto(audiencia.getNumeroProcesso(), assunto);
		
		ItemBody body = new ItemBody();
		body.contentType = BodyType.HTML;
		body.content = getCorpoMensagem(audiencia);
		event.body = body;
		
		event.start = getDateTimeTimeZone(audiencia.getDtInicio());
		event.end = getDateTimeTimeZone(audiencia.getDtFim());
		
		event.location = getLocation(audiencia.getLocalAudiencia());
		
		LinkedList<Attendee> attendeesList = new LinkedList<Attendee>();
		Attendee attendees = new Attendee();
		EmailAddress emailAddress = new EmailAddress();
		emailAddress.address = audiencia.getEmailConciliador();
		emailAddress.name = audiencia.getNomeOrgaoConciliador();
		attendees.emailAddress = emailAddress;
		
		attendees.type = AttendeeType.REQUIRED;
		attendeesList.add(attendees);
		event.attendees = attendeesList;
		
		return event;
	}		
	
	/*
	 * Gera um subject para o convite no formato "0000000-00.2020.0.00.0000|<Algum assunto>".
	 */
	protected String getAssunto(String numeroDoProcesso, String assunto) {
		return numeroDoProcesso + "|" + assunto;
	}	
	
	/*
	 * Obtêm uma data/hora no formado específico da Microsoft.
	 */
	protected DateTimeTimeZone getDateTimeTimeZone(Date data) {
		DateTimeTimeZone dttz = new DateTimeTimeZone();
		Format formatter = new SimpleDateFormat(MICROSOFT_TEAMS_DATE_FORMAT);
		System.out.println(formatter.format(data));
		dttz.dateTime = formatter.format(data);
		dttz.timeZone = TIME_ZONE;
		return dttz;
	}	
	
	private Location getLocation(String salaAudiencia) {
		Location location = new Location();
		location.displayName = salaAudiencia;
		return location;
	}
	
	private String getCorpoMensagem(ProcessoAudiencia audiencia) {
		StringBuilder sb = new StringBuilder();
		sb.append("Nº do Processo: ").append(audiencia.getNumeroProcesso()).append("<br />");
		
		String orgaoOrigem = audiencia.getNomeOrgaoOrigem(); 
		if (orgaoOrigem != null && !orgaoOrigem.isEmpty()) {
			sb.append("Órgão Julgador: ").append(orgaoOrigem).append("<br />");	
		}
		
		String observacao = audiencia.getObservacao(); 
		if (observacao != null && !observacao.isEmpty()) {
			sb.append("Observação: ").append(observacao).append("<br />");			
		}
		return sb.toString();
	}		
}