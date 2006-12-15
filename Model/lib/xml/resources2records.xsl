<?xml version="1.0" ?>
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">

<xsl:output method="xml"/> 

<xsl:template name="record">
	<xsl:param name="category"/>
    <record>
    <xsl:attribute name="id">
    <xsl:value-of select="@resource"/>
    </xsl:attribute>

    <xsl:element name="attribute">
    <xsl:attribute name="name">resource</xsl:attribute>
    <xsl:value-of select="@displayName"/>
    </xsl:element>

    <xsl:element name="attribute">
    <xsl:attribute name="name">version</xsl:attribute>
    <xsl:value-of select="@version"/>
    </xsl:element>

    <xsl:element name="attribute">
    <xsl:attribute name="name">publicUrl</xsl:attribute>
    <xsl:value-of select="@publicUrl"/>
    </xsl:element>

    <xsl:element name="attribute">
    <xsl:attribute name="name">organisms</xsl:attribute>
    <xsl:value-of select="@organisms"/>
    </xsl:element>

    <xsl:element name="attribute">
    <xsl:attribute name="name">category</xsl:attribute>
    <xsl:value-of select="$category"/>
    </xsl:element>

    <xsl:if test="count(description)=0">
        <attribute name="description"></attribute>
    </xsl:if>
    <xsl:for-each select="description">
        <xsl:element name="attribute">
        <xsl:attribute name="name">description</xsl:attribute>
        <xsl:value-of select="text()"/>
        </xsl:element>

        </xsl:for-each>
            <xsl:element name="table">
            <xsl:attribute name="name">publications</xsl:attribute>

            <xsl:for-each select="publication">
              <xsl:element name="row">
              <xsl:element name="attribute">
                  <xsl:attribute name="name">pmid</xsl:attribute>
                  <xsl:value-of select="@pmid"/>
              </xsl:element>

              <xsl:if test="@pmdetails">
              <xsl:element name="attribute">
                <xsl:attribute name="name">pmdetails</xsl:attribute>
                <xsl:value-of select="@pmdetails"/>
              </xsl:element>
              </xsl:if>

              <xsl:if test="@pmauthors">
              <xsl:element name="attribute">
                <xsl:attribute name="name">pmauthors</xsl:attribute>
                <xsl:value-of select="@pmauthors"/>
              </xsl:element>
              </xsl:if>

              <xsl:if test="@pmtitle">
              <xsl:element name="attribute">
                <xsl:attribute name="name">pmtitle</xsl:attribute>
                <xsl:value-of select="@pmtitle"/>
              </xsl:element>
              </xsl:if>

            </xsl:element>
          </xsl:for-each>
    </xsl:element>
    </record>
</xsl:template>

<xsl:template match="/">
<xmlAnswer>
	<xsl:choose>
	<xsl:when test="resourcesPipeline/categoryOrder/categoryRef">
    <xsl:for-each select="resourcesPipeline/categoryOrder/categoryRef">
        <xsl:variable name="cat" select="@name"/>
        <xsl:for-each select="/resourcesPipeline/resource[@category=$cat]">
        <xsl:call-template name="record">
        	<xsl:with-param name="category"><xsl:value-of select="$cat"/></xsl:with-param>
        </xsl:call-template>
        </xsl:for-each>
    </xsl:for-each>
    </xsl:when>
    
    <xsl:otherwise>
    <!-- When categoryOrder is not specified in the resources.xml file -->
    <xsl:for-each select="/resourcesPipeline/resource[@category!='']">
    <xsl:sort select="@category"/>
	<xsl:call-template name="record">
        	<xsl:with-param name="category"><xsl:value-of select="@category"/></xsl:with-param>
        </xsl:call-template>
    </xsl:for-each>
    </xsl:otherwise>
    </xsl:choose>
    
	<xsl:for-each select="resourcesPipeline/resource[@category='']">
        <xsl:call-template name="record">
        	<xsl:with-param name="category">Miscellaneous</xsl:with-param>
        </xsl:call-template>
    </xsl:for-each>
</xmlAnswer>
</xsl:template>
</xsl:stylesheet>
