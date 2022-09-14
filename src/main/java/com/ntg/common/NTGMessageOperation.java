/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ntg.common;

import org.slf4j.MDC;

/**
 * @author IBOSS
 */
public class NTGMessageOperation {
    public static Exception lastPrintedException;

    public static void PrintErrorTrace(Exception err) {
        PrintErrorTrace(err, "");
    }

    public static void PrintErrorTrace(Exception err, String TxID) {
        if (lastPrintedException == null || !lastPrintedException.equals(err)) {
            lastPrintedException = err;
            String e = GetErrorTrace(err,TxID);
            System.err.println(e);

        }
    }

    static long ErrorNo = 0;

    public static String GetErrorTrace(Exception err, String in_txID) {
        long ErrNo = ErrorNo++;
        String M = err.getMessage();
        String TxID = (in_txID == null) ? MDC.get("transactionId") : in_txID;
        if (TxID == null) {
            TxID = "";
        } else {
            TxID = TxID + ":";
        }
        StringBuffer er = new StringBuffer(TxID).append("**************Error#" + ErrNo + " **********************\n");
        Throwable cerr = null;

        if (M == null) {
            M = "UnKnow Error";
        }

        if (err instanceof org.springframework.web.client.HttpServerErrorException) {
            M = ((org.springframework.web.client.HttpServerErrorException) err).getResponseBodyAsString();
        }
        er.append("Err Msg : " + M + "(" + err.getClass().getName() + ")");

        Throwable rootEx = err.getCause();
        while (rootEx != null) {
            if (rootEx.getMessage() != null) {
                er.append("\n");
                StackTraceElement[] t = rootEx.getStackTrace();
                if (t != null && t.length > 0) {
                    er.append(" Caused By (" + t[0].getClassName() + ") : ");
                    er.append(",Msg:").append(rootEx.getMessage());
                } else {
                    er.append("Caused By Msg:").append(rootEx.getMessage());
                }
            }
            rootEx = rootEx.getCause();
        }

        er.append("\n").append("ErrTrace: ..\n");

        java.lang.StackTraceElement[] ErrList = (cerr == null) ? err.getStackTrace() : cerr.getStackTrace();
        for (int i = 0; i < ErrList.length; i++) {
            String e = ErrList[i].toString();
            if (e.indexOf("com.ntg") > -1) {
                er.append("\n");
                er.append(e);
            }
        }

        er.append("\n").append("************** End Error#" + ErrNo + " **********************");

        return er.toString().replaceAll("\n", "\n" + TxID);
    }

    public static void Debug(String DebugText) {
    }

    public static String GetCurrentTrace() {
        String er = "";
        boolean foundNTGPak = false;
        java.lang.StackTraceElement[] ErrList = Thread.currentThread().getStackTrace();
        for (int i = 2; i < ErrList.length; i++) {
            String e = ErrList[i].toString();
            if (e.indexOf("com.ntg") > -1) {
                er += "\n" + e;
                foundNTGPak = true;
            }
        }

        if (foundNTGPak == false) {
            for (int i = 2; i < 5; i++) {
                String e = ErrList[i].toString();
                er += "\n" + e;
            }
        }
        return er;
    }

}
