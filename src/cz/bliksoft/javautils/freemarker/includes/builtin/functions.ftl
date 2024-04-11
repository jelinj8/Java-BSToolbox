<#function min(a, b)>
  <#return (a < b)?then(a, b)>
</#function>
<#function max(a, b)>
  <#return (a > b)?then(a, b)>
</#function>
<#function duration(ms)>
  <#assign sec=(ms/1000)?abs>
  <#assign h=(sec/3600)?floor>
  <#assign m=((sec%3600)/60)?floor>
  <#assign s=sec-((sec/216000)?floor)>
  <#return h?c+':'+m?string('00')+':'+s?string('00.000')+' ('+ms?c+'ms)'>
</#function>
