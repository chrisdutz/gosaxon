<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="/">
        <output>
            <xsl:apply-templates select="*"/>
        </output>
    </xsl:template>

    <xsl:template match="node() | @*">
        <xsl:copy-of select="."/>
    </xsl:template>

</xsl:stylesheet>