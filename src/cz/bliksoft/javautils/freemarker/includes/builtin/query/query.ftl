<#assign dateTimeFormatString="yyyy-MM-dd HH:mm:ss" />
<#include "inc/macros.ftl">

<@htmlquery_flex>
<#assign maxrows = GUIPrompt('Maximální počet řádků:','Dotaz do ' + environment,'10')!'cancel'>
<#assign result=query('QUERY.ftl', maxrows)>
</@htmlquery_flex>
