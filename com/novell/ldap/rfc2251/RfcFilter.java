/* **************************************************************************
 * $OpenLDAP$
 *
 * Copyright (C) 1999, 2000, 2001 Novell, Inc. All Rights Reserved.
 *
 * THIS WORK IS SUBJECT TO U.S. AND INTERNATIONAL COPYRIGHT LAWS AND
 * TREATIES. USE, MODIFICATION, AND REDISTRIBUTION OF THIS WORK IS SUBJECT
 * TO VERSION 2.0.1 OF THE OPENLDAP PUBLIC LICENSE, A COPY OF WHICH IS
 * AVAILABLE AT HTTP://WWW.OPENLDAP.ORG/LICENSE.HTML OR IN THE FILE "LICENSE"
 * IN THE TOP-LEVEL DIRECTORY OF THE DISTRIBUTION. ANY USE OR EXPLOITATION
 * OF THIS WORK OTHER THAN AS AUTHORIZED IN VERSION 2.0.1 OF THE OPENLDAP
 * PUBLIC LICENSE, OR OTHER PRIOR WRITTEN CONSENT FROM NOVELL, COULD SUBJECT
 * THE PERPETRATOR TO CRIMINAL AND CIVIL LIABILITY.
 ******************************************************************************/

package com.novell.ldap.rfc2251;

import java.io.UnsupportedEncodingException;
import java.util.StringTokenizer;
import java.util.Stack;

import com.novell.ldap.asn1.*;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPLocalException;
import com.novell.ldap.resources.*;

/**
 * Represents an LDAP Filter.
 *
 *<pre>
 *       Filter ::= CHOICE {
 *               and             [0] SET OF Filter,
 *               or              [1] SET OF Filter,
 *               not             [2] Filter,
 *               equalityMatch   [3] AttributeValueAssertion,
 *               substrings      [4] SubstringFilter,
 *               greaterOrEqual  [5] AttributeValueAssertion,
 *               lessOrEqual     [6] AttributeValueAssertion,
 *               present         [7] AttributeDescription,
 *               approxMatch     [8] AttributeValueAssertion,
 *               extensibleMatch [9] MatchingRuleAssertion }
 *</pre>
 */
public class RfcFilter extends ASN1Choice
{
    //*************************************************************************
    // Public variables for Filter
    //*************************************************************************

    /**
     * Context-specific TAG for AND component.
     */
    public final static int AND = 0;
    /**
     * Context-specific TAG for OR component.
     */
    public final static int OR = 1;
    /**
     * Context-specific TAG for NOT component.
     */
    public final static int NOT = 2;
    /**
     * Context-specific TAG for EQUALITY_MATCH component.
     */
    public final static int EQUALITY_MATCH = 3;
    /**
     * Context-specific TAG for SUBSTRINGS component.
     */
    public final static int SUBSTRINGS = 4;
    /**
     * Context-specific TAG for GREATER_OR_EQUAL component.
     */
    public final static int GREATER_OR_EQUAL = 5;
    /**
     * Context-specific TAG for LESS_OR_EQUAL component.
     */
    public final static int LESS_OR_EQUAL = 6;
    /**
     * Context-specific TAG for PRESENT component.
     */
    public final static int PRESENT = 7;
    /**
     * Context-specific TAG for APPROX_MATCH component.
     */
    public final static int APPROX_MATCH = 8;
    /**
     * Context-specific TAG for EXTENSIBLE_MATCH component.
     */
    public final static int EXTENSIBLE_MATCH = 9;

    /**
     * Context-specific TAG for INITIAL component.
     */
    public final static int INITIAL = 0;
    /**
     * Context-specific TAG for ANY component.
     */
    public final static int ANY = 1;
    /**
     * Context-specific TAG for FINAL component.
     */
    public final static int FINAL = 2;

    //*************************************************************************
    // Private variables for Filter
    //*************************************************************************

    private FilterTokenizer ft;
    private Stack filterStack;
    private boolean finalFound;

    //*************************************************************************
    // Constructor for Filter
    //*************************************************************************

    /**
     * Constructs a Filter object by parsing an RFC 2254 Search Filter String.
     */
    public RfcFilter(String filter)
            throws LDAPException
    {
       super(null);
       setChoiceValue(parse(filter));
       return;
    }

    /**
     * Constructs a Filter object that will be built up piece by piece.   */
    public RfcFilter() {
        super(null);
        filterStack = new Stack();
        //The choice value must be set later: setChoiceValue(rootFilterTag)
        return;
    }

    //*************************************************************************
    // Helper methods for RFC 2254 Search Filter parsing.
    //*************************************************************************

    /**
     * Parses an RFC 2251 filter string into an ASN.1 LDAP Filter object.
     */
    private final ASN1Tagged parse(String filterExpr)
            throws LDAPException
    {
        if(filterExpr == null || filterExpr.equals("")) {
            filterExpr = new String("(objectclass=*)");
        }

        int idx;
        if( (idx = filterExpr.indexOf('\\')) != -1) {
            StringBuffer sb = new StringBuffer(filterExpr);
            int i = idx;
            while(i < (sb.length()-1)) {
                char c = sb.charAt(i++);
                if (c == '\\') {
                    // found '\' (backslash)
                    // If V2 escape, turn to a V3 escape
                    c = sb.charAt(i);
                    if(c =='*' || c =='(' || c==')' || c =='\\' ) {
                        // LDAP v2 filter, convert them into hex chars
                        sb.delete(i,i+1);
                        sb.insert(i, Integer.toHexString((int)c));
                        i+=2;
                    }
                }
            }
            filterExpr = sb.toString();
        }

        // missing opening and closing parentheses, must be V2, add parentheses
        if( (filterExpr.charAt(0) != '(') &&
            (filterExpr.charAt(filterExpr.length()-1) != ')')) {
                filterExpr = "(" + filterExpr + ")";
        }

        char ch = filterExpr.charAt(0);
        int len = filterExpr.length();

        // missing opening parenthesis ?
        if (ch!='(') {
            throw new LDAPLocalException( ExceptionMessages.MISSING_LEFT_PAREN,
                                          LDAPException.FILTER_ERROR);
        }

        // missing closing parenthesis ?
        if( filterExpr.charAt(len-1) != ')') {
            throw new LDAPLocalException( ExceptionMessages.MISSING_RIGHT_PAREN,
                                          LDAPException.FILTER_ERROR);
        }

        // unmatched parentheses ?
        int parenCount = 0;
        for (int i=0; i<len; i++) {
            if (filterExpr.charAt(i) == '(') {
                parenCount++;
            }

            if (filterExpr.charAt(i) == ')' ) {
                parenCount--;
            }
        }

        if (parenCount > 0) {
            throw new LDAPLocalException( ExceptionMessages.MISSING_RIGHT_PAREN,
                                          LDAPException.FILTER_ERROR);
        }
        
        if (parenCount < 0) {
            throw new LDAPLocalException( ExceptionMessages.MISSING_LEFT_PAREN,
                                          LDAPException.FILTER_ERROR);
        }

        ft = new FilterTokenizer(filterExpr);

        return parseFilter();
    }

    /**
     * Parses an RFC 2254 filter
     */
    private final ASN1Tagged parseFilter()
            throws LDAPException
    {
        ft.getLeftParen();

        ASN1Tagged filter = parseFilterComp();

        ft.getRightParen();

        return filter;
    }

    /**
     * RFC 2254 filter helper method. Will Parse a filter component.
     */
    private final ASN1Tagged parseFilterComp()
            throws LDAPException
    {
        ASN1Tagged tag = null;
        int filterComp = ft.getOpOrAttr();

        switch(filterComp) {
        case AND:
        case OR:
            tag = new ASN1Tagged(
                new ASN1Identifier(ASN1Identifier.CONTEXT, true, filterComp),
                parseFilterList(),
                false);
            break;
        case NOT:
            tag = new ASN1Tagged(
                new ASN1Identifier(ASN1Identifier.CONTEXT, true, filterComp),
                parseFilter(),
                true);
            break;
        default:
            int filterType = ft.getFilterType();
            String value = ft.getValue();

            switch(filterType) {
            case GREATER_OR_EQUAL:
            case LESS_OR_EQUAL:
            case APPROX_MATCH:
                tag = new ASN1Tagged(
                    new ASN1Identifier(ASN1Identifier.CONTEXT, true,
                                    filterType),
                    new RfcAttributeValueAssertion(
                        new RfcAttributeDescription(ft.getAttr()),
                        new RfcAssertionValue(unescapeString(value))),
                    false);
                break;
            case EQUALITY_MATCH: // may be PRESENT or SUBSTRINGS also
                if(value.equals("*")) {
                    // present
                    tag = new ASN1Tagged(
                        new ASN1Identifier(ASN1Identifier.CONTEXT, false, PRESENT),
                        new RfcAttributeDescription(ft.getAttr()),
                        false);
                } else
                if(value.indexOf('*') != -1) {
                    // substrings parse:
                    //    [initial], *any*, [final] into an ASN1SequenceOf
                    StringTokenizer sub =
                        new StringTokenizer(value, "*", true);
                    ASN1SequenceOf seq = new ASN1SequenceOf(5);
                    int tokCnt = sub.countTokens();
                    int cnt = 0;

                    String lastTok = new String("");

                    while(sub.hasMoreTokens()) {
                        String subTok = sub.nextToken();
                        cnt++;
                        if(subTok.equals("*")) {
                            // if previous token was '*', and since the current
                            // token is a '*', we need to insert 'any'
                            if (lastTok.equals(subTok)) {
                                // '**'
                                seq.add(
                                    new ASN1Tagged(
                                        new ASN1Identifier(ASN1Identifier.CONTEXT,
                                                    false, ANY),
                                        new RfcLDAPString(unescapeString("")),
                                        false));
                            }
                        } else {
                            // value (RfcLDAPString)
                            if(cnt == 1) {
                                // initial
                                seq.add(
                                    new ASN1Tagged(
                                        new ASN1Identifier(ASN1Identifier.CONTEXT,
                                                    false, INITIAL),
                                        new RfcLDAPString(unescapeString(subTok)),
                                        false));
                            } else
                            if(cnt < tokCnt) {
                                // any
                                seq.add(
                                    new ASN1Tagged(
                                        new ASN1Identifier(ASN1Identifier.CONTEXT,
                                                    false, ANY),
                                        new RfcLDAPString(unescapeString(subTok)),
                                        false));
                            } else {
                                // final
                                seq.add(
                                    new ASN1Tagged(
                                        new ASN1Identifier(ASN1Identifier.CONTEXT,
                                                    false, FINAL),
                                        new RfcLDAPString(unescapeString(subTok)),
                                        false));
                            }
                        }
                        lastTok = subTok;
                    }

                    tag = new ASN1Tagged(
                        new ASN1Identifier(ASN1Identifier.CONTEXT, true, SUBSTRINGS),
                        new RfcSubstringFilter(
                            new RfcAttributeDescription(ft.getAttr()), seq),
                        false);
                } else {
                    // simple
                    tag = new ASN1Tagged(
                        new ASN1Identifier(ASN1Identifier.CONTEXT, true,
                                    EQUALITY_MATCH),
                        new RfcAttributeValueAssertion(
                            new RfcAttributeDescription(ft.getAttr()),
                            new RfcAssertionValue(unescapeString(value))),
                        false);
                }
                break;
            case EXTENSIBLE_MATCH:
                String type = null, matchingRule = null;
                boolean dnAttributes = false;
                StringTokenizer st =
                new StringTokenizer(ft.getAttr(), ":", true);

                boolean first = true;
                while(st.hasMoreTokens()) {
                    String s = st.nextToken().trim();
                    if(first && !s.equals(":")) {
                        type = s;
                    } else
                    // dn must be lower case to be considered dn of the Entry.
                    if(s.equals("dn")) {
                        dnAttributes = true;
                    } else
                    if(!s.equals(":")) {
                        matchingRule = s;
                    }
                    first = false;
                }

                tag = new ASN1Tagged(
                    new ASN1Identifier(ASN1Identifier.CONTEXT, true,
                                EXTENSIBLE_MATCH),
                    new RfcMatchingRuleAssertion(
                                (matchingRule == null) ? null :
                        new RfcMatchingRuleId(matchingRule),
                                (type == null) ? null :
                        new RfcAttributeDescription(type),
                        new RfcAssertionValue(unescapeString(value)),
                                (dnAttributes == false) ? null :
                        new ASN1Boolean(true)),
                    false);
            }
        }
        return tag;
    }

    /**
     * Must have 1 or more Filters
     */
    private final ASN1SetOf parseFilterList()
            throws LDAPException
    {
        ASN1SetOf set = new ASN1SetOf();

        set.add(parseFilter()); // must have at least 1 filter

        while(ft.peekChar() == '(') { // check for more filters
            set.add(parseFilter());
        }
        return set;
    }

    /**
     * Convert hex character to an integer. Return -1 if char is something
     * other than a hex char.
     */
    static final int hex2int(char c)
    {
        return
            (c >= '0' && c <= '9') ? c - '0'      :
            (c >= 'A' && c <= 'F') ? c - 'A' + 10 :
            (c >= 'a' && c <= 'f') ? c - 'a' + 10 :
                -1;
    }

    /**
     * Replace escaped hex digits with the equivalent binary representation.
     * Assume either V2 or V3 escape mechanisms:
     * V2: \*,  \(,  \),  \\.
     * V3: \2A, \28, \29, \5C, \00.
     *
     * @param string    A part of the input filter string to be converted.
     *
     * @return octet-string encoding of the specified string.
     */
    private final byte[] unescapeString(String string)
            throws LDAPException
    {
        byte octets[] = new byte[string.length()];
        // index for string and octets
        int iString, iOctets;
        // escape==true means we are in an escape sequence.
        boolean escape = false;
        // escStart==true means we are reading the first character of an escape.
        boolean escStart = false;

        int ival, length = string.length();
        char ch, temp = 0;

       /*
        * loop through each character of the string and copy them into octets
        *  converting escaped sequences when needed
        */
       for(iString = 0, iOctets = 0; iString < length; iString++) {
            ch = string.charAt(iString);
            if(escape) {
                if((ival = hex2int(ch)) < 0) {
                    // Invalid escape value
                    throw new LDAPLocalException(
                                      ExceptionMessages.INVALID_ESCAPE,
                                      new Object[] {new Character(ch)},
                                      LDAPException.FILTER_ERROR);
                } else {
                    // V3 escaped: \\**
                    if(escStart) {
                        temp = (char)(ival<<4);
                        escStart = false;
                    } else {
                        temp |= (char)(ival);
                        octets[iOctets++] = (byte) temp;
                        escStart = escape = false;
                    }
                }
            } else
            if(ch == '\\') {
              escStart = escape = true;
            } else {
                // place the character into octets.
                byte b = (byte) ch;
                if (( b >= 0x01 && b <= 0x27 ) ||
                    ( b >= 0x2B && b <= 0x5B ) ||
                    ( b >= 0x5D && b <= 0x7F )) {

                    // found valid character = %x01-27 / %x2b-5b / %x5d-7f
                    octets[iOctets++] = (byte)ch;
                    escape = false;
                } else {
                    // found invalid character
                    String escString = "";
                    try {
                        char[] ca = new char[1];
                        ca[0] = ch;
                        byte[] utf8Bytes = new String(ca).getBytes("UTF-8");
                        for( int i=0; i < utf8Bytes.length; i++) {
                            byte u = utf8Bytes[i];
                            if( (u >= 0) && (u < 0x10)) {
                                escString = escString + "\\0" + Integer.toHexString(u & 0xff);
                            } else {
                                escString = escString + "\\" + Integer.toHexString(u & 0xff);
                            }
                        }
                    } catch ( UnsupportedEncodingException ue) {
                        throw new RuntimeException(
                            "UTF-8 String encoding not supported by JVM");
                    }

                    throw new LDAPLocalException(
                            ExceptionMessages.INVALID_CHAR_IN_FILTER,
                            new Object[] { new Character(ch), escString },
                            LDAPException.FILTER_ERROR);
                }
            }
        }

        // Verify that any escape sequence completed
        if (escStart || escape) {
            throw new LDAPLocalException(ExceptionMessages.SHORT_ESCAPE,
                                    LDAPException.FILTER_ERROR);
        }

        byte toReturn[] = new byte[iOctets];
        System.arraycopy(octets, 0, toReturn, 0, iOctets);
        octets = null;
        return toReturn;
    }

    /* **********************************************************************
     *  The following methods aid in building filters sequentially,
     *  and is used by DSMLHandler:
     ***********************************************************************/

    /**
     * Called by sequential filter building methods to add to a filter.
     *
     * <p>Verifies that the specified ASN1Object can be added, then adds the
     * object to the filter.</p>
     * @param current   Filter component to be added to the filter
     * @throws LDAPLocalException Occurs when an invalid component is added, or
     * when the component is out of sequence.
     */
    private void addObject(ASN1Object current) throws LDAPLocalException
    {
        if ( choiceValue() == null ) {
            //ChoiceValue is the root ASN1 node
            setChoiceValue(current);
        } else {
            ASN1Tagged topOfStack = (ASN1Tagged)filterStack.peek();
            ASN1Object value = topOfStack.taggedValue();
            if (value == null){
                topOfStack.setTaggedValue(current);
                filterStack.add(current);
            } else if (value instanceof ASN1Set) {
                ((ASN1Set)value).add( current );
                //don't add this to the stack:
            } else if (value.getIdentifier().getTag() == RfcFilter.NOT){
                throw new LDAPLocalException(
                        "Attemp to create more than one 'not' sub-filter",
                        LDAPException.FILTER_ERROR);
            }
        }
        int type = current.getIdentifier().getTag();
        if (type == RfcFilter.AND || type == RfcFilter.OR ||
                type == RfcFilter.NOT ){
            filterStack.add(current);
        }
        return;
    }

    /**
     * Creates and adds the ASN1Tagged value for a nestedFilter: AND, OR, or
     * NOT.
     *
     * <p>Note that a Not nested filter can only have one filter, where AND
     * and OR do not</p>
     *
     * @param rfcType Filter type:
     *              [RfcFilter.AND | RfcFilter.OR | RfcFilter.NOT]
     * @throws LDAPLocalException
     */
    public void startNestedFilter(int rfcType) throws LDAPLocalException
    {
        ASN1Object current;
        if (rfcType == RfcFilter.AND || rfcType == RfcFilter.OR){
            current = new ASN1Tagged(
                    new ASN1Identifier(ASN1Identifier.CONTEXT, true, rfcType),
                    new ASN1Set(),  //content to be set later
                    false);
        } else if (rfcType == RfcFilter.NOT){
            current = new ASN1Tagged(
                    new ASN1Identifier(ASN1Identifier.CONTEXT, true, rfcType),
                    null,  //content to be set later
                    true);
        } else {
            throw new LDAPLocalException(
                "Attempt to create a nested filter other than AND, OR or NOT",
                LDAPException.FILTER_ERROR);
        }
        addObject(current);
        return;
    }

    /**
     * Completes a nested filter and checks for the valid filter type.
     * @param rfcType  Type of filter to complete.
     * @throws LDAPLocalException  Occurs when the specified type differs from
     * the current filter component.
     */
    public void endNestedFilter(int rfcType) throws LDAPLocalException
    {
        int topOfStackType = ((ASN1Object)
                filterStack.peek()).getIdentifier().getTag();
        if (topOfStackType != rfcType){
            throw new LDAPLocalException("Missmatched ending of nested filter",
                    LDAPException.FILTER_ERROR);
        }
        filterStack.pop();
        return;
    }

    /**
     * Creates and addes a substrings filter component.
     *
     * <p>startSubstrings must be immediatly followed by at least one
     * addSubstring method and one terminating endSubstrings method</p>
     * @throws LDAPLocalException
     * Occurs when this component is created out of sequence.
     */
    public void startSubstrings(String attrName) throws LDAPLocalException
    {
        finalFound = false;
        ASN1SequenceOf seq = new ASN1SequenceOf(5);
        ASN1Object current = new ASN1Tagged(
                        new ASN1Identifier(ASN1Identifier.CONTEXT, true, RfcFilter.SUBSTRINGS),
                        new RfcSubstringFilter(
                            new RfcAttributeDescription(attrName), seq),
                            //this sequence will be filled in later
                        false);
        addObject(current);
        filterStack.push(seq);
        return;
    }

    /**
     * Adds a Substring component of initial, any or final substring matching.
     *
     * <p>This method can be invoked only if startSubString was the last filter-
     * building method called.  A substring is not required to have an 'INITIAL'
     * substring.  However, when a filter contains an 'INITIAL' substring only
     * one can be added, and it must be the first substring added. Any number of
     * 'ANY' substrings can be added. A substring is not required to have a
     * 'FINAL' substrings either.  However, when a filter does contain a 'FINAL'
     * substring only one can be added, and it must be the last substring added.
     * </p>
     * @param type Substring type: RfcFilter.[INITIAL | ANY | FINAL]
     * @param value Value to use for matching
     * @throws LDAPLocalException   Occurs if this method is called out of
     * sequence or the type added is out of sequence.
     */
    public void addSubstring(int type, byte[] value)
            throws LDAPLocalException
    {
        try {
            ASN1SequenceOf substringSeq = (ASN1SequenceOf)filterStack.peek();
            if (type != RfcFilter.INITIAL && type != RfcFilter.ANY &&
                    type != RfcFilter.FINAL){
                throw new LDAPLocalException("Attempt to add an invalid " +
                        "substring type", LDAPException.FILTER_ERROR);
            }

            if (type == RfcFilter.INITIAL && substringSeq.size() !=0)
            {
                throw new LDAPLocalException("Attempt to add an initial " +
                        "substring match after the first substring",
                        LDAPException.FILTER_ERROR);
            }
            if (finalFound){
                throw new LDAPLocalException("Attempt to add a substring " +
                        "match after a final substring match",
                        LDAPException.FILTER_ERROR);
            }
            if (type == RfcFilter.FINAL){
                finalFound = true;
            }
            substringSeq.add(
                new ASN1Tagged(
                    new ASN1Identifier(ASN1Identifier.CONTEXT, false, type),
                    new RfcLDAPString(value),
                    false));
        } catch (ClassCastException e){
            throw new LDAPLocalException("A call to addSubstring occured " +
                "without calling startSubstring", LDAPException.FILTER_ERROR);
        }
        return;
    }

    /**
     * Completes a SubString filter component.
     *
     * @throws LDAPLocalException Occurs when this is called out of sequence,
     * or the substrings filter is empty.
     */
    public void endSubstrings() throws LDAPLocalException
    {
        try {
            finalFound = false;
            ASN1SequenceOf substringSeq = (ASN1SequenceOf) filterStack.peek();
            if (substringSeq.size() == 0){
                throw new LDAPLocalException("Empty substring filter",
                        LDAPException.FILTER_ERROR);
            }
        } catch (ClassCastException e){
            throw new LDAPLocalException("Missmatched ending of substrings",
                        LDAPException.FILTER_ERROR);
        }
        filterStack.pop();
        return;
    }

    /**
     * Creates and adds an AttributeValueAssertion to the filter.
     *
     * @param rfcType Filter type: RfcFilter.[EQUALITY_MATCH | GREATER_OR_EQUAL
     *  | LESS_OR_EQUAL | APPROX_MATCH ]
     * @param attrName Name of the attribute to be asserted
     * @param value Value of the attribute to be asserted
     * @throws LDAPLocalException
     *  Occurs when the filter type is not a valid attribute assertion.
     */
    public void addAttributeValueAssertion(int rfcType,
                      String attrName, byte[] value) throws LDAPLocalException
    {
        if (!filterStack.empty() &&
             filterStack.peek() instanceof ASN1SequenceOf)
        { //If a sequenceof is on the stack then substring is left on the stack
            throw new LDAPLocalException(
                    "Cannot insert an attribute assertion in a substring",
                    LDAPException.FILTER_ERROR);
        }
        if ((rfcType != RfcFilter.EQUALITY_MATCH) &&
            (rfcType != RfcFilter.GREATER_OR_EQUAL) &&
            (rfcType != RfcFilter.LESS_OR_EQUAL) &&
            (rfcType != RfcFilter.APPROX_MATCH)) {
            throw new LDAPLocalException(
                    "Invalid filter type for AttributeValueAssertion",
                    LDAPException.FILTER_ERROR);
        }
        ASN1Object current = new ASN1Tagged(
                    new ASN1Identifier(ASN1Identifier.CONTEXT, true, rfcType),
                    new RfcAttributeValueAssertion(
                            new RfcAttributeDescription(attrName),
                            new RfcAssertionValue(value)),
                    false);
        addObject(current);
        return;
    }

    /**
     * Creates and adds a present matching to the filter.
     *
     * @param attrName Name of the attribute to check for presence.
     * @throws LDAPLocalException
     *      Occurs if addPresent is called out of sequence.
     */
    public void addPresent(String attrName) throws LDAPLocalException
    {
        ASN1Object current = new ASN1Tagged(
                new ASN1Identifier(ASN1Identifier.CONTEXT, false,
                        RfcFilter.PRESENT),
                new RfcAttributeDescription(attrName),
                false);
        addObject(current);
        return;
    }

    /**
     * Adds an extensible match to the filter.
     *
     * @param matchingRule
     *      OID or name of the matching rule to use for comparison
     * @param attrName  Name of the attribute to match.
     * @param value  Value of the attribute to match against.
     * @param useDNMatching Indicates whether DN matching should be used.
     * @throws LDAPLocalException
     *      Occurs when addExtensibleMatch is called out of sequence.
     */
    public void addExtensibleMatch(String matchingRule, String attrName,
                                   byte[] value, boolean useDNMatching)
            throws LDAPLocalException
    {
        ASN1Object current = new ASN1Tagged(
            new ASN1Identifier(ASN1Identifier.CONTEXT, true,
                    RfcFilter.EXTENSIBLE_MATCH),
            new RfcMatchingRuleAssertion(
                (matchingRule==null) ? null:new RfcMatchingRuleId(matchingRule),
                (attrName==null) ? null:new RfcAttributeDescription(attrName),
                new RfcAssertionValue(value),
                (useDNMatching == false) ? null :
                new ASN1Boolean(true)),
            false);
        addObject(current);
        return;
    }
}

/**
  * This class will tokenize the components of an RFC 2254 search filter.
  */
class FilterTokenizer
{

    //*************************************************************************
    // Private variables
    //*************************************************************************

    private String filter;    // The filter string to parse
    private String attr;      // Name of the attribute just parsed
    private int offset;       // Offset pointer into the filter string
    private int filterLength; // Length of the filter string to parse

    //*************************************************************************
    // Constructor
    //*************************************************************************

    /**
     * Constructs a FilterTokenizer for a filter.
     */
    public FilterTokenizer(String filter) {
        this.filter = filter;
        this.offset = 0;
        this.filterLength = filter.length();
        return;
    }

    //*************************************************************************
    // Tokenizer methods
    //*************************************************************************

    /**
     * Reads the current char and throws an Exception if it is not a left
     * parenthesis.
     */
    public final void getLeftParen()
            throws LDAPException
    {
        if(offset >= filterLength) {
            //"Unexpected end of filter",
            throw new LDAPLocalException(ExceptionMessages.UNEXPECTED_END,
                                    LDAPException.FILTER_ERROR);
        }
        if(filter.charAt(offset++) != '(') {
            //"Missing left paren",
            throw new LDAPLocalException(ExceptionMessages.EXPECTING_LEFT_PAREN,
                    new Object[] { new Character( filter.charAt(offset-=1)) },
                    LDAPException.FILTER_ERROR);
        }
        return;
    }

    /**
     * Reads the current char and throws an Exception if it is not a right
     * parenthesis.
     */
    public final void getRightParen()
            throws LDAPException
    {
        if(offset >= filterLength) {
            //"Unexpected end of filter",
            throw new LDAPLocalException(ExceptionMessages.UNEXPECTED_END,
                                    LDAPException.FILTER_ERROR);
        }
        if(filter.charAt(offset++) != ')') {
            //"Missing right paren",
            throw new LDAPLocalException(ExceptionMessages.EXPECTING_RIGHT_PAREN,
                    new Object[] { new Character(filter.charAt(offset-1)) },
                    LDAPException.FILTER_ERROR);
        }
        return;
    }

    /**
     * Reads either an operator, or an attribute, whichever is
     * next in the filter string.
     *
     * <p>Operators are &, |, or !.<p>
     *
     * <p>If the next component is an attribute, it is read and stored in the
     * attr field of this class which may be retrieved with getAttr()
     * and a -1 is returned. Otherwise, the int value of the operator read is
     * returned.</p>
     */
    public final int getOpOrAttr()
            throws LDAPException
    {
        int index;

        if(offset >= filterLength) {
            //"Unexpected end of filter",
            throw new LDAPLocalException(ExceptionMessages.UNEXPECTED_END,
                                    LDAPException.FILTER_ERROR);
        }
        int ret;
        int testChar = filter.charAt(offset);
        if(testChar == '&') {
            offset++;
            ret = RfcFilter.AND;
        }
        else if(testChar == '|') {
            offset++;
            ret = RfcFilter.OR;
        }
        else if(testChar == '!') {
            offset++;
            ret = RfcFilter.NOT;
        }
        else {
            if (filter.startsWith(":=", offset) == true) {
                throw new LDAPLocalException(
                    ExceptionMessages.NO_MATCHING_RULE,
                    LDAPException.FILTER_ERROR);
            }

            if (filter.startsWith("::=", offset) == true ||
                filter.startsWith(":::=", offset) == true ) {
                throw new LDAPLocalException(
                    ExceptionMessages.NO_DN_NOR_MATCHING_RULE,
                    LDAPException.FILTER_ERROR);
            }


            // get first component of 'item' (attr or :dn or :matchingrule)
            String delims = "=~<>()";
            StringBuffer sb = new StringBuffer();

            while(delims.indexOf(filter.charAt(offset)) == -1 &&
                  filter.startsWith(":=", offset) == false) {
                sb.append(filter.charAt(offset++));
            }

            attr = sb.toString().trim();

            // is there an attribute name specified in the filter ?
            if (attr.length() == 0 || attr.charAt(0) == ';') {
                throw new LDAPLocalException(
                    ExceptionMessages.NO_ATTRIBUTE_NAME,
                    LDAPException.FILTER_ERROR);
            }

            for (index=0; index<attr.length(); index++) {
                char atIndex = attr.charAt(index);
                if (!(Character.isLetterOrDigit(atIndex) ||
                      atIndex == '-' ||
                      atIndex == '.' ||
                      atIndex == ';' ||
                      atIndex == ':'    )) {
                
                    if( atIndex == '\\' ) {
                        throw new LDAPLocalException(
                            ExceptionMessages.INVALID_ESC_IN_DESCR,
                            LDAPException.FILTER_ERROR);
                    } else {
                        throw new LDAPLocalException(
                            ExceptionMessages.INVALID_CHAR_IN_DESCR,
                            new Object[] {new Character(atIndex)},
                            LDAPException.FILTER_ERROR);
                    }            
                }
            }

            // is there an option specified in the filter ?
            index = attr.indexOf(';');
            if (index!=-1 && index==attr.length()-1) {
                throw new LDAPLocalException(
                    ExceptionMessages.NO_OPTION,
                    LDAPException.FILTER_ERROR);
            }
            ret = -1;
        }
        return ret;
    }

    /**
     * Reads an RFC 2251 filter type from the filter string and returns its
     * int value.
     */
    public final int getFilterType()
            throws LDAPException
    {
        if(offset >= filterLength) {
            //"Unexpected end of filter",
            throw new LDAPLocalException(ExceptionMessages.UNEXPECTED_END,
                                    LDAPException.FILTER_ERROR);
        }
        int ret;
        if(filter.startsWith(">=", offset)) {
            offset+=2;
            ret = RfcFilter.GREATER_OR_EQUAL;
        } else
        if(filter.startsWith("<=", offset)) {
            offset+=2;
            ret = RfcFilter.LESS_OR_EQUAL;
        } else
        if(filter.startsWith("~=", offset)) {
            offset+=2;
            ret = RfcFilter.APPROX_MATCH;
        } else
        if(filter.startsWith(":=", offset)) {
            offset+=2;
            ret = RfcFilter.EXTENSIBLE_MATCH;
        } else
        if(filter.charAt(offset) == '=') {
            offset++;
            ret = RfcFilter.EQUALITY_MATCH;
        } else {
            //"Invalid comparison operator",
            throw new LDAPLocalException(
                ExceptionMessages.INVALID_FILTER_COMPARISON,
                LDAPException.FILTER_ERROR);
        }
        return ret;
    }

    /**
     * Reads a value from a filter string.
     */
    public final String getValue()
            throws LDAPException
    {
        if(offset >= filterLength) {
            //"Unexpected end of filter",
            throw new LDAPLocalException(ExceptionMessages.UNEXPECTED_END,
                                    LDAPException.FILTER_ERROR);
        }

        int idx = filter.indexOf( ')', offset);
        if( idx == -1) {
            idx = filterLength;
        } 
        String ret = filter.substring( offset, idx);
        offset = idx;
        
        return ret;
    }

    /**
     * Returns the current attribute identifier.
     */
    public final String getAttr()
    {
        return attr;
    }

    /**
     * Return the current char without advancing the offset pointer. This is
     * used by ParseFilterList when determining if there are any more
     * Filters in the list.
     */
    public final char peekChar()
            throws LDAPException
    {
        if(offset >= filterLength) {
            //"Unexpected end of filter",
            throw new LDAPLocalException(ExceptionMessages.UNEXPECTED_END,
                                    LDAPException.FILTER_ERROR);
        }
        return filter.charAt(offset);
    }

}
