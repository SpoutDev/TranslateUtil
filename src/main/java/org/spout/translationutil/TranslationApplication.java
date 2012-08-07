package org.spout.translationutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.spout.api.exception.InvalidDescriptionFileException;
import org.spout.api.lang.LanguageDictionary;
import org.spout.api.lang.Locale;
import org.spout.api.lang.PluginDictionary;
import org.spout.api.plugin.PluginDescriptionFile;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class TranslationApplication {
	@Parameter(names = {"--source", "-s"}, description = "Directory of the source code.")
	private File sourceDirectory;
	@Parameter(names = {"--resource", "-r"}, description = "Directory of the resources, where the lang/ folder and it's contents will be found.")
	private File resourceDirectory;
	@Parameter(names = {"--generate", "-g"}, description = "Comma-delimited list of locale files to generate/update", converter = LocaleListConverter.class)
	private List<Locale> toGenerate = Collections.emptyList();
	private static final String UNDONE_MARKUP = " <translate>";
	
	private LinkedList<File> javaFiles = new LinkedList<File>();
	private PluginDescriptionFile pdf;
	private MyPluginDictionary dictionary;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		TranslationApplication app = new TranslationApplication();
		JCommander commands = new JCommander(app);
		commands.parse(args);
		
		app.initialize();
	}

	protected void initialize() {
		// INIT
		try {
			pdf = new PluginDescriptionFile(new FileInputStream(new File(resourceDirectory, "properties.yml")));
		} catch (IOException e) {
		} catch (InvalidDescriptionFileException e) {
		}
		if (pdf != null) {
			System.out.println("Found plugin: " + pdf.getName());
		} else {
			System.out.println("No properties.yml found or error parsing it!");
		}
		dictionary = new MyPluginDictionary(resourceDirectory);
		
		// SEARCH FOR JAVA CLASSES
		searchJavaFiles(sourceDirectory);

		System.out.println("Found " + javaFiles.size() + " source file"
				+ (javaFiles.size() != 1 ? "s" : "") + ".");

		// SEARCH NEW STRINGS
		int untranslated = 0;
		int progress = 0;
		int lastP = -1;
		for (File file:javaFiles) {
			List<Occurence> occurences = search(file);
			String classname = getClassName(file, sourceDirectory);
			for (Occurence occurence : occurences) {
				int key = dictionary.getKey(occurence.text, classname);
				if (key == PluginDictionary.NO_ID) { // This is a new string
					System.out.println(classname + ":"+(occurence.line + 1)+": "+occurence.text);
					key = dictionary.getNextKey();
					dictionary.setKey(occurence.text, classname, key);
					untranslated ++;
				}
			}
			progress ++;
			if (javaFiles.size() >= 10) {
				int p = (int) ((double) progress / (double) javaFiles.size() * 100);
				if (p % 10==0 && lastP != p) {
					System.out.println("Scanning: "+ p + "%");
					lastP = p;
				}
			}
		}
		if (untranslated > 0) {
			System.out.println("New strings: "+untranslated);
			System.out.println("Saved new strings in "+new File(resourceDirectory, "lang/keymap.yml"));
			dictionary.save(new File(resourceDirectory, "lang/keymap.yml"));
		} else {
			System.out.println("No new strings");
		}
		
		// GENERATE LANGUAGE FILES
		TextTable table = new TextTable(5);
		table.setHeader("Language", "Strings", "new", "undone", "progress");
		List<Integer> ids = dictionary.getIdList();
		for (int i = 0; i < toGenerate.size(); i++) {
			Locale locale = toGenerate.get(i);
			int changes = 0;
			int undone = 0;
			LanguageDictionary ld = dictionary.getDictionary(locale);
			if (ld == null) {
				ld = new LanguageDictionary(locale);
				dictionary.setDictionary(locale, ld);
			}
			for (int id:ids) {
				String translation = ld.getTranslation(id);
				if (translation == null) {
					ld.setTranslation(id, dictionary.getCodedSource(id) + UNDONE_MARKUP);
					changes ++;
					undone ++;
				} else if (translation.endsWith(UNDONE_MARKUP)) {
					undone ++;
				}
			}
			try {
				ld.save(new FileWriter(new File(resourceDirectory, "lang/lang-"+locale.getCode()+".yml")));
			} catch (IOException e) {
			}
			table.addLine(locale.getCode(), ids.size(), changes, undone, ((int) ((double) (ids.size() - undone) / (double) ids.size() * 100)) + "%");
		}
		table.print();
	}
	
	public static String getClassName(File clazz, File sourceDirectory) {
		String classname = clazz.getAbsolutePath().replaceFirst(sourceDirectory.getAbsolutePath()+File.separator, "");
		classname = classname.replaceAll("\\.java$", "");
		classname = classname.replaceAll("\\/", ".");
		return classname;
	}
	
	private static String BEGIN = "tr(\"";
	private static String END = "\",";
	
	public static class Occurence {
		public final int line;
		public final int column;
		public final String text;
		
		public Occurence(int line, int column, String text) {
			this.line = line;
			this.column = column;
			this.text = text;
		}
	}
	
	public static List<Occurence> search(File file) {
		List<Occurence> results = new LinkedList<Occurence>();
		try {
			Scanner scanner = new Scanner(file);
			int lineNum = 0;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				int next;
				while ((next = line.indexOf(BEGIN)) != -1) {
					line = line.substring(next + BEGIN.length());
					
					int end = line.indexOf(END);
					
					if (end == -1) { //TODO multiline strings
						break;
					}
					String result = line.substring(0, end);
					line = line.substring(end + END.length());
					Occurence o = new Occurence(lineNum, next, result);
					results.add(o);
				}
				lineNum ++;
			}
			scanner.close();
		} catch (IOException e) {
			System.out.println("Could not read file: "+file+", because of this error: "+e.getMessage());
		}
		return results;
	}

	public void searchJavaFiles(File baseDir) {
		if (baseDir.isDirectory()) {
			File[] files = baseDir.listFiles();
			for (File file : files) {
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
