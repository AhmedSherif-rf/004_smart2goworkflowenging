package com.ntg.common;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company:
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */

public class STAGESystemOut extends PrintStream {

	public static boolean OutPutOnScreen = false;
	public static boolean OutPutOnNTSLoger = true;

	static NTSLogger logger;
	static {
		logger = NTSLogger.getInstance();
	}

	public STAGESystemOut() {
		super(new OutputStream() {
			public void write(int int0) {
				// do nothing
			}
		});

	}

	 static PrintStream out;
	 static PrintStream err;

	public static void PrintNonDebugError(String s,String fileName) {
		if (err == null) {
			System.out.println(s);
		} else {
			err.println(s);
		}
		logger.LogError(s,fileName);
	}
	
	public static void PrintNonDebugError(String s) {
		if (err == null) {
			System.out.println(s);
		} else {
			err.println(s);
		}
		logger.LogError(s);
	}

	public static void PrintNonDebugInfo(String s,String fileName) {
		if (out == null) {
			System.out.println(s);
		} else {
			out.println(s);
		}
		logger.LogInfo(s,fileName);
	}
	
	public static void PrintNonDebugInfo(String s) {
		if (out == null) {
			System.out.println(s);
		} else {
			out.println(s);
		}
		logger.LogInfo(s);
	}

	public static void PrintNonDebugInfoWithNoNewLine(String s,String fileName) {
		if (out == null) {
			System.out.print(s);
		} else {
			out.print(s);
		}
		logger.LogInfo(s,fileName);

	}
	
	public static void PrintNonDebugInfoWithNoNewLine(String s) {
		if (out == null) {
			System.out.print(s);
		} else {
			out.print(s);
		}
		logger.LogInfo(s);

	}

	public static void PrintNonDebugInfo(double s,String fileName) {
		if (out == null) {
			System.out.println(s);
		} else {
			out.println(s);
		}
		logger.LogInfo(s + "", fileName);

	}
	
	public static void PrintNonDebugInfo(double s) {
		if (out == null) {
			System.out.println(s);
		} else {
			out.println(s);
		}
		logger.LogInfo(s + "");

	}

	public static void PrintNonDebugInfo(long s,String fileName) {
		if (out == null) {
			System.out.println(s);
		} else {
			out.println(s);
		}
		logger.LogInfo(s + "",fileName);

	}
	
	public static void PrintNonDebugInfo(long s) {
		if (out == null) {
			System.out.println(s);
		} else {
			out.println(s);
		}
		logger.LogInfo(s + "");

	}

	public static void OverrideSystemErrorOutput() {
		err = checkIfOverrided(System.err, "err");
		System.setErr(news);
		OutPutOnScreen = true;
	}

	static STAGESystemOut news;

	public static void OverrideSystemOutput() {
		if (!(System.out instanceof STAGESystemOut)) {

			out = checkIfOverrided(System.out, "out");
			if (news == null) {
				news = new STAGESystemOut();
			}
			System.setOut(news);
			out.println("System Out is Overrides, you can find the logs @ " + System.getProperty("user.dir")
					+ File.separator + "Smart2GoLog");
		}

	}

	private static PrintStream checkIfOverrided(Object o, String oName) {
		try {

			Field[] flist = o.getClass().getDeclaredFields();
			for (Field f : flist) {
				if (f.getName().equalsIgnoreCase(oName)) {
					f.setAccessible(true);
					o = f.get(null);
				}
			}

		} catch (Throwable ex) {
			NTGMessageOperation.PrintErrorTrace(new Exception(ex));
		}
		return (PrintStream) o;
	}

	public void println(double x,String fileName) {
		if (OutPutOnScreen) {
			out.println(x);
		}
		if (OutPutOnNTSLoger) {
			logger.LogInfo(x + "", fileName);
		}
	}
	
	


	public void print(char c,String fileName) {
		if (OutPutOnScreen) {
			out.print(c);
		}
		if (OutPutOnNTSLoger) {
			logger.LogInfo(c + "",fileName);
		}

	}
	


	public void print(float f,String fileName) {
		if (OutPutOnScreen) {
			out.print(f);
		}
		if (OutPutOnNTSLoger) {
			logger.LogInfo(f + "",fileName);
		}

	}
	


	public void print(long l,String fileName) {
		if (OutPutOnScreen) {
			out.print(l);
		}
		if (OutPutOnNTSLoger) {
			logger.LogInfo(l + "",fileName);
		}

	}
	


	public void println(char[] parm1,String fileName) {
		if (OutPutOnScreen) {
			out.println(parm1);
		}
		if (OutPutOnNTSLoger) {
			logger.LogInfo(String.valueOf(parm1),fileName);
		}

	}


	public void print(Object obj,String fileName) {
		if (OutPutOnScreen) {
			out.print(obj);
		}
		if (OutPutOnNTSLoger) {
			logger.LogInfo(String.valueOf(obj) + "",fileName);
		}
	}
	


	public void print(char[] parm1,String fileName) {
		if (OutPutOnScreen) {
			out.print(parm1);
		}
		if (OutPutOnNTSLoger) {
			logger.LogInfo(String.valueOf(parm1),fileName);
		}
	}
	


	public void println(Object x,String fileName) {
		if (OutPutOnScreen) {
			out.println(x);
		}
		if (OutPutOnNTSLoger) {
			logger.LogInfo(String.valueOf(x),fileName);
		}

	}
	


	public void print(boolean b) {
		if (OutPutOnScreen) {
			out.print(b);
		}

	}

	public void println(String x,String fileName) {
		if (OutPutOnScreen) {
			out.println(x);
		}
		if (OutPutOnNTSLoger) {
			logger.LogInfo(x,fileName);
		}

	}


	public void println(boolean x) {
		if (OutPutOnScreen) {
			out.println(x);
		}
	}

	public void println() {
		if (OutPutOnScreen) {
			out.println();
		}
	}

	public void println(long x) {
		if (OutPutOnScreen) {
			out.println(x);
		}
	}

	public void println(int x) {
		if (OutPutOnScreen) {
			out.println(x);
		}
	}

	public void println(char x) {
		if (OutPutOnScreen) {
			out.println(x);
		}

	}

	public void println(float x) {
		if (OutPutOnScreen) {
			out.println(x);
		}

	}

	public void print(int i) {
		if (OutPutOnScreen) {
			out.print(i);
		}

	}

	public void print(String s) {
		if (OutPutOnScreen) {
			out.print(s);
		}

	}

	public void print(double d) {
		if (OutPutOnScreen) {
			out.print(d);
		}

	}

	
	
	public void println(double x) {
		if (OutPutOnScreen) {
			out.println(x);
		}
		if (OutPutOnNTSLoger) {
			logger.LogInfo(x + "",null);
		}
	}

	public void print(char c) {
		if (OutPutOnScreen) {
			out.print(c);
		}
		if (OutPutOnNTSLoger) {
			logger.LogInfo(c + "", null);
		}

	}

	public void print(float f) {
		if (OutPutOnScreen) {
			out.print(f);
		}
		if (OutPutOnNTSLoger) {
			logger.LogInfo(f + "", null);
		}

	}

	public void print(long l) {
		if (OutPutOnScreen) {
			out.print(l);
		}
		if (OutPutOnNTSLoger) {
			logger.LogInfo(l + "", null);
		}

	}





	public void print(char[] parm1) {
		if (OutPutOnScreen) {
			out.print(parm1);
		}
		if (OutPutOnNTSLoger) {
			logger.LogInfo(String.valueOf(parm1), null);
		}
	}

	public void println(Object x) {
		if (OutPutOnScreen) {
			out.println(x);
		}
		if (OutPutOnNTSLoger) {
			logger.LogInfo(String.valueOf(x), null);
		}

	}



	public void println(String x) {
		if (OutPutOnScreen) {
			out.println(x);
		}
		if (OutPutOnNTSLoger) {
			logger.LogInfo(x, null);
		}

	}

	public static void err_println(String e) {
		if (STAGESystemOut.err == null) {
			if (OutPutOnScreen) {
				System.err.println(e);
			}
		} else {
			if (OutPutOnScreen) {
				STAGESystemOut.err.println(e);
			}
		}
		
		if (OutPutOnNTSLoger) {
			logger.LogError(e, null);
		}
	}

}
