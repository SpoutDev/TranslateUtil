package org.spout.translationutil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import org.spout.api.lang.JavaPluginDictionary;

public class MyPluginDictionary extends JavaPluginDictionary {
	File resourceDirectory;
	
	public MyPluginDictionary(File resourceDirectory) {
		this.resourceDirectory = resourceDirectory;
		load();
	}
	
	@Override
	protected File getLangDirectory() {
		return new File(resourceDirectory, "lang");
	}

	public void save(File file) {
		try {
			super.save(new FileWriter(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
