package org.spout.translationutil;

import java.util.LinkedList;

public class TextTable {
	int columns = 0;
	Object header[] = null;
	LinkedList<Object[]> lines = new LinkedList<Object[]>();
	
	public TextTable(int columns) {
		this.columns = columns;
	}
	
	public void setHeader(Object ...cells) {
		if (cells.length == columns) {
			header = cells;
		}
	}
	
	public void addLine(Object ...cells) {
		if (cells.length == columns) {
			lines.add(cells);
		}
	}
	
	public void print() {
		// Calculate cell widths
		int widths[] = new int[columns];
		getMaxWidth(header, widths);
		for (Object[] cells:lines) {
			getMaxWidth(cells, widths);
		}
		// Print header
		StringBuilder delimiter = new StringBuilder();
		delimiter.append("|");
		for (int i = 0; i < widths.length; i++) {
			int length = widths[i];
			addStringTimes("=", length + 2, delimiter);
			delimiter.append("|");
		}
		System.out.println(delimiter);
		System.out.println(getLine(header, widths));
		System.out.println(delimiter);
		for (Object cells[]:lines) {
			System.out.println(getLine(cells, widths));
		}		
		System.out.println(delimiter);
	}
	
	private void addStringTimes(String str, int times, StringBuilder builder) {
		for (int i = 0; i < times; i++) {
			builder.append(str);
		}
	}
	
	private String getLine(Object cells[], int widths[]) {
		StringBuilder builder = new StringBuilder();
		builder.append("|");
		for (int i = 0; i < cells.length; i++) {
			builder.append(" ");
			if (!(cells[i] instanceof Number) && !cells[i].toString().endsWith("%")) {
				builder.append(cells[i]);
				addStringTimes(" ", widths[i] - cells[i].toString().length() + 1, builder);
			} else {				
				addStringTimes(" ", widths[i] - cells[i].toString().length(), builder);
				builder.append(cells[i]);
				builder.append(" ");
			}
			builder.append("|");
		}
		return builder.toString();
	}
	
	private void getMaxWidth(Object cells[], int widths[]) {
		for (int i = 0; i < cells.length; i++) {
			if (cells[i].toString().length() > widths[i]) {
				widths[i] = cells[i].toString().length();
			}
		}
	}
}
