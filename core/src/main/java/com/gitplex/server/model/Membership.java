package com.gitplex.server.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.gitplex.server.util.facade.MembershipFacade;

@Entity
@Table(
		indexes={@Index(columnList="g_user_id"), @Index(columnList="g_group_id")},
		uniqueConstraints={@UniqueConstraint(columnNames={"g_user_id", "g_group_id"})
})
public class Membership extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable=false)
	private User user;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable=false)
	private Group group;
	
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public MembershipFacade getFacade() {
		return new MembershipFacade(this);
	}
	
}
