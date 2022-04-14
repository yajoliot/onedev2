package io.onedev.server.model.support.pullrequest;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import io.onedev.server.model.User;

@Embeddable
public class CloseInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String COLUMN_USER = "CLOSE_USER";
	
	public static final String COLUMN_USER_NAME = "CLOSE_USER_NAME";
	
	public static final String COLUMN_DATE = "CLOSE_DATE";
	
	public static final String PROP_USER = "user";
	
	public static final String PROP_DATE = "date";
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name=COLUMN_USER)
	private User user;
	
	@Column(name=COLUMN_USER_NAME)
	private String userName;

	@Column(name=COLUMN_DATE)
	private Date date = new Date();
	
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
	
}
