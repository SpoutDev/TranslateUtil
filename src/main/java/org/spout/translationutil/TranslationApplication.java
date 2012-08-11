package org.spout.translationutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.spout.api.exception.InvalidDescriptionFileException;
import org.spout.api.lang.LanguageDictionary;
import org.spout.api.lang.Locale;
import org.spout.api.lang.LocaleNumberHandler;
import org.spout.api.lang.PluginDictionary;
import org.spout.api.lang.Translation;
import org.spout.api.plugin.PluginDescriptionFile;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class TranslationApplication {
	@Parameter(names = {"--source", "-s"}, description = "Comma-delimited list of source directories.")
	private List<File> sourceDirectories;
	@Parameter(names = {"--resource", "-r"}, description = "Directory of the resources, where the lang/ folder and it's contents will be found.")
	private File resourceDirectory;
	@Parameter(names = {"--generate", "-g"}, description = "Comma-delimited list of locale files to generate/update", converter = LocaleListConverter.class)
	private List<Locale> toGenerate = Collections.emptyList();
	private static final String UNDONE_MARKUP = " <translate>";
	private static Set<String> methods = new HashSet<String>();
	
	
	private LinkedList<SourceClass> javaFiles = new LinkedList<SourceClass>();
	private PluginDescriptionFile pdf;
	private MyPluginDictionary dictionary;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TranslationApplication app = new TranslationApplication();
		JCommander commands = new JCommander(app);
		commands.parse(args);
		
		initMethods();
		
		app.initialize();
	}
	
	private static void initMethods() {
		for (Method m:Translation.class.getMethods()) {
			if (Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers()) && !Modifier.isNative(m.getModifiers())) {
				methods.add(m.getName());
			}
		}
	}

	protected void initialize() {
		// INIT
		File langDir = new File(resourceDirectory, "lang/");
		if (!langDir.exists()) {
			langDir.mkdirs();
		}
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
		for (File source:sourceDirectories) {
			searchJavaFiles(source, source);
		}

		System.out.println("Found " + javaFiles.size() + " source file"
				+ (javaFiles.size() != 1 ? "s" : "") + ".");

		// SEARCH NEW STRINGS
		int untranslated = 0;
		int progress = 0;
		int lastP = -1;
		for (SourceClass clazz:javaFiles) {
			List<Occurence> occurences = search(clazz.getFile());
			String classname = clazz.getClassName();
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
					String codedSource = dictionary.getCodedSource(id) + UNDONE_MARKUP;
					if (codedSource.contains("%n")) {
						try {
							LocaleNumberHandler handler = locale.getNumberHandler().newInstance();
							handler.init(codedSource);
							ld.setTranslation(id, handler);
						} catch (InstantiationException e) {
						} catch (IllegalAccessException e) {
						}
					} else {
						ld.setTranslation(id, codedSource);
					}
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
	
	private static String BEGIN = "(\"";
	private static String END = "\",";
	private static String ALTERNATIVE_END = ("\");");
	
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
	
	private static final String STATIC_IMPORT = "import static "+Translation.class.getName()+".";
	private static final String IMPORT = "import "+Translation.class.getName()+";";
	
	public static List<Occurence> search(File file) {
		List<Occurence> results = new LinkedList<Occurence>();
		Set<String> imported = new HashSet<String>();
		try {
			Scanner scanner = new Scanner(file);
			int lineNum = 0;
			boolean importedClass = false;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				
				int next;
				next = line.indexOf(STATIC_IMPORT);
				if (next != -1) {
					String imp = line.substring(next + STATIC_IMPORT.length(), line.length());
					imp = imp.substring(0, imp.indexOf(";"));
					imported.add(imp);
				}
				if (line.startsWith(IMPORT)) {
					importedClass = true;
				}
				for (String m:methods) {
					String begin = m + BEGIN;
					if (importedClass) {
						searchLine(results, lineNum, line, "Translation."+begin);
					}
					if (imported.contains(m)) {
						searchLine(results, lineNum, line, begin);
					}
					
				}
				lineNum ++;
			}
			scanner.close();
		} catch (IOException e) {
			System.out.println("Could not read file: "+file+", because of this error: "+e.getMessage());
		}
		return results;
	}

	protected static void searchLine(List<Occurence> results, int lineNum,
			String line, String begin) {
		int next;
		while ((next = line.indexOf(begin)) != -1) {
			char before = line.charAt(next - 1); // Check if this isn't part of another method
			if (Character.isLetter(before) || before == '.') {
				continue;
			}
			line = line.substring(next + begin.length());
			
			int end = line.indexOf(END);
			String foundEnd = END;
			if (end == -1) { //TODO multi-line strings
				end = line.indexOf(ALTERNATIVE_END);
				if (end == -1) {
					continue;
				} else {
					foundEnd = ALTERNATIVE_END;
				}
			}
			String result = line.substring(0, end);
			line = line.substring(end + foundEnd.length());
			Occurence o = new Occurence(lineNum, next, result);
			results.add(o);
		}
	}
	
//	public void skipWhitespace(Scanner scanner) {
//		while (scanner.hasNext("\\s")) {
//			scanner.next("\\s");
//		}
//	}

	public void searchJavaFiles(File baseDir, File persistentBaseDir) {
		if (baseDir.isDirectory()) {
			File[] files = baseDir.listFiles();
			for (File file : files) {
				if (file.getName().endsWith(".java")) {
					String className = getClassName(file, persistentBaseDir);
					javaFiles.add(new SourceClass(file, className));
				}
				if (file.isDirectory()) {
					searchJavaFiles(file, persistentBaseDir);
				}
			}
		}
	}
}
