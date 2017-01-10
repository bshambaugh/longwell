<?xml version="1.0" encoding="utf-8"?>

<!--+
    |
    | XSLT to transform the XML result of a Fresnel lens into an HTML representation   
    |
    +-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:f="http://www.w3.org/2004/09/fresnel-tree"
  xmlns="http://www.w3.org/1999/xhtml"
  exclude-result-prefixes="f"
  version="1.0">

 <xsl:output method="html" encoding="utf-8" indent="yes"/>

 <xsl:template name="titler">
  <xsl:param name="base"/>
  <span style="display: none;" id="{concat('fresnel_title_',$base/@uri)}">
    <xsl:value-of select="$base/f:title"/>
  </span>
  <xsl:if test="//f:link">
   <span style="display: none;" id="{concat('fresnel_styles_',$base/@uri)}">
    <xsl:for-each select="//f:link">
     <span><xsl:value-of select="."/></span>
    </xsl:for-each>
   </span>
  </xsl:if>
 </xsl:template>

 <xsl:template match="/">
  <xsl:apply-templates/>
 </xsl:template>

 <xsl:template match="f:results">
   <div class="fresnel-results">
    <xsl:call-template name="titler">
     <xsl:with-param name="base" select="f:resource[position()=1]"/>
    </xsl:call-template>
    <xsl:apply-templates select="f:resource"/>
   </div>
 </xsl:template>

 <xsl:template match="f:resource">
  <div>
   <xsl:choose>
    <xsl:when test="@class">
     <xsl:attribute name="class">
       <xsl:value-of select="concat('f-resource ',@class)"/>
     </xsl:attribute>
    </xsl:when>
    <xsl:otherwise>
     <xsl:attribute name="class">
       <xsl:value-of select="'f-resource'"/>
     </xsl:attribute>
    </xsl:otherwise>
   </xsl:choose>
   <xsl:variable name="encoded-uri">
    <xsl:call-template name="url-encode">
     <xsl:with-param name="str" select="@uri"/>
    </xsl:call-template>
   </xsl:variable>
   <xsl:if test="f:content/f:before">
    <xsl:value-of select="f:content/f:before"/>
   </xsl:if>
   <div class="f-title">
    <a href="{concat('default?command=focus&amp;objectURI=',$encoded-uri)}" class="focus" title="Focus on this resource"><xsl:value-of select="f:title"/></a>
   </div>
   <xsl:apply-templates select="f:property"/>
   <xsl:if test="f:content/f:after">
    <xsl:value-of select="f:content/f:after"/>
   </xsl:if>
  </div>
 </xsl:template>

 <xsl:template match="f:property">
  <div>
   <xsl:choose>
    <xsl:when test="@class">
     <xsl:attribute name="class">
       <xsl:value-of select="concat('f-property ',@class)"/>
     </xsl:attribute>
    </xsl:when>
    <xsl:otherwise>
     <xsl:attribute name="class">
       <xsl:value-of select="'f-property'"/>
     </xsl:attribute>
    </xsl:otherwise>
   </xsl:choose>
   <xsl:if test="f:label">
    <xsl:if test="f:content/f:before">
     <xsl:value-of select="f:content/f:before"/>
    </xsl:if>
    <div>
     <xsl:choose>
      <xsl:when test="f:label/@class">
       <xsl:attribute name="class">
        <xsl:value-of select="concat('f-label ',f:label/@class)"/>
       </xsl:attribute>
      </xsl:when>
      <xsl:otherwise>
       <xsl:attribute name="class">
        <xsl:value-of select="'f-label'"/>
       </xsl:attribute>
      </xsl:otherwise>
     </xsl:choose>
     <xsl:apply-templates select="f:label"/>
     <xsl:if test="f:content/f:after">
      <xsl:value-of select="f:content/f:after"/>
     </xsl:if>
    </div>
   </xsl:if>
   <xsl:apply-templates select="f:values"/>
  </div>
 </xsl:template>

 <xsl:template match="f:label">
  <xsl:if test="f:content/f:before">
    <xsl:value-of select="f:content/f:before"/>
  </xsl:if>
  <xsl:value-of select="f:title"/>
  <xsl:if test="f:content/f:before">
    <xsl:value-of select="f:content/f:after"/>
  </xsl:if>
 </xsl:template>

 <xsl:template match="f:values">
  <xsl:choose>
    <xsl:when test="f:content/f:first">
      <xsl:value-of select="f:content/f:first"/>
    </xsl:when>
    <xsl:when test="f:content/f:before">
      <xsl:value-of select="f:content/f:before"/>
    </xsl:when>
  </xsl:choose>
  <xsl:apply-templates select="f:value">
   <xsl:with-param name="before" select="f:content/f:before"/>
   <xsl:with-param name="after" select="f:content/f:after"/>
  </xsl:apply-templates>
  <xsl:choose>
    <xsl:when test="f:content/f:last">
      <xsl:value-of select="f:content/f:last"/>
    </xsl:when>
    <xsl:when test="f:content/f:after">
      <xsl:value-of select="f:content/f:after"/>
    </xsl:when>
  </xsl:choose>
 </xsl:template>

 <xsl:template match="f:value">
  <xsl:param name="before"/>
  <xsl:param name="after"/>
  <div>
   <xsl:choose>
    <xsl:when test="@class">
     <xsl:attribute name="class">
       <xsl:value-of select="concat('f-value ',@class)"/>
     </xsl:attribute>
    </xsl:when>
    <xsl:otherwise>
     <xsl:attribute name="class">
       <xsl:value-of select="'f-value'"/>
     </xsl:attribute>
    </xsl:otherwise>
   </xsl:choose>
   <xsl:if test="position()&gt;1">
    <xsl:value-of select="$before"/>
   </xsl:if>
   <xsl:choose>
    <xsl:when test="@output-type='image'">
     <div>
      <xsl:choose>
       <xsl:when test="f:resource/@class">
        <xsl:attribute name="class">
         <xsl:value-of select="concat('f-resource ',f:resource/@class)"/>
        </xsl:attribute>
       </xsl:when>
       <xsl:otherwise>
        <xsl:attribute name="class">
         <xsl:value-of select="'f-resource'"/>
        </xsl:attribute>
       </xsl:otherwise>
      </xsl:choose>
      <xsl:choose>
       <xsl:when test="f:resource">
        <img src="{f:resource/@uri}" alt="{f:resource/f:title}" />
       </xsl:when>
       <xsl:otherwise>
        <img src="{f:title}" />
       </xsl:otherwise>
      </xsl:choose>
     </div>
    </xsl:when>
    <xsl:when test="@output-type='uri'">
     <div>
      <xsl:choose>
       <xsl:when test="f:resource/@class">
        <xsl:attribute name="class">
         <xsl:value-of select="concat('f-resource ',f:resource/@class)"/>
        </xsl:attribute>
       </xsl:when>
       <xsl:otherwise>
        <xsl:attribute name="class">
         <xsl:value-of select="'f-resource'"/>
        </xsl:attribute>
       </xsl:otherwise>
      </xsl:choose>
      <div class="f-title">
       <xsl:choose>
        <xsl:when test="f:resource">
         <xsl:value-of select="f:resource/@uri"/>
        </xsl:when>
        <xsl:otherwise>
         <xsl:value-of select="f:title" />
        </xsl:otherwise>
       </xsl:choose>
      </div>
     </div>
    </xsl:when>
    <xsl:when test="@output-type='link'">
     <div>
      <xsl:choose>
       <xsl:when test="f:resource/@class">
        <xsl:attribute name="class">
         <xsl:value-of select="concat('f-resource ',f:resource/@class)"/>
        </xsl:attribute>
       </xsl:when>
       <xsl:otherwise>
        <xsl:attribute name="class">
         <xsl:value-of select="'f-resource'"/>
        </xsl:attribute>
       </xsl:otherwise>
      </xsl:choose>
      <div class="f-title">
       <xsl:choose>
        <xsl:when test="f:resource">
         <a href="{f:resource/@uri}" title="{f:resource/f:title}">[external link]</a>
        </xsl:when>
        <xsl:otherwise>
         <a href="{f:title}">[external link]</a>
        </xsl:otherwise>
       </xsl:choose>
      </div>
     </div>
    </xsl:when>
    <xsl:otherwise>
     <xsl:choose>
      <xsl:when test="f:title">
       <div class="f-title">
        <xsl:value-of select="f:title"/>
       </div>
      </xsl:when>
      <xsl:otherwise>
       <xsl:apply-templates select="f:resource"/>
      </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
   </xsl:choose>
   <xsl:if test="position()!=last()">
    <xsl:value-of select="$after"/>
   </xsl:if>
  </div>
 </xsl:template>

  <!--+
      | ISO-8859-1 based URL-encoding
      | Written by Mike J. Brown, mike@skew.org.
      | Updated 2002-05-20.
      |
      | No license; use freely, but credit me if reproducing in print.
      |
      | Also see http://skew.org/xml/misc/URI-i18n/ for a discussion of
      | non-ASCII characters in URIs.
      +-->

  <!--+
      | Characters we'll support.
      | We could add control chars 0-31 and 127-159, but we won't. 
      +-->
  <xsl:variable name="ascii"> !"#$%&amp;'()*+,-./0123456789:;&lt;=&gt;?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz{|}~</xsl:variable>
  <xsl:variable name="latin1">&#160;&#161;&#162;&#163;&#164;&#165;&#166;&#167;&#168;&#169;&#170;&#171;&#172;&#173;&#174;&#175;&#176;&#177;&#178;&#179;&#180;&#181;&#182;&#183;&#184;&#185;&#186;&#187;&#188;&#189;&#190;&#191;&#192;&#193;&#194;&#195;&#196;&#197;&#198;&#199;&#200;&#201;&#202;&#203;&#204;&#205;&#206;&#207;&#208;&#209;&#210;&#211;&#212;&#213;&#214;&#215;&#216;&#217;&#218;&#219;&#220;&#221;&#222;&#223;&#224;&#225;&#226;&#227;&#228;&#229;&#230;&#231;&#232;&#233;&#234;&#235;&#236;&#237;&#238;&#239;&#240;&#241;&#242;&#243;&#244;&#245;&#246;&#247;&#248;&#249;&#250;&#251;&#252;&#253;&#254;&#255;</xsl:variable>

  <!--+
      | Characters that usually don't need to be escaped 
      +-->
  <xsl:variable name="safe">!'()*-.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz~</xsl:variable>

  <xsl:variable name="hex" >0123456789ABCDEF</xsl:variable>

  <xsl:template name="url-encode">
    <xsl:param name="str"/>   
    <xsl:if test="$str">
      <xsl:variable name="first-char" select="substring($str,1,1)"/>
      <xsl:choose>
        <xsl:when test="contains($safe,$first-char)">
          <xsl:value-of select="$first-char"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="codepoint">
            <xsl:choose>
              <xsl:when test="contains($ascii,$first-char)">
                <xsl:value-of select="string-length(substring-before($ascii,$first-char)) + 32"/>
              </xsl:when>
              <xsl:when test="contains($latin1,$first-char)">
                <xsl:value-of select="string-length(substring-before($latin1,$first-char)) + 160"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:message terminate="no">Warning: string contains a character that is out of range! Substituting "?".</xsl:message>
                <xsl:text>63</xsl:text>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
        <xsl:variable name="hex-digit1" select="substring($hex,floor($codepoint div 16) + 1,1)"/>
        <xsl:variable name="hex-digit2" select="substring($hex,$codepoint mod 16 + 1,1)"/>
        <xsl:value-of select="concat('%',$hex-digit1,$hex-digit2)"/>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:if test="string-length($str) &gt; 1">
        <xsl:call-template name="url-encode">
          <xsl:with-param name="str" select="substring($str,2)"/>
        </xsl:call-template>
      </xsl:if>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
