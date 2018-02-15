package tests;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import app_config.PropertiesReader;

public class Test {

	public static void main(String[] args) throws UnsupportedEncodingException {
		System.out.println("Start");
		boolean ok = PropertiesReader.openMailPanel();
		System.out.println("Done=" + ok);
	}
}
