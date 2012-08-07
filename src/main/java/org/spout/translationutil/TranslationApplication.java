package org.spout.translationutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;

import org.spout.api.exception.InvalidDescriptionFileException;
import org.spout.api.plugin.PluginDescriptionFile;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class TranslationApplication {
	@Parameter(names = {"--source", "-s"}, description = "Directory of the source code.")
	private File sourceDirectory;
	@Parameter(names = {"--resource", "-r"}, description = "Directory of the resources, where the lang/ folder and it's contents will be found.")
	private File resourceDirectory;
	
	private LinkedList<File> javaFiles = new LinkedList<File>();
	private PluginDescriptionFile pdf;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		TranslationApplication app = new TranslationApplication();
		JCommander commands = new JCommander(app);
		commands.parse(args);
		
		System.out.println("Source: "+app.sourceDirectory);
		System.out.println("Resource: "+app.resourceDirectory);
		
		try {
			app.pdf = new PluginDescriptionFile(new FileInputStream(new File(app.resourceDirectory, "properties.yml")));
		} catch (IOException e) {
			System.out.println("Error while reading plugin's properties.yml: "+e.getMessage());
		} catch (InvalidDescriptionFileException e) {
			System.out.println("Error while reading plugin's properties.yml: "+e.getMessage());
		}
		System.out.println("Found plugin: "+app.pdf.getName());
		
		app.searchJavaFiles(app.sourceDirectory);
		
		System.out.println(app.javaFiles.size() + " source files found.");
		
	}
	
	public void searchJavaFiles(File baseDir) {
		if (baseDir.isDirectory()) {
			File [] files = baseDir.listFiles();
			for (File file:files) {
				if (file.getName().endsWith(".java")) {
					javaFiles.add(file);
				}
				if (file.isDirectory()) {
					searchJavaFiles(file);
				}
			}
		}
	}
}
