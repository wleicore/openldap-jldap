/* **************************************************************************
 * $Novell: /ldap/src/jldap/com/novell/ldap/LDAPObjectClassSchema.java,v 1.21 2001/04/23 21:09:31 cmorris Exp $
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

package org.ietf.ldap;

/**
 *  Represents the schematic definition of a particular object class in
 *  a particular directory server.
 *
 * @see com.novell.ldap.LDAPObjectClassSchema
 */
public class LDAPObjectClassSchema extends LDAPSchemaElement
{
    private com.novell.ldap.LDAPObjectClassSchema schema;

    /**
     * This class definition defines an abstract schema class.
     *
     * @see com.novell.ldap.LDAPObjectClassSchema#ABSTRACT
     */
     public final static int ABSTRACT =
                com.novell.ldap.LDAPObjectClassSchema.ABSTRACT;
   
    /**
     * This class definition defines a structural schema class.
     *
     * @see com.novell.ldap.LDAPObjectClassSchema#STRUCTURAL
     */
     public final static int STRUCTURAL =
                com.novell.ldap.LDAPObjectClassSchema.STRUCTURAL;
   
    /**
     * This class definition defines an auxiliary schema class.
     *
     * @see com.novell.ldap.LDAPObjectClassSchema#AUXILIARY
     */
     public final static int AUXILIARY =
                com.novell.ldap.LDAPObjectClassSchema.AUXILIARY;
   
    /**
     * Constructs an object class definition for adding to or deleting from
     * a directory's schema.
     *
     * @see com.novell.ldap.LDAPObjectClassSchema#LDAPObjectClassSchema(
          String,String,String[],String,String[],String[],int,String[],boolean)
     */
    public LDAPObjectClassSchema(String name,
                                 String oid,
                                 String[] superiors,
                                 String description,
                                 String[] required,
                                 String[] optional,
                                 int type,
                                 String[] aliases,
                                 boolean obsolete)
    {
        super( new com.novell.ldap.LDAPObjectClassSchema( name,
                                                          oid,
                                                          superiors,
                                                          description,
                                                          required,
                                                          optional,
                                                          type,
                                                          aliases,
                                                          obsolete));
        schema = (com.novell.ldap.LDAPObjectClassSchema)getWrappedObject();
        return;
    }

    /**
     * Constructs an object class definition from the raw string value
     * returned from a directory query for "objectClasses".
     *
     * @see com.novell.ldap.LDAPObjectClassSchema#LDAPObjectClassSchema(String)
     */
    public LDAPObjectClassSchema(String raw)
    {
        super( new com.novell.ldap.LDAPObjectClassSchema( raw));
        schema = (com.novell.ldap.LDAPObjectClassSchema)getWrappedObject();
        return;
    }

    /**
     * Constructs from com.novell.ldap.LDAPObjectClassSchema
     */
    /* package */
    LDAPObjectClassSchema( com.novell.ldap.LDAPObjectClassSchema schema)
    {
        super(schema);
        this.schema = schema;
        return;
    }

    /**
     * Returns the object classes from which this one derives.
     *
     * @see com.novell.ldap.LDAPObjectClassSchema#getSuperiors()
     */
    public String[] getSuperiors()
    {
        return schema.getSuperiors();
    }

    /**
     * Returns a list of attributes required for an entry with this object
     * class.
     *
     * @see com.novell.ldap.LDAPObjectClassSchema#getRequiredAttributes()
     */
    public String[] getRequiredAttributes()
    {
        return schema.getRequiredAttributes();
    }

    /**
     * Returns a list of optional attributes but not required of an entry
     * with this object class.
     *
     * @see com.novell.ldap.LDAPObjectClassSchema#getOptionalAttributes()
     */
    public String[] getOptionalAttributes()
    {
        return schema.getOptionalAttributes();
    }

    /**
     * Returns the type of object class.
     *
     * @see com.novell.ldap.LDAPObjectClassSchema#getType()
     */
    public int getType() {
        return schema.getType();
    }
}
