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

import com.novell.ldap.asn1.*;

/**
 * Represents an LDAP Attribute Value Assertion.
 *
 *<pre>
 *       AttributeValueAssertion ::= SEQUENCE {
 *               attributeDesc   AttributeDescription,
 *               assertionValue  AssertionValue }
 *</pre>
 */
public class RfcAttributeValueAssertion extends ASN1Sequence {

    /**
     * Creates an Attribute Value Assertion.
     *
     * @parameter ad The assertion description
     * 
     * @parameter av The assertion value
     */
    public RfcAttributeValueAssertion(RfcAttributeDescription ad, RfcAssertionValue av)
    {
        super(2);
        add(ad);
        add(av);
    }
    
    /**
     * Returns the attribute description.
     *
     * @return the attribute description
     */
    public String getAttributeDescription()
    {
        return ((RfcAttributeDescription)get(0)).stringValue();
    }
    
    /**
     * Returns the assertion value.
     *
     * @return the assertion value.
     */
    public byte[] getAssertionValue()
    {
        return ((RfcAssertionValue)get(1)).byteValue();
    }
}
