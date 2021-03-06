package com.gitplex.server.web.page.project.setting.authorization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.gitplex.server.GitPlex;
import com.gitplex.server.manager.CacheManager;
import com.gitplex.server.manager.UserAuthorizationManager;
import com.gitplex.server.manager.UserManager;
import com.gitplex.server.model.User;
import com.gitplex.server.model.UserAuthorization;
import com.gitplex.server.security.ProjectPrivilege;
import com.gitplex.server.security.SecurityUtils;
import com.gitplex.server.util.facade.GroupAuthorizationFacade;
import com.gitplex.server.util.facade.GroupFacade;
import com.gitplex.server.util.facade.MembershipFacade;
import com.gitplex.server.util.facade.UserAuthorizationFacade;
import com.gitplex.server.util.facade.UserFacade;
import com.gitplex.server.web.WebConstants;
import com.gitplex.server.web.behavior.OnTypingDoneBehavior;
import com.gitplex.server.web.component.avatar.AvatarLink;
import com.gitplex.server.web.component.datatable.DefaultDataTable;
import com.gitplex.server.web.component.datatable.SelectionColumn;
import com.gitplex.server.web.component.floating.FloatingPanel;
import com.gitplex.server.web.component.link.DropdownLink;
import com.gitplex.server.web.component.link.UserLink;
import com.gitplex.server.web.component.projectprivilege.privilegeselection.PrivilegeSelectionPanel;
import com.gitplex.server.web.component.projectprivilege.privilegesource.PrivilegeSourcePanel;
import com.gitplex.server.web.component.select2.Response;
import com.gitplex.server.web.component.select2.ResponseFiller;
import com.gitplex.server.web.component.select2.SelectToAddChoice;
import com.gitplex.server.web.component.userchoice.AbstractUserChoiceProvider;
import com.gitplex.server.web.component.userchoice.UserChoiceResourceReference;
import com.gitplex.server.web.page.project.setting.ProjectSettingPage;

@SuppressWarnings("serial")
public class ProjectAuthorizationsPage extends ProjectSettingPage {

	private String searchInput;
	
	private DataTable<Long, Void> authorizationsTable;
	
	private SelectionColumn<Long, Void> selectionColumn;
	
	private IModel<Map<Long, UserAuthorizationFacade>> explicitUserAuthorizationsModel = 
			new LoadableDetachableModel<Map<Long, UserAuthorizationFacade>>() {

		@Override
		protected Map<Long, UserAuthorizationFacade> load() {
			Map<Long, UserAuthorizationFacade> authorizations = new HashMap<>();
			for (UserAuthorizationFacade authorization: 
					GitPlex.getInstance(CacheManager.class).getUserAuthorizations().values()) {
				if (authorization.getProjectId().equals(getProject().getId()))
					authorizations.put(authorization.getUserId(), authorization);
			}
			return authorizations;
		}
		
	};
	
	private IModel<Map<Long, ProjectPrivilege>> userAuthorizationsModel = 
			new LoadableDetachableModel<Map<Long, ProjectPrivilege>>() {

		@Override
		protected Map<Long, ProjectPrivilege> load() {
			Map<Long, ProjectPrivilege> userAuthorizations = new HashMap<>();
			Collection<Long> usersAuthorizedFromOtherSources = new HashSet<>();
			userAuthorizations.put(User.ROOT_ID, ProjectPrivilege.ADMIN);
			usersAuthorizedFromOtherSources.add(User.ROOT_ID);

			CacheManager cacheManager = GitPlex.getInstance(CacheManager.class);
			Set<Long> adminGroupIds = new HashSet<>();
			for (GroupFacade group: cacheManager.getGroups().values()) {
				if (group.isAdministrator()) 
					adminGroupIds.add(group.getId());
			}
			for (MembershipFacade membership: cacheManager.getMemberships().values()) {
				if (adminGroupIds.contains(membership.getGroupId())) {
					userAuthorizations.put(membership.getUserId(), ProjectPrivilege.ADMIN);
					usersAuthorizedFromOtherSources.add(membership.getUserId());
				}
			}

			Map<Long, ProjectPrivilege> groupAuthorizations = new HashMap<>();
			for (GroupAuthorizationFacade authorization: cacheManager.getGroupAuthorizations().values()) {
				if (authorization.getProjectId().equals(getProject().getId()))
					groupAuthorizations.put(authorization.getGroupId(), authorization.getPrivilege());
			}
			for (MembershipFacade membership: cacheManager.getMemberships().values()) {
				ProjectPrivilege groupPrivilege = groupAuthorizations.get(membership.getGroupId());
				if (groupPrivilege != null) {
					ProjectPrivilege userPrivilege = userAuthorizations.get(membership.getUserId());
					if (userPrivilege == null || groupPrivilege.implies(userPrivilege))
						userAuthorizations.put(membership.getUserId(), groupPrivilege);
					usersAuthorizedFromOtherSources.add(membership.getUserId());
				}
			}
			for (UserAuthorizationFacade authorization: cacheManager.getUserAuthorizations().values()) {
				if (authorization.getProjectId().equals(getProject().getId())) {
					ProjectPrivilege userPrivilege = userAuthorizations.get(authorization.getUserId());
					if (userPrivilege == null || authorization.getPrivilege().implies(userPrivilege))
						userAuthorizations.put(authorization.getUserId(), authorization.getPrivilege());
				}
			}
			if (getProject().isPublicRead()) {
				for (UserFacade user: cacheManager.getUsers().values()) {
					ProjectPrivilege userPrivilege = userAuthorizations.get(user);
					if (userPrivilege == null || ProjectPrivilege.READ.implies(userPrivilege))
						userAuthorizations.put(user.getId(), ProjectPrivilege.READ);
					usersAuthorizedFromOtherSources.add(user.getId());
				}
			}
			
			Map<Long, UserAuthorizationFacade> explicitUserAuthorizations = getExplicitUserAuthorizations();
			List<Long> userIds = new ArrayList<>(userAuthorizations.keySet());
			Collections.sort(userIds, new Comparator<Long>() {

				@Override
				public int compare(Long o1, Long o2) {
					if (usersAuthorizedFromOtherSources.contains(o1)) {
						if (usersAuthorizedFromOtherSources.contains(o2)) {
							return o1.compareTo(o2);
						} else {
							return 1;
						}
					} else {
						if (usersAuthorizedFromOtherSources.contains(o2)) {
							return -1;
						} else {
							return explicitUserAuthorizations.get(o2).getId()
									.compareTo(explicitUserAuthorizations.get(o1).getId());
						}
					}
				}
				
			});	
			
			Map<Long, ProjectPrivilege> sortedUserAuthorizations = new LinkedHashMap<>();
			for (Long userId: userIds) {
				sortedUserAuthorizations.put(userId, userAuthorizations.get(userId));
			}
			
			return sortedUserAuthorizations;
		}
		
	};
	
	public ProjectAuthorizationsPage(PageParameters params) {
		super(params);
	}

	@Override
	protected void onDetach() {
		explicitUserAuthorizationsModel.detach();
		userAuthorizationsModel.detach();
		super.onDetach();
	}
	
	private Map<Long, ProjectPrivilege> getUserAuthorizations() {
		return userAuthorizationsModel.getObject();
	}
	
	private Map<Long, UserAuthorizationFacade> getExplicitUserAuthorizations() {
		return explicitUserAuthorizationsModel.getObject();
	}
	
	private boolean isPrivilegeFromOtherSources(Long userId) {
		UserAuthorizationFacade authorization = getExplicitUserAuthorizations().get(userId);
		ProjectPrivilege privilege = getUserAuthorizations().get(userId);
		return authorization == null || !authorization.getPrivilege().implies(privilege);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		TextField<String> searchField;
		
		add(searchField = new TextField<String>("filterUsers", Model.of("")));
		searchField.add(new OnTypingDoneBehavior(100) {

			@Override
			protected void onTypingDone(AjaxRequestTarget target) {
				searchInput = searchField.getInput();
				target.add(authorizationsTable);
			}
			
		});
		
		add(new SelectToAddChoice<UserFacade>("addNew", new AbstractUserChoiceProvider() {

			@Override
			public void query(String term, int page, Response<UserFacade> response) {
				List<UserFacade> notAuthorized = new ArrayList<>();
				
				for (UserFacade user: GitPlex.getInstance(CacheManager.class).getUsers().values()) {
					if (user.matchesQuery(term) && !getUserAuthorizations().containsKey(user.getId()))
						notAuthorized.add(user);
				}
				Collections.sort(notAuthorized);
				Collections.reverse(notAuthorized);
				new ResponseFiller<UserFacade>(response).fill(notAuthorized, page, WebConstants.PAGE_SIZE);
			}

		}) {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				
				getSettings().setPlaceholder("Add user...");
				getSettings().setFormatResult("gitplex.server.userChoiceFormatter.formatResult");
				getSettings().setFormatSelection("gitplex.server.userChoiceFormatter.formatSelection");
				getSettings().setEscapeMarkup("gitplex.server.userChoiceFormatter.escapeMarkup");
			}
			
			@Override
			protected void onSelect(AjaxRequestTarget target, UserFacade selection) {
				UserAuthorization authorization = new UserAuthorization();
				authorization.setProject(getProject());
				authorization.setUser(GitPlex.getInstance(UserManager.class).load(selection.getId()));
				GitPlex.getInstance(UserAuthorizationManager.class).save(authorization);
				target.add(authorizationsTable);
				Session.get().success("User added");
			}
			
			@Override
			public void renderHead(IHeaderResponse response) {
				super.renderHead(response);
				
				response.render(JavaScriptHeaderItem.forReference(new UserChoiceResourceReference()));
			}
			
		});			
		
		AjaxLink<Void> deleteSelected = new AjaxLink<Void>("deleteSelected") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				Collection<UserAuthorization> authorizationsToDelete = new HashSet<>();
				Map<Long, UserAuthorizationFacade> explicitUserAuthorizations = getExplicitUserAuthorizations();
				UserAuthorizationManager userAuthorizationManager = GitPlex.getInstance(UserAuthorizationManager.class);
				for (IModel<Long> model: selectionColumn.getSelections()) {
					UserAuthorizationFacade authorization = explicitUserAuthorizations.get(model.getObject());
					if (authorization != null) {
						authorizationsToDelete.add(userAuthorizationManager.load(authorization.getId()));
						explicitUserAuthorizations.remove(model.getObject());
					}
				}
				userAuthorizationManager.delete(authorizationsToDelete);
				target.add(authorizationsTable);
				selectionColumn.getSelections().clear();
				target.add(this);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				boolean hasLocalPrivileges = false;
				for (IModel<Long> model: selectionColumn.getSelections()) {
					Long userId = model.getObject();
					if (!isPrivilegeFromOtherSources(userId)) {
						hasLocalPrivileges = true;
						break;
					}
				}
				setVisible(hasLocalPrivileges);
			}
			
		};
		deleteSelected.setOutputMarkupPlaceholderTag(true);
		add(deleteSelected);

		List<IColumn<Long, Void>> columns = new ArrayList<>();
		
		selectionColumn = new SelectionColumn<Long, Void>() {
			
			@Override
			protected void onSelectionChange(AjaxRequestTarget target) {
				target.add(deleteSelected);
			}
			
		};
		columns.add(selectionColumn);
		
		columns.add(new AbstractColumn<Long, Void>(Model.of("User")) {

			@Override
			public void populateItem(Item<ICellPopulator<Long>> cellItem, String componentId, IModel<Long> rowModel) {
				Fragment fragment = new Fragment(componentId, "userFrag", ProjectAuthorizationsPage.this);
				User user = GitPlex.getInstance(UserManager.class).load(rowModel.getObject());
				fragment.add(new AvatarLink("avatarLink", user));
				fragment.add(new UserLink("nameLink", user));
				cellItem.add(fragment);
			}
		});
		
		columns.add(new AbstractColumn<Long, Void>(Model.of("Permission")) {

			@Override
			public void populateItem(Item<ICellPopulator<Long>> cellItem, String componentId, IModel<Long> rowModel) {
				Fragment fragment = new Fragment(componentId, "privilegeFrag", ProjectAuthorizationsPage.this) {

					@Override
					public void renderHead(IHeaderResponse response) {
						super.renderHead(response);

						Long userId = rowModel.getObject();
						boolean canNotDelete = isPrivilegeFromOtherSources(userId) 
								|| userId.equals(SecurityUtils.getUser().getId());
						String script = String.format(""
								+ "$('#%s').closest('tr').find('.row-selector input').prop('disabled', %b);", 
								getMarkupId(), canNotDelete);
						
						response.render(OnDomReadyHeaderItem.forScript(script));
					}
					
				};
				WebMarkupContainer dropdown = new DropdownLink("dropdown") {

					@Override
					protected Component newContent(String id, FloatingPanel dropdown) {
						ProjectPrivilege privilege = getUserAuthorizations().get(rowModel.getObject());
						return new PrivilegeSelectionPanel(id, privilege) {
							
							@Override
							protected void onSelect(AjaxRequestTarget target, ProjectPrivilege privilege) {
								dropdown.close();
								Long userId = rowModel.getObject();
								
								Map<Long, UserAuthorizationFacade> explicitUserAuthorizations = 
										getExplicitUserAuthorizations();
								UserAuthorizationFacade userAuthorizationFacade = 
										explicitUserAuthorizations.get(userId);
								UserAuthorizationManager userAuthorizationManager = 
										GitPlex.getInstance(UserAuthorizationManager.class);
								UserAuthorization userAuthorization;
								if (userAuthorizationFacade == null) {
									userAuthorization = new UserAuthorization();
									userAuthorization.setProject(getProject());
									userAuthorization.setUser(GitPlex.getInstance(UserManager.class).load(userId));
								} else {
									userAuthorization = userAuthorizationManager.load(userAuthorizationFacade.getId()); 
								}
								userAuthorization.setPrivilege(privilege);
								userAuthorizationManager.save(userAuthorization);
								explicitUserAuthorizations.put(userId, userAuthorization.getFacade());
								
								target.add(fragment);
								
								if (getUserAuthorizations().get(userId) != privilege) {
									Session.get().warn("Specified permission is not taking effect as a higher "
											+ "permission is granted from other sources");
								}
							}
							
						};
					}
					
				};
				dropdown.add(new Label("label", new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return getUserAuthorizations().get(rowModel.getObject()).name();
					}
					
				}));
				
				Long userId = rowModel.getObject();
				if (userId.equals(SecurityUtils.getUser().getId()) && !isPrivilegeFromOtherSources(userId)) {
					dropdown.add(AttributeAppender.append("disabled", "true"));
					dropdown.setEnabled(false);
				}
				fragment.add(dropdown);
				
				fragment.add(new DropdownLink("otherSources") {

					@Override
					protected void onConfigure() {
						super.onConfigure();
						setVisible(isPrivilegeFromOtherSources(rowModel.getObject()));
					}

					@Override
					protected Component newContent(String id, FloatingPanel dropdown) {
						Long userId = rowModel.getObject();
						User user = GitPlex.getInstance(UserManager.class).load(userId);
						return new PrivilegeSourcePanel(id, user, getProject(), getUserAuthorizations().get(userId));
					}
					
				});
				
				fragment.setOutputMarkupId(true);
				cellItem.add(fragment);
			}
		});
		
		SortableDataProvider<Long, Void> dataProvider = new SortableDataProvider<Long, Void>() {

			private List<Long> getUserIds() {
				List<Long> userIds = new ArrayList<>();
				CacheManager cacheManager = GitPlex.getInstance(CacheManager.class);
				for (Long userId: getUserAuthorizations().keySet()) {
					if (cacheManager.getUser(userId).matchesQuery(searchInput))
						userIds.add(userId);
				}
				return userIds;
			}
			
			@Override
			public Iterator<? extends Long> iterator(long first, long count) {
				List<Long> userIds = getUserIds();
				if (first + count <= userIds.size())
					return userIds.subList((int)first, (int)(first+count)).iterator();
				else
					return userIds.subList((int)first, userIds.size()).iterator();
			}

			@Override
			public long size() {
				return getUserIds().size();
			}

			@Override
			public IModel<Long> model(Long object) {
				return Model.of(object);
			}
		};
		
		add(authorizationsTable = new DefaultDataTable<Long, Void>("authorizations", columns, 
				dataProvider, WebConstants.PAGE_SIZE));
	}
}
