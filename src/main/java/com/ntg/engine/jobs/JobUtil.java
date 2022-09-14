package com.ntg.engine.jobs;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.quartz.JobDataMap;

import com.ntg.common.NTGEHCacher;
import com.ntg.engine.entites.ToDoList;
import com.ntg.engine.util.Setting;

public class JobUtil {

    // Reading Jobs Data
    public static volatile HashMap<String, Map<String, Object>> PendingTasksPool = new HashMap<String, Map<String, Object>>();
    public static volatile NTGEHCacher<String, Map<String, Object>> UnderProcessingPool = new NTGEHCacher<String, Map<String, Object>>(600000 /*1000 * 10 * 60 = 10M*/, "U1");
    public static volatile NTGEHCacher<String, Object> CompletedTasksPool = new NTGEHCacher<String, Object>(10000);

    // Main Job Data
    static volatile ArrayList<JobDataMap> AvilableUDAForProcessing = new ArrayList<JobDataMap>();
    static volatile HashMap<String, Boolean> AvilableUDAForProcessingMap = new HashMap<String, Boolean>();
    private static int[] UDAForProcessingCurrentIndex = new int[]{-1, -1, -1}; // check out Table for Processing


    // Task Escalation
    public static volatile HashMap<String, Map<String, Object>> PendingTaskEscalationsPool = new HashMap<String, Map<String, Object>>();
    public static volatile NTGEHCacher<String, Map<String, Object>> UnderProcessingEscalationPool = new NTGEHCacher<String, Map<String, Object>>(600000 /*1000 * 10 * 60 = 10M*/, "U2");

    // Switch Condition Task Jobs
    public static volatile HashMap<String, Map<String, Object>> PendingSwitchConditionTasksPool = new HashMap<String, Map<String, Object>>();
    public static volatile NTGEHCacher<String, Map<String, Object>> UnderProcessingSwitchConditionPool = new NTGEHCacher<String, Map<String, Object>>(600000 /*1000 * 10 * 60 = 10M*/, "U3");

    // Process Task Job
    public static volatile HashMap<String, Map<String, Object>> PendingProcessTaskPool = new HashMap<String, Map<String, Object>>();
    public static volatile NTGEHCacher<String, Map<String, Object>> UnderProcessingProcessTaskPool = new NTGEHCacher<String, Map<String, Object>>(600000 /*1000 * 10 * 60 = 10M*/, "U4");

    // Mail Job
    public static volatile Map<String, ToDoList> PendingMailPool = new HashMap<String, ToDoList>();


    // Expiry Job
    public static volatile Map<String, Map<String, Object>> ExpiredTasks = new HashMap<String, Map<String, Object>>();
    public static volatile NTGEHCacher<String, Map<String, Object>> UnderProcessingExpiredTasks = new NTGEHCacher<String, Map<String, Object>>(600000 /*1000 * 10 * 60 = 10M*/, "U4");


    // Employees Functions
    static final String defaultImage = "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";

    static long SaveThreadLastSaveTime = -1;

    public static synchronized JobDataMap getOneJobDataMap(int ThreadNUmber) throws Exception {
        JobDataMap re = null;
        int MaxNoRatry = AvilableUDAForProcessing.size() * 10 * 10;
        int NRtry = 0;
        while (re == null && MaxNoRatry > 0) {
            MaxNoRatry--;
            NRtry++;

            UDAForProcessingCurrentIndex[ThreadNUmber]++;
            if (UDAForProcessingCurrentIndex[ThreadNUmber] > AvilableUDAForProcessing.size()) {
                UDAForProcessingCurrentIndex[ThreadNUmber] = 0;
            }
            if (UDAForProcessingCurrentIndex[ThreadNUmber] < AvilableUDAForProcessing.size()) {
                re = AvilableUDAForProcessing.get(UDAForProcessingCurrentIndex[ThreadNUmber]);
                    String CheckKey = "IsUsed" + ThreadNUmber;
                if (re.get(CheckKey) == null || (boolean) re.get(CheckKey) == false) {
                    re.put(CheckKey, true);
                } else {
                    re = null;
                    if (NRtry > AvilableUDAForProcessing.size()) {
                        NRtry = 0;
                        Thread.sleep(10000);
                    }
                }
            }
        }
        return re;
    }

    public static synchronized void FreeJob(JobDataMap job,int ThreadNumber) {
        job.put("IsUsed" + ThreadNumber, false);
    }

    public static void ForceSaveThreadForSave() {
        SaveThreadLastSaveTime = -1;
    }

    public static synchronized Map<String, Object> CheckOutPendingTasks() {
        Object[] Keys = PendingTasksPool.keySet().toArray();
        if (Keys != null && Keys.length > 0) {
            Map<String, Object> row = PendingTasksPool.get(Keys[0]);
            PendingTasksPool.remove(Keys[0]);
            UnderProcessingPool.put((String) Keys[0], row);
            return row;
        }
        return null;
    }

    // Added to push process task in it
    public static synchronized Map<String, Object> CheckOutPendingProcessTask() {
        Object[] Keys = PendingProcessTaskPool.keySet().toArray();
        if (Keys != null && Keys.length > 0) {
            Map<String, Object> row = PendingProcessTaskPool.get(Keys[0]);
            PendingProcessTaskPool.remove(Keys[0]);
            UnderProcessingProcessTaskPool.put((String) Keys[0], row);
            return row;
        }
        return null;
    }

    // Added to push switch and condition tasks in it
    public static synchronized Map<String, Object> CheckOutSwitchAndConditionTasks() {
        Object[] Keys = PendingSwitchConditionTasksPool.keySet().toArray();
        if (Keys != null && Keys.length > 0) {
            Map<String, Object> row = PendingSwitchConditionTasksPool.get(Keys[0]);
            PendingSwitchConditionTasksPool.remove(Keys[0]);
            UnderProcessingSwitchConditionPool.put((String) Keys[0], row);
            return row;
        }
        return null;
    }

    public static synchronized Map<String, Object> CheckOutPendingTaskEscalation() {
        Object[] Keys = PendingTaskEscalationsPool.keySet().toArray();
        if (Keys != null && Keys.length > 0) {
            Map<String, Object> row = PendingTaskEscalationsPool.get(Keys[0]);
            PendingTaskEscalationsPool.remove(Keys[0]);
            UnderProcessingEscalationPool.put((String) Keys[0], row);
            return row;
        }
        return null;
    }

    public static void AddMailTaskToPool(ToDoList row) {
        //todo.taskID + "_" + todo.object_Id\
        String key = row.udaName + "_" + row.taskID + "_" + row.object_Id;
        if (PendingMailPool.get(key) != null) {
            System.out.println("War : Mail Task Key Duplicate Issue " + key);
        }
        JobUtil.PendingMailPool.put(key, row);
    }

    public static synchronized ToDoList CheckOutMailTask() {
        Object[] Keys = PendingMailPool.keySet().toArray();
        if (Keys != null && Keys.length > 0) {
            ToDoList row = PendingMailPool.get(Keys[0]);
            PendingMailPool.remove(Keys[0]);
            return row;
        }
        return null;
    }

    public static synchronized Map<String, Object> CheckOutExpiryTask() {

        Object[] Keys = ExpiredTasks.keySet().toArray();
        if (Keys != null && Keys.length > 0) {
            Map<String, Object> row = ExpiredTasks.get(Keys[0]);
            ExpiredTasks.remove(Keys[0]);
            UnderProcessingExpiredTasks.put((String) Keys[0], row);
            return row;
        }
        return null;
    }


    public static synchronized void CheckInPendingTask(String Key) {
        Map<String, Object> row = UnderProcessingPool.get(Key);
        if (row != null) {
            CompletedTasksPool.put(Key, row);
        }
        UnderProcessingPool.remove(Key);
    }

    // added By Mahmoud To handle escalation check in
    public static synchronized void CheckInPendingEscalationTask(String Key) {
        Map<String, Object> row = UnderProcessingEscalationPool.get(Key);
        if (row != null) {
            CompletedTasksPool.put(Key, row);
        }
        UnderProcessingEscalationPool.remove(Key);
    }

    // added By Mahmoud To handle Process Tasks check in
    public static synchronized void CheckInPendingProcessTasks(String Key) {
        Map<String, Object> row = UnderProcessingProcessTaskPool.get(Key);
        if (row != null) {
            CompletedTasksPool.put(Key, row);
        }
        UnderProcessingProcessTaskPool.remove(Key);
    }

    // added By Mahmoud To handle Switch/Condition Tasks check in
    public static synchronized void CheckInPendingSwitchCondtionTasks(String Key) {
        Map<String, Object> row = UnderProcessingSwitchConditionPool.get(Key);
        if (row != null) {
            CompletedTasksPool.put(Key, row);
        }
        UnderProcessingSwitchConditionPool.remove(Key);
    }

    public static synchronized void CheckInExpiryTask(String key) {
        UnderProcessingExpiredTasks.remove(key);
    }


    public static Class getJobClassByName(String type) {
        switch (type) {
            case "PendingTaskHandler":
                return ScanningTaskHandler.class;
            case "PendingTaskProcessing":
                return PendingTaskProcessing.class;
            case "saveThread":
                return SaveThread.class;
            case "SendMailJob":
                return SendMailJob.class;
            case "PendingProcessTask":
                return PendingProcessTask.class;
            case "PendingSwitchAndConditionTasks":
                return PendingSwitchAndConditionTasks.class;
            case "PendingTaskEscalation":
                return PendingTaskEscalation.class;

            case "expiryTaskHandler":
                return expiryTasksHandler.class;

            default:
                break;
        }
        return null;
    }

    public static String CreatePendingTaskKey(Map<String, Object> DataMap) {

        if (DataMap.get("tableType").toString().equals("workPackage")) {
            return DataMap.get("taskId") + (String) DataMap.get("tableName");
        } else if (DataMap.get("tableType").toString().equals("process")) {
            return DataMap.get("recid") + (String) DataMap.get("processTableName");
        }

        return "N/A";
    }

    public static String CreateEscalationTaskKey(Map<String, Object> DataMap) {
        if (DataMap.size() > 0) {
            String Key = null;
            if (DataMap.get("tableType").toString().equals("workPackage")) {
                Key = "Esc_" + DataMap.get("taskId") + (String) DataMap.get("tableName");
            } else if (DataMap.get("tableType").toString().equals("process")) {
                Key = "Esc_" + DataMap.get("recid") + (String) DataMap.get("processTableName");
            }
            return Key;
        } else {
            return "N/A";
        }
    }

    public static String CreateSwitchConditionTasksKey(Map<String, Object> DataMap) {
        if (DataMap.size() > 0) {
            String Key = null;
            if (DataMap.get("tableType").toString().equals("workPackage")) {
                Key = "Switch_Condition_" + DataMap.get("taskId") + (String) DataMap.get("tableName");
            } else if (DataMap.get("tableType").toString().equals("process")) {
                Key = "Switch_Condition_" + DataMap.get("recid") + (String) DataMap.get("processTableName");
            }
            return Key;
        } else {
            return "N/A";
        }
    }

    public static String CreateProcessTasksKey(Map<String, Object> DataMap) {
        if (DataMap.size() > 0) {
            String Key = null;
            if (DataMap.get("tableType").toString().equals("workPackage")) {
                Key = "Process_Task_" + DataMap.get("taskId") + (String) DataMap.get("tableName");
            } else if (DataMap.get("tableType").toString().equals("process")) {
                Key = "Process_Task_" + DataMap.get("recid") + (String) DataMap.get("processTableName");
            }
            return Key;
        } else {
            return "N/A";
        }
    }

    public static String CreateExpiryTasksKey(Map<String, Object> DataMap) {
        if (DataMap.size() > 0) {
            String Key = null;
            if (DataMap.get("tableType").toString().equals("workPackage")) {
                Key = "Process_Task_" + DataMap.get("taskId") + (String) DataMap.get("tableName");
            } else if (DataMap.get("tableType").toString().equals("process")) {
                Key = "Process_Task_" + DataMap.get("recid") + (String) DataMap.get("processTableName");
            }
            return Key;
        } else {
            return "N/A";
        }
    }

    public static Long convertBigDecimalToLong(Object value) {
        if (value != null) {
            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof BigDecimal) {
                return (((BigDecimal) value).setScale(0, BigDecimal.ROUND_HALF_UP).longValue());
            } else {
                if (value.equals("undefined") || value.equals("null") || value.equals("")) {
                    return null;
                } else {
                    return Long.valueOf(value.toString());
                }
            }
        } else
            return null;
    }

    public static boolean getBooleanValue(Object v) {

        if (v != null) {
            if (v instanceof Boolean)
                return (Boolean) v;
            long n = Long.parseLong(v.toString());
            return (n == 1) ? true : false;

        } else {
            return false;
        }

    }

    final static String[] formats = {"dd-MMM-yyyy"};

    public static java.util.Date getDateValue(String udaValue) {
        java.util.Date v = null;
        if (udaValue != null && !udaValue.toString().trim().equals("")) {
            SimpleDateFormat sdf = null;
            for (String format : formats) {
                sdf = new SimpleDateFormat(format);
                try {
                    v = sdf.parse(udaValue);
                    break;
                } catch (ParseException e) {
                }
            }
        }
        return v;
    }

    public static void Debug(String Info) {
        if (Setting.EnbaleDebugPrint) {
            System.out.println("(Deb)-->:" + Info);
        }
    }

    public static void Info(String Info) {

        System.out.println("(Inf)==>:" + Info);

    }

}
