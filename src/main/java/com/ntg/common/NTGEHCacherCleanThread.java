package com.ntg.common;

import java.util.ArrayList;
import java.util.Random;

public class NTGEHCacherCleanThread implements Runnable {

    private java.util.ArrayList<NTGEHCacher> ListForCheck = new ArrayList<>();

    public static NTGEHCacherCleanThread getInstance() {
        if (CleanThread == null) {
            CleanThread = new NTGEHCacherCleanThread();
        }
        return CleanThread;
    }

    private NTGEHCacherCleanThread() {

    }

    private static NTGEHCacherCleanThread CleanThread;
    private static Thread runThread;

    @Override
    public void run() {
        try {
            while (true) {
                runThread.setName("NTGEHCacher_" + System.currentTimeMillis() + new Random().nextLong());
                while (true) {
                    runThread.sleep(1000);
                    for (int i = 0, n = ListForCheck.size(); i < n; i++) {
                        ListForCheck.get(i).DoCleanCheck();
                    }
                }
            }
        } catch (Exception e) {
            NTGMessageOperation.PrintErrorTrace(e);
        }
        runThread = null;

    }

    public <V, K> void Add(NTGEHCacher<K, V> cacher) {

        ListForCheck.add(cacher);

    }

    public void startMe() {
        if (runThread == null) {
            runThread = new Thread(this);
            runThread.start();
        }
    }
}
