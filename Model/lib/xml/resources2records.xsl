<?xml version="1.0" ?>
<xsl:stylesheet 
      xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
      version="1.0">
  <xsl:output method="xml" /> 
  <xsl:template match="/">
    <xmlAnswer>
      <xsl:for-each select="resourcesPipeline/resource">
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
            <xsl:attribute name="name">category</xsl:attribute>
            <xsl:value-of select="@category"/>
          </xsl:element>
          <xsl:if test="count(description)=0">
             <attribute name="description"> </attribute>
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
              </xsl:element>
            </xsl:for-each>

          </xsl:element>
 
        </record>
      </xsl:for-each>
    </xmlAnswer>
  </xsl:template>
</xsl:stylesheet>
