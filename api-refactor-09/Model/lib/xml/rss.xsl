<?xml version="1.0" ?>
<xsl:stylesheet 
      xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
      version="1.0">
  <xsl:output method="xml" /> 
  <xsl:template match="/rss">
    <xmlAnswer>
      <xsl:for-each select="channel/item">
        <record>
          <xsl:element name="attribute">
            <xsl:attribute name="attributeFieldRef">title</xsl:attribute>
            <xsl:attribute name="value">
              <xsl:value-of select="title"/>
            </xsl:attribute>
          </xsl:element>
          <xsl:element name="attribute">
            <xsl:attribute name="attributeFieldRef">link</xsl:attribute>
            <xsl:attribute name="value">
              <xsl:value-of select="link"/>
            </xsl:attribute>
          </xsl:element>
          <xsl:element name="attribute">
            <xsl:attribute name="attributeFieldRef">description</xsl:attribute>
            <xsl:attribute name="value">
              <xsl:value-of select="description"/>
            </xsl:attribute>
          </xsl:element>
          <xsl:element name="attribute">
            <xsl:attribute name="attributeFieldRef">date</xsl:attribute>
            <xsl:attribute name="value">
              <xsl:value-of select="pubDate"/>
            </xsl:attribute>
          </xsl:element>
        </record>
      </xsl:for-each>
    </xmlAnswer>
  </xsl:template>
</xsl:stylesheet>