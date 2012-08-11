package org.spout.translationutil;

import java.io.File;

public class SourceClass {
	private File file;
	private String className;
	
	public SourceClass(File file, String className) {
		this.file = file;
		this.className = className;
	}

	public File getFile() {
		return file;
	}

	public String getClassName() {
		return className;
	}
}
