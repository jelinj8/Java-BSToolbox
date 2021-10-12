<#list result>
<table>
<thead>
<tr>
	<#list LastQuery['columnTypes'] as col><th class="right">${col!'-'}</th></#list>
</tr>
<tr>
	<#list LastQuery['columns'] as col><th class="right">${col!'-'}</th></#list>
</tr>
</thead>
<tbody>
<#items as line>
<tr>
	<#list LastQuery['columns'] as col><#if line[col]??>
	<#switch LastQuery['columnTypes'][col?index]>
		<#case 'DATE'>
		<#case 'TIMESTAMP'>
			<td class="right">${TXTTOHTML_WHITESPACE((line[col])?string[dateTimeFormatString])}
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
