package org.sitenetsoft.quarkus.topaz.security;

import java.security.Permission;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A Topaz directory relation check for use with {@code @PermissionsAllowed}, for example
 * {@code @PermissionsAllowed(value = "todo#owner", permission = TopazRelation.class, params = "id")}.
 * <p>
 * The name is {@code objectType#relation} or {@code objectType#relation@subjectType}. Each part is a Topaz identifier.
 * An identifier has 3 to 64 ASCII letters, digits, dots, underscores or dashes. It starts with a letter and ends with
 * a letter or digit. The subject type defaults to {@code user}.
 * <p>
 * The object id is the second constructor parameter, {@code id}. Quarkus matches constructor parameters to the secured
 * method's parameters by name. The secured method needs a parameter named {@code id}. Otherwise {@code params} must
 * name the parameter, or a {@code param.property} expression, that supplies the id.
 * <p>
 * The check runs only when {@code quarkus.topaz.enforcement.permissions.enabled=true}. It asks the directory whether
 * the caller, mapped by the {@link SubjectMapper}, has the relation or permission on {@code objectType:id}.
 * <p>
 * Any failure denies, including a malformed name or a missing id. Quarkus builds the permission when the secured method
 * runs and denies with HTTP 403 when the constructor throws.
 * <p>
 * {@link #implies(Permission)} always returns {@code false}. Another permission held by the identity never grants this
 * permission. Only the Topaz check grants it.
 */
public final class TopazRelation extends Permission {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_SUBJECT_TYPE = "user";

    private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z][a-zA-Z0-9._-]{1,62}[a-zA-Z0-9]");

    /** The object type. */
    private final String objectType;

    /** The relation or permission. */
    private final String relation;

    /** The subject type. */
    private final String subjectType;

    /** The object id. */
    private final String objectId;

    /**
     * Create a relation check permission.
     *
     * @param name {@code objectType#relation} or {@code objectType#relation@subjectType}
     * @param id the object id
     * @throws IllegalArgumentException if the name is malformed or the id is null or blank
     */
    public TopazRelation(String name, String id) {
        super(name);
        if (name == null) {
            throw new IllegalArgumentException("TopazRelation name must not be null");
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
        checkPart("objectType", objectType, name);
        checkPart("relation", relation, name);
        checkPart("subjectType", subjectType, name);
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("TopazRelation '" + name + "' has no object id");
        }
        this.objectId = id;
    }

    /**
     * Check that a name part is a Topaz identifier.
     *
     * @param part the part label used in the message
     * @param value the part value
     * @param name the full permission name
     * @throws IllegalArgumentException if the value is not a Topaz identifier
     */
    private static void checkPart(String part, String value, String name) {
        if (!IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(part + " '" + value + "' in TopazRelation name '" + name
                    + "' must match " + IDENTIFIER.pattern());
        }
    }

    /**
     * Create the exception for a name without the expected shape.
     *
     * @param name the full permission name
     * @return the exception to throw
     */
    private static IllegalArgumentException malformed(String name) {
        return new IllegalArgumentException("TopazRelation name '" + name
                + "' is not objectType#relation or objectType#relation@subjectType");
    }

    /**
     * Get the object type.
     *
     * @return the object type
     */
    public String objectType() {
        return objectType;
    }

    /**
     * Get the object id.
     *
     * @return the object id
     */
    public String objectId() {
        return objectId;
    }

    /**
     * Get the relation or permission.
     *
     * @return the relation or permission
     */
    public String relation() {
        return relation;
    }

    /**
     * Get the subject type.
     *
     * @return the subject type, which is {@code user} unless the name says otherwise
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
