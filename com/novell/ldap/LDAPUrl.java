/* **************************************************************************
* $Novell: /ldap/src/jldap/com/novell/ldap/LDAPUrl.java,v 1.11 2000/09/26 18:17:33 vtag Exp $
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

import java.util.*;
import java.net.*;
import com.novell.ldap.client.Debug;

/*
* 4.38 public class LDAPUrl
*/

/**
*
*  Encapsulates parameters of an LDAP URL query.  
*
*  An LDAPUrl object can be passed to LDAPConnection.search to retrieve
*  search results.
*/
public class LDAPUrl {

    // Broken out parts of the URL
    static private boolean    enclosed = false;             // URL is enclosed by < & >
    static private boolean    secure = false;               // URL scheme ldap/ldaps
    static private boolean    ipV6 = false;                 // TCP/IP V6
    static private String     host = null;                  // Host
    static private int        port;                         // Port
    static private String     dn = "";                      // Base DN
    static private String[]   attrs = null;                 // Attributes
    static private String     filter = "(objectClass=*)";   // Filter
    static private int        scope  = LDAPv2.SCOPE_BASE;   // Scope
    static private String[]   extensions = null;            // Extensions

    /*
    * 4.38.1 Constructors
    */

    /**
    * Constructs a URL object with the specified string as the URL.
    *
    * @param url       An explicit LDAP URL string, for example
    *                  "ldap://ldap.acme.com:80/o=Ace%20Industry,c=us?cn
    *                  ,sn?sub?(objectclass=inetOrgPerson)".
    *
    * @exception MalformedURLException The specified URL cannot be parsed.
    */
    public LDAPUrl(String url) throws MalformedURLException {
        parseURL( url );
        return;
    }


    /**
    * Constructs a URL object with the specified host, port, and DN. 
    *
    * <p>This form is used to create URL references to a particular object 
    * in the directory.</p>
    *
    *  @param host     The host name of LDAP server, or null for "nearest
    *                  X.500/LDAP".
    *<br><br>
    *  @param port     The port number for LDAP server (use
    *                  LDAPConnection.DEFAULT_PORT for default port).
    *<br><br>
    *  @param dn      The distinguished name of the object to fetch.
    *
    */
    public LDAPUrl(String host,
                   int port,
                   String dn) {
		this.host = host;
		this.port = port;
		this.dn = dn;
		return;
    }

    /**
    * Constructs a full-blown LDAP URL to specify an LDAP search operation.
    *
    *
    *  @param host     The host name of LDAP server, or null for "nearest
    *                  X.500/LDAP".
    *<br><br>
    *  @param port     The port number for LDAP server (use
    *                  LDAPConnection.DEFAULT_PORT for default port).
    *<br><br>
    *  @param dn       The distinguished name of object to fetch.
    *<br><br>
    *  attrNames       The names of attributes to retrieve (use null for all
    *                  attributes).
    *<br><br>
    *  @param scope    The depth of search and uses one of the following 
    *                  from LDAPv2: SCOPE_BASE, SCOPE_ONE, SCOPE_SUB.
    *<br><br>
    *  @param filter   The search filter specifying the search criteria.
    */
    public LDAPUrl(String host,
                   int port,
                   String dn,
                   String attrNames[],
                   int scope,
                   String filter) {
		this.host = host;
		this.port = port;
		this.dn = dn;
		this.attrs = attrNames;
		this.scope = scope;
		this.filter = filter;
		return;
    }

    /*
    * 4.38.2 decode
    */

    /**
    * Decodes a URL-encoded string. 
    *
    * <p>Any occurences of %HH are decoded to the hex value represented. 
    * However, this method does NOT decode "+" into " ".
    * 
    *  @param URLEncoded     String to decode.
    *
    *  @return The decoded string.
    *
    *  @exception MalformedURLException The URL could not be parsed.
    */
    public static String decode(String URLEncoded) throws
    MalformedURLException {

        if( Debug.LDAP_DEBUG)
            Debug.trace( Debug.urlParse, "decode(" + URLEncoded + ")");

		int searchStart = 0;
		int fieldStart;

        fieldStart = URLEncoded.indexOf("%", searchStart);                
		// Return now if no encoded data
		if( fieldStart < 0 ) {
			return URLEncoded;
		}

		// Decode the %HH value and copy to new string buffer
		int fieldEnd = 0;	// end of previous field
		int value;
		int dataLen = URLEncoded.length();

		StringBuffer decoded = new StringBuffer( dataLen );

		while( true ) {
			if( fieldStart > (dataLen-3) ) {
	            throw new MalformedURLException(
	            "LDAPUrl.decode: must be two hex characters following escape character '%'");
			}
			if( fieldStart < 0 )
				fieldStart = dataLen;
			// Copy to string buffer from end of last field to start of next
			decoded.append( URLEncoded.substring(fieldEnd, fieldStart) );
			fieldStart += 1;
			if( fieldStart >= dataLen)
				break;
			fieldEnd = fieldStart + 2;
			try {
				decoded.append(
					(char)Integer.parseInt(
						URLEncoded.substring( fieldStart, fieldEnd ), 16) );
			} catch ( NumberFormatException ex ) {
	            throw new MalformedURLException(
		            "LDAPUrl.decode: error converting hex characters to integer \""
	            	+ ex.getMessage() + "\"");
			}
			searchStart = fieldEnd;
			if( searchStart == dataLen )
				break;
	        fieldStart = URLEncoded.indexOf("%", searchStart);                
		}

        if( Debug.LDAP_DEBUG)
            Debug.trace( Debug.urlParse, "decode returns(" + decoded + ")");
		return( decoded.toString() );
    }

    /*
    * 4.38.3 encode
    */

    /**
    * Encodes an arbitrary string using the URL encoding rules. 
    *
    * <p> Any illegal characters are encoded as %HH.  
    * However, this method does NOT encode " " into "+".</p>
    *
    *
    *  @param toEncode     The string to encode.
    *
    * @return The URL-encoded string.
    */
    public static String encode(String toEncode) {
        throw new RuntimeException("LDAPUrl: encode() not implemented");
    }

    /*
    * 4.38.4 getAttributeArray
    */

    /**
    * Returns an array of attribute names specified in the URL.
    *
    * @return An array of attribute names in the URL.
    */
    public String[] getAttributeArray() {
		return attrs;
    }

    /*
    * 4.38.5 getAttributes
    */

    /**
    * Returns an enumerator for the attribute names specified in the URL.
    *
    * @return An enumeration of attribute names.
    */
    public Enumeration getAttributes() {
        return new AttributeEnumeration( attrs );
    }

    /*
    * 4.38.6 getDN
    */

    /**
    * Returns the distinguished name encapsulated in the URL.
    *
    * @return The base distinguished name specified in the URL.
    */
    public String getDN() {
		return dn;
    }

    /*
    * 4.38.8 getFilter
    */

    /**
    * Returns the search filter or the default filter 
    * (objectclass=*) if none was specified.
    *
    * @return The search filter.
    */
    public String getFilter() {
		return filter;
    }

    /*
    * 4.38.9 getHost
    */

    /**
    * Returns the name of the LDAP server in the URL.
    *
    * @return The host name specified in the URL.
    */
    public String getHost() {
		return host;
    }

    /*
    * 4.38.10 getPort
    */

    /**
    * Returns the port number of the LDAP server in the URL.
    *
    * @return The port number in the URL.
    */
    public int getPort()
    {
		return port;
    }

    /*
    * 4.38.12 getUrl
    */

    /**
    * Returns a valid string representation of this LDAP URL.
    *
    * @return The string representation of the LDAP URL.
    */
    public String getUrl()
    {
		StringBuffer url = new StringBuffer( 256 );
		if( enclosed ) {
			url.append( "<" );
		}
		if( secure ) {
			url.append( "ldaps://" );
		} else {
			url.append( "ldap://" );
		}
		if( ipV6 ) {
			url.append( "[" );
		}
		url.append( host + ":" + port + "/");

        throw new RuntimeException("LDAPUrl: getUrl() not implemented");
    }

    private String[] parseList( String listStr,    // input String
                                char delimiter,    // list item delimiter
                                int listStart,     // start of list
                                int listEnd)       // end of list + 1
    {
        String[] list;
        if( Debug.LDAP_DEBUG)
            Debug.trace( Debug.urlParse, "parseList(" + listStr.substring(listStart,listEnd) + ")");
        // Check for and empty string
        if( (listEnd - listStart) <= 2) {
            return null;
        }
        // First count how many items are specified
        int itemStart = listStart;
        int itemEnd;
        int itemCount = 0;
        while( itemStart > 0 ) {
            // itemStart == 0 if no delimiter found
            itemCount += 1;
            itemEnd = listStr.indexOf(delimiter, itemStart);                
            if( (itemEnd > 0) && (itemEnd < listEnd) ) {
                itemStart = itemEnd + 1;
            } else {
                break;
            }
        }
        // Now fill in the array with the attributes
        itemStart = listStart;
        list = new String[itemCount];
        itemCount = 0;
        while( itemStart > 0 ) {
            itemEnd = listStr.indexOf(delimiter, itemStart);                
            if( itemStart <= listEnd ) {
                if (itemEnd < 0 )
                    itemEnd = listEnd;
                if( itemEnd > listEnd )
                    itemEnd = listEnd;
                list[itemCount] = listStr.substring( itemStart, itemEnd);
                itemStart = itemEnd + 1;
                itemCount += 1;
            } else {
                break;
            }
        }
        return list;
    }


    private void parseURL( String url) throws MalformedURLException
    {
        int scanStart = 0;
        int scanEnd = url.length();

        if( Debug.LDAP_DEBUG)
            Debug.trace(  Debug.urlParse, "parseURL(" + url + ")");
        if( url == null)
            throw new MalformedURLException("LDAPUrl: URL cannot be null");

        // Check if URL is enclosed by < & >
        if( url.charAt(scanStart) == '<') {
            if( url.charAt(scanEnd - 1) != '>')
                throw new MalformedURLException("LDAPUrl: URL bad enclosure");
            enclosed = true;
            scanStart += 1;
            scanEnd -= 1;
            if( Debug.LDAP_DEBUG)
                Debug.trace(  Debug.urlParse, "LDAPUrl: parseURL: Url is enclosed");
        }

        // Determine the URL scheme and set appropriate default port
        if( url.substring(scanStart, scanStart + 4).equalsIgnoreCase( "URL:")) {
            scanStart += 4;        
        }
        if( url.substring(scanStart, scanStart + 7).equalsIgnoreCase( "ldap://")) {
            scanStart += 7;
            port = LDAPConnection.DEFAULT_PORT;
        } else
        if( url.substring(scanStart, scanStart + 8).equalsIgnoreCase( "ldaps://")) {
            secure = true;
            scanStart += 8;
            port = LDAPConnection.DEFAULT_SSL_PORT;
        } else {
            throw new MalformedURLException("LDAPUrl: URL scheme is not ldap");
        }
        if( Debug.LDAP_DEBUG)
            Debug.trace(  Debug.urlParse, "parseURL: scheme is " + (secure?"ldaps":"ldap"));

        // Find where host:port ends and dn begins
        int dnStart = url.indexOf("/", scanStart);
        int hostPortEnd = scanEnd;
		boolean novell = false;
        if( dnStart < 0) {
            /*
             * Kludge. check for ldap://111.222.333.444:389??cn=abc,o=company
             *
             * Check for broken Novell referral format.  The dn is in
             * the scope position, but the required slash is missing.
             * This is illegal syntax but we need to account for it.
             * Fortunately it can't be confused with anything real.
             */
            dnStart = url.indexOf("?", scanStart);
            if( dnStart > 0) {
                if( url.charAt( dnStart+1) == '?') {
                    hostPortEnd = dnStart;
                    dnStart += 1;
					novell = true;
                    if( Debug.LDAP_DEBUG)
                        Debug.trace(  Debug.urlParse, "parseURL: wierd novell syntax found");
                } else {
                    dnStart = -1;
                }
            }
        } else {
            hostPortEnd = dnStart;
        }
        // Check for IPV6 "[ipaddress]:port"
        int portStart;
        int hostEnd = hostPortEnd;
        if( url.charAt(scanStart) == '[') {
            hostEnd = url.indexOf(']', scanStart + 1);
            if( (hostEnd >= hostPortEnd) || (hostEnd == -1)) {
                throw new MalformedURLException("LDAPUrl: \"]\" is missing on IPV6 host name");
            }
            // Get host w/o the [ & ]
            host = url.substring( scanStart +1, hostEnd);
            portStart = url.indexOf(":", hostEnd);
            if( (portStart < hostPortEnd) && (portStart != -1)) {
                // port is specified
                port = Integer.decode( url.substring(portStart+1, hostPortEnd) ).intValue();
                if( Debug.LDAP_DEBUG)
                    Debug.trace(  Debug.urlParse, "parseURL: IPV6 host " + host + " port " + port);
            } else {
                if( Debug.LDAP_DEBUG)
                    Debug.trace(  Debug.urlParse, "parseURL: IPV6 host " + host + " default port " + port);
            }
        } else {
            portStart = url.indexOf(":", scanStart);
            // Isolate the host and port
            if( (portStart < 0) || (portStart > hostPortEnd)) {
                // no port is specified, we keep the default
                host = url.substring(scanStart, hostPortEnd);
                if( Debug.LDAP_DEBUG)
                    Debug.trace(  Debug.urlParse, "parseURL: host " + host + " default port " + port);
            } else {
                // port specified in URL
                host = url.substring(scanStart, portStart);
                port = Integer.decode( url.substring(portStart+1, hostPortEnd) ).intValue();
                if( Debug.LDAP_DEBUG)
                    Debug.trace(  Debug.urlParse, "parseURL: host " + host + " port " + port);
            }
        }

        scanStart = hostPortEnd + 1;
        if( (scanStart >= scanEnd) || (dnStart < 0) )
            return;

        // Parse out the base dn
        scanStart = dnStart + 1;                    

        int attrsStart = url.indexOf('?', scanStart);
        if( attrsStart < 0 ) {
            dn = url.substring( scanStart, scanEnd);
        } else {
            dn = url.substring( scanStart, attrsStart);
        }

        if( Debug.LDAP_DEBUG)
            Debug.trace(  Debug.urlParse, "parseURL: dn " + dn);
        scanStart = attrsStart + 1;                    
		// Wierd novell syntax can have nothing beyond the dn
        if( (scanStart >= scanEnd) || (attrsStart < 0) || novell )
            return;

        // Parse out the attributes
        int scopeStart = url.indexOf('?', scanStart);
        if( scopeStart < 0)
            scopeStart = scanEnd - 1;
        attrs = parseList( url, ',', attrsStart + 1, scopeStart);
        if( Debug.LDAP_DEBUG) {
            if( attrs != null) {
                Debug.trace(  Debug.urlParse, "parseURL: " + attrs.length + " attributes" );
                for( int i = 0; i < attrs.length; i++) {
                    Debug.trace(  Debug.urlParse, "\t" + attrs[i] );
                }
            } else {
                Debug.trace(  Debug.urlParse, "parseURL: no attributes");
            }
        }

        scanStart = scopeStart + 1;                    
        if( scanStart >= scanEnd)
            return;

        // Parse out the scope
        int filterStart = url.indexOf('?',scanStart);
        String scopeStr;
        if( filterStart < 0 ) {
             scopeStr = url.substring( scanStart, scanEnd);
        } else {
             scopeStr = url.substring( scanStart, filterStart);
        }
        if( scopeStr.equalsIgnoreCase("")) {
            scope = LDAPv2.SCOPE_BASE;
            if( Debug.LDAP_DEBUG)
                scopeStr = "sub";
        } else
        if( scopeStr.equalsIgnoreCase("base")) {
            scope = LDAPv2.SCOPE_BASE;
        } else
        if( scopeStr.equalsIgnoreCase("one")) {
            scope = LDAPv2.SCOPE_ONE;
        } else
        if( scopeStr.equalsIgnoreCase("sub")) {
            scope = LDAPv2.SCOPE_SUB;
        } else {
            throw new MalformedURLException("LDAPUrl: URL invalid scope");
        }

        if( Debug.LDAP_DEBUG)
            Debug.trace(  Debug.urlParse, "parseURL: scope(" + scope + ") " + scopeStr);

        scanStart = filterStart + 1;
        if( (scanStart >= scanEnd) || (filterStart < 0) )
            return;

        // Parse out the filter
        scanStart = filterStart + 1;                    

        String filterStr;
        int extStart = url.indexOf('?', scanStart);
        if( extStart < 0 ) {
            filterStr = url.substring( scanStart, scanEnd);
        } else {
            filterStr = url.substring( scanStart, extStart);
        }

        if( ! filterStr.equals("") ) {
            filter = filterStr;    // Only modify if not the default filter
        }
        if( Debug.LDAP_DEBUG)
            Debug.trace(  Debug.urlParse, "parseURL: filter " + filter);

        scanStart = extStart + 1;                    
        if( (scanStart >= scanEnd) || (extStart < 0) )
            return;
        
        // Parse out the extensions
        int end = url.indexOf('?', scanStart);
        if( end > 0)
            throw new MalformedURLException("LDAPUrl: URL has too many ? fields");
        extensions = parseList( url, ',', scanStart, scanEnd);
        if( Debug.LDAP_DEBUG) {
            if( extensions != null) {
                Debug.trace(  Debug.urlParse, "parseURL: " + extensions.length + " extensions" );
                for( int i = 0; i < extensions.length; i++) {
                    Debug.trace(  Debug.urlParse, "\t" + extensions[i] );
                }
            } else {
                Debug.trace(  Debug.urlParse, "parseURL: no extensions");
            }
        }

        return;
    }
}
