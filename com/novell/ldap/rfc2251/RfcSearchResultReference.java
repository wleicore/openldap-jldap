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

import java.io.IOException;
import java.io.InputStream;
import com.novell.ldap.*;
import com.novell.ldap.asn1.*;

/**
 * Represents an LDAP Search Result Reference.
 *
 *<pre>
 *       SearchResultReference ::= [APPLICATION 19] SEQUENCE OF LDAPURL
 *</pre>
 */
public class RfcSearchResultReference extends ASN1SequenceOf
{

    //*************************************************************************
    // Constructors for SearchResultReference
    //*************************************************************************

    /**
     * The only time a client will create a SearchResultReference is when it is
     * decoding it from an InputStream
     */
    public RfcSearchResultReference(ASN1Decoder dec, InputStream in, int len)
       throws IOException
    {
        super(dec, in, len);
        return;
    }

    //*************************************************************************
    // Accessors
    //*************************************************************************

    /**
     * Override getIdentifier to return an application-wide id.
     */
    public final ASN1Identifier getIdentifier()
    {
        return new ASN1Identifier(ASN1Identifier.APPLICATION, true,
                                LDAPMessage.SEARCH_RESULT_REFERENCE);
    }
}
