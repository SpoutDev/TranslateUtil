package org.spout.translationutil;

import java.util.ArrayList;
import java.util.List;

import org.spout.api.lang.Locale;

import com.beust.jcommander.IStringConverter;

public class LocaleListConverter implements IStringConverter<Locale> {
	public Locale convert(String arg0) {
//		String[] sp=arg0.split(",");
//		List<Locale> result = new ArrayList<Locale>(sp.length);
//		for (String s:sp) {
//			Locale l = Locale.getByCode(s);
//			result.add(l);
//		}
//		return result;
		return Locale.getByCode(arg0);
	}
}
