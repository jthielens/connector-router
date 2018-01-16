package com.cleo.labs.connector.router;

import java.util.ArrayList;
import java.util.List;

public class EDIElement {
    private String element;
    private String[] subelements;

    protected EDIElement(String element, char subelementSeparator, boolean escape, char escapeCharacter) {
        this.element = EDISegment.escape(element, subelementSeparator, subelementSeparator, escape, escapeCharacter);

        List<String> list = new ArrayList<>();
        int index = 0;
        for (int i = 0; i <= element.length(); i++) {
            /*------------------------------------------------------------------------------
             *    At the end of a subelement if
             *      - At the end of the element or
             *      - At an element separator and
             *          At the beginning of the segment or
             *          No escape character supported or
             *          Escape character doesn't precede element separator
             *----------------------------------------------------------------------------*/
            if ((i == element.length() && list.size() > 0)
                    || (i < element.length() && element.charAt(i) == subelementSeparator
                            && (i == 0 || !escape || element.charAt(i - 1) != escapeCharacter))) {
                list.add(EDISegment.escape(element.substring(index, i), subelementSeparator, subelementSeparator,
                        escape, escapeCharacter));
                index = i + 1;
            }
        }
        this.subelements = list.toArray(new String[list.size()]);
    }

    /**
     * Returns the EDI element's original unparsed string
     * 
     * @return Unparsed EDI element
     */
    public String getElement() {
        return this.element;
    }

    /**
     * Returns the parsed EDI subelement array
     * 
     * @return EDI subelement array
     */
    public String[] getSubelements() {
        return this.subelements;
    }

    /**
     * Returns the parsed EDI subelement count
     * 
     * @return EDI subelement count
     */
    public int getSubelementCount() {
        return this.subelements.length;
    }

    /**
     * Returns the requested, parsed EDI subelement
     * 
     * @return EDI subelement
     */
    public String getSubelement(int index) {
        return this.subelements[index];
    }
}
