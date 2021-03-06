package com.bendude56.goldenapple.permissions;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.bendude56.goldenapple.GoldenApple;
import com.bendude56.goldenapple.permissions.PermissionManager.Permission;
import com.bendude56.goldenapple.util.Serializer;

/**
 * Represents a group in the GoldenApple permissions database.
 * <p>
 * <em><strong>Note:</strong> Do not store direct references to this class. Store the
 * ID of the instance instead!</em>
 * 
 * @author Deaboy
 * @author ben_dude56
 */
public class PermissionGroup implements IPermissionObject {
	private long					id;
	private String					name;
	private ArrayList<Long>			members		= new ArrayList<Long>();
	private ArrayList<Long>			subGroups	= new ArrayList<Long>();
	private ArrayList<Permission>	permissions	= new ArrayList<Permission>();

	protected PermissionGroup(long id, String name) {
		this.id = id;
		this.name = name;
		this.members = new ArrayList<Long>();
		this.subGroups = new ArrayList<Long>();
		this.permissions = new ArrayList<Permission>();
	}

	@SuppressWarnings("unchecked")
	protected PermissionGroup(long id, String name, String users, String groups, String permissions) {
		this.id = id;
		this.name = name;
		try {
			this.members = (ArrayList<Long>)Serializer.deserialize(users);
			this.subGroups = (ArrayList<Long>)Serializer.deserialize(groups);
			List<String> p = (List<String>)Serializer.deserialize(permissions);
			for (String permission : p) {
				this.permissions.add(GoldenApple.getInstance().permissions.registerPermission(permission));
			}
		} catch (Exception e) {
			GoldenApple.log(Level.SEVERE, "Failed to deserialize info for group " + name + ":");
			GoldenApple.log(Level.SEVERE, e);
		}
	}

	private String serializePermissions() {
		try {
			ArrayList<String> p = new ArrayList<String>();
			for (Permission permission : permissions) {
				p.add(permission.getFullName());
			}
			return Serializer.serialize(p);
		} catch (Exception e) {
			GoldenApple.log(Level.SEVERE, "Failed to serialize permissions for " + name + ":");
			GoldenApple.log(Level.SEVERE, e);
			return "";
		}
	}

	/**
	 * Pushes any changes made to this group to the SQL database
	 */
	public void save() {
		try {
			GoldenApple.getInstance().database.execute("UPDATE Groups SET Permissions=?, Users=?, Groups=? WHERE ID=?", new Object[] { serializePermissions(), Serializer.serialize(members), Serializer.serialize(subGroups), id });
		} catch (SQLException | IOException e) {
			GoldenApple.log(Level.SEVERE, "Failed to save changes to group '" + name + "':");
			GoldenApple.log(Level.SEVERE, e);
		}
	}

	@Override
	public long getId() {
		return id;
	}

	/**
	 * Gets the name of the group represented by this instance.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets a list of user IDs for users that inherit this group's permissions.
	 */
	public List<Long> getMembers() {
		return members;
	}
	
	public void addMember(IPermissionUser user) {
		if (!members.contains(user.getId())) {
			members.add(user.getId());
			save();
		}
	}
	
	public void removeMember(IPermissionUser user) {
		if (members.contains(user.getId())) {
			members.remove((Object) user.getId());
			save();
		}
	}
	
	public boolean isMember(IPermissionUser user, boolean directOnly) {
		if (members.contains(user.getId())) {
			return true;
		} else if (directOnly) {
			return false;
		} else {
			for (Long g : subGroups) {
				if (GoldenApple.getInstance().permissions.getGroup(g).isMember(user, false))
					return true;
			}
			return false;
		}
	}

	/**
	 * Gets a list of group IDs for groups that inherit this group's
	 * permissions.
	 */
	public List<Long> getSubGroups() {
		return subGroups;
	}
	
	public void addSubGroup(PermissionGroup group) {
		if (!subGroups.contains(group.getId())) {
			subGroups.add(group.getId());
			save();
		}
	}
	
	public void removeSubGroup(PermissionGroup group) {
		if (subGroups.contains(group.getId())) {
			subGroups.remove((Object) group.getId());
			save();
		}
	}

	@Override
	public List<Permission> getPermissions(boolean inherited) {
		List<Permission> returnPermissions = permissions;
		if (inherited) {
			List<Long> previousGroups = new ArrayList<Long>();
			for (Long groupID : GoldenApple.getInstance().permissions.getGroups().keySet()) {
				if (!previousGroups.contains(groupID)) {
					for (Long checkedGroupID : previousGroups) {
						if (GoldenApple.getInstance().permissions.getGroup(groupID).getSubGroups().contains(checkedGroupID)) {
							for (Permission perm : GoldenApple.getInstance().permissions.getGroup(groupID).getPermissions(false)) {
								if (!returnPermissions.contains(perm)) {
									returnPermissions.add(perm);
								}
							}
						}
					}
				}
			}
		}
		return returnPermissions;
	}
	
	@Override
	public void addPermission(Permission permission) {
		if (!permissions.contains(permission)) {
			permissions.add(permission);
			save();
		}
	}
	
	@Override
	public void addPermission(String permission) {
		addPermission(GoldenApple.getInstance().permissions.getPermissionByName(permission));
	}
	
	@Override
	public void removePermission(Permission permission) {
		if (permissions.contains(permission)) {
			permissions.remove(permission);
			save();
		}
	}
	
	@Override
	public void removePermission(String permission) {
		removePermission(GoldenApple.getInstance().permissions.registerPermission(permission));
	}

	/**
	 * Checks whether this group has a given permission.
	 * 
	 * @param permission The permission to check for.
	 * @return True if the group has the specified permission, false otherwise.
	 */
	public boolean hasPermission(String permission) {
		return hasPermission(permission, false);
	}

	/**
	 * Checks whether this group has a given permission.
	 * 
	 * @param permission The permission to check for.
	 * @return True if the group has the specified permission, false otherwise.
	 */
	public boolean hasPermission(Permission permission) {
		return hasPermission(permission, false);
	}

	/**
	 * Checks whether this group has a given permission.
	 * 
	 * @param permission The permission to check for.
	 * @param specific Determines whether or not indirect permissions should be
	 *            considered. If true, only permissions given specifically to
	 *            this group will be checked. If false, all permissions
	 *            (including indirect permissions) will be considered.
	 * @return True if the group has the specified permission, false otherwise.
	 */
	public boolean hasPermission(String permission, boolean specific) {
		return hasPermission(GoldenApple.getInstance().permissions.registerPermission(permission), specific);
	}

	/**
	 * Checks whether this group has a given permission.
	 * 
	 * @param permission The permission to check for.
	 * @param specific Determines whether or not indirect permissions should be
	 *            considered. If true, only permissions given specifically to
	 *            this group will be checked. If false, all permissions
	 *            (including indirect permissions) will be considered.
	 * @return True if the group has the specified permission, false otherwise.
	 */
	public boolean hasPermission(Permission permission, boolean specific) {
		return getPermissions(!specific).contains(permission);
	}
}
