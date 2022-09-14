package com.ntg.engine.jobs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ntg.engine.entites.HumanTaskActions;
import com.ntg.engine.jobs.service.ToDoListRecordBuilder;
import com.ntg.engine.repository.HumanTaskActionsRepository;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ntg.common.NTGMessageOperation;
import com.ntg.engine.entites.ToDoList;
import com.ntg.engine.repository.custom.SqlHelperDao;
import com.ntg.engine.service.JobService;
import com.ntg.engine.util.Setting;

public class SaveThread implements Job {

    @Autowired
    SqlHelperDao sqlHelperDao;

    @Autowired
    JdbcTemplate jdbtemplateInst;


    @Value("${wf_numberMailJobs}")
    String numberOfJobs;


    static long startBlockTime;

    long BlockingTime = 0;

    static Long SaveTime = null;

    static Boolean BlockChanges = false;

    Boolean IsSavedSinceLastChanges = true;

    @Autowired
    private JobService jobService;

    // SaveThread Pools
    /* saveThreadPool */
    static volatile ArrayList<List<SaveThreadTransactionInformation>> saveThreadPool = new ArrayList<List<SaveThreadTransactionInformation>>();
    /* saveThreadSecondPool */
    private static volatile ArrayList<List<SaveThreadTransactionInformation>> saveThreadSecondPool = null;

    public static void AddSaveThreadTransaction(List<SaveThreadTransactionInformation> Txs) {

        String CallingTrace = GetCurrentTrace();
        if (Txs != null) {
            for (SaveThreadTransactionInformation tx : Txs) {
                tx.CallingTrace = CallingTrace;
            }
        }
        boolean WasBlocking = false;
        if (BlockChanges) {
            JobUtil.Debug("Save thread Start sleeping for Blocking ");
            WasBlocking = true;
        }
        while (BlockChanges) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                NTGMessageOperation.PrintErrorTrace(ex);
            }
        }
        if (WasBlocking) {
            JobUtil.Debug("Save thread End sleeping for Blocking ");
            WasBlocking = false;
        }

        saveThreadPool.add(Txs);

        int n;
        if (saveThreadSecondPool != null && (n = saveThreadPool.size()) > Setting.BlockIfPoolSizeRetchTo) {
            JobUtil.Info("Warring: Secondary Save pool retched to maximum (" + n
                    + ") and system will be blocked till save thread finalized First pool saving ("
                    + saveThreadSecondPool.size() + ")");
            BlockChanges = true;
            startBlockTime = System.currentTimeMillis();

        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        String jobName = context.getJobDetail().getKey().toString().split("\\.")[1];
        JobUtil.Debug("==============Start SaveThread ============== <" + jobName + ">");

        try {
            while (true) {
                run();
            }
        } catch (Throwable e) {
            NTGMessageOperation.PrintErrorTrace(new Exception(e));
        }
        jobService.deleteJob(jobName);
        //JobUtil.Debug("==============End SaveThread ============== <" + jobName + ">");

    }

    public void run() throws InterruptedException {

        long d = System.currentTimeMillis() - JobUtil.SaveThreadLastSaveTime;
        int savePoolSize = SaveThread.saveThreadPool.size();
        if (savePoolSize > 0) {
            if (savePoolSize > Setting.PoolSizeForDBPost || d > Setting.ForceSaveEvery) {
                try {
                    long StartSaveTime = System.currentTimeMillis();


                    synchronized (saveThreadPool) {
                        saveThreadSecondPool = saveThreadPool;
                        saveThreadPool = new ArrayList<List<SaveThreadTransactionInformation>>();
                    }
                    JobUtil.Info("Start Saving " + saveThreadSecondPool.size() + " Tx(s)/*");

                    int N = PostToDataBase();


                    JobUtil.Debug("Save Done in "
                            + (System.currentTimeMillis() - StartSaveTime) + " MSec for " + N +
                            " Record(s) */");

                } catch (Exception e) {
                    JobUtil.Info("Warring Save Exception .../*");
                    NTGMessageOperation.PrintErrorTrace(e);
                    JobUtil.Info("*/");
                }
            }
            saveThreadSecondPool = null;
            if (BlockChanges) {
                BlockChanges = false;
                long DltaStartBlockTime = System.currentTimeMillis() - startBlockTime;
                BlockingTime += DltaStartBlockTime;
                JobUtil.Info("Free the blocking after " + ((double) DltaStartBlockTime / 1000.0) + " Sec");
            }
            JobUtil.SaveThreadLastSaveTime = System.currentTimeMillis();
            IsSavedSinceLastChanges = true;
        } else {
            Thread.sleep(100);
        }
    }

    private int PostToDataBase() {
        int n = 0;
        int PooolSize = saveThreadSecondPool.size();
        for (int i = 0; i < PooolSize; i++) {
            List<SaveThreadTransactionInformation> tx = saveThreadSecondPool.get(i);
            n += tx.size();
            try {
                SaveTx(tx);
            } catch (Exception e) {
                // Error Already Logged In, Exception was only for rollBack
            } finally {
                Checkin(tx);
            }
        }

        saveThreadSecondPool.clear();

        return n;

    }


    @Autowired
    private HumanTaskActionsRepository humanTaskActionsRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = java.lang.Exception.class)
    void SaveTx(List<SaveThreadTransactionInformation> txs) throws Exception {
        for (SaveThreadTransactionInformation tx : txs) {
            try {
                // SaveTX
                if (tx.TxOperationID == SaveThreadTransactionInformation.TxOperaiotns.Add) {
                    if (tx.TxObject instanceof ToDoListRecordBuilder) {
                        ToDoListRecordBuilder todo = (ToDoListRecordBuilder) tx.TxObject;
                        //Dev-00000042 , For " Push Notification"
                        if (todo.toDo.getRecId() != null && todo.toDo.getRecId() == -1) {
                            todo.setRecId(null);
                        }
                        //handle ToDOList Save
                        //Dev-00002993 : Work Flow Duplicate the tasks after out of memory , need duplicate check in insert tasks
                        todo.save(this.sqlHelperDao, this.jdbtemplateInst);
                    } else if (tx.TxObject instanceof HumanTaskActions) {

                        HumanTaskActions row = (HumanTaskActions) tx.TxObject;
                        this.humanTaskActionsRepo.save(row);

                    } else {
                        throw new Exception("UnSupported Repo --> " + tx.TxObject.toString());
                    }
                } else if (tx.TxOperationID == SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql) {
                    // JobUtil.Debug(tx.TxObject);
                    this.jdbtemplateInst.update((String) tx.TxObject, tx.SqlParams.toArray());
                }
            } catch (Throwable e) {
                Exception ex = new Exception(e);
                LogError(ex, tx);
                throw ex;
            }

        }
    }

    private void Checkin(List<SaveThreadTransactionInformation> txs) {

        for (SaveThreadTransactionInformation tx : txs) {
            // Updated By Mahmoud to handle CheckIn in all jobs
            if (tx.TransactionKey != null) {
                if (tx.TransactionSource == SaveThreadTransactionInformation.TxSources.PendingTaskHandler) {
                    JobUtil.CheckInPendingTask(tx.TransactionKey);
                } else if (tx.TransactionSource == SaveThreadTransactionInformation.TxSources.PendingTaskEscalation) {
                    JobUtil.CheckInPendingEscalationTask(tx.TransactionKey);
                } else if (tx.TransactionSource == SaveThreadTransactionInformation.TxSources.PendingSwitchCondtionTasks) {
                    JobUtil.CheckInPendingSwitchCondtionTasks(tx.TransactionKey);
                } else if (tx.TransactionSource == SaveThreadTransactionInformation.TxSources.PendingProcessTask) {
                    JobUtil.CheckInPendingProcessTasks(tx.TransactionKey);
                }
            }

            // begin to send mail action and Notification
            if (tx.TxObject instanceof ToDoListRecordBuilder) {
                ToDoListRecordBuilder
                        todo = (ToDoListRecordBuilder) tx.TxObject;
                if (todo.toDo.sendEmailonAssign) {
                    JobUtil.AddMailTaskToPool(todo.toDo);
                }
            }
        } // end for of txs

        if (JobUtil.PendingMailPool.size() > 0) {
            int RunThread = (JobUtil.PendingMailPool.size() > Integer.valueOf(numberOfJobs))
                    ? Integer.valueOf(numberOfJobs)
                    : JobUtil.PendingMailPool.size();
            for (int i = 1; i <= RunThread; i++) {
                String jobName = "SendMailJob#" + i;
                if (jobService.isJobRunning(jobName) == false) {
                    jobService.scheduleOneTimeJob(jobName, JobUtil.getJobClassByName("SendMailJob"), new Date(), null);
                }
            }
        }

    } // end of method

    private void LogError(Exception e, SaveThreadTransactionInformation tx) {
        JobUtil.Info("**************** Tx Error***********************");
        JobUtil.Info(e.getMessage());
        JobUtil.Info(tx.CallingTrace);
        JobUtil.Info("TxInfo : " + tx.TxObject.toString() + " / Key: " + tx.TransactionKey);
        JobUtil.Info("**************** End Tx Error***********************");

    }

    public static String GetCurrentTrace() {
        String er = "";
        java.lang.StackTraceElement[] ErrList = Thread.currentThread().getStackTrace();
        for (int i = 2; i < ErrList.length; i++) {
            String e = ErrList[i].toString();
            if (e.indexOf("com.ntg.") > -1) {
                er += "\n" + e;
            }
        }

        return er;
    }
}
