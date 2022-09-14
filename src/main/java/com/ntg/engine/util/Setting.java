package com.ntg.engine.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.ntg.engine.Application;
import org.springframework.stereotype.Component;

import com.ntg.common.NTGMessageOperation;

@Component

public class Setting {

    public static int PoolSizeForDBPost;
    public static int BlockIfPoolSizeRetchTo;
    public static int ForceSaveEvery;
    public static boolean EnbaleDebugPrint = false;
    public static int SleepPeriodOnNoDataFound;
    public static String databasetype;
    public static long CacheEmployeeInfoFor;

    static {
        try {
            LoadSetting();
            com.ntg.engine.jobs.JobUtil.Info("Setting Loaded ... EnbaleDebugPrint is " + Setting.EnbaleDebugPrint);
        } catch (Exception e) {
            NTGMessageOperation.PrintErrorTrace(e);
        }
    }

    public static void LoadSetting() throws Exception {

        Properties props = Application.intializeSettings("applicationWFEngin.properties", true, false);

        Setting.PoolSizeForDBPost = Integer.valueOf(props.getProperty("NTG.WFEngine.SaveThread.PoolSizeForDBPost"));
        Setting.BlockIfPoolSizeRetchTo = Integer
                .valueOf(props.getProperty("NTG.WFEngine.SaveThread.BlockIfPoolSizeRetchTo"));
        Setting.ForceSaveEvery = Integer.valueOf(props.getProperty("NTG.WFEngine.SaveThread.ForceSaveEvery"));
        Setting.EnbaleDebugPrint = Boolean.valueOf(props.getProperty("NTG.WFEngine.EnbaleDebugPrint"));
        Setting.SleepPeriodOnNoDataFound = Integer.valueOf(props.getProperty("NTG.WFEngine.SleepPeriodOnNoDataFound"));
        Setting.databasetype = props.getProperty("spring.jpa.database").toLowerCase();
        Setting.CacheEmployeeInfoFor = Integer.valueOf(props.getProperty("NTG.WFEngine.CacheEmployeeInfoFor"));
    }

}
