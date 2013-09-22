package com.pmease.gitop.core.manager.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.pmease.commons.hibernate.dao.DefaultGenericDao;
import com.pmease.commons.hibernate.dao.GeneralDao;
import com.pmease.gitop.core.manager.RepositoryAuthorizationByIndividualManager;
import com.pmease.gitop.core.model.RepositoryAuthorizationByIndividual;

@Singleton
public class DefaultRepositoryAuthorizationByIndividualManager 
		extends DefaultGenericDao<RepositoryAuthorizationByIndividual> 
		implements RepositoryAuthorizationByIndividualManager {

	@Inject
	public DefaultRepositoryAuthorizationByIndividualManager(GeneralDao generalDao) {
		super(generalDao);
	}

}