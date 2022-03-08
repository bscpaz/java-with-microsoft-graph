package br.bscpaz.msgraph.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class PropertyReader {

	private static final String PROPERTIES_FILE = "application.properties";
	private static Properties properties = null;
	
	private static void onInit() throws IOException {
		if (properties == null) {
			properties = new Properties();
			properties.load(PropertyReader.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE));
		}
	}
	
    public static String getProperty(String property)  {
    	try {
			onInit();
			return properties.getProperty(property);
		} catch (IOException e) {
			System.out.println("Erro: Arquivo de propriedade " + PROPERTIES_FILE + " do conector não encontrado.");
			e.printStackTrace();
		}
    	return null;
    }
    
    public static List<String> getListProperty(String property)  {
    	try {
			onInit();
			String values = properties.getProperty(property);
			return Arrays.asList(values.split(","));
		} catch (IOException e) {
			System.out.println("Erro: Arquivo de propriedade " + PROPERTIES_FILE + " do conector não encontrado.");
			e.printStackTrace();
		}
    	return null;
    }    
}
