/* **************************************************************************
 * $Id: ListReplicasResponse.java,v 1.3 2000/08/28 22:19:19 vtag Exp $
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
import com.novell.asn1.*;
import java.io.*;
 
/**
 *      This class is generated by the ExtendedResponseFactory class from
 *  an ExtendedResponse object which has the following OID 
 *  "2.16.840.1.113719.1.27.20.".
 *
 */
public class ListReplicasResponse implements ParsedExtendedResponse {
   
   // Identity returned by the server
   private String[] replicaList;
   
   /**
    *
    * The constructor parses the responseValue which has the following ASN<br><br>
    *  responseValue ::=<br>
    *  &nbsp;&nbsp;&nbsp;&nbsp;     replicaList    SEQUENCE OF OCTET STRINGS
    *
    */   
   public ListReplicasResponse (LDAPExtendedResponse r) 
         throws IOException {
        
        // parse the contents of the reply
        byte [] returnedValue = r.getValue();
        if (returnedValue == null)
            throw new IOException("No returned value");
        
        // Create a decoder object
        LBERDecoder decoder = new LBERDecoder();
        if (decoder == null)
            throw new IOException("Decoding error");
           
        // We should get back a sequence
        ASN1Sequence returnedSequence = (ASN1Sequence)decoder.decode(returnedValue);        
        if (returnedSequence == null)
            throw new IOException("Decoding error");
        
        // How many replicas were returned
        int len = returnedSequence.size();
        replicaList = new String[len];
        
        // Copy each one into our String array
      for(int i=0; i < len; i++) {
          // Get the next ASN1Octet String in the sequence
          ASN1OctetString asn1_nextReplica = (ASN1OctetString)returnedSequence.get(i);
          if (asn1_nextReplica == null)
                throw new IOException("Decoding error");
            
            // Convert to a string
         replicaList[i] = new String(asn1_nextReplica.getContent());
         if (replicaList[i] == null)
                throw new IOException("Decoding error");
      }

   }
   
   /** 
    * @return String value specifying the identity returned by the server
    */
   public String[] getReplicaList() {
        return replicaList;
   }
    
}
