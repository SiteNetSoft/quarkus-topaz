package org.sitenetsoft.quarkus.topaz.security;

import java.security.Permission;
import java.util.Objects;

/**
 * A Topaz directory relation check usable with {@code @PermissionsAllowed}, for example
 * {@code @PermissionsAllowed(value = "todo#owner", permission = TopazRelation.class, params = "id")}.
 * <p>
 * The name is {@code objectType#relation} or {@code objectType#relation@subjectType}; the subject type defaults to
 * {@code user}. The object id is the second constructor parameter, {@code id}: Quarkus matches constructor parameters
 * to the secured method's parameters by name, so the secured method needs a parameter named {@code id}, or
 * {@code params} must name the parameter (or a {@code param.property} expression) that supplies it.
 * <p>
 * The check is performed only when {@code quarkus.topaz.enforcement.permissions.enabled=true}; it asks the directory
 * whether the caller, mapped by the {@link SubjectMapper}, has the relation (or permission) on
 * {@code objectType:id}. Any failure denies, including a malformed name or a missing id: Quarkus builds the permission
 * when the secured method is invoked and denies (403) when the constructor throws. {@link #implies(Permission)} always
 * returns {@code false}: this permission is never granted by another permission held by the identity, only by the
 * Topaz check.
 */
public final class TopazRelation extends Permission {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_SUBJECT_TYPE = "user";

    private final String objectType;
    private final String relation;
    private final String subjectType;
    private final String objectId;

    /**
     * @param name {@code objectType#relation} or {@code objectType#relation@subjectType}
     * @param id the object id
     * @throws IllegalArgumentException if the name is malformed or the id is null or blank
     */
    public TopazRelation(String name, String id) {
        super(name);
        if (name == null) {
            throw new IllegalArgumentException("TopazRelation name must not be null");
        }
        if (name.chars().anyMatch(Character::isWhitespace)) {
            throw malformed(name);
        }
        int hash = name.indexOf('#');
        if (hash < 0) {
            throw malformed(name);
        }
        String rest = name.substring(hash + 1);
        int at = rest.indexOf('@');
        this.objectType = name.substring(0, hash);
        this.relation = at < 0 ? rest : rest.substring(0, at);
        this.subjectType = at < 0 ? DEFAULT_SUBJECT_TYPE : rest.substring(at + 1);
        if (objectType.isBlank() || relation.isBlank() || subjectType.isBlank() || relation.indexOf('#') >= 0
                || subjectType.indexOf('@') >= 0) {
            throw malformed(name);
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("TopazRelation '" + name + "' has no object id");
        }
        this.objectId = id;
    }

    private static IllegalArgumentException malformed(String name) {
        return new IllegalArgumentException("TopazRelation name '" + name
                + "' is not objectType#relation or objectType#relation@subjectType");
    }

    /**
     * @return the object type
     */
    public String objectType() {
        return objectType;
    }

    /**
     * @return the object id
     */
    public String objectId() {
        return objectId;
    }

    /**
     * @return the relation or permission
     */
    public String relation() {
        return relation;
    }

    /**
     * @return the subject type, {@code user} unless the name says otherwise
     */
    public String subjectType() {
        return subjectType;
    }

    @Override
    public boolean implies(Permission permission) {
        return false;
    }

    @Override
    public String getActions() {
        return "";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TopazRelation other && getName().equals(other.getName())
                && objectId.equals(other.objectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), objectId);
    }

    @Override
    public String toString() {
        return "TopazRelation[" + subjectType + " " + relation + " " + objectType + ":" + objectId + "]";
    }
}
