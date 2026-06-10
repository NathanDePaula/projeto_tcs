package com.projetotcs.instagram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.swing.JOptionPane;

@SpringBootApplication
@EnableScheduling
public class InstagramApplication {

	public static void main(String[] args) {
		String port = null;

		// Try graphical popup first if desktop environment is available
		if (!GraphicsEnvironment.isHeadless()) {
			try {
				port = JOptionPane.showInputDialog(null, 
					"Digite a porta do servidor a ser usada (deixe vazio para a padrão 23900):", 
					"Configuração da Porta", 
					JOptionPane.QUESTION_MESSAGE);
			} catch (Exception e) {
				// Fail silently and fall back to console
			}
		}

		// Fall back to console input if popup is not available or returned empty
		if (port == null || port.trim().isEmpty()) {
			try {
				System.out.println("Digite a porta do servidor a ser usada (pressione ENTER para a padrão 23900):");
				BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				// Check if there is data available to avoid blocking in non-interactive tasks
				if (System.console() != null || System.in.available() > 0) {
					String line = reader.readLine();
					if (line != null && !line.trim().isEmpty()) {
						port = line.trim();
					}
				}
			} catch (Exception e) {
				// Fail silently
			}
		}

		if (port != null && !port.trim().isEmpty()) {
			try {
				int portNum = Integer.parseInt(port.trim());
				System.setProperty("server.port", String.valueOf(portNum));
				System.out.println("Servidor configurado para iniciar na porta: " + portNum);
			} catch (NumberFormatException e) {
				System.out.println("Porta inválida. Usando porta padrão 23900.");
			}
		} else {
			System.out.println("Usando porta padrão 23900.");
		}

		SpringApplication.run(InstagramApplication.class, args);
	}

}
