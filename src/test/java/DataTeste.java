import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

public class DataTeste {

	private final static String TIME_ZONE = "America/Sao_Paulo";
	
	public static void main(String[] args) {
		System.out.println(getDateTimeTimeZone(new Date()));

	}

	/*
	 * Obtêm uma data/hora no formado específico da Microsoft.
	 */
	protected static String getDateTimeTimeZone(Date data) {
		LocalDateTime localDt = data.toInstant().atZone(ZoneId.of(TIME_ZONE)).toLocalDateTime();
		ZonedDateTime zoneDt = ZonedDateTime.of(localDt, ZoneId.of(TIME_ZONE));
		return zoneDt.toString().replace("[America/Sao_Paulo]", "");
	}
	
}
