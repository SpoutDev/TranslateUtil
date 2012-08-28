package org.spout.translationutil;

public class Occurence {
	public final int line;
	public final int column;
	public final String text;
	
	public Occurence(int line, int column, String text) {
		this.line = line;
		this.column = column;
		this.text = text;
	}
}