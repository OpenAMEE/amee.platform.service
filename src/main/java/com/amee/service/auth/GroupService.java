package com.amee.service.auth;

import com.amee.domain.AMEEEntity;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.Pager;
import com.amee.domain.auth.Group;
import com.amee.domain.auth.GroupPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class GroupService {

    @Autowired
    private GroupServiceDAO dao;

    // Groups

    public Group getGroupByUid(String uid) {
        return dao.getGroupByUid(uid);
    }

    public Group getGroupByName(String name) {
        return dao.getGroupByName(name);
    }

    public List<Group> getGroups() {
        return dao.getGroups();
    }

    public List<Group> getGroups(Pager pager) {
        return dao.getGroups(pager);
    }

    public void save(Group group) {
        dao.save(group);
    }

    public void remove(Group group) {
        dao.remove(group);
    }

    // GroupPrincipals

    public GroupPrincipal getGroupPrincipalByUid(String uid) {
        return dao.getGroupPrincipalByUid(uid);
    }

    public GroupPrincipal getGroupPrincipal(Group group, IAMEEEntityReference entity) {
        return dao.getGroupPrincipal(group, entity);
    }

    public List<GroupPrincipal> getGroupPrincipals(Group group, Pager pager) {
        return dao.getGroupPrincipals(group, pager);
    }

    public List<GroupPrincipal> getGroupPrincipalsForPrincipal(IAMEEEntityReference principal, Pager pager) {
        return dao.getGroupPrincipalsForPrincipal(principal, pager);
    }

    public Set<Group> getGroupsForPrincipal(AMEEEntity principal) {
        return dao.getGroupsForPrincipal(principal);
    }

    public List<GroupPrincipal> getGroupPrincipalsForPrincipal(IAMEEEntityReference principal) {
        return dao.getGroupPrincipalsForPrincipal(principal);
    }

    public List<GroupPrincipal> getGroupPrincipals() {
        return dao.getGroupPrincipals();
    }

    public void save(GroupPrincipal groupPrincipal) {
        dao.save(groupPrincipal);
    }

    public void remove(GroupPrincipal groupPrincipal) {
        dao.remove(groupPrincipal);
    }
}
