/**
 * 
 */
package org.apidb.apicommon.model;

/**
 * @author xingao
 * 
 */
public class ExternalDatabase {

    private String externalDbName;
    private String externalDbVersion;

    /**
     * @return Returns the externalDbName.
     */
    public String getExternalDbName() {
        return externalDbName;
    }

    /**
     * @param externalDbName
     *            The externalDbName to set.
     */
    public void setExternalDbName(String externalDbName) {
        this.externalDbName = externalDbName;
    }

    /**
     * @return Returns the externalDbVersion.
     */
    public String getExternalDbVersion() {
        return externalDbVersion;
    }

    /**
     * @param externalDbVersion
     *            The externalDbVersion to set.
     */
    public void setExternalDbVersion(String externalDbVersion) {
        this.externalDbVersion = externalDbVersion;
    }
}
