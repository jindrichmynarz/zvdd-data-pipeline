<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:functx="http://www.functx.com/"
    xmlns:ddb="http://www.deutsche-digitale-bibliothek.de/edm/"
    xmlns:edm="http://www.europeana.eu/schemas/edm/"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:wgs84_pos="http://www.w3.org/2003/01/geo/wgs84_pos#"
    exclude-result-prefixes="functx xs"
    xpath-default-namespace="http://www.deutsche-digitale-bibliothek.de/cortex"
    version="2.0">
    
    <xsl:function name="functx:is-absolute-uri" as="xs:boolean">
        <xsl:param name="uri" as="xs:string?"/>
        <xsl:sequence select="matches($uri,'^[a-z]+:')"/>
    </xsl:function>
    
    <!-- Taken from <http://stackoverflow.com/a/1571108/385505> -->
    
    <!-- identity template -->
    <xsl:template match="node() | @*">
        <xsl:copy>
            <xsl:apply-templates select="node() | @*" />
        </xsl:copy>
    </xsl:template>
    
    <!-- Convert relative URIs to blank nodes. -->
    <xsl:template match="@rdf:about | @rdf:resource">
        <xsl:choose>
            <xsl:when test="not(functx:is-absolute-uri(.))">
                <xsl:attribute name="rdf:nodeID">
                    <!-- Make sure the BNode ID doesn't start with a number. -->
                    <xsl:value-of select="concat('b', .)"/>
                </xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
                <xsl:attribute name="{name()}" namespace="{namespace-uri()}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- Add explicit datatypes -->
    
    <xsl:template match="wgs84_pos:lat | wgs84_pos:long">
       <xsl:element name="{name()}" namespace="{namespace-uri()}">
           <xsl:attribute name="rdf:datatype">http://www.w3.org/2001/XMLSchema#double</xsl:attribute>
           <xsl:apply-templates/>
       </xsl:element>
    </xsl:template>
    
    <xsl:template match="ddb:aggregationEntity">
       <xsl:element name="{name()}" namespace="{namespace-uri()}">
           <xsl:attribute name="rdf:datatype">http://www.w3.org/2001/XMLSchema#boolean</xsl:attribute>
           <xsl:apply-templates/>
       </xsl:element> 
    </xsl:template>
    
    <!-- template for the document element -->
    <xsl:template match="/*">
        <xsl:apply-templates select="node()" />
    </xsl:template>
    
</xsl:stylesheet>