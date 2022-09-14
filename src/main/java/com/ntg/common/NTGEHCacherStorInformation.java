package com.ntg.common;

public class NTGEHCacherStorInformation<V> {
    public NTGEHCacherStorInformation(V paramV) {
        this.paramV = paramV;
        this.LastAccessTime = System.currentTimeMillis();
        this.CallTrace = GetCurrentTrace();
    }

    V paramV;
    long LastAccessTime;
    String CallTrace;


    public static String GetCurrentTrace() {
        String er = "";
        java.lang.StackTraceElement[] ErrList = Thread.currentThread().getStackTrace();
        for (int i = 2; i < ErrList.length; i++) {
            String e = ErrList[i].toString();
            if (e.indexOf("com.ntg.") > -1) {
                er += "<-->" + e;
            }
        }

        return er;
    }

}