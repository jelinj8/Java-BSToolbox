<#macro html>
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

<#macro formlink><script type="text/javascript"><#include 'formlink.js'></script></#macro>
<#macro jquery><script src="https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js"></script></#macro>
<#macro ajaxsubmit><script type="text/javascript"><#include 'ajaxsubmit.js'></script></#macro>
