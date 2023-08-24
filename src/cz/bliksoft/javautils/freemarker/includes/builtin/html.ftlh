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

<#macro body>
<body>
<#nested>
</body>
</#macro>

<#macro box>
<div class="box">
<#nested>
</div>
</#macro>

<#macro box_header>
<div class="row header">
<#nested>
</div>
</#macro>

<#macro box_content>
<div class="row content">
<#nested>
</div>
</#macro>

<#macro inlineform action method='post' target='' class=''><form action="${action}" method="${method}"<#if target?has_content> target="${target}"</#if><#if class?has_content> class="${class}"</#if> style=" display:inline!important;"><#nested></form></#macro>
<#macro form       action method='post' target='' class=''><form action="${action}" method="${method}"<#if target?has_content> target="${target}"</#if><#if class?has_content> class="${class}"</#if>><#nested></form></#macro>
<#macro hiddenvalue name value><input type="hidden" name="${name}" value="${value}" /></#macro>
<#macro button text name='submit' value='submit' class='' title=''><button name="${name}" value="${value}" type="submit"<#if class?has_content> class="${class}"</#if><#if title?has_content> title="${title}"</#if>>${text}</button></#macro>

<#macro box_footer>
<div class="row footer">
<#nested>
</div>
</#macro>

<#macro utf8>
<meta charset="UTF-8">
</#macro>

<#macro jquery><script src="https://ajax.googleapis.com/ajax/libs/jquery/3.6.1/jquery.min.js"></script></#macro>
<#macro formlink><script type="text/javascript"><#include 'formlink.js' parse=false></script></#macro>
<#macro ajaxsubmit><script type="text/javascript"><#include 'ajaxsubmit.js' parse=false></script></#macro>
<#macro expander><script type="text/javascript"><#include 'expander.js' parse=false></script></#macro>
<#macro highlighter><script type="text/javascript"><#include 'highlighter.js' parse=false></script></#macro>

<#macro syntaxhighlighter theme='default.min'>
<style><#include 'syntaxhighlighter/css/'+theme+'.css' parse=false></style>
<script type="text/javascript"><#include 'syntaxhighlighter/highlight.min.js' parse=false></script>
<script>hljs.highlightAll();</script>
</#macro>

<#macro code syntax='default'><pre class="pre-wrap"><code<#if syntax!='default'> class="${syntax}"</#if>><#nested></code></pre></#macro>

<#macro css_styles><style><#include 'styles.css' parse=false></style></#macro>

<#macro css_fullframe><style>
html,
body {
  height: 100%;
  margin: 0;
}
</style></#macro>

<#macro imgsrcdata base64data format='png'>src="data:image/${format};base64,${base64data}"</#macro>
<#macro qr data id='' alt='' class='' modulus=1><img <@imgsrcdata Base64QR(data, modulus) /><#if id?has_content> id="${id}"</#if><#if class?has_content> class="${class}"</#if><#if alt?has_content> alt="${alt}"</#if></#macro>
