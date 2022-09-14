package com.ntg.engine.jobs;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public class SaveThreadTransactionInformation {

	public static final class TxOperaiotns {
		public final static int Add = 1;
		public final static int ExecuteUpdateSql = 2;
	}

	public static final class TxSources {
		public final static int PendingTaskHandler = 1;
		public final static int PendingTaskEscalation = 2;
		public final static int PendingSwitchCondtionTasks = 3;
		public final static int PendingProcessTask = 4;

	}

	public SaveThreadTransactionInformation(Object TxObject, int TxOperationID) {
		this.TxObject = TxObject;
		this.TxOperationID = TxOperationID;

	}

	public SaveThreadTransactionInformation(Object TxObject, int TxOperationID, List<Object> SqlParams) {
		this.TxObject = TxObject;
		this.TxOperationID = TxOperationID;
		this.SqlParams = SqlParams;

	}

	public Object TxObject;

	// Operation
	// 1 for Add Object
	// 2 for Execute Update Query
	public int TxOperationID = 0;

	public List<Object> SqlParams;

	public String CallingTrace;


	public int TransactionSource;
	public String TransactionKey;

}
