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

<#macro box_footer>
<div class="row footer">
<#nested>
</div>
</#macro>

<#macro utf8>
<meta charset="UTF-8">
</#macro>

<#macro jquery><script src="https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js"></script></#macro>
<#macro formlink><script type="text/javascript"><#include 'formlink.js' parse=false></script></#macro>
<#macro ajaxsubmit><script type="text/javascript"><#include 'ajaxsubmit.js' parse=false></script></#macro>
<#macro expander><script type="text/javascript"><#include 'expander.js' parse=false></script></#macro>
<#macro highlighter><script type="text/javascript"><#include 'highlighter.js' parse=false></script></#macro>

<#macro css_styles><style><#include 'styles.css' parse=false></style></#macro>
<#macro css_fullframe><style>
html,
body {
  height: 100%;
  margin: 0;
}
</style></#macro>
