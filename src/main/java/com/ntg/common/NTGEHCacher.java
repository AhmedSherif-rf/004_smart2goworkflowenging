package com.ntg.common;

import com.ntg.engine.jobs.JobUtil;

import java.util.Date;
import java.util.Hashtable;
import java.util.Random;

public class NTGEHCacher<K, V> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private String PrintRemovedItem = null;
    long KeepLifeFor;


    public NTGEHCacher(long KeepLifeFor /* Keep Life by MSec */, String PrintRemovedItem) {
        this.KeepLifeFor = KeepLifeFor;
        this.PrintRemovedItem = PrintRemovedItem;
        NTGEHCacherCleanThread.getInstance().Add(this);
    }

    public NTGEHCacher(long KeepLifeFor /* Keep Life by MSec */) {
        this.KeepLifeFor = KeepLifeFor;
        NTGEHCacherCleanThread.getInstance().Add(this);
    }

    private Hashtable<K, NTGEHCacherStorInformation<V>> Storge = new Hashtable<K, NTGEHCacherStorInformation<V>>();

    public int size() {
        return Storge.size();
    }

    public synchronized void put(K paramK, V paramV) {
        Storge.put(paramK, new NTGEHCacherStorInformation<V>(paramV));
        NTGEHCacherCleanThread.getInstance().startMe();
    }

    public synchronized V IsExist(K paramK) {
        NTGEHCacherStorInformation<V> info = Storge.get(paramK);
        if (info == null) {
            return null;
        } else {
            return info.paramV;
        }
    }

    public synchronized V get(K paramK) {
        NTGEHCacherStorInformation<V> info = Storge.get(paramK);
        if (info == null) {
            return null;
        } else {
            info.LastAccessTime = System.currentTimeMillis();
            return info.paramV;
        }
    }


    public synchronized void remove(K paramK) {

        Storge.remove(paramK);

        if (Storge.get(paramK) != null) {
            JobUtil.Debug(paramK + " Not Removed Try Again ");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            Storge.remove(paramK);
            if (Storge.get(paramK) != null) {
                JobUtil.Debug(paramK + " Not Removed Try Again Second!");
            }
        }
    }

    void DoCleanCheck() throws Exception {
        long currentTime = System.currentTimeMillis();
        Object[] Keys = Storge.keySet().toArray();
        if (Keys != null) {
            for (Object Key : Keys) {
                NTGEHCacherStorInformation info = Storge.get(Key);
                if (currentTime - info.LastAccessTime > KeepLifeFor) {
                    // Expire
                    if (PrintRemovedItem != null) {
                        NTGEHCacherStorInformation data = ((NTGEHCacherStorInformation) Storge.get(Key));
                        JobUtil.Debug("Info : " + PrintRemovedItem + ":Removing:" + Key + "//" + ((data != null) ? data.paramV + "//" + new Date(data.LastAccessTime) + "//" + data.CallTrace : ""));
                    }
                    Storge.remove(Key);
                }

            }
        }

    }

}
