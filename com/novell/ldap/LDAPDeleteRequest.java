/* Generated by Together */

package com.novell.ldap.message;

import com.novell.ldap.*;
import com.novell.ldap.rfc2251.*;

/* 
 *       DelRequest ::= [APPLICATION 10] LDAPDN
 */
public class LDAPDeleteRequest extends LDAPMessage
{
    private String dn;
    
    /**
     */
    public LDAPDeleteRequest( String dn,
                              LDAPConstraints cons)
        throws LDAPException
    {
        super( LDAPMessage.DEL_REQUEST,
               new RfcDelRequest(dn),
               (cons != null) ? cons.getControls() : null);
        this.dn = dn;
        return;
    }
}
