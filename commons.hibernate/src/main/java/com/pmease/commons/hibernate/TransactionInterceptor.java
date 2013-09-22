/*
 * Copyright PMEase (c) 2005-2008,
 * Date: Feb 24, 2008
 * Time: 4:29:05 PM
 * All rights reserved.
 * 
 * Revision: $Id: TransactionInterceptor.java 1209 2008-07-28 00:16:18Z robin $
 */
package com.pmease.commons.hibernate;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.google.inject.Inject;

public class TransactionInterceptor implements MethodInterceptor {

	@Inject
	private UnitOfWork unitOfWork;
	
	public Object invoke(MethodInvocation mi) throws Throwable {
		unitOfWork.begin();
		try {
			Session session = unitOfWork.getSession();
			if (session.getTransaction().isActive()) {
				return mi.proceed();
			} else {
				FlushMode previousMode = session.getFlushMode();
				session.setFlushMode(FlushMode.COMMIT);

				Transaction tx = session.beginTransaction();
				try {
					Object result = mi.proceed();
					tx.commit();
					return result;
				} catch (Throwable t) {
					tx.rollback();
					throw t;
				} finally {
					session.setFlushMode(previousMode);
				}
			}
			
		} finally {
			unitOfWork.end();
		}
	}
}