package com.ntg.engine.repository.custom;

import java.util.List;
import java.util.Map;

//import com.ntg.engine.entites.SlaMilestones;

public interface SqlHelperDao {

	public List<Map<String, Object>> queryForList(String query);

	public List<Map<String, Object>> queryForList(String query, Object[] params);

	void updateTable(List<Long> recids, String sql);

	List<Map<String, Object>> queryForObjectWithLobItem(String query,Long ObjectId,String propertyName);
	public long FtechRecID(String SeqnaceName);

}
