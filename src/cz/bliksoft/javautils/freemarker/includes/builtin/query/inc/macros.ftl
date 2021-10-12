<#macro htmlquery_flex>
	<html>
	<head>
	<style>
	<#include "styles.css">
	<#include "flexbox.css">
	</style>
	<#if tray_port??>
	<#include "javascript.htm">
	</#if>
	</head>
	<body>
	
	<#nested>
	
	<div class="box">
		<div class="row header">
		<#include 'queryinfo.ftl'>
		</div>
		<div class="row content tableFixHead">
		<#include 'resulttable.ftl'>
		</div>
	</div>
	</body>
	</html>
</#macro>

<#macro htmlquery>
	<html>
	<head>
	<style>
	<#include "styles.css">
	</style>
	<#if tray_port??>
	<#include "javascript.htm">
	</#if>
	</head>
	<body>
	
	<#nested>
	<#include 'queryinfo.ftl'>
	<hr>
	<#include 'resulttable.ftl'>
	</body>
	</html>
</#macro>