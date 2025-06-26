package com.carrington.WIA.IO;


import java.io.IOException;
import java.nio.charset.StandardCharsets;


public class EnclosedTxtFileReader {
	
	
	public static String getWavesHelp() {
		
		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/waveshelp.txt").readAllBytes();
			
			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {
			
			
			e.printStackTrace();
			return "";
		}
		
	}
	
	public static String getDiameterHelp() {
		
		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/diameterhelp.txt").readAllBytes();
			
			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {
			
			
			e.printStackTrace();
			return "";
		}
		
	}
	
	
	public static String getWavePanelHelp() {
		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/wavepickerpanelhelp.txt").readAllBytes();
			
			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {
			
			
			e.printStackTrace();
			return "";
		}
	}
	
	public static String getSelectBeatPanelHelp() {
		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/selectbeatshelp.txt").readAllBytes();
			
			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {
			
			
			e.printStackTrace();
			return "";
		}
	}
	
	public static String getAlignByTimeHelp() {
		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/alignbytimehelp.txt").readAllBytes();
			
			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {
			
			
			e.printStackTrace();
			return "";
		}
	}
}
