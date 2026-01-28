/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.envers.Audited;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtract;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import com.wci.umls.server.Project;
import com.wci.umls.server.User;
import com.wci.umls.server.UserPreferences;
import com.wci.umls.server.UserRole;
import com.wci.umls.server.jpa.helpers.MapIdBridge;
import com.wci.umls.server.jpa.helpers.ObjectToStringBridge;
import com.wci.umls.server.jpa.helpers.ProjectRoleBridge;
import com.wci.umls.server.jpa.helpers.ProjectRoleMapAdapter;

/**
 * JPA and JAXB enabled implementation of {@link User}.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = {
    "userName"
}))
//@Audited
@Indexed
@XmlRootElement(name = "user")
public class UserJpa implements User {

  /** The id. */
  @TableGenerator(name = "EntityIdGenUser", table = "table_generator_users", pkColumnValue = "Entity", initialValue = 50)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "EntityIdGenUser")
  @GenericField(searchable = Searchable.YES)
  private Long id;

  /** The user name. */
  @Column(nullable = false, unique = true)
  @KeywordField(searchable = Searchable.YES, sortable = Sortable.YES)
  private String userName;

  /** The name. */
  @Column(nullable = false)
  @FullTextField
  @KeywordField(name = "nameSort", sortable = Sortable.YES)
  private String name;

  /** The team. */
  @Column(nullable = true)
  @KeywordField(searchable = Searchable.YES)
  private String team;

  /** The email. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String email;

  /** The editor level. */
  @Column(nullable = false)
  private int editorLevel = 0;

  /** The application role. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  private UserRole applicationRole;

  /** The auth token. */
  @Transient
  private String authToken;

  /** The user preferences. */
  @OneToOne(mappedBy = "user", targetEntity = UserPreferencesJpa.class, optional = true)
  private UserPreferences userPreferences;

  /** The project role map. */
  @ElementCollection
  @MapKeyClass(value = ProjectJpa.class)
  @Enumerated(EnumType.STRING)
  @CollectionTable(name = "user_project_role_map")
  @MapKeyJoinColumn(name = "project_id")
  @Column(name = "role")
  @FullTextField(valueBridge = @ValueBridgeRef(type = ProjectRoleBridge.class),
      extraction = @ContainerExtraction(extract = ContainerExtract.NO))
  @FullTextField(name = "projectAnyRole",
      valueBridge = @ValueBridgeRef(type = MapIdBridge.class),
      extraction = @ContainerExtraction(extract = ContainerExtract.NO))
  private Map<Project, UserRole> projectRoleMap;

  /**
   * The default constructor.
   */
  public UserJpa() {
  }

  /**
   * Instantiates a new user jpa.
   *
   * @param user the user
   */
  public UserJpa(User user) {
    super();
    id = user.getId();
    userName = user.getUserName();
    name = user.getName();
    team = user.getTeam();
    email = user.getEmail();
    editorLevel = user.getEditorLevel();
    applicationRole = user.getApplicationRole();
    authToken = user.getAuthToken();
    userPreferences = user.getUserPreferences();
    projectRoleMap = new HashMap<>(user.getProjectRoleMap());
  }

  /* see superclass */
  @Override
  public Long getId() {
    return id;
  }

  /* see superclass */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /* see superclass */
  @Override
  public String getUserName() {
    return userName;
  }

  /* see superclass */
  @Override
  public void setUserName(String username) {
    this.userName = username;
  }

  /* see superclass */
  @Override
  public String getName() {
    return name;
  }

  /* see superclass */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getTeam() {
    return team;
  }

  /* see superclass */
  @Override
  public void setTeam(String team) {
    this.team = team;
  }

  /* see superclass */
  @Override
  public String getEmail() {
    return email;
  }

  /* see superclass */
  @Override
  public void setEmail(String email) {
    this.email = email;
  }

  /* see superclass */
  @Override
  public int getEditorLevel() {
    return editorLevel;
  }

  /* see superclass */
  @Override
  public void setEditorLevel(int editorLevel) {
    this.editorLevel = editorLevel;
  }

  /* see superclass */
  @Override
  public UserRole getApplicationRole() {
    return applicationRole;
  }

  /* see superclass */
  @Override
  public void setApplicationRole(UserRole role) {
    this.applicationRole = role;
  }

  /* see superclass */
  @Override
  public String getAuthToken() {
    return authToken;
  }

  /* see superclass */
  @Override
  public void setAuthToken(String authToken) {
    this.authToken = authToken;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result += editorLevel;
    result = prime * result
        + ((applicationRole == null) ? 0 : applicationRole.hashCode());
    result = prime * result + ((authToken == null) ? 0 : authToken.hashCode());
    result = prime * result + ((email == null) ? 0 : email.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((team == null) ? 0 : team.hashCode());
    result = prime * result + ((userName == null) ? 0 : userName.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    UserJpa other = (UserJpa) obj;
    if (applicationRole != other.applicationRole)
      return false;
    if (authToken == null) {
      if (other.authToken != null)
        return false;
    } else if (!authToken.equals(other.authToken))
      return false;
    if (email == null) {
      if (other.email != null)
        return false;
    } else if (!email.equals(other.email))
      return false;
    if (editorLevel != other.editorLevel)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (team == null) {
      if (other.team != null)
        return false;
    } else if (!team.equals(other.team))
      return false;
    if (userName == null) {
      if (other.userName != null)
        return false;
    } else if (!userName.equals(other.userName))
      return false;
    return true;
  }

  /* see superclass */
  @XmlElement(type = UserPreferencesJpa.class)
  @Override
  public UserPreferences getUserPreferences() {
    return userPreferences;
  }

  /* see superclass */
  @Override
  public void setUserPreferences(UserPreferences preferences) {
    this.userPreferences = preferences;
  }

  /* see superclass */
  @Override
  public String toString() {
    return "UserJpa [id=" + id + ", userName=" + userName + ", name=" + name
        + ", team=" + team + ", email=" + email + ", applicationRole="
        + applicationRole + ", authToken=" + authToken + ", editorLevel="
        + editorLevel + "]";
  }

  /*
   * <pre> This supports searching both for a particular role on a particular
   * project or to determine if this user is assigned to any project. For
   * example:
   *
   * "projectRoleMap:10ADMIN" -> finds where the user has an ADMIN role on
   * project 10 "projectAnyRole:10" -> finds where the user has any role on
   * project 10 </pre>
   */
  @XmlJavaTypeAdapter(ProjectRoleMapAdapter.class)
  @Override
  public Map<Project, UserRole> getProjectRoleMap() {
    if (projectRoleMap == null) {
      projectRoleMap = new HashMap<>();
    }
    return projectRoleMap;
  }

  /* see superclass */
  @Override
  public void setProjectRoleMap(Map<Project, UserRole> projectRoleMap) {
    this.projectRoleMap = projectRoleMap;
  }

}