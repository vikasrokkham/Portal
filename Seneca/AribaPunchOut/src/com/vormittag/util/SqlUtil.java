package com.vormittag.util;

import java.util.logging.Logger;

public class SqlUtil {
	private static final String cName = SqlUtil.class.getCanonicalName();
	private static Logger log = Logger.getLogger(cName);
	
	public static String magicPrintSQL(String sql, Object... repl) throws SanityException {
		String cMethod = "magicPrintSQL";
		log.entering(cName, cMethod);
		StringBuffer sb = new StringBuffer();
		String[] st = sql.split("\\?");
		if (st.length < repl.length) {
			throw new SanityException("ID10T Error Detected. Too Few Question Marks or Too Many Parameters! nTok=" + st.length + "nRepl=" + repl.length);				
		}
		else if (st.length > repl.length + 1) {
			throw new SanityException("ID10T Error Detected. Too many Question Marks or Not Enough Parameters! nTok=" + st.length + "nRepl=" + repl.length);
		}
		int i = 0;
		try {
			log.finest("DEBUG:magicPrintSQL: initial string = " + sql);
			for (int ij = 0; ij < st.length; ij++) {
				sb.append(st[i]);
				if (i < repl.length) {
					sb.append(repl[i]);
					i++;
				}
				log.finest("DEBUG:magicPrintSQL: ij = " + ij + " str=" + sb.toString());
			}
		}
		catch (IndexOutOfBoundsException iobe) {
			throw new SanityException("ID10T Error Detected. Too many Question Marks or Not Enough Parameters!", iobe);
		}
		log.exiting(cName, cMethod, "SQL:" + sb.toString());
		return sb.toString();
	}

}
