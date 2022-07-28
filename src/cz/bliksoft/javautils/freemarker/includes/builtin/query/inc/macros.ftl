<#macro htmlquery_flex>
	<html>
	<head>
	<style>
	<#include "../../styles.css">
	<#include "../../flexbox.css">
	</style>
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
	<#include "../../styles.css">
	</style>
	</head>
	<body>
	
	<#nested>
	<#include 'queryinfo.ftl'>
	<hr>
	<#include 'resulttable.ftl'>
	</body>
	</html>
</#macro>