/* **************************************************************************
 * $Novell: /ldap/src/jldap/com/novell/ldap/util/DN.java,v 1.2 2001/03/09 23:18:56 cmorris Exp $
 *
 * Copyright (C) 1999, 2000, 2001 Novell, Inc. All Rights Reserved.
 *
 * THIS WORK IS SUBJECT TO U.S. AND INTERNATIONAL COPYRIGHT LAWS AND
 * TREATIES. USE, MODIFICATION, AND REDISTRIBUTION OF THIS WORK IS SUBJECT
 * TO VERSION 2.0.7 OF THE OPENLDAP PUBLIC LICENSE, A COPY OF WHICH IS
 * AVAILABLE AT HTTP://WWW.OPENLDAP.ORG/LICENSE.HTML OR IN THE FILE "LICENSE"
 * IN THE TOP-LEVEL DIRECTORY OF THE DISTRIBUTION. ANY USE OR EXPLOITATION
 * OF THIS WORK OTHER THAN AS AUTHORIZED IN VERSION 2.0.7 OF THE OPENLDAP
 * PUBLIC LICENSE, OR OTHER PRIOR WRITTEN CONSENT FROM NOVELL, COULD SUBJECT
 * THE PERPETRATOR TO CRIMINAL AND CIVIL LIABILITY.
 ******************************************************************************/
package com.novell.ldap.util;
import java.util.Vector;

/**
 * <P>A DN encapsulates an ldap name with multiple object names(a Distinguished
 * name. It provides methods to get information about the DN and to manipulate
 * the DN.  Multivalued attributes are all considered to be one component and
 * are represented in one RDN (see RDN)</P>
 *
 * <P> The following are examples of valid DN:
 * <ul>
 *     <li>cn=admin,ou=marketing,o=corporation</li>
 *     <li>cn=admin,ou=marketing</li>
 *     <li>oid.2.5.4.3=#616460696E,ou=marketing</li>
 *     <!-- a=97=x61, d=100=x64, m=109=x60, i=105=x69, n=110=x6E -->
 *     <li>2.5.4.3=admin,ou=marketing</li>
 * </ul>
 *
 * <P>Note: While a DN generally means a name that extends to the Root, The
 * Components encapsulated in this class do not neccesarily need to extend to
 * to the Root. Likewise the RDN class only contains one single component
 * </P>
 *
 * @see RDN
 */

public class DN extends Object
{

    //parser state identifiers.
    private static final int LOOK_FOR_RDN_ATTR_TYPE = 1;
    private static final int ALPHA_ATTR_TYPE        = 2;
    private static final int OID_ATTR_TYPE          = 3;
    private static final int LOOK_FOR_RDN_VALUE     = 4;
    private static final int QUOTED_RDN_VALUE       = 5;
    private static final int HEX_RDN_VALUE          = 6;
    private static final int UNQUOTED_RDN_VALUE     = 7;

    private Vector rdnList = new Vector();

    /**
     * Constructs a new DN based on the specified string representation of a
     * distinguished name. The syntax of the DN must conform to that specified
     * in RFC 2253.
     *
     * @param      dnString a string representation of the distinguished name
     * @exception  IllegalArgumentException  if the the value of the dnString
     *               parameter does not adhere to the syntax described in
     *               RFC 2253
     */
   public DN(String dnString){
      char     currChar;
      char     nextChar;
      int      currIndex;
      char[]   tokenBuf = new char[dnString.length()];
      int      tokenIndex;
      int      lastIndex;
      int      valueStart;
      boolean  escapedChar = false;
      int      state;
      int      trailingSpaceCount = 0;
      String   attrType = "";
      String   attrValue = "";
      String   rawValue = "";
      int      hexDigitCount = 0;
      RDN      currRDN = new RDN();

      tokenIndex = 0;
      currIndex = 0;
      valueStart = 0;
      state = LOOK_FOR_RDN_ATTR_TYPE;
      lastIndex = dnString.length()-1;
      while (currIndex <= lastIndex)
      {
         currChar = dnString.charAt(currIndex);
         switch (state)
         {
         case LOOK_FOR_RDN_ATTR_TYPE: //parsing and RDName
            if (Character.isLetter(currChar))
            {
               if (dnString.startsWith("oid.", currIndex) ||
                   dnString.startsWith("OID.", currIndex))
               {  //form is "oid.###.##.###... or OID.###.##.###...
                  currIndex += 4; //skip oid. prefix and get to actual oid
                  if (currIndex > lastIndex)
                     throw new IllegalArgumentException(dnString);
                  currChar = dnString.charAt(currIndex);
                  if (Character.isDigit(currChar))
                  {
                     tokenBuf[tokenIndex++] = currChar;
                     state = OID_ATTR_TYPE;
                  }
                  else
                     throw new IllegalArgumentException(dnString);
               }
               else
               {
                  tokenBuf[tokenIndex++] = currChar;
                  state = ALPHA_ATTR_TYPE;
               }
            }
            else if (Character.isDigit(currChar))
            {
               tokenBuf[tokenIndex++] = currChar;
               state = OID_ATTR_TYPE;
            }
            else if (!Character.isSpaceChar(currChar))
               throw new IllegalArgumentException(dnString);
            break;

         case ALPHA_ATTR_TYPE:
            if (Character.isLetter(currChar) || Character.isDigit(currChar) ||
                    (currChar == '-'))
               tokenBuf[tokenIndex++] = currChar;
            else
            {
               //skip any white space
               while ((currChar == ' ') && (currIndex < lastIndex))
                  currChar = dnString.charAt(++currIndex);
               if (currChar == '=')
               {
                  attrType = new String(tokenBuf, 0, tokenIndex);
                  tokenIndex = 0;
                  state = LOOK_FOR_RDN_VALUE;
               }
               else
                  throw new IllegalArgumentException(dnString);
            }
            break;

         case OID_ATTR_TYPE:
            if (Character.isDigit(currChar) || (currChar == '.'))
               tokenBuf[tokenIndex++] = currChar;
            else
            {
               //skip any white space
               while (currChar == ' ')
                  currChar = dnString.charAt(++currIndex);
               if (currChar == '=')
               {
                  attrType = new String(tokenBuf, 0, tokenIndex);
                  tokenIndex = 0;
                  state = LOOK_FOR_RDN_VALUE;
               }
               else
                  throw new IllegalArgumentException(dnString);
            }
            break;

         case LOOK_FOR_RDN_VALUE:
            //skip any white space
            while (currChar == ' ')
            {
               if (currIndex < lastIndex)
                  currChar = dnString.charAt(++currIndex);
               else
                  throw new IllegalArgumentException(dnString);
            }
            if (currChar == '"')
            {
               state = QUOTED_RDN_VALUE;
               valueStart = currIndex;
            }
            else if (currChar == '#')
            {
               hexDigitCount = 0;
               tokenBuf[tokenIndex++] = currChar;
               valueStart = currIndex;
               state = HEX_RDN_VALUE;
            }
            else
            {
               valueStart = currIndex;
               //check this character again in the UNQUOTED_RDN_VALUE state
               currIndex--;
               state = UNQUOTED_RDN_VALUE;
            }
            break;

         case UNQUOTED_RDN_VALUE:
            if (currChar == '\\')
            {
               if (!(currIndex < lastIndex))
                  throw new IllegalArgumentException(dnString);
               currChar = dnString.charAt(++currIndex);
               if (isHexDigit(currChar))
               {
                  if (!(currIndex < lastIndex))
                     throw new IllegalArgumentException(dnString);
                  nextChar = dnString.charAt(++currIndex);
                  if (isHexDigit(nextChar))
                  {
                     tokenBuf[tokenIndex++] = hexToChar(currChar, nextChar);
                     trailingSpaceCount = 0;
                  }
                  else
                     throw new IllegalArgumentException(dnString);
               }
               else if (needsEscape(currChar))
               {
                  tokenBuf[tokenIndex++] = currChar;
                  trailingSpaceCount = 0;
               }
               else
                  throw new IllegalArgumentException(dnString);
            }
            else if (currChar == ' ')
            {
               trailingSpaceCount++;
               tokenBuf[tokenIndex++] = currChar;
            }
            else if ((currChar == ',') ||
                     (currChar == ';') ||
                     (currChar == '+'))
            {
               attrValue =
                new String(tokenBuf, 0, tokenIndex - trailingSpaceCount);
               rawValue =
                dnString.substring(valueStart, currIndex-trailingSpaceCount);

               //added by cameron
               currRDN.add(attrType, attrValue, rawValue);
               if (currChar != '+'){
                   rdnList.addElement(currRDN);
                   currRDN = new RDN();
               }

               /*taken out by cameron
               addRDN(attrType, attrValue, rawValue, levelID);
               if (currChar != '+')
                  levelID++;*/
               trailingSpaceCount = 0;
               tokenIndex = 0;
               state = LOOK_FOR_RDN_ATTR_TYPE;
            }
            else
            {
               trailingSpaceCount = 0;
               tokenBuf[tokenIndex++] = currChar;
            }
            break;

         case QUOTED_RDN_VALUE:
            if (currChar == '"')
            {
               rawValue = dnString.substring(valueStart, currIndex+1);
               if (currIndex < lastIndex)
                  currChar = dnString.charAt(++currIndex);
               //skip any white space
               while ((currChar == ' ') && (currIndex < lastIndex))
                  currChar = dnString.charAt(++currIndex);
               if ((currChar == ',') ||
                   (currChar == ';') ||
                   (currChar == '+') ||
                   (currIndex == lastIndex))
               {
                  attrValue = new String(tokenBuf, 0, tokenIndex);

                   //added by cameron
                   currRDN.add(attrType, attrValue, rawValue);
                   if (currChar != '+'){
                       rdnList.addElement(currRDN);
                       currRDN = new RDN();
                   }

 /*                 addRDN(attrType, attrValue, rawValue, levelID);
                  if (currChar != '+')
                     levelID++;*/
                  trailingSpaceCount = 0;
                  tokenIndex = 0;
                  state = LOOK_FOR_RDN_ATTR_TYPE;
               }
               else
                  throw new IllegalArgumentException(dnString);
            }
            else if (currChar == '\\')
            {
               currChar = dnString.charAt(++currIndex);
               if (isHexDigit(currChar))
               {
                  nextChar = dnString.charAt(++currIndex);
                  if (isHexDigit(nextChar))
                  {
                     tokenBuf[tokenIndex++] = hexToChar(currChar, nextChar);
                     trailingSpaceCount = 0;
                  }
                  else
                     throw new IllegalArgumentException(dnString);
               }
               else if (needsEscape(currChar))
               {
                  tokenBuf[tokenIndex++] = currChar;
                  trailingSpaceCount = 0;
               }
               else
                  throw new IllegalArgumentException(dnString);
            }
            else
               tokenBuf[tokenIndex++] = currChar;
            break;

         case HEX_RDN_VALUE:
            if ((!isHexDigit(currChar)) || (currIndex == lastIndex))
            {
               //check for odd number of hex digits
               if ((hexDigitCount%2) != 0)
                  throw new IllegalArgumentException(dnString);
               else
               {
                  rawValue = dnString.substring(valueStart, currIndex);
                  //skip any white space
                  while ((currChar == ' ') && (currIndex < lastIndex))
                     currChar = dnString.charAt(++currIndex);
                  if ((currChar == ',') ||
                      (currChar == ';') ||
                      (currChar == '+') ||
                      (currIndex == lastIndex))
                  {
                      attrValue = new String(tokenBuf, 0, tokenIndex);

                      //added by cameron
                      currRDN.add(attrType, attrValue, rawValue);
                      if (currChar != '+'){
                          rdnList.addElement(currRDN);
                          currRDN = new RDN();
                      }

                      /*addRDN(attrType, attrValue, rawValue, levelID);
                      if (currChar != '+')
                          levelID++;*/

                      tokenIndex = 0;
                      state = LOOK_FOR_RDN_ATTR_TYPE;
                  }
                  else
                  {
                     throw new IllegalArgumentException(dnString);
                  }

               }
            }
            else
            {
               tokenBuf[tokenIndex++] = currChar;
               hexDigitCount++;
            }
            break;
         }
         currIndex++;
      }

      //check ending state
      if (state == UNQUOTED_RDN_VALUE)
      {
         attrValue =
          new String(tokenBuf, 0, tokenIndex - trailingSpaceCount);
         rawValue =
          dnString.substring(valueStart, currIndex - trailingSpaceCount);
         currRDN.add(attrType,attrValue,rawValue);
         rdnList.addElement(currRDN);
         //addRDN(attrType, attrValue, rawValue, levelID);
      }
      else if (state != LOOK_FOR_RDN_ATTR_TYPE)
      {
         throw new IllegalArgumentException(dnString);
      }
   }



    /**
     * Checks a character to see if it is valid hex digit 0-9, a-f, or
     * A-F (ASCII value ranges 48-47, 65-70, 97-102).
     *
     * @param   ch the character to be tested.
     * @return  <code>true</code> if the character is a valid hex digit
     */

   public static boolean isHexDigit(char ch){
      if (((ch < 58) && (ch > 47)) || //ASCII 0-9
          ((ch < 71) && (ch > 64)) || //ASCII a-f
          ((ch < 103) && (ch > 96)))  //ASCII A-F
         return true;
      else
         return false;
   }

    /**
     * Checks a character to see if it ever needs to be escaped in the
     * string representation of a DN.
     *
     * @param   ch the character to be tested.
     * @return  <code>true</code> if the character needs to be escaped in at
     *            least some instances.
     */
   private boolean needsEscape( char ch) {
      if ((ch == ' ') || //space (only needs escape at end of value
          (ch == ',') ||
          (ch == ';') ||
          (ch == '=') ||
          (ch == '\\') ||
          (ch == '+') ||
          (ch == '<') ||
          (ch == '>') ||
          (ch == '#'))
         return true;
      else
         return false;
   }

    /**
     * Converts two valid hex digit characters that form the string
     * representation of an ascii character value to the actual ascii
     * character.
     *
     * @param   hex1 the hex digit for the high order byte.
     * @param   hex0 the hex digit for the low order byte.
     * @return  the character whose value is represented by the parameters.
     */

    public static char hexToChar(char hex1, char hex0)
        throws IllegalArgumentException {
      int result;

      if ((hex1 < 58) && (hex1 > 47)) //ASCII 0-9
         result = (hex1-48) * 16;
      else if ((hex1 < 71) && (hex1 > 64)) //ASCII a-f
         result = (hex1-55) * 16;
      else if ((hex1 < 103) && (hex1 > 96))  //ASCII A-F
         result = (hex1-87) * 16;
      else
         throw new IllegalArgumentException("Not hex digit");

      if ((hex0 < 58) && (hex0 > 47)) //ASCII 0-9
         result += (hex0-48);
      else if ((hex0 < 71) && (hex0 > 64)) //ASCII a-f
         result += (hex0-55);
      else if ((hex0 < 103) && (hex0 > 96))  //ASCII A-F
         result += (hex0-87);
      else
         throw new IllegalArgumentException("Not hex digit");

      return (char)result;
   }



    /**
     * Return a string representation of the DN
     *
     * @param   attrType the type of the RDN
     * @param   attrValue the value of the RDN
     * @param   level the level of the RDN
     */

    public String toString() {
        int length=rdnList.size();
        String dn = "";
        if (length < 1)
            return null;
        dn = ((RDN)rdnList.get(0)).toString();
        for (int i=1; i<length; i++)
            dn += ", " + ((RDN)rdnList.get(i)).toString();
        return dn;
    }


    /**
     * Compares this DN to the specified DN to determine if they are equal.
     *
     * @param   toDN the DN to compare to
     * @return  <code>true</code> if the DNs are equal; otherwise
     *          <code>false</code>
     */
    public boolean equals( DN toDN ){
        int length = toDN.rdnList.size();

        if( this.rdnList.size() != length)
            return false;

        for(int i=0; i<length; i++){
            if (!((RDN)rdnList.get(i)).equals( (RDN)toDN.rdnList.get(i) ))
                return false;
        }
        return true;
   }

    /**
     * return a string array of the individual RDNs contained in the DN
     *
     * @param noTypes   If true, returns only the values of the
     *                  components, and not the names, e.g. "Babs
     *                  Jensen", "Accounting", "Acme", "us" - instead of
     *                  "cn=Babs Jensen", "ou=Accounting", "o=Acme", and
     *                  "c=us".
     * @return  <code>String[]</code> containing the rdns in the DN with
     *                 the leftmost rdn in the first element of the array
     *
     */

    public String[] explode(boolean  noTypes) {
        int length = rdnList.size();
        String[] rdns = new String[length];
        for(int i=0; i<length; i++)
            rdns[i]=((RDN)rdnList.get(i)).toString(noTypes);
        return rdns;
    }

} //end class DN