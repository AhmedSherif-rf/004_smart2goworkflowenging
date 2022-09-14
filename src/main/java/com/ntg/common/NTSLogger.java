package com.ntg.common;

/**
 * <p>
 * Title: JNTS
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2005
 * </p>
 * <p>
 * Company: NTG Clarity Egypt Branch
 * </p>
 *
 * @author Ahmed Hashim
 * @version 1.0
 */


import com.ntg.engine.Application;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

/**
 * @author Ahmed Hashim
 * @see <a href=
 * "http://supportweb.cs.bham.ac.uk/documentation/tutorials/docsystem/build/tutorials/log4j/log4j.html">log4j</a>
 */
public class NTSLogger extends Thread {

     @Value("${log_files}")
     static String ConfigLogFileUrl;

    public static final int MSG_TYPE_WARNING = 1;
    public static final int MSG_TYPE_DEBUG = 2;
    public static final int MSG_TYPE_INFO = 3;
    public static final int MSG_TYPE_ERROR = 7;
    public static final int MSG_TYPE_FATAL_ERROR = 8;
    private static NTSLogger thisInstance;
    private static Class thisClass;

    // private static Logger me = null;
    static {
        thisInstance = new NTSLogger();
        thisInstance.setName("Smart2GoLog");
        thisInstance.start();
    }

    public void run() {
        while (true) {
            NTSLogger.compressOldNTSLogFile();
            try {
                sleep(36000000);
            } catch (InterruptedException e) {
                NTGMessageOperation.PrintErrorTrace(e);
            }

        }
    }

    /*
     * private static File checkOldFile() { try { String sep = System.getProperty("file.separator"); String date =
     * GetCurDate(java.util.Calendar.getInstance(). getTime()); date = date.replace('/', '-'); String
     * file = System.getProperty("user.dir") + sep + "Smart2GoLog"; File f = new File(file); if (!f.exists()) { f.mkdirs();
     * } file += sep + "Smart2GoLog-" + date + ".log"; //System.out.println(file); f = new File(file); //compress old file
     * Calendar c = Calendar.getInstance(); c.add(c.DAY_OF_MONTH, -1); date =
     * GetCurDate(c.getTime()).replace('/', '-'); String oldFilePath = System.getProperty("user.dir") +
     * sep + "Smart2GoLog"; String FName = "Smart2GoLog-" + date; oldFilePath += sep + FName + ".log"; File oldFile = new
     * File(oldFilePath); if (oldFile.exists()) { NTGCompressor.Compress(oldFilePath, oldFilePath + ".zip"); //delete
     * old file oldFile.delete(); } //End compress old file if (!f.exists()) { f.createNewFile(); } return f; } catch
     * (Exception ex) { NTGMessageOperation.PrintErrorTrace(ex); } return null; }
     */
    private NTSLogger() {
        createDirectoryIfNotExists();
        // thisClass = this.getClass();
        // initMe();
    }

    /*
     * private static void initMe() { try { me = Logger.getLogger(thisClass); //String str = Customization.Me.LogFile;
     * File file = checkOldFile(); PropertyConfigurator.configure(file.toString()); WriterAppender appender = null;
     * String pattern = ""; //pattern += "Classname: %C %n"; // pattern += "Date: %d{ISO8601} %n"; //pattern +=
     * "Location of log event: %l %n"; pattern += "{%d{ISO8601}}: %m%n"; PatternLayout layout = new
     * PatternLayout(pattern); appender = new WriterAppender(layout, new FileOutputStream(file, true));
     * me.addAppender(appender); //System.out.println("ok"); } catch (Exception ex) {
     * NTGMessageOperation.PrintErrorTrace(ex); } }
     */
    public static synchronized NTSLogger getInstance() {
        return thisInstance;
    }

    public void createDirectoryIfNotExists() {




        if (ConfigLogFileUrl != null && ConfigLogFileUrl.equals("") == false) {
            ConfigLogFileUrl = ConfigLogFileUrl.replace("file:", "").replace("${user.home}", System.getProperty("user.dir"));
        } else {
            ConfigLogFileUrl = System.getProperty("user.home") + File.separator + ".Smart2GoConfig" + File.separator + "logs";
        }
        if (!File.separator.equals("/")) {
            while (ConfigLogFileUrl.contains("/")) {
                ConfigLogFileUrl = ConfigLogFileUrl.replace("/", File.separator);
            }
        } else if (!File.separator.equals("\\")) {
            while (ConfigLogFileUrl.contains("\\")) {
                ConfigLogFileUrl = ConfigLogFileUrl.replace("\\", File.separator);
            }
        }

        File f = new File(ConfigLogFileUrl);
        if (f.exists() == false) {
            f.mkdirs();
        }

    }

    public static String GetCurDate(Date SpecificDate, String Format) {
        try {
            SimpleDateFormat DF;
            DF = new SimpleDateFormat(Format);
            return DF.format(SpecificDate);
        } catch (Exception err) {
            return "";
        }

    }

    private static void logMessage(int msgType, String message, String fileName) {
        try {
            java.io.FileWriter w = null;

            String CompanyName = "engine";
            ;
            String LoginUserName = "wf";


            if (fileName != null) {
                fileName = ConfigLogFileUrl + File.separator + fileName;
            } else {
                fileName = ConfigLogFileUrl + File.separator + ((CompanyName == null) ? "" : (CompanyName + "-" + LoginUserName + "_")) + "-"
                        + GetCurDate(Calendar.getInstance().getTime(), "yyyyMMdd") + ".log";
            }
            File f = new File(fileName);
            boolean isNewFile = !f.exists();
            if (isNewFile) {
                f.createNewFile();
                w = new java.io.FileWriter(f, true);
                w.write("******************************************************************************************\r\n");
                w.write("*             STAGE Loger File \r\n");
                w.write("******************************************************************************************");

                w.write("\r\n");

            }
            if (w == null) {
                w = new java.io.FileWriter(f, true);
            }
            w.write(GetCurDate(new Date(System.currentTimeMillis()), "HH:mm:ss") + "(" + msgType + ")" + " : " + message);
            w.write("\r\n");
            w.close();

        } catch (Exception ex) {
            STAGESystemOut.OutPutOnNTSLoger = false;
            System.out.println("Can't log to log file.");
            NTGMessageOperation.PrintErrorTrace(ex);
        }

    }

    private static void logMessage(int msgType, String message) {
        try {
            java.io.FileWriter w = null;

            String CompanyName = "engine";
            ;
            String LoginUserName = "wf";


            String fileName = ((ConfigLogFileUrl != null && ConfigLogFileUrl.equals("") == false) ? ConfigLogFileUrl
                    : System.getProperty("user.dir") + File.separator + "Smart2GoLog") + File.separator
                    + ((CompanyName == null) ? "" : (CompanyName + "-" + LoginUserName + "_")) + "STAG-"
                    + GetCurDate(Calendar.getInstance().getTime(), "yyyyMMdd") + ".log";
            File f = new File(fileName);
            boolean isNewFile = !f.exists();
            if (isNewFile) {
                f.createNewFile();
                w = new java.io.FileWriter(f, true);
                w.write("******************************************************************************************\r\n");
                w.write("*             STAGE Loger File \r\n");
                w.write("******************************************************************************************");

                w.write("\r\n");

            }
            if (w == null) {
                w = new java.io.FileWriter(f, true);
            }
            w.write(GetCurDate(new Date(System.currentTimeMillis()), "HH:mm:ss") + "(" + msgType + ")" + " : " + message);
            w.write("\r\n");
            w.close();

        } catch (Exception ex) {
            STAGESystemOut.OutPutOnNTSLoger = false;
            System.out.println("Can't log to log file.");
            NTGMessageOperation.PrintErrorTrace(ex);
        }

    }

    /*
     * private static void logMessage(int msgType, String message,String fileName) { try { java.io.FileWriter w = null;
     * java.io.File f = new java.io.File(System.getProperty("user.dir") + File.separator + "Smart2GoLog" + File.separator
     * +fileName + "_" + "STAG-" + GetCurDate(java.util.Calendar .getInstance().getTime(), "yyyyMMdd")
     * + ".log"); boolean isNewFile = !f.exists(); if (isNewFile) { f.createNewFile(); w = new java.io.FileWriter(f,
     * true); w.write("******************************************************************************************\r\n");
     * w.write("*             STAGE Loger File \r\n");
     * w.write("******************************************************************************************");
     * w.write("\r\n"); } if (w == null) { w = new java.io.FileWriter(f, true); } w.write(GetCurDate(
     * new java.util.Date(System.currentTimeMillis()), "HH:mm:ss") + "(" + msgType + ")" + " : " + message);
     * w.write("\r\n"); w.close(); } catch (Exception ex) { STAGESystemOut.OutPutOnNTSLoger = false;
     * System.out.println("Can't log to log file."); NTGMessageOperation.PrintErrorTrace(ex); } }
     */
    public static void compressOldNTSLogFile() {
        Calendar c = Calendar.getInstance();

        String date = GetCurDate(c.getTime(), "yyyyMMdd");
        String BasePath = ((ConfigLogFileUrl != null && ConfigLogFileUrl.equals("") == false) ? ConfigLogFileUrl
                : System.getProperty("user.dir") + File.separator + "Smart2GoLog");
        String BasePathAr = ((ConfigLogFileUrl != null && ConfigLogFileUrl.equals("") == false) ? ConfigLogFileUrl
                : System.getProperty("user.dir") + File.separator + "Smart2GoLog") + File.separator + "Archive";
        File f = new File(BasePathAr);
        if (f.exists() == false) {
            f.mkdirs();
        }
        f = new File(BasePath);
        String[] list = f.list();
        if (list != null) {
            for (String FileName : list) {
                if (FileName.contains(date) == false) {
                    String oldFilePath = BasePath + File.separator + FileName;

                    File oldFile = new File(oldFilePath);
                    if (oldFile.exists() && oldFile.isDirectory() == false) {
                        NTGCompressor.Compress(oldFilePath, BasePathAr + File.separator + FileName + ".zip", FileName);
                        // delete old file
                        oldFile.delete();
                    }
                }
            }
        }

    }

    /*
     * public static void LogInfo(String message) { logMessage(NTSLogger.MSG_TYPE_INFO, message); } public static void
     * LogDebug(String message) { // checkOldFile(); // initMe(); // me.debug(message);
     * logMessage(NTSLogger.MSG_TYPE_DEBUG, message); } public static void LogError(String message) { // checkOldFile();
     * // initMe(); // me.error(message); logMessage(NTSLogger.MSG_TYPE_ERROR, message); } public static void
     * LogFatalError(String message) { // checkOldFile(); // initMe(); // me.fatal(message);
     * logMessage(NTSLogger.MSG_TYPE_FATAL_ERROR, message); } public static void LogWarining(String message) { //
     * checkOldFile(); // initMe(); // me.warn(message); logMessage(NTSLogger.MSG_TYPE_WARNING, message); }
     */
    public static void LogInfo(String message, String fileName) {
        logMessage(NTSLogger.MSG_TYPE_INFO, message, fileName);
    }

    public static void LogInfo(String message) {
        logMessage(NTSLogger.MSG_TYPE_INFO, message);
    }

    public static void LogDebug(String message, String fileName) {

        logMessage(NTSLogger.MSG_TYPE_DEBUG, message, fileName);
    }

    public static void LogError(String message, String fileName) {

        logMessage(NTSLogger.MSG_TYPE_ERROR, message, fileName);
    }

    public static void LogError(String message) {

        logMessage(NTSLogger.MSG_TYPE_ERROR, message);
    }

    public static void LogFatalError(String message, String fileName) {

        logMessage(NTSLogger.MSG_TYPE_FATAL_ERROR, message, fileName);
    }

    public static void LogWarining(String message, String fileName) {

        logMessage(NTSLogger.MSG_TYPE_WARNING, message, fileName);
    }
    /*
     * public static void SetLevel(String level) { if (Level.INFO.toString().equalsIgnoreCase(level)) {
     * me.setLevel(Level.INFO); } else if (Level.ALL.toString().equalsIgnoreCase(level)) { me.setLevel(Level.ALL); }
     * else if (Level.DEBUG.toString().equalsIgnoreCase(level)) { me.setLevel(Level.DEBUG); } else if
     * (Level.OFF.toString().equalsIgnoreCase(level)) { me.setLevel(Level.OFF); } else if
     * (Level.ERROR.toString().equalsIgnoreCase(level)) { me.setLevel(Level.ERROR); } else if
     * (Level.FATAL.toString().equalsIgnoreCase(level)) { me.setLevel(Level.FATAL); } }
     */

    // public static void LogInfo(String message) {
    // // checkOldFile();
    // initMe();
    // me.info(message);
    // }
}
