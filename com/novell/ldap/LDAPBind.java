/* **************************************************************************
 * $Novell: /ldap/src/jldap/com/novell/ldap/LDAPBind.java,v 1.5 2000/08/28 22:18:54 vtag Exp $
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
 
package com.novell.ldap;

/*
* 4.4 public interface LDAPBind
*/
 
/**
 *
 *  Used to do explicit bind processing on a referral. 
 *
 *  <p>This interface allows a programmer to override the default
 *  authentication and reauthentication behavior when automatically
 *  following referrals. It is typically used to control the
 *  authentication mechanism used on automatic referral following.</p>
 *
 *  <p>A client can specify an instance of this class to be used
 *  on a single operation (through the LDAPConstraints object) 
 *  or for all operations (through the LDAPContraints object
 *  associated with the connection).
 *  
 */
public interface LDAPBind extends LDAPReferralHandler
{

   /*
    * 4.4.1 bind
    */

   /**
    * Called by LDAPConnection when a referral is received.
    *
    * <p>This method has the responsibility to bind to one of the
    * hosts in the list specified by the ldaprul parameter which corresponds
    * exactly to the list of hosts returned in a single referral response.
    * An implementation may access the host, port, credentials, and other
    * information in the original LDAPConnection object to decide on an
    * appropriate authentication mechanism, and/or interact with a user or
    * external module. The object implementing LDAPBind creates a new
    * LDAPConnection object to perform its connect and bind calls.  It
    * returns the new connection when both the connect and bind operations
    * succeed on one host from the list.  The LDAPConnection object referral
    * following code uses the new LDAPConnection object when it resends the
    * search request, updated with the new search base and possible search
    * filter. An LDAPException is thrown on failure, as in the
    * LDAPConnection.bind method. </p>
    *
    * @param ldapurl The list of servers contained in a referral response.
    * @param conn    An established connection to an LDAP server.
    *
    * @return       An established connection to one of the ldap servers
    *               in the referral list.
    *
    * @exception  LDAPException A general exception which includes an error
    * message and an LDAP error code.
    */
   public LDAPConnection bind (String[] ldapurl, LDAPConnection conn)
            throws LDAPException;
}
