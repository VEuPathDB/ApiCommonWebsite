<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml"/>
  <xsl:param name="displayName"></xsl:param>
  <xsl:param name="channelTitle"></xsl:param>
  <xsl:param name="linkTmpl"></xsl:param>
  <xsl:template match="/xmlAnswer">
    <rss version="0.91">
      <channel>
        <title><xsl:value-of select="$channelTitle"/></title>
        <link><xsl:value-of select="$linkTmpl"/></link>
        <description>News from <xsl:value-of select="$displayName"/></description>
        <language>en</language>
        <xsl:for-each select="record">
            <item>
              <title>
                <xsl:value-of select="attribute[@name='headline']"/>
              </title>
              <link><xsl:value-of select="$linkTmpl"/><xsl:value-of select="attribute[@name='tag']"/></link>
              <description>
                <xsl:value-of select="attribute[@name='item']"
                  disable-output-escaping="no"/>
                  &lt;br /&gt;
                <xsl:value-of select="attribute[@name='date']"/>
              </description>
            </item>
        </xsl:for-each>
      </channel>
    </rss>
  </xsl:template>
</xsl:stylesheet>
