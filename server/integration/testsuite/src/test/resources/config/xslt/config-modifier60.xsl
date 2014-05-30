<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:endpoint="urn:infinispan:server:endpoint:6.0">
    <xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>

    <!-- Parameter declarations with defaults set -->
    <xsl:param name="removeRestSecurity60">true</xsl:param>

    <xsl:template match="node()|@*" name="copynode">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="endpoint:subsystem/endpoint:rest-connector">
        <xsl:if test="$removeRestSecurity60 != 'true'">
            <xsl:call-template name="copynode"/>
        </xsl:if>
        <xsl:if test="$removeRestSecurity60 = 'true'">
            <xsl:copy>
                <xsl:copy-of select="@*[not(name() = 'security-domain' or name() = 'auth-method')]"/>
                <xsl:apply-templates/>
            </xsl:copy>
        </xsl:if>
    </xsl:template>

    <!-- matches on the remaining tags and recursively applies templates to their children and copies them to the result -->
    <xsl:template match="*">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
