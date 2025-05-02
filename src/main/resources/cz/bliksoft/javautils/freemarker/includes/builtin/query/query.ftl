<#assign dateTimeFormatString="yyyy-MM-dd HH:mm:ss" />
<#assign timestampFormatString="yyyy-MM-dd HH:mm:ss.SSS" />

<#macro query_info lastQuery>
${TXTTOHTML_WHITESPACE(lastQuery.SQL)}<br/>
<#list lastQuery.parameters>
<#items as par>
${lastQuery.parameterTypes[par?index]}[${(par?index)?c}]=${TXTTOHTML_WHITESPACE(par)}<br/>
</#items>
<#else>
No parameters.<br/>
</#list>
Result count: ${lastQuery.resultCount}
</#macro>

<#macro result_table lastQuery result>
<#list result>
<table>
<thead>
<tr>
	<#list lastQuery.columnTypes as col><th class="right">${col!'-'}</th></#list>
</tr>
<tr>
	<#list lastQuery.columns as col><th class="right">${col!'-'}</th></#list>
</tr>
</thead>
<tbody>
<#items as line>
<tr>
	<#list lastQuery.columns as col><#if line[col]??>
	<#switch lastQuery.columnTypes[col?index]>
		<#case 'DATE'>
			<td class="right">${TXTTOHTML_WHITESPACE((line[col]).format(dateTimeFormatString)!)}
		<#break>
		<#case 'TIMESTAMP'>
		<#case 'TIMESTAMP_WITH_TIMEZONE'>
			<td class="right">${TXTTOHTML_WHITESPACE((line[col]).format(timestampFormatString)!)}
		<#break>
		<#case 'LONG'>
		<#case 'INTEGER'>
		<#case 'DECIMAL'>
		<#case 'DOUBLE'>
			<td class="right">${TXTTOHTML_WHITESPACE((line[col])?c)}
		<#break>
		<#default>
			<td>${TXTTOHTML_WHITESPACE(line[col])}
	</#switch>
	</td>
	<#else>
	<td class="gray_background right"> </td>
	</#if>
	</#list>
</tr>
</#items>
</tbody>
</table>
<#else>
Empty result.
</#list>
</#macro>

<#macro htmlquery_flex>
<@html.document>
<@html.head>
<@html.css_fullframe />
<@html.css_styles />
</@html.head>
<@html.body>
<@html.box>
<@html.box_header>
<@query_info .main.lastQuery />
</@html.box_header>
<@html.box_content>
<@result_table .main.lastQuery, .main.result />
</@html.box_content>
</@html.box>
</@html.body>
</@html.document>
</#macro>

<#macro htmlquery>
<@html.document>
<@html.header>
<@html.css_fullframe />
<@html.css_styles />
</@html.header>
<@html.body>
<#nested>
<@query_info />
<hr>
<@result_table />
</@html.body>
</@html.document>
</#macro>