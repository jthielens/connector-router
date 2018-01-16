package com.cleo.labs.connector.router;

import com.google.common.base.Strings;

/**
 * Encapsulates the common EDI pattern of a qualified identifier,
 * comprising a <em>qualifier</em> and an <em>id</em>, either of
 * which may be absert ({@code null}) or empty.  This class also
 * allows these identifiers to be matched against a pair of regular
 * expressions, which also may be {@code null} or empty, indicating
 * a pattern that matches anything (including {@code null}) by default.
 */
public class EDIID {
    private String qualifier;
    private String id;
    
    /**
     * Sets the qualifier.
     * @param qualifier a possibly {@code null} String
     * @return {@code this} to allow for fluent-style setting
     */
    public EDIID qualifier(String qualifier) {
        this.qualifier = qualifier;
        return this;
    }
    /**
     * Returns the possibly {@code null} qualifier.
     * @return the qualifier.
     */
    public String qualifier() {
        return qualifier;
    }
    /**
     * Sets the id.
     * @param id a possibly {@code null} String
     * @return {@code this} to allow for fluent-style setting
     */
    public EDIID id(String id) {
        this.id = id;
        return this;
    }
    /**
     * Returns the possibly {@code null} id.
     * @return the id.
     */
    public String id() {
        return id;
    }

    /**
     * Constructs a new empty {@code EDIID} where
     * both the qualifier and the id are {@code null}.
     */
    public EDIID() {
        this.qualifier = null;
        this.id = null;
    }

    /**
     * Constructs a new {@code EDIID} with initial values
     * for qualifier and id, either or both possibl {@code null}.
     * @param qualifier the possibly {@code null} qualifier
     * @param id the possibly {@code null} id
     */
    public EDIID(String qualifier, String id) {
        this();
        this.qualifier(qualifier).id(id);
    }

    /**
     * Copy constructor.
     * @param copy the EDIID to copy
     */
    public EDIID(EDIID copy) {
        this.id = copy.id;
        this.qualifier = copy.qualifier;
    }
    /**
     * Sets the qualifier and id for the {@code EDIID} by
     * parsing element {@code index} (0-relative) from an
     * EDISegment.  According to EDIFACT conventions, the
     * element has subelements containing the id (in subelement
     * 0) and qualifier (in subelement 1).  If there are no
     * subelements, the entire element is assumed to contain
     * the id.
     * @param segment the EDIFACT EDISegment
     * @param index the (0-relative) index of the ID element
     * @return {@code this} to allow for fluent-style setting
     */
    public EDIID fromEdifact(EDISegment segment, int index) {
        if (segment.getElementCount() > index) {
            EDIElement e = segment.getElement(index);
            if (e.getSubelementCount() > 0) {
                id(e.getSubelement(0).trim());
                if (e.getSubelementCount() > 1) {
                    qualifier(e.getSubelement(1).trim());
                } else {
                    qualifier(null);
                }
            } else {
                id(e.getElement().trim());
                qualifier(null);
            }
        }
        return this;
    }

    /**
     * Matches this {@code EDIID} against a pair of regular expressions,
     * returning {@code true} only if both match the corresponding
     * components of the {@code EDIID}.  If a regular expression is
     * {@code null} or empty, it matches its target, including {@code null},
     * by default.  Otherwise if a component is {@code null} or empty, it
     * matches only if the corresponding expression matches the empty
     * string.
     * @param qualifierRegex the regular expression to match against the qualifier
     * @param idRegex the regular expression to match against the id
     * @return {@code true} if the {@code EDIID} matches
     */
    public boolean matches(String qualifierRegex, String idRegex) {
        if (!Strings.isNullOrEmpty(qualifierRegex)) {
            if (!Strings.nullToEmpty(qualifier).matches(qualifierRegex)) {
                return false;
            }
        }
        if (!Strings.isNullOrEmpty(idRegex)) {
            if (!Strings.nullToEmpty(id).matches(idRegex)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if both components of the {@code EDIID} are
     * either {@code null} or the empty string.
     * @return {@code true} if the {@code EDIID} is empty
     */
    public boolean isEmpty() {
        return Strings.isNullOrEmpty(qualifier) && Strings.isNullOrEmpty(id);
    }

    /**
     * Returns a string representation of the {@code EDIID} in the
     * format {@code qualifier:id} (note that this matches X12
     * conventions, but is the reverse of typical EDIFACT
     * conventions).  If the qualifier is empty, the {@code :}
     * is omitted.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!Strings.isNullOrEmpty(qualifier)) {
            sb.append(qualifier).append(':');
        }
        if (!Strings.isNullOrEmpty(id)) {
            sb.append(id);
        }
        return sb.toString();
    }
}