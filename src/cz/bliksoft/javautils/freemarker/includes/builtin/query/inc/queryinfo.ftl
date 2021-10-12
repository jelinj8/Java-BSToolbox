${TXTTOHTML_WHITESPACE(LastQuery['SQL'])}<br/>
<#list LastQuery['parameters']>
<#items as par>
?[${(par?index)?c}]=${TXTTOHTML_WHITESPACE(par)}<br/>
</#items>
<#else>
No parameters.
</#list>