package com.cleo.labs.connector.router;

import com.cleo.lexicom.edi.EDIElement;
import com.cleo.lexicom.edi.EDISegment;
import com.google.common.base.Strings;

public class EDIID {
    private String qualifier;
    private String id;
    
    public EDIID qualifier(String qualifier) {
        this.qualifier = qualifier;
        return this;
    }
    public String qualifier() {
        return qualifier;
    }
    public EDIID id(String id) {
        this.id = id;
        return this;
    }
    public String id() {
        return id;
    }

    public EDIID() {
        this.qualifier = null;
        this.id = null;
    }

    public EDIID(String qualifier, String id) {
        this();
        this.qualifier(qualifier).id(id);
    }

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

    public boolean matches(String qualifierRegex, String idRegex) {
        if (!Strings.isNullOrEmpty(qualifierRegex)) {
            if (Strings.isNullOrEmpty(qualifier) || !qualifier.matches(qualifierRegex)) {
                return false;
            }
        }
        if (!Strings.isNullOrEmpty(idRegex)) {
            if (Strings.isNullOrEmpty(id) || !id.matches(idRegex)) {
                return false;
            }
        }
        return true;
    }

    public boolean isEmpty() {
        return qualifier == null && id == null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (qualifier != null) {
            sb.append(qualifier).append(':');
        }
        if (id != null) {
            sb.append(id);
        }
        return sb.toString();
    }
}