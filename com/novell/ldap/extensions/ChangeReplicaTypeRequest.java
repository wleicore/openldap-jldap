/* **************************************************************************
 * $Id: ChangeReplicaTypeRequest.java,v 1.1 2000/07/28 20:24:07 javed Exp $
 *
 * Copyright (C) 1999, 2000 Novell, Inc. All Rights Reserved.
 * 
 * THIS WORK IS SUBJECT TO U.S. AND INTERNATIONAL COPYRIGHT LAWS AND
 * TREATIES. USE, MODIFICATION, AND REDISTRIBUTION OF THIS WORK IS SUBJECT
 * TO VERSION 2.0.1 OF THE OPENLDAP PUBLIC LICENSE, A COPY OF WHICH IS
 * AVAILABLE AT HTTP://WWW.OPENLDAP.ORG/LICENSE.HTML OR IN THE FILE "LICENSE"
 * IN THE TOP-LEVEL DIRECTORY OF THE DISTRIBUTION. ANY USE OR EXPLOITATION
 * OF THIS WORK OTHER THAN AS AUTHORIZED IN VERSION 2.0.1 OF THE OPENLDAP
 * PUBLIC LICENSE, OR OTHER PRIOR WRITTEN CONSENT FROM NOVELL, COULD SUBJECT
 * THE PERPETRATOR TO CRIMINAL AND CIVIL LIABILITY. 
 ***************************************************************************/
package com.novell.ldap.ext; 

import com.novell.ldap.*;
import com.novell.ldap.client.protocol.lber.*;
import java.io.IOException;
 
/**
 *
 *      This class inherits from the LDAPExtendedOperation class
 *  and is used to change the type of the replica that resides 
 *  on the specified directory server.  Create an instance of this 
 *  class and then call the extendedOperation method with this
 *  object as the required LDAPExtendedOperation parameter.<br>
 *
 *  The OID used for this extended operation is:
 *      "2.16.840.1.113719.1.27.100.15"<br>
 *
 *  The RequestValue has the folling ASN:<br><br>
 *
 *  requestValue ::=<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;       flags       INTEGER<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;       replicaType INTEGER<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;       serverName  LDAPDN<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;       dn          LDAPDN<br>
 */
public class ChangeReplicaTypeRequest extends LDAPExtendedOperation {
   
/**
 *
 *      The constructor takes four parameters:<br><br>
 *
 *      String dn:          Specify the distinguished name of the replica's
 *                          partition root<br><br>
 *
 *      String serverDN:    Points to the server on which the
 *							interesting replica resides<br><br>
 *      
 *      int replicaType:    The new replica type. The replica types are defined 
 *							in the NamingContextConstants class<br><br>
 *
 *      int flags:          Specifies if all servers in the replica ring 
 *                          must be up before proceeding.  Set to 
 *                          LDAP_ENSURE_SERVERS_UP field defined in the 
 *                          NamingContextConstants class .<br><br>
 */   
 public ChangeReplicaTypeRequest(String dn, String serverDN, int replicaType, int flags) 
                throws LDAPException {
        
        super(NamingContextConstants.CHANGE_REPLICA_TYPE_REQ, null);
        
        try {
            // ber encode the parameters and set the requestValue
            LberEncoder requestlber = new LberEncoder();
            
            if ( (dn == null) || (serverDN == null) )
                throw new LDAPException("Invalid parameter",
				                        LDAPException.PARAM_ERROR);
				                        
		    requestlber.encodeInt(flags);
            requestlber.encodeInt(replicaType);
            requestlber.encodeString(serverDN, true);            
            requestlber.encodeString(dn, true);
                    
            setValue(requestlber.getTrimmedBuf());
            
        }
		catch(IOException ioe) {
			throw new LDAPException("Encoding Error",
				                     LDAPException.ENCODING_ERROR);
		}
   }

}