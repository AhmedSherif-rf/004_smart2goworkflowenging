package com.ntg.engine.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.ntg.engine.entites.HumanTaskActions;

public interface HumanTaskActionsRepository extends CrudRepository<HumanTaskActions, Long> {

	public List<HumanTaskActions> findByRowID (Long taskID);
}
