<#macro document>
<!DOCTYPE html>
<html>
<#nested>
</html>
</#macro>

<#macro head>
<head>
<#nested>
</head>
</#macro>

<#macro title><title><#nested></title></#macro>

<#macro body class=''>
<body <#if class??>class="${class}"</#if>>
<#nested>
</body>
</#macro>

<#macro box class=''>
<div class="<#if class??>${class} </#if>box">
<#nested>
</div>
</#macro>

<#macro box_header class=''>
<div class="<#if class??>${class} </#if>row header">
<#nested>
</div>
</#macro>

<#macro box_content class=''>
<div class="<#if class??>${class} </#if>row content">
<#nested>
</div>
</#macro>

<#macro box_footer class=''>
<div class="<#if class??>${class} </#if>row footer">
<#nested>
</div>
</#macro>

<#macro inlineform action method='post' target='' class=''><form action="${action}" method="${method}"<#if target?has_content> target="${target}"</#if><#if class?has_content> class="${class}"</#if> style=" display:inline!important;"><#nested></form></#macro>
<#macro form       action method='post' target='' class=''><form action="${action}" method="${method}"<#if target?has_content> target="${target}"</#if><#if class?has_content> class="${class}"</#if>><#nested></form></#macro>
<#macro hiddenvalue name value><input type="hidden" name="${name}" value="${value}" /></#macro>
<#macro button text name='submit' value='submit' class='' title=''><button name="${name}" value="${value}" type="submit"<#if class?has_content> class="${class}"</#if><#if title?has_content> title="${title}"</#if>>${text}</button></#macro>

<#macro utf8>
<meta charset="UTF-8">
</#macro>

<#macro jquery><script src="https://ajax.googleapis.com/ajax/libs/jquery/3.6.1/jquery.min.js"></script></#macro>
<#macro jquery_inline><script type="text/javascript"><#include 'jquery.js' parse=false></script></#macro>
<#macro formlink><script type="text/javascript"><#include 'formlink.js' parse=false></script></#macro>
<#macro ajaxsubmit><script type="text/javascript"><#include 'ajaxsubmit.js' parse=false></script></#macro>
<#macro expander><script type="text/javascript"><#include 'expander.js' parse=false></script></#macro>
<#macro highlighter><script type="text/javascript"><#include 'highlighter.js' parse=false></script></#macro>

<#macro syntaxhighlighter theme='default.min'>
<style><#include 'syntaxhighlighter/css/'+theme+'.css' parse=false></style>
<script type="text/javascript"><#include 'syntaxhighlighter/highlight.min.js' parse=false></script>
<script>hljs.highlightAll();</script>
</#macro>

<#macro code syntax='default'>
<pre class="pre-wrap"><code<#if syntax!='default'> class="${syntax}"</#if>><#nested></code></pre>
</#macro>

<#macro css_styles>
<style><#include 'styles.css' parse=false></style>
</#macro>

<#macro css_fullframe>
<style>
html,
body {
  height: 100%;
  margin: 0;
}
</style>
</#macro>

<#macro imgdatafile file format='png'>data:image/${format};base64,${Base64File(file)}</#macro>
<#macro imgdata base64data format='png'>data:image/${format};base64,${base64data}</#macro>
<#macro qr data id='' alt='' class='' modulus=1><img src="<@imgsrcdata Base64QR(data, modulus) />"<#if id?has_content> id="${id}"</#if><#if class?has_content> class="${class}"</#if><#if alt?has_content> alt="${alt}"</#if></#macro>

<#macro additionalHead><#assign additionalHeadContent><#nested></#assign></#macro>
<#macro documentHeader class=''><#assign definedHeaderClass=class /><#assign definedHeader><#nested></#assign></#macro>
<#macro documentFooter class=''><#assign definedFooterClass=class /><#assign definedFooter><#nested></#assign></#macro>

<#macro simpleDocument class=''>
<#import "builtin/debug.ftl" as debug>
<@document>
<@head>
<script><#include 'jquery.js' parse=false></script>
<@highlighter />
<@expander />
<@css_styles />
<@css_fullframe />
<#if additionalHeadContent??>
${additionalHeadContent?no_esc}
</#if>
</@head>
<@body class>
<#if definedHeader?? || definedFooter??>
<@html.box>
<#if definedHeader??>
<@html.box_header class=definedHeaderClass!>
${definedHeader?no_esc}
</@html.box_header>
</#if>
<@html.box_content>
<#nested>
</@html.box_content>
<#if definedFooter??>
<@html.box_footer class=definedFooterClass!>
${definedFooter?no_esc}
</@html.box_footer>
</#if>
</@html.box>
<#else>
<#nested>
</#if>
</@body>
</@document>
</#macro>