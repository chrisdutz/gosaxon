<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" indent="yes"/>

    <xsl:template match="/fruits">
        <apples-and-friends>
            <xsl:for-each select="*">
                <xsl:choose>
                    <xsl:when test="name() = 'pair'">
                        <apple-like-fruit/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:copy-of select="."/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
        </apples-and-friends>
    </xsl:template>

</xsl:stylesheet>